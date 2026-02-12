"""Unit tests for SceneSummarizer."""

import pytest
from glass_engine.reasoning.summary import SceneSummarizer
from glass_engine.models import Detection


@pytest.fixture
def summarizer():
    return SceneSummarizer()


def _det(label="person", distance=2.0, direction="CENTER", approaching=False):
    d = Detection(label=label, confidence=0.9, bbox=(0.3, 0.1, 0.7, 0.9))
    d.distance = distance
    d.direction = direction
    d.approaching = approaching
    return d


class TestSceneSummary:
    """Verify natural-language scene descriptions."""

    def test_empty_scene(self, summarizer):
        assert summarizer.summarize([]) == "Scene is clear"

    def test_single_person(self, summarizer):
        result = summarizer.summarize([_det("person", 3.0, "CENTER")])
        assert "1 person" in result
        assert "ahead" in result
        assert "3.0m" in result

    def test_multiple_same_type(self, summarizer):
        dets = [
            _det("person", 2.0, "CENTER"),
            _det("person", 3.0, "CENTER"),
        ]
        result = summarizer.summarize(dets)
        assert "2 people" in result
        assert "2.0m" in result  # min distance

    def test_approaching_annotation(self, summarizer):
        dets = [_det("car", 4.0, "LEFT", approaching=True)]
        result = summarizer.summarize(dets)
        assert "approaching" in result
        assert "left" in result.lower()

    def test_mixed_objects(self, summarizer):
        dets = [
            _det("person", 2.0, "CENTER"),
            _det("car", 5.0, "LEFT"),
        ]
        result = summarizer.summarize(dets)
        assert "person" in result
        assert "car" in result
        # Person is closer, should come first
        person_pos = result.index("person")
        car_pos = result.index("car")
        assert person_pos < car_pos

    def test_pluralization(self, summarizer):
        """Test various plural forms."""
        dets = [
            _det("bus", 3.0, "CENTER"),
            _det("bus", 4.0, "CENTER"),
        ]
        result = summarizer.summarize(dets)
        assert "2 buses" in result
