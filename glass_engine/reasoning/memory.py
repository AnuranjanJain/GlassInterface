"""Hazard memory — reduces alert fatigue for persistent objects.

Tracks how many consecutive frames each (label, direction) pair has
been observed.  Objects seen for longer than ``HAZARD_MEMORY_FRAMES``
are demoted one priority tier to avoid spamming the user.
"""

from __future__ import annotations

from glass_engine.config import GlassConfig
from glass_engine.models import Alert

# Demotion map: one tier lower
_DEMOTE = {
    "CRITICAL": "WARNING",
    "WARNING": "INFO",
    "INFO": "INFO",  # INFO stays INFO (can't go lower without suppressing)
}


class HazardMemory:
    """Persistent hazard tracker for alert fatigue reduction.

    Parameters:
        config: Engine configuration with memory frame thresholds.
    """

    def __init__(self, config: GlassConfig | None = None) -> None:
        self._config = config or GlassConfig()
        # Key: (label, direction) → consecutive frame count
        self._seen: dict[tuple[str, str], int] = {}
        # Track which keys were updated this frame
        self._current_frame_keys: set[tuple[str, str]] = set()

    def begin_frame(self) -> None:
        """Call at the start of each frame before processing alerts."""
        self._current_frame_keys = set()

    def process(self, alerts: list[Alert]) -> list[Alert]:
        """Apply hazard memory: demote long-seen objects.

        Args:
            alerts: Alerts from the RiskScorer (sorted by risk).

        Returns:
            Alerts with priorities adjusted for persistent hazards.
        """
        result: list[Alert] = []
        cfg = self._config

        for alert in alerts:
            key = (alert.label, alert.direction)
            self._current_frame_keys.add(key)

            # Increment frame count
            self._seen[key] = self._seen.get(key, 0) + 1
            count = self._seen[key]

            # Demote if seen for too long
            if count > cfg.HAZARD_MEMORY_FRAMES:
                alert.priority = _DEMOTE.get(alert.priority, alert.priority)

            result.append(alert)

        return result

    def end_frame(self) -> None:
        """Call at the end of each frame to age out unseen hazards."""
        cfg = self._config
        keys_to_remove = []

        for key in list(self._seen.keys()):
            if key not in self._current_frame_keys:
                # Object not seen this frame — increment absence counter
                # We use negative values to track absence
                self._seen[key] = -(abs(self._seen.get(key, 0)) if self._seen[key] < 0 else 0) - 1

                if abs(self._seen[key]) >= cfg.HAZARD_FORGET_FRAMES:
                    keys_to_remove.append(key)

        for key in keys_to_remove:
            del self._seen[key]

    def reset(self) -> None:
        """Clear all hazard memory."""
        self._seen.clear()
        self._current_frame_keys.clear()

    def get_frame_count(self, label: str, direction: str) -> int:
        """Return how many frames a hazard has been seen (for testing)."""
        val = self._seen.get((label, direction), 0)
        return val if val > 0 else 0
