package com.autoclock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class CoordinateMapperTest {

    @Test
    fun `maps center tap to center ratio when aspect ratio matches`() {
        val result = CoordinateMapper.mapFitCenterTapToRatio(
            touchX = 50f,
            touchY = 100f,
            viewWidth = 100,
            viewHeight = 200,
            imageWidth = 100,
            imageHeight = 200
        )

        assertNotNull(result)
        assertEquals(0.5f, result!!.xRatio, 0.001f)
        assertEquals(0.5f, result.yRatio, 0.001f)
    }

    @Test
    fun `returns null when tap is in top letterbox area`() {
        val result = CoordinateMapper.mapFitCenterTapToRatio(
            touchX = 50f,
            touchY = 10f,
            viewWidth = 100,
            viewHeight = 200,
            imageWidth = 100,
            imageHeight = 100
        )

        assertNull(result)
    }

    @Test
    fun `returns null when tap is in left letterbox area`() {
        val result = CoordinateMapper.mapFitCenterTapToRatio(
            touchX = 10f,
            touchY = 50f,
            viewWidth = 200,
            viewHeight = 100,
            imageWidth = 100,
            imageHeight = 200
        )

        assertNull(result)
    }

    @Test
    fun `maps visible image tap after vertical letterboxing`() {
        val result = CoordinateMapper.mapFitCenterTapToRatio(
            touchX = 50f,
            touchY = 100f,
            viewWidth = 100,
            viewHeight = 200,
            imageWidth = 100,
            imageHeight = 100
        )

        assertNotNull(result)
        assertEquals(0.5f, result!!.xRatio, 0.001f)
        assertEquals(0.5f, result.yRatio, 0.001f)
    }

    @Test
    fun `returns null for invalid dimensions`() {
        val result = CoordinateMapper.mapFitCenterTapToRatio(
            touchX = 0f,
            touchY = 0f,
            viewWidth = 0,
            viewHeight = 200,
            imageWidth = 100,
            imageHeight = 100
        )

        assertNull(result)
    }
}
