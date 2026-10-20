package com.nila.ghostradar.camera

import com.nila.ghostradar.detection.Box
import org.junit.Assert.assertEquals
import org.junit.Test

class CoordinateMapperTest {

    private val eps = 1e-4f

    private fun assertBox(expected: Box, actual: Box) {
        assertEquals("left", expected.left, actual.left, eps)
        assertEquals("top", expected.top, actual.top, eps)
        assertEquals("right", expected.right, actual.right, eps)
        assertEquals("bottom", expected.bottom, actual.bottom, eps)
    }

    @Test
    fun `fitted size letterboxes a wide box in a tall container`() {
        val s = fittedBoxSize(containerSize = FitSize(400f, 800f), boxSize = FitSize(640f, 480f))
        assertEquals(400f, s.width, eps)
        assertEquals(300f, s.height, eps)
    }

    @Test
    fun `fitted size pillarboxes a tall box in a wide container`() {
        val s = fittedBoxSize(containerSize = FitSize(800f, 400f), boxSize = FitSize(480f, 640f))
        assertEquals(300f, s.width, eps)
        assertEquals(400f, s.height, eps)
    }

    @Test
    fun `fitted size scales a portrait frame to the full width of a portrait phone`() {
        val s = fittedBoxSize(containerSize = FitSize(1080f, 2400f), boxSize = FitSize(480f, 640f))
        assertEquals(1080f, s.width, eps)
        assertEquals(1440f, s.height, eps)
    }

    @Test
    fun `image box scales uniformly to preview`() {
        val g = FrameGeometry(imageWidth = 480, imageHeight = 640, previewWidth = 1080f, previewHeight = 1440f)
        val p = Box(48f, 64f, 96f, 128f).toPreview(g)
        assertBox(Box(108f, 144f, 216f, 288f), p)
    }

    @Test
    fun `front camera mirrors horizontally only`() {
        val g = FrameGeometry(imageWidth = 100, imageHeight = 200, previewWidth = 100f, previewHeight = 200f, mirrored = true)
        val p = Box(10f, 20f, 30f, 50f).toPreview(g)
        assertBox(Box(70f, 20f, 90f, 50f), p)
    }

    @Test
    fun `rotate 90 moves top-left of a landscape sensor frame to top-right of portrait`() {
        // 640x480 sensor frame, box in the top-left corner.
        val r = Box(0f, 0f, 100f, 50f).rotate(90, srcWidth = 640, srcHeight = 480)
        // Upright frame is 480x640; the box lands at the top-right.
        assertBox(Box(430f, 0f, 480f, 100f), r)
    }

    @Test
    fun `rotate 180 flips both axes`() {
        val r = Box(0f, 0f, 100f, 50f).rotate(180, srcWidth = 640, srcHeight = 480)
        assertBox(Box(540f, 430f, 640f, 480f), r)
    }

    @Test
    fun `rotate 270 moves top-left to bottom-left`() {
        val r = Box(0f, 0f, 100f, 50f).rotate(270, srcWidth = 640, srcHeight = 480)
        assertBox(Box(0f, 540f, 50f, 640f), r)
    }

    @Test
    fun `rotate 0 and 360 are identity`() {
        val b = Box(1f, 2f, 3f, 4f)
        assertBox(b, b.rotate(0, 640, 480))
        assertBox(b, b.rotate(360, 640, 480))
    }

    @Test
    fun `rotating four times by 90 returns to the start`() {
        val b = Box(10f, 20f, 110f, 70f)
        val r = b.rotate(90, 640, 480).rotate(90, 480, 640).rotate(90, 640, 480).rotate(90, 480, 640)
        assertBox(b, r)
    }

    @Test
    fun `horizontal offset is -1 at the left edge, 0 at centre, 1 at the right edge`() {
        val g = FrameGeometry(imageWidth = 100, imageHeight = 100, previewWidth = 200f, previewHeight = 200f)
        assertEquals(-1f, Box(0f, 0f, 0f, 10f).horizontalOffset(g), eps)
        assertEquals(0f, Box(40f, 0f, 60f, 10f).horizontalOffset(g), eps)
        assertEquals(1f, Box(100f, 0f, 100f, 10f).horizontalOffset(g), eps)
        // Mirrored preview flips the sign.
        assertEquals(1f, Box(0f, 0f, 0f, 10f).horizontalOffset(g.copy(mirrored = true)), eps)
    }
}
