"""Simple IoU-based object tracker with velocity estimation.

Assigns persistent IDs to detections and computes velocity based on
distance changes over time.
"""

from __future__ import annotations

import time
from collections import deque
from dataclasses import dataclass, field
import numpy as np

from glass_engine.models import Detection


@dataclass
class TrackedObject:
    """State for a single tracked object."""
    id: int
    label: str
    hits: int = 0           # number of frames successfully matched
    misses: int = 0         # number of frames lost
    history: deque = field(default_factory=lambda: deque(maxlen=5))  # (timestamp, distance) samples

    # The last known detection (for bbox IoU matching)
    last_detection: Detection | None = None

    def update(self, det: Detection, now: float) -> None:
        """Update track with a new detection."""
        self.hits += 1
        self.misses = 0
        self.last_detection = det
        self.history.append((now, det.distance))

    def predict_velocity(self) -> float:
        """Compute velocity in m/s using linear regression over history.

        Returns:
            Velocity in m/s. Negative = approaching, Positive = moving away.
        """
        if len(self.history) < 2:
            return 0.0

        # Extract arrays
        times = np.array([t for t, _ in self.history])
        dists = np.array([d for _, d in self.history])

        # Simple linear regression: distance = slope * time + intercept
        # slope is velocity (m/s)
        try:
            slope, _ = np.polyfit(times, dists, 1)
            return float(slope)
        except np.linalg.LinAlgError:
            return 0.0


class ObjectTracker:
    """Matches detections across frames to assign IDs and compute velocity."""

    def __init__(self, iou_threshold: float = 0.3, max_age: int = 3):
        self.iou_threshold = iou_threshold
        self.max_age = max_age
        self.next_id = 1
        self.tracks: list[TrackedObject] = []

    def update(self, detections: list[Detection]) -> list[Detection]:
        """Update tracks with new detections and return enhanced detections.

        Args:
            detections: List of detections from the current frame.

        Returns:
            The same list of detections, but with `id`, `velocity`, and
            `approaching` fields populated.
        """
        now = time.time()

        # 1. Match active tracks to new detections using IoU
        matches = []
        unmatched_tracks = set(range(len(self.tracks)))
        unmatched_detections = set(range(len(detections)))

        if len(self.tracks) > 0 and len(detections) > 0:
            iou_matrix = np.zeros((len(self.tracks), len(detections)), dtype=float)

            for t_idx, track in enumerate(self.tracks):
                for d_idx, det in enumerate(detections):
                    if track.last_detection.label == det.label:
                        iou_matrix[t_idx, d_idx] = self._calculate_iou(track.last_detection.bbox, det.bbox)
                    else:
                        iou_matrix[t_idx, d_idx] = 0.0

            # Greedy matching (could use Hungarian algorithm, but greedy is faster/simpler)
            # Find max IoU, assign, repeat
            while True:
                if iou_matrix.size == 0:
                    break
                
                # Find max value in matrix
                flat_idx = np.argmax(iou_matrix)
                max_iou = iou_matrix.flat[flat_idx]

                if max_iou < self.iou_threshold:
                    break

                t_idx, d_idx = np.unravel_index(flat_idx, iou_matrix.shape)

                matches.append((t_idx, d_idx))
                
                # Remove rows/cols from consideration by setting to -1
                iou_matrix[t_idx, :] = -1
                iou_matrix[:, d_idx] = -1

                if t_idx in unmatched_tracks: unmatched_tracks.remove(t_idx)
                if d_idx in unmatched_detections: unmatched_detections.remove(d_idx)

        # 2. Update matched tracks
        for t_idx, d_idx in matches:
            track = self.tracks[t_idx]
            det = detections[d_idx]
            track.update(det, now)

            # Populate detection fields
            det.id = track.id
            det.velocity = track.predict_velocity()
            det.approaching = det.velocity < -0.5  # threshold: moving closer faster than 0.5 m/s

        # 3. Create new tracks for unmatched detections
        for d_idx in unmatched_detections:
            det = detections[d_idx]
            new_track = TrackedObject(id=self.next_id, label=det.label)
            new_track.update(det, now)
            self.tracks.append(new_track)
            self.next_id += 1
            
            det.id = new_track.id
            det.velocity = 0.0
            det.approaching = False

        # 4. Handle lost tracks
        # Remove tracks that haven't been seen for `max_age` frames
        active_tracks = []
        for t_idx, track in enumerate(self.tracks):
             if t_idx in unmatched_tracks:
                 track.misses += 1
             
             if track.misses <= self.max_age:
                 active_tracks.append(track)
        
        self.tracks = active_tracks

        return detections

    @staticmethod
    def _calculate_iou(bbox1: tuple, bbox2: tuple) -> float:
        """Calculate Intersection over Union (IoU) between two bboxes."""
        x1a, y1a, x2a, y2a = bbox1
        x1b, y1b, x2b, y2b = bbox2

        # Intersection
        x1 = max(x1a, x1b)
        y1 = max(y1a, y1b)
        x2 = min(x2a, x2b)
        y2 = min(y2a, y2b)

        w = max(0.0, x2 - x1)
        h = max(0.0, y2 - y1)
        inter = w * h

        # Union
        area1 = (x2a - x1a) * (y2a - y1a)
        area2 = (x2b - x1b) * (y2b - y1b)
        union = area1 + area2 - inter

        if union <= 0:
            return 0.0
        
        return inter / union
