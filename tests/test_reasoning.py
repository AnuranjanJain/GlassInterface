"""Unit tests for the ReasoningEngine (Level 2 — risk scoring delegate)."""

import pytest
from glass_engine.config import ContextMode, GlassConfig
from glass_engine.reasoning.engine import ReasoningEngine
from glass_engine.models import Detection


@pytest.fixture
def engine():
    return ReasoningEngine(GlassConfig())


def _det(label="person", distance=1.0, direction="CENTER", conf=0.9,
         velocity=0.0, approaching=False):
    """Helper to create a Detection with distance/direction pre-set."""
    d = Detection(label=label, confidence=conf, bbox=(0.3, 0.1, 0.7, 0.9))
    d.distance = distance
    d.direction = direction
    d.velocity = velocity
    d.approaching = approaching
    return d


class TestCriticalAlerts:
    """Close objects with high risk → CRITICAL."""

    def test_person_close_center(self, engine):
        alerts = engine.evaluate([_det("person", 1.0, "CENTER")])
        assert len(alerts) == 1
        assert alerts[0].priority == "CRITICAL"

    def test_car_close_center(self, engine):
        alerts = engine.evaluate([_det("car", 1.2, "CENTER")])
        assert alerts[0].priority == "CRITICAL"


class TestWarningAlerts:
    """Medium-range safety objects → WARNING."""

    def test_person_medium_distance(self, engine):
        alerts = engine.evaluate([_det("person", 4.0, "RIGHT")])
        assert len(alerts) == 1
        assert alerts[0].priority == "WARNING"

    def test_bicycle_warning(self, engine):
        alerts = engine.evaluate([_det("bicycle", 5.0, "LEFT")])
        assert alerts[0].priority == "WARNING"


class TestInfoAlerts:
    """Low-risk objects → INFO."""

    def test_non_safety_close(self, engine):
        """A backpack (non-safety, priority=0.5) at moderate range → INFO."""
        alerts = engine.evaluate([_det("backpack", 3.0, "CENTER")])
        assert len(alerts) == 1
        assert alerts[0].priority == "INFO"


class TestSuppression:
    """Far objects are suppressed entirely."""

    def test_far_person_suppressed(self, engine):
        """Person at 9m (beyond MAX_RISK_RANGE=8) → no alert."""
        alerts = engine.evaluate([_det("person", 9.0, "CENTER")])
        assert len(alerts) == 0

    def test_far_non_safety_suppressed(self, engine):
        """Backpack at 9m → no alert."""
        alerts = engine.evaluate([_det("backpack", 9.0, "LEFT")])
        assert len(alerts) == 0


class TestSorting:
    """Alerts should be sorted by risk (highest first)."""

    def test_highest_risk_first(self, engine):
        dets = [
            _det("backpack", 3.0, "LEFT"),     # low risk
            _det("person", 1.0, "CENTER"),      # high risk
        ]
        alerts = engine.evaluate(dets)
        assert len(alerts) >= 2
        assert alerts[0].risk_score >= alerts[1].risk_score


class TestRiskScorePopulation:
    """Risk scores should be populated on detections and alerts."""

    def test_detection_has_risk_score(self, engine):
        det = _det("person", 2.0, "CENTER")
        engine.evaluate([det])
        assert det.risk_score > 0

    def test_alert_has_risk_score(self, engine):
        alerts = engine.evaluate([_det("person", 2.0, "CENTER")])
        assert alerts[0].risk_score > 0


class TestContextDelegation:
    """ReasoningEngine should delegate context to RiskScorer."""

    def test_set_context(self, engine):
        engine.context = ContextMode.WALKING
        assert engine.context == ContextMode.WALKING

    def test_context_affects_risk(self):
        cfg = GlassConfig()
        engine_walk = ReasoningEngine(cfg)
        engine_walk.context = ContextMode.WALKING

        engine_stat = ReasoningEngine(cfg)
        engine_stat.context = ContextMode.STATIONARY

        det_walk = _det("car", 3.0, "CENTER")
        det_stat = _det("car", 3.0, "CENTER")

        engine_walk.evaluate([det_walk])
        engine_stat.evaluate([det_stat])

        assert det_walk.risk_score > det_stat.risk_score


class TestVelocityEscalation:
    """Approaching objects should have boosted risk."""

    def test_approaching_boosts_risk(self, engine):
        det_static = _det("person", 3.0, "CENTER", velocity=0.0)
        det_fast = _det("person", 3.0, "CENTER", velocity=-2.0, approaching=True)

        engine.evaluate([det_static])
        engine.evaluate([det_fast])

        assert det_fast.risk_score > det_static.risk_score
