package com.autoclock

data class CoordinateRatio(
    val xRatio: Float,
    val yRatio: Float
)

object CoordinateMapper {

    fun mapFitCenterTapToRatio(
        touchX: Float,
        touchY: Float,
        viewWidth: Int,
        viewHeight: Int,
        imageWidth: Int,
        imageHeight: Int
    ): CoordinateRatio? {
        if (viewWidth <= 0 || viewHeight <= 0 || imageWidth <= 0 || imageHeight <= 0) {
            return null
        }

        val imageAspect = imageWidth.toFloat() / imageHeight.toFloat()
        val viewAspect = viewWidth.toFloat() / viewHeight.toFloat()

        val displayedWidth: Float
        val displayedHeight: Float
        val displayedLeft: Float
        val displayedTop: Float

        if (imageAspect > viewAspect) {
            displayedWidth = viewWidth.toFloat()
            displayedHeight = displayedWidth / imageAspect
            displayedLeft = 0f
            displayedTop = (viewHeight - displayedHeight) / 2f
        } else {
            displayedHeight = viewHeight.toFloat()
            displayedWidth = displayedHeight * imageAspect
            displayedLeft = (viewWidth - displayedWidth) / 2f
            displayedTop = 0f
        }

        val displayedRight = displayedLeft + displayedWidth
        val displayedBottom = displayedTop + displayedHeight
        if (touchX < displayedLeft || touchX > displayedRight || touchY < displayedTop || touchY > displayedBottom) {
            return null
        }

        val xRatio = ((touchX - displayedLeft) / displayedWidth).coerceIn(0f, 1f)
        val yRatio = ((touchY - displayedTop) / displayedHeight).coerceIn(0f, 1f)
        return CoordinateRatio(xRatio, yRatio)
    }
}
