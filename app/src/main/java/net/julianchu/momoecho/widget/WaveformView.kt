package net.julianchu.momoecho.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.annotation.FloatRange
import net.julianchu.momoecho.R
import net.julianchu.momoecho.model.AmplitudeDiagram
import net.julianchu.momoecho.utils.ViewUtil
import kotlin.math.min

private const val DEFAULT_SAMPLE_POINT_DRAWING_SIZE = 0.1f

private const val MAX_VALUE = Short.MAX_VALUE
private const val MIN_VALUE = Short.MIN_VALUE

class WaveformView : View {

    private val toPixelUnit = ViewUtil.convertDpToPixel(context, 1.0f)

    /* How much of screen size to draw a point of sample, in dp */
    private var sizePerSamplePoint = DEFAULT_SAMPLE_POINT_DRAWING_SIZE

    private val fgLeftPaint = Paint().also {
        it.color = Color.argb(0xFF, 0xFF, 0x44, 0x44)
        it.strokeWidth = 2f.dpToPx() // dpToPx should be called after toPixelUnit initialized
    }
    private val fgRightPaint = Paint().also { it.color = Color.GREEN }

    private var amplitudeDiagram: AmplitudeDiagram? = null
    private val tmpRect = Rect()
    private var drawingPointsCache = FloatArray(0)
    private var leftDrawData: FloatArray = FloatArray(0)
    private var rightDrawData: FloatArray = FloatArray(0)

    @FloatRange(from = 0.01, to = 1.0)
    private var precision = 1.0f

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    constructor(
        context: Context, attrs: AttributeSet, defStyle: Int
    ) : super(context, attrs, defStyle) {
        val array = context.obtainStyledAttributes(attrs, R.styleable.WaveformView)
        sizePerSamplePoint = array.getFloat(
            R.styleable.WaveformView_resolution,
            DEFAULT_SAMPLE_POINT_DRAWING_SIZE
        )

        precision = array.getFloat(R.styleable.WaveformView_precision, 1.0f)

        val color = array.getColor(R.styleable.WaveformView_tint, Color.rgb(0xA0, 0x06, 0x3A))
        val strokeWidth = array.getDimensionPixelSize(
            R.styleable.WaveformView_stroke_width,
            fgLeftPaint.strokeWidth.toInt()
        )
        fgLeftPaint.color = color
        fgLeftPaint.strokeWidth = strokeWidth.toFloat()

        array.recycle()
    }

    fun setDiagram(amplitudeDiagram: AmplitudeDiagram) {
        this.amplitudeDiagram = amplitudeDiagram
        render()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val suggestedWidth = suggestedMinimumWidth + paddingStart + paddingEnd
        val suggestedHeight = suggestedMinimumHeight + paddingTop + paddingEnd
        val measuredWidth = measureDimensionWidth(suggestedWidth, widthMeasureSpec)
        val measuredHeight = measureDimensionHeight(suggestedHeight, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private fun measureDimensionWidth(suggestedSize: Int, measureSpec: Int): Int {
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        return when (specMode) {
            MeasureSpec.EXACTLY -> specSize
            MeasureSpec.AT_MOST -> min(specSize, suggestedSize)
            else -> suggestedSize
        }
    }

    private fun measureDimensionHeight(suggestedSize: Int, measureSpec: Int): Int {
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        return when (specMode) {
            MeasureSpec.EXACTLY -> specSize
            MeasureSpec.AT_MOST -> min(specSize, suggestedSize)
            else -> calculateDimensionByParams()
        }
    }

    private fun calculateDimensionByParams(): Int {
        return (sizePerSamplePoint.dpToPx() * (leftDrawData.size / 2)).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.getClipBounds(tmpRect)
//        canvas.drawRect(tmpRect, bgPaint)

        // draw left channel only
        val center = (width / 2)
        val yOffset = height.toFloat() / leftDrawData.size
        for (i in leftDrawData.indices) {
            // x
            drawingPointsCache[i * 2] = center + center * leftDrawData[i]
            drawingPointsCache[i * 2 + 1] = (i * yOffset)
        }

        canvas.drawLines(drawingPointsCache, fgLeftPaint)
    }

    fun getResolution(): Float = sizePerSamplePoint

    fun setResolution(resolution: Float) {
        sizePerSamplePoint = resolution
        render()
    }

    fun setPrecision(@FloatRange(from = 0.01, to = 1.0) precision: Float) {
        this.precision = when {
            precision < 0.01 -> 0.01f
            precision > 1 -> 1f
            else -> precision
        }
        render()
    }

    private fun render() {
        val amplitude = amplitudeDiagram ?: return

        leftDrawData = normalize(amplitude.left, precision)
        rightDrawData = normalize(amplitude.right, precision)
        drawingPointsCache = FloatArray(leftDrawData.size * 2)

        requestLayout()
        invalidate()
    }

    /**
     * Amplitude to value of (-1.0 ~ 1.0)
     */
    private fun normalize(rawData: IntArray, precision: Float): FloatArray {
        // if precision < 1, it means we don't want to draw 100% of rawData.
        // ie. for precision = 0.75, we only want to draw 75% of rawData
        val size = (rawData.size * precision).toInt()
        val drawData = FloatArray(size)

        for (i in drawData.indices) {
            var rawIdx = (i / precision).toInt()
            rawIdx = min(rawIdx, rawData.size - 1)
            drawData[i] = when {
                rawData[rawIdx] > MAX_VALUE -> 1f
                rawData[rawIdx] < MIN_VALUE -> -1f
                else -> rawData[rawIdx].toFloat() / MAX_VALUE
            }
        }
        return drawData
    }

    private fun Float.dpToPx() = this * toPixelUnit
}
