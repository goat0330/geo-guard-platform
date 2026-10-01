import pytest

from app.embedding import _YuxiEmbedding, _vectors_in_input_order
from yuxi.models.embed import OtherEmbedding


def test_embedding_vectors_restore_input_order():
    assert _vectors_in_input_order([
        {"index": 1, "embedding": [0.0, 1.0]},
        {"index": 0, "embedding": [1.0, 0.0]},
    ], 2) == [[1.0, 0.0], [0.0, 1.0]]


def test_embedding_rejects_bad_index_or_dimensions():
    for data in (
        [{"index": 0, "embedding": [1.0, 0.0]}],
        [{"index": 0, "embedding": [1.0]}, {"index": 1, "embedding": [1.0, 2.0]}],
        [{"index": 0, "embedding": [1.0, 0.0]}, {"index": 0, "embedding": [0.0, 1.0]}],
    ):
        with pytest.raises(ValueError):
            _vectors_in_input_order(data, 2)


def test_active_embedding_client_uses_yuxi_model_and_restores_provider_indexes():
    assert issubclass(_YuxiEmbedding, OtherEmbedding)
    assert _YuxiEmbedding._extract_embeddings(
        {
            "data": [
                {"index": 1, "embedding": [0.0, 1.0]},
                {"index": 0, "embedding": [1.0, 0.0]},
            ]
        }
    ) == [[1.0, 0.0], [0.0, 1.0]]
