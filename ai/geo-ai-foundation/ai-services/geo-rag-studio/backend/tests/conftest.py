from pathlib import Path

import pymupdf
import pytest


@pytest.fixture
def pdf_sample(tmp_path: Path) -> Path:
    """Create a tiny, text-based PDF so parser tests run without private seed files."""
    path = tmp_path / "slope-hazard-test-fixture.pdf"
    document = pymupdf.open()
    page = document.new_page(width=595, height=842)
    page.insert_text((72, 72), "Geological hazard emergency plan", fontname="helv", fontsize=14)
    page.insert_text((72, 100), "Slope cracks, rainfall, displacement monitoring, and field inspection.", fontname="helv", fontsize=11)
    document.save(path)
    document.close()
    return path
