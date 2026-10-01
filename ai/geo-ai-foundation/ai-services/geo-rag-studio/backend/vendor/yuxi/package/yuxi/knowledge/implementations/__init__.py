"""知识库具体实现模块，按需加载各类知识库实现。"""

from importlib import import_module

__all__ = ["MilvusKB", "DifyKB", "NotionKB", "ReadOnlyConnectors"]


def __getattr__(name: str):
    modules = {
        "MilvusKB": ".milvus",
        "DifyKB": ".dify",
        "NotionKB": ".notion",
        "ReadOnlyConnectors": ".read_only_connectors",
    }
    module_name = modules.get(name)
    if module_name is None:
        raise AttributeError(f"module {__name__!r} has no attribute {name!r}")
    return getattr(import_module(module_name, __name__), name)
