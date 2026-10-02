"""Yuxi-compatible OCR configuration and health APIs backed by local SQLite."""

import asyncio
import importlib
import os
from urllib.parse import urlparse

from fastapi import APIRouter, Body, HTTPException

from . import db, provider_store
from .yuxi_port import _upstream  # noqa: F401
from yuxi.knowledge.parser.capabilities import PARSER_CAPABILITIES

router = APIRouter()

SYSTEM_FIELDS = {
    "default_model": {"label": "默认对话模型", "type": "model", "default": "siliconflow-cn:deepseek-ai/DeepSeek-V4-Flash"},
    "fast_model": {"label": "快速响应模型", "type": "model", "default": "siliconflow-cn:deepseek-ai/DeepSeek-V4-Flash"},
    "embed_model": {"label": "默认 Embedding 模型", "type": "model", "default": "siliconflow-cn:Pro/BAAI/bge-m3"},
    "reranker": {"label": "默认 Re-Ranker 模型", "type": "model", "default": "siliconflow-cn:Pro/BAAI/bge-reranker-v2-m3"},
    "default_ocr_engine": {"label": "默认 OCR 解析引擎", "type": "ocr_engine", "default": "rapid_ocr"},
}

OCR_OPTIONS = {
    "mineru_ocr_host_opts": {
        "name": "MinerU 服务",
        "description": "配置自托管 MinerU 服务地址。",
        "fields": [{"key": "server_url", "label": "服务地址", "type": "url", "environment": "MINERU_API_URI", "placeholder": "http://mineru-api:30001", "help": "留空时读取 MINERU_API_URI。"}],
    },
    "mineru_official_api_opts": {
        "name": "MinerU Official",
        "description": "配置 MinerU 官方云服务凭证。",
        "fields": [{"key": "api_key", "label": "API Key", "type": "password", "environment": "MINERU_API_KEY", "sensitive": True, "help": "留空时读取 MINERU_API_KEY，建议优先使用环境变量。"}],
    },
    "pp_structure_v3_ocr_host_opts": {
        "name": "PP-Structure-V3 服务",
        "description": "配置自托管 PaddleX 服务地址。",
        "fields": [{"key": "server_url", "label": "服务地址", "type": "url", "environment": "PADDLEX_URI", "placeholder": "http://paddlex:8080", "help": "留空时读取 PADDLEX_URI。"}],
    },
    "paddleocr_api_opts": {
        "name": "PaddleOCR API",
        "description": "PaddleOCR-VL 和 PP-OCRv6 共用此配置。",
        "fields": [
            {"key": "api_url", "label": "API 地址", "type": "url", "environment": "PADDLEOCR_API_URL", "placeholder": "https://paddleocr.aistudio-app.com/api/v2/ocr/jobs", "help": "留空时读取 PADDLEOCR_API_URL。"},
            {"key": "api_token", "label": "Access Token", "type": "password", "environment": "PADDLEOCR_API_TOKEN", "sensitive": True, "help": "留空时读取 PADDLEOCR_API_TOKEN，建议优先使用环境变量。"},
        ],
    },
}

_PARSER_CLASSES = {
    "rapid_ocr": ("yuxi.knowledge.parser.rapid_ocr", "RapidOCRParser"),
    "mineru_ocr": ("yuxi.knowledge.parser.mineru", "MinerUParser"),
    "mineru_official": ("yuxi.knowledge.parser.mineru_official", "MinerUOfficialParser"),
    "pp_structure_v3_ocr": ("yuxi.knowledge.parser.pp_structure_v3", "PPStructureV3Parser"),
    "deepseek_ocr": ("yuxi.knowledge.parser.deepseek_ocr", "DeepSeekOCRParser"),
    "paddleocr_vl_1_6": ("yuxi.knowledge.parser.paddleocr_api", "PaddleOCRVLParser"),
    "paddleocr_pp_ocrv6": ("yuxi.knowledge.parser.paddleocr_api", "PaddleOCRPPOCRv6Parser"),
}


def _stored_option(key: str) -> dict:
    return db.get_system_option(f"ocr:{key}", {}) or {}


def _effective_fields(key: str) -> dict:
    definition = OCR_OPTIONS[key]
    stored = _stored_option(key)
    result = {}
    for field in definition["fields"]:
        name = field["key"]
        result[name] = stored.get(name) or os.getenv(field.get("environment", ""), "") or field.get("default", "")
    return result


def _processor_kwargs(engine_id: str) -> dict:
    if engine_id == "mineru_ocr":
        options = _effective_fields("mineru_ocr_host_opts")
        return {"server_url": options.get("server_url")} if options.get("server_url") else {}
    if engine_id == "mineru_official":
        options = _effective_fields("mineru_official_api_opts")
        if not options.get("api_key"):
            raise ValueError("MINERU_API_KEY 未配置")
        return {"api_key": options["api_key"]}
    if engine_id == "pp_structure_v3_ocr":
        options = _effective_fields("pp_structure_v3_ocr_host_opts")
        return {"server_url": options.get("server_url")} if options.get("server_url") else {}
    if engine_id == "deepseek_ocr":
        provider = provider_store.get_provider("siliconflow-cn") or {}
        api_key = provider.get("api_key") or os.getenv(provider.get("api_key_env") or "") or os.getenv("SILICONFLOW_API_KEY")
        if not api_key:
            raise ValueError("未配置 SiliconFlow API Key")
        base_url = str(provider.get("base_url") or "https://api.siliconflow.cn/v1").rstrip("/")
        return {"api_key": api_key, "api_url": f"{base_url}/chat/completions"}
    if engine_id in {"paddleocr_vl_1_6", "paddleocr_pp_ocrv6"}:
        options = _effective_fields("paddleocr_api_opts")
        if not options.get("api_token"):
            raise ValueError("PADDLEOCR_API_TOKEN 未配置")
        return {"api_token": options["api_token"], "api_url": options.get("api_url") or None}
    return {}


def _build_processor(engine_id: str):
    module_path, class_name = _PARSER_CLASSES[engine_id]
    parser_class = getattr(importlib.import_module(module_path), class_name)
    return parser_class(**_processor_kwargs(engine_id))


def _system_values() -> dict:
    stored = db.get_system_option("system", {}) or {}
    values = {key: stored.get(key, field["default"]) for key, field in SYSTEM_FIELDS.items()}
    fields = {
        key: {"des": field["label"], "default": field["default"], "type": field["type"], "exclude": False}
        for key, field in SYSTEM_FIELDS.items()
    }
    return {**values, "_config_items": fields}


def _save_system_values(values: dict) -> dict:
    if not isinstance(values, dict):
        raise HTTPException(status_code=400, detail="配置必须是对象")
    unknown = set(values) - set(SYSTEM_FIELDS)
    if unknown:
        raise HTTPException(status_code=400, detail=f"未知配置项: {', '.join(sorted(unknown))}")
    current = db.get_system_option("system", {}) or {}
    for key, value in values.items():
        if key == "default_ocr_engine" and value not in {"disable", *PARSER_CAPABILITIES}:
            raise HTTPException(status_code=400, detail=f"不支持的默认 OCR 引擎: {value}")
        current[key] = value
    db.set_system_option("system", current)
    return _system_values()


def _mask(value: str) -> str:
    if len(value) <= 4:
        return "*******"
    return f"{value[:2]}*******{value[-2:]}"


def _serialized_option(key: str) -> dict:
    definition = OCR_OPTIONS[key]
    stored = _stored_option(key)
    value = dict(stored)
    sensitive_state = {}
    sensitive_configured = {}
    for field in definition["fields"]:
        if not field.get("sensitive"):
            continue
        name = field["key"]
        configured_value = str(stored.get(name) or "")
        environment_value = os.getenv(field.get("environment", ""), "")
        if configured_value:
            state = {"source": "database", "configured": True, "preview": _mask(configured_value)}
        elif environment_value:
            state = {"source": "environment", "configured": True, "preview": None}
        else:
            state = {"source": "none", "configured": False, "preview": None}
        value[name] = ""
        sensitive_state[name] = state
        sensitive_configured[name] = state["configured"]
    return {
        "key": key,
        "name": definition["name"],
        "description": definition["description"],
        "params": {"fields": definition["fields"]},
        "value": value,
        "sensitive_configured": sensitive_configured,
        "sensitive_state": sensitive_state,
    }


@router.get("/api/system/config")
async def get_system_config():
    return _system_values()


@router.post("/api/system/config/update")
async def update_system_config(values: dict = Body(...)):
    return _save_system_values(values)


@router.post("/api/system/config")
async def update_system_config_value(payload: dict = Body(...)):
    return _save_system_values({payload.get("key"): payload.get("value")})


@router.get("/api/system/config/options")
async def get_system_config_options():
    return {"options": [_serialized_option(key) for key in OCR_OPTIONS]}


@router.put("/api/system/config/options/{key}")
async def update_system_config_option(key: str, payload: dict = Body(...)):
    definition = OCR_OPTIONS.get(key)
    if not definition:
        raise HTTPException(status_code=404, detail=f"配置项不存在: {key}")
    value = payload.get("value")
    if not isinstance(value, dict):
        raise HTTPException(status_code=400, detail="配置值必须是对象")
    fields = {field["key"]: field for field in definition["fields"]}
    unknown = set(value) - set(fields)
    if unknown:
        raise HTTPException(status_code=400, detail=f"未知配置字段: {', '.join(sorted(unknown))}")
    normalized = _stored_option(key)
    for field_key, raw_value in value.items():
        field = fields[field_key]
        result = str(raw_value or "").strip()
        if field.get("type") == "url" and result and urlparse(result).scheme not in {"http", "https"}:
            raise HTTPException(status_code=400, detail=f"{field_key} 必须是 http 或 https URL")
        normalized[field_key] = result
    db.set_system_option(f"ocr:{key}", normalized)
    return {"option": _serialized_option(key)}


@router.get("/api/system/ocr/options")
async def get_ocr_options():
    return {
        "default_engine": _system_values()["default_ocr_engine"],
        "engines": [
            {
                "engine_id": engine_id,
                "service_name": capability.service_name,
                "display_name": capability.display_name,
                "supported_extensions": list(capability.supported_extensions),
            }
            for engine_id, capability in PARSER_CAPABILITIES.items()
        ],
    }


@router.get("/api/system/ocr/health")
async def get_ocr_health():
    async def check(engine_id: str) -> tuple[str, dict]:
        try:
            processor = _build_processor(engine_id)
            if engine_id == "rapid_ocr":
                importlib.import_module("rapidocr.inference_engine.onnxruntime.main")
            result = await asyncio.to_thread(processor.check_health)
            return engine_id, {key: result[key] for key in ("status", "message") if key in result}
        except ImportError as exc:
            return engine_id, {"status": "unavailable", "message": f"依赖未安装: {exc.name or str(exc)}"}
        except (OSError, ValueError) as exc:
            return engine_id, {"status": "unavailable", "message": str(exc)}
        except Exception as exc:
            return engine_id, {"status": "error", "message": f"健康检查失败: {type(exc).__name__}"}

    checked = await asyncio.gather(*(check(engine_id) for engine_id in PARSER_CAPABILITIES))
    return {"health": dict(checked)}
