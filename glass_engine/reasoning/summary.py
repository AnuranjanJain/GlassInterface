"""On-demand scene summarizer.

Produces a concise natural-language summary of the current detections,
suitable for TTS or display.  Called explicitly via ``sdk.get_scene_summary()``.
"""

from __future__ import annotations

from collections import Counter
from glass_engine.models import Detection


class SceneSummarizer:
    """Generates human-readable scene descriptions from detections."""

    def summarize(self, detections: list[Detection]) -> str:
        """Produce a natural-language summary of the current scene.

        Args:
            detections: All detections from the current frame.

        Returns:
            A string like "3 people ahead, 1 car approaching from left".
            Returns "Scene is clear" if no detections.
        """
        if not detections:
            return "Scene is clear"

        # Group by (label, direction) and track approaching status
        groups: dict[tuple[str, str], dict] = {}

        for det in detections:
            key = (det.label, det.direction)
            if key not in groups:
                groups[key] = {
                    "count": 0,
                    "approaching": 0,
                    "min_distance": float("inf"),
                }
            groups[key]["count"] += 1
            if det.approaching:
                groups[key]["approaching"] += 1
            groups[key]["min_distance"] = min(groups[key]["min_distance"], det.distance)

        # Build phrases, sorted by closest first
        phrases = []
        sorted_groups = sorted(groups.items(), key=lambda x: x[1]["min_distance"])

        for (label, direction), info in sorted_groups:
            count = info["count"]
            approaching = info["approaching"]
            dist = info["min_distance"]

            # Pluralize
            noun = self._pluralize(label, count)
            count_str = str(count) if count > 1 else "1"

            # Direction
            dir_text = self._direction_text(direction)

            # Build phrase
            phrase = f"{count_str} {noun} {dir_text}"

            if approaching > 0:
                phrase += ", approaching"

            phrase += f" ({dist:.1f}m)"
            phrases.append(phrase)

        return "; ".join(phrases)

    @staticmethod
    def _pluralize(label: str, count: int) -> str:
        if count <= 1:
            return label
        # Simple English pluralization
        if label.endswith("s") or label.endswith("x") or label.endswith("sh") or label.endswith("ch"):
            return label + "es"
        if label.endswith("y") and label[-2] not in "aeiou":
            return label[:-1] + "ies"
        if label == "person":
            return "people"
        return label + "s"

    @staticmethod
    def _direction_text(direction: str) -> str:
        return {
            "LEFT": "to the left",
            "RIGHT": "to the right",
            "CENTER": "ahead",
        }.get(direction, "nearby")
