"""Yuxi-compatible knowledge-base permissions for the local RAG service."""

from fastapi import HTTPException, Request
from yuxi.permissions import (
    ResourcePermission,
    ResourcePermissionDenied,
    normalize_permission_config,
    require_knowledge_base_permission,
    resolve_knowledge_base_permission,
)

DEFAULT_SHARE_CONFIG = {
    "version": 2,
    "read_scope": {"access_level": "global", "department_ids": [], "user_uids": []},
    "manage_scope": None,
}


def request_user(request: Request) -> dict:
    uid = request.headers.get("x-geo-user-uid")
    role = request.headers.get("x-geo-user-role")
    department_id = request.headers.get("x-geo-user-department-id")
    if not uid:
        return {"uid": "1", "role": "superadmin", "department_id": None}
    if role not in {"user", "admin", "superadmin"}:
        role = "user"
    try:
        department_id = int(department_id) if department_id else None
    except ValueError:
        department_id = None
    return {"uid": uid, "role": role, "department_id": department_id}


def _resource(knowledge_base: dict) -> dict:
    config = knowledge_base.get("config") or {}
    return {
        "created_by": knowledge_base.get("created_by"),
        "share_config": config.get("share_config") or DEFAULT_SHARE_CONFIG,
    }


def permission_for(user: dict, knowledge_base: dict) -> ResourcePermission:
    try:
        return resolve_knowledge_base_permission(user, _resource(knowledge_base))
    except ValueError as exc:
        raise HTTPException(status_code=403, detail="知识库共享权限配置无效") from exc


def can_read(user: dict, knowledge_base: dict) -> bool:
    return permission_for(user, knowledge_base) in {
        ResourcePermission.READ,
        ResourcePermission.MANAGE,
    }


def can_manage(user: dict, knowledge_base: dict) -> bool:
    return permission_for(user, knowledge_base) == ResourcePermission.MANAGE


def require_access(user: dict, knowledge_base: dict, required: ResourcePermission) -> ResourcePermission:
    try:
        return require_knowledge_base_permission(user, _resource(knowledge_base), required)
    except ResourcePermissionDenied as exc:
        raise HTTPException(status_code=403, detail="无权操作该知识库") from exc
    except ValueError as exc:
        raise HTTPException(status_code=403, detail="知识库共享权限配置无效") from exc


def require_request_access(request: Request, knowledge_base: dict, required: ResourcePermission) -> ResourcePermission:
    return require_access(request.state.rag_user, knowledge_base, required)


def normalize_share_config(config: dict | None) -> dict:
    try:
        return normalize_permission_config(config or DEFAULT_SHARE_CONFIG, strict=True)
    except ValueError as exc:
        raise ValueError(str(exc)) from exc
