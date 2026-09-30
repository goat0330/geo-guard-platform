import pytest

from app.embedding import _vectors_in_input_order


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
