"""Rule-based reasoning engine for safety-critical alerting.

Level 2: Delegates to ``RiskScorer`` for continuous risk scoring, with
backward-compatible ``evaluate()`` API.

The old rule table is replaced by:
    risk_score = distance_factor × velocity_factor × object_priority × context_weight

Thresholds (configurable):
    risk ≥ 0.7  → CRITICAL
    risk ≥ 0.4  → WARNING
    risk ≥ 0.15 → INFO
    risk < 0.15 → suppress
"""

from __future__ import annotations

from glass_engine.config import ContextMode, GlassConfig
from glass_engine.models import Alert, Detection
from glass_engine.reasoning.scorer import RiskScorer


class ReasoningEngine:
    """Converts raw detections into prioritised alerts.

    Backward-compatible wrapper around the Level 2 ``RiskScorer``.

    Parameters:
        config: Engine configuration with risk thresholds and context weights.
    """

    def __init__(self, config: GlassConfig | None = None) -> None:
        self._config = config or GlassConfig()
        self._scorer = RiskScorer(self._config)

    @property
    def context(self) -> ContextMode:
        """Current context mode."""
        return self._scorer.context

    @context.setter
    def context(self, mode: ContextMode) -> None:
        self._scorer.context = mode

    # ── Public API ───────────────────────────────────────────────────

    def evaluate(self, detections: list[Detection]) -> list[Alert]:
        """Apply risk scoring to a list of enriched detections.

        This is the backward-compatible entry point.  Internally it
        delegates to ``RiskScorer.score()``.

        Args:
            detections: Detections with ``distance``, ``direction``,
                        ``velocity``, and ``approaching`` populated.

        Returns:
            Alerts sorted by risk (highest first).
        """
        return self._scorer.score(detections)
