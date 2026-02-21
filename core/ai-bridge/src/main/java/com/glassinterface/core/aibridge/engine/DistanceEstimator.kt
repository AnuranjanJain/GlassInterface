package com.glassinterface.core.aibridge.engine

import com.glassinterface.core.common.BoundingBox
import kotlin.math.max
import kotlin.math.round

/**
 * Estimates relative distance and direction from bounding-box geometry.
 *
 * Uses a pinhole-camera heuristic:
 *     distance = (visible_height × focal_length) / bbox_pixel_height
 *
 * Accounts for partial-body visibility (person sitting, body cut off by
 * frame edge) by scaling the reference height based on what fraction of
 * the object is likely visible.
 */
class DistanceEstimator {

    companion object {
        // Margin (normalised) — if a bbox edge is within this distance of the
        // frame boundary, the object is considered partially visible.
        private const val EDGE_MARGIN = 0.05f

        // Ported from config.py
        private const val FOCAL_SCALE = 600f
        private const val DEFAULT_REFERENCE_HEIGHT = 0.30f

        private const val LEFT_BOUNDARY = 0.33f
        private const val RIGHT_BOUNDARY = 0.67f

        // Specific category heights
        private val CATEGORY_REFERENCE_HEIGHTS = mapOf(
            "furniture" to 0.8f,
            "vehicle" to 1.5f,
            "electronic" to 0.15f,
            "animal" to 0.6f,
            "person" to 1.7f
        )

        private val LABEL_TO_CATEGORY = mapOf(
            "chair" to "furniture", "couch" to "furniture", "bed" to "furniture", "dining table" to "furniture",
            "car" to "vehicle", "bus" to "vehicle", "truck" to "vehicle", "motorcycle" to "vehicle", "bicycle" to "vehicle",
            "laptop" to "electronic", "cell phone" to "electronic", "tv" to "electronic", "mouse" to "electronic", "keyboard" to "electronic",
            "dog" to "animal", "cat" to "animal", "bird" to "animal", "horse" to "animal", "sheep" to "animal", "cow" to "animal", "elephant" to "animal", "bear" to "animal", "zebra" to "animal", "giraffe" to "animal"
        )

        private val REFERENCE_HEIGHTS = mapOf(
            "person" to 1.70f,
            "car" to 1.50f,
            "bicycle" to 1.05f,
            "bus" to 3.00f,
            "truck" to 2.50f,
            "stairs" to 0.20f,
            "door" to 2.00f,
            "chair" to 0.90f,
            "laptop" to 0.20f,
            "cell phone" to 0.15f,
            "cup" to 0.10f,
            "bottle" to 0.20f
        )
    }

    /**
     * Enrich each detection with `distance` and `direction`.
     */
    fun estimate(boxes: List<BoundingBox>, frameHeight: Int = 640): List<BoundingBox> {
        return boxes.map { det ->
            // Normalised height = bottom - top
            val hNorm = det.rect.bottom - det.rect.top
            val bboxHeightPx = hNorm * frameHeight

            var refH = REFERENCE_HEIGHTS[det.label]
            if (refH == null) {
                val category = LABEL_TO_CATEGORY[det.label] ?: "unknown"
                refH = CATEGORY_REFERENCE_HEIGHTS[category] ?: DEFAULT_REFERENCE_HEIGHT
            }

            // Scale reference height for partial visibility
            val visibleH = refH * visibilityFraction(det, hNorm)

            val distance = if (bboxHeightPx > 0) {
                roundTo((visibleH * FOCAL_SCALE) / bboxHeightPx, 2)
            } else {
                999.0f
            }

            // Direction
            val cx = det.rect.centerX()
            val direction = when {
                cx < LEFT_BOUNDARY -> "LEFT"
                cx > RIGHT_BOUNDARY -> "RIGHT"
                else -> "CENTER"
            }

            det.copy(distance = distance, direction = direction)
        }
    }

    private fun visibilityFraction(det: BoundingBox, hNorm: Float): Float {
        val y1 = det.rect.top
        val y2 = det.rect.bottom
        val h = hNorm

        val topClipped = y1 < EDGE_MARGIN
        val bottomClipped = y2 > (1.0f - EDGE_MARGIN)

        if (topClipped && bottomClipped) return 0.50f
        if (h > 0.75f) return 0.55f
        if (bottomClipped) return 0.85f
        if (topClipped) return 0.70f

        // Aspect ratio check for sitting
        val aspect = (det.rect.right - det.rect.left) / max(h, 0.01f)
        if (det.label == "person" && aspect > 0.9f && h < 0.5f) {
            return 0.50f
        }

        return 1.0f
    }

    private fun roundTo(value: Float, decimals: Int): Float {
        var multiplier = 1.0f
        repeat(decimals) { multiplier *= 10 }
        return round(value * multiplier) / multiplier
    }
}
