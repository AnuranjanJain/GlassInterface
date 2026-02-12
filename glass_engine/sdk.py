"""GlassInterfaceSDK — single entry-point facade for the AI engine.

The app layer only needs to import this class. It wires together
detection, distance estimation, tracking, risk scoring, hazard memory,
and alert management into one ``process_frame()`` call.
"""

from __future__ import annotations

import time
from typing import Optional

import numpy as np

from glass_engine.config import ContextMode, GlassConfig
from glass_engine.models import FrameResult
from glass_engine.detection.detector import ObjectDetector
from glass_engine.distance.estimator import DistanceEstimator
from glass_engine.tracking.tracker import ObjectTracker
from glass_engine.reasoning.engine import ReasoningEngine
from glass_engine.reasoning.memory import HazardMemory
from glass_engine.reasoning.summary import SceneSummarizer
from glass_engine.alert.manager import AlertManager


class GlassInterfaceSDK:
    """Production facade for the GlassInterface vision pipeline.

    Usage::

        from glass_engine import GlassInterfaceSDK

        sdk = GlassInterfaceSDK()
        sdk.set_context(ContextMode.WALKING)
        result = sdk.process_frame(frame_bgr)
        print(result.to_json())

    Parameters:
        config: Optional custom configuration; uses defaults if omitted.
    """

    def __init__(self, config: Optional[GlassConfig] = None) -> None:
        self._config = config or GlassConfig()
        self._detector = ObjectDetector(self._config)
        self._estimator = DistanceEstimator(self._config)
        self._tracker = ObjectTracker()
        self._reasoner = ReasoningEngine(self._config)
        self._memory = HazardMemory(self._config)
        self._summarizer = SceneSummarizer()
        self._alert_mgr = AlertManager(self._config)
        self._last_detections: list = []  # cached for scene summary

    # ── Public API ───────────────────────────────────────────────────

    def process_frame(self, frame: np.ndarray) -> FrameResult:
        """Run the full vision pipeline on a single frame.

        Pipeline stages:
            1. Object detection  (YOLOv8n)
            2. Distance estimation (bbox heuristic)
            3. Tracking            (IoU matching + velocity)
            4. Risk scoring        (continuous formula)
            5. Hazard memory       (persistence demotion)
            6. Alert management    (adaptive cooldown + dedup)

        Args:
            frame: HxWx3 uint8 BGR numpy array.

        Returns:
            ``FrameResult`` containing all detections and filtered alerts.
        """
        t0 = time.perf_counter()
        h, w = frame.shape[:2]

        # Stage 1 — Detect
        detections = self._detector.detect(frame)

        # Stage 2 — Distance + Direction
        detections = self._estimator.estimate(detections, frame_height=h)

        # Stage 3 — Tracking (ID + velocity)
        detections = self._tracker.update(detections)

        # Stage 4 — Risk Scoring
        alerts = self._reasoner.evaluate(detections)

        # Stage 5 — Hazard Memory (demote persistent hazards)
        self._memory.begin_frame()
        alerts = self._memory.process(alerts)
        self._memory.end_frame()

        # Stage 6 — Alert filtering (adaptive cooldown)
        alerts = self._alert_mgr.filter(alerts)

        elapsed_ms = (time.perf_counter() - t0) * 1000.0
        self._last_detections = detections

        return FrameResult(
            detections=detections,
            alerts=alerts,
            processing_time_ms=round(elapsed_ms, 2),
        )

    def set_context(self, mode: ContextMode) -> None:
        """Set the operating context for risk weight adjustment.

        Args:
            mode: One of ContextMode.INDOOR, OUTDOOR, WALKING, STATIONARY.
        """
        self._reasoner.context = mode

    def get_context(self) -> ContextMode:
        """Return the current context mode."""
        return self._reasoner.context

    def get_scene_summary(self) -> str:
        """Generate a natural-language summary of the last processed frame.

        Returns:
            A string like "3 people ahead, 1 car approaching from left".
        """
        return self._summarizer.summarize(self._last_detections)

    def reset_alerts(self) -> None:
        """Clear alert cooldowns and hazard memory (call on scene change)."""
        self._alert_mgr.reset()
        self._memory.reset()

    def release(self) -> None:
        """Free all resources (model memory, etc.)."""
        self._detector.release()
        self._alert_mgr.reset()
        self._memory.reset()
