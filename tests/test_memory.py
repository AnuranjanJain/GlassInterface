"""Unit tests for HazardMemory."""

import pytest
from glass_engine.config import GlassConfig
from glass_engine.reasoning.memory import HazardMemory
from glass_engine.models import Alert


@pytest.fixture
def memory():
    cfg = GlassConfig(HAZARD_MEMORY_FRAMES=3, HAZARD_FORGET_FRAMES=2)
    return HazardMemory(cfg)


def _alert(label="person", direction="CENTER", priority="WARNING"):
    return Alert(
        priority=priority,
        message=f"{label} test alert",
        label=label,
        distance=2.0,
        direction=direction,
    )


class TestHazardDemotion:
    """Verify priority demotion after persistent observation."""

    def test_no_demotion_initially(self, memory):
        """Alerts should not be demoted on first appearance."""
        memory.begin_frame()
        result = memory.process([_alert()])
        memory.end_frame()
        assert result[0].priority == "WARNING"

    def test_demotion_after_threshold(self, memory):
        """After 3 frames (HAZARD_MEMORY_FRAMES), priority should demote."""
        alert = _alert(priority="WARNING")

        for _ in range(3):
            memory.begin_frame()
            memory.process([_alert(priority="WARNING")])
            memory.end_frame()

        # Frame 4 — should trigger demotion
        memory.begin_frame()
        result = memory.process([_alert(priority="WARNING")])
        memory.end_frame()

        assert result[0].priority == "INFO"  # WARNING → INFO

    def test_critical_demoted_to_warning(self, memory):
        """CRITICAL should demote to WARNING, not skip tiers."""
        for _ in range(3):
            memory.begin_frame()
            memory.process([_alert(priority="CRITICAL")])
            memory.end_frame()

        memory.begin_frame()
        result = memory.process([_alert(priority="CRITICAL")])
        memory.end_frame()

        assert result[0].priority == "WARNING"

    def test_info_stays_info(self, memory):
        """INFO cannot be demoted further — stays INFO."""
        for _ in range(5):
            memory.begin_frame()
            memory.process([_alert(priority="INFO")])
            memory.end_frame()

        memory.begin_frame()
        result = memory.process([_alert(priority="INFO")])
        memory.end_frame()

        assert result[0].priority == "INFO"


class TestHazardReset:
    """Verify hazards are forgotten after absence."""

    def test_reset_after_absence(self, memory):
        """Object absent for HAZARD_FORGET_FRAMES should reset counter."""
        # See it for 3 frames
        for _ in range(4):
            memory.begin_frame()
            memory.process([_alert()])
            memory.end_frame()

        # Now it disappears for 2 frames (HAZARD_FORGET_FRAMES)
        for _ in range(2):
            memory.begin_frame()
            memory.process([])  # no alerts
            memory.end_frame()

        # Should be forgotten — frame count resets
        assert memory.get_frame_count("person", "CENTER") == 0

    def test_manual_reset(self, memory):
        """reset() should clear all memory."""
        memory.begin_frame()
        memory.process([_alert()])
        memory.end_frame()

        memory.reset()
        assert memory.get_frame_count("person", "CENTER") == 0
