import json
import zipfile

import pymupdf

from app.parser import _mineru_blocks


def test_mineru_adapter_keeps_valid_page_bbox_and_rejects_unknown_scale(tmp_path):
    pdf_path = tmp_path / "source.pdf"
    document = pymupdf.open()
    document.new_page(width=300, height=400)
    document.save(pdf_path)
    document.close()

    zip_path = tmp_path / "mineru.zip"
    items = [
        {"text": "valid", "page_idx": 0, "bbox": [10, 20, 100, 60]},
        {"text": "unknown scale", "page_idx": 0, "bbox": [1000, 2000, 3000, 4000]},
    ]
    with zipfile.ZipFile(zip_path, "w") as archive:
        archive.writestr("auto/content_list.json", json.dumps(items))
    blocks = _mineru_blocks(zip_path.read_bytes(), str(pdf_path))

    assert blocks[0]["page"] == 1
    assert blocks[0]["bbox"] == [10.0, 20.0, 100.0, 60.0]
    assert blocks[1]["bbox"] is None
