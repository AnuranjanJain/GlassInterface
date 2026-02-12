"""Unit tests for velocity-based risk scoring (Level 2)."""

import pytest
from glass_engine.config import GlassConfig
from glass_engine.reasoning.engine import ReasoningEngine
from glass_engine.models import Detection


@pytest.fixture
def engine():
    return ReasoningEngine(GlassConfig())


class TestVelocityEscalation:
    """Velocity boosts risk via the velocity_factor in the scoring formula."""

    def test_approaching_fast_boosts_risk(self, engine):
        """Fast approach (-1.5 m/s) should produce higher risk than static."""
        d_static = Detection(label="person", confidence=0.9, bbox=(0.3, 0.1, 0.7, 0.9))
        d_static.distance = 4.0
        d_static.direction = "CENTER"
        d_static.velocity = 0.0
        d_static.approaching = False

        d_fast = Detection(label="person", confidence=0.9, bbox=(0.3, 0.1, 0.7, 0.9))
        d_fast.distance = 4.0
        d_fast.direction = "CENTER"
        d_fast.velocity = -1.5
        d_fast.approaching = True

        engine.evaluate([d_static])
        engine.evaluate([d_fast])

        assert d_fast.risk_score > d_static.risk_score
        # Fast approach at 4m should be at least WARNING
        alerts = engine.evaluate([d_fast])
        assert alerts[0].priority in ("CRITICAL", "WARNING")

    def test_slow_approach_lower_boost(self, engine):
        """Slow approach (-0.2 m/s) should barely boost risk."""
        d = Detection(label="person", confidence=0.9, bbox=(0.3, 0.1, 0.7, 0.9))
        d.distance = 4.0
        d.direction = "CENTER"
        d.velocity = -0.2
        d.approaching = True

        alerts = engine.evaluate([d])
        assert len(alerts) >= 1
        # Slow approach doesn't dramatically change priority
        assert d.risk_score < 0.9  # not super high

    def test_moving_away_no_boost(self, engine):
        """Positive velocity (moving away) should not boost risk."""
        d = Detection(label="car", confidence=0.9, bbox=(0.3, 0.1, 0.7, 0.9))
        d.distance = 5.0
        d.direction = "CENTER"
        d.velocity = 1.0
        d.approaching = False

        alerts = engine.evaluate([d])
        # Moving away at 5m — velocity_factor stays at 1.0
        # Should not be CRITICAL
        if alerts:
            assert alerts[0].priority != "CRITICAL" or d.risk_score < 0.7
