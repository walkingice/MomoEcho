package net.julianchu.momoecho.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import net.julianchu.momoecho.R
import net.julianchu.momoecho.utils.ViewUtil

private const val MAX = 100
private const val MIN = 0

private typealias OnSeekBarChangeListener = (VerticalSeekBar, Int, Boolean) -> Unit

class VerticalSeekBar : View {

    private var tickSize = 15f
    private var touchDownStart = 0f

    private var highlightRange = Point(0, 0)

    private var segments: Array<Int> = emptyArray()

    private val toPixelUnit = ViewUtil.convertDpToPixel(context, 1.0f)

    private var seekBarChangeListener: OnSeekBarChangeListener? = null

    private val linePaint = Paint().also {
        it.color = Color.LTGRAY
        it.strokeWidth = 2f.dpToPx()
    }

    private val highlightPaint = Paint().also { it.color = Color.argb(0x33, 0xFF, 0xFF, 0) }

    private val tickEnabledPaint = Paint().also { it.color = Color.RED }
    private val tickDisabledPaint = Paint().also { it.color = Color.GRAY }

    private var _progress: Int = 0
    var progress: Int
        get() {
            return _progress
        }
        set(value) {
            val oldValue = _progress
            _progress = Math.min(value, max)
            _progress = Math.max(_progress, MIN)
            if (oldValue != _progress) {
                onProgressChanged(_progress, false)
            }
        }

    var max: Int = MAX

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    constructor(
        context: Context, attrs: AttributeSet, defStyle: Int
    ) : super(context, attrs, defStyle) {
        tickSize = tickSize.dpToPx()

        val array = context.obtainStyledAttributes(attrs, R.styleable.VerticalSeekBar)
        max = array.getInt(R.styleable.VerticalSeekBar_max, max)

        highlightPaint.color =
            array.getColor(R.styleable.VerticalSeekBar_highlight_color, highlightPaint.color)

        linePaint.color =
            array.getColor(R.styleable.VerticalSeekBar_bar_color, linePaint.color)
        linePaint.strokeWidth = array.getDimensionPixelSize(
            R.styleable.VerticalSeekBar_bar_width,
            linePaint.strokeWidth.toInt()
        ).toFloat()

        array.recycle()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val totalBarHeight = c.height.toFloat() - tickSize

        if (highlightRange.x != 0 || highlightRange.y != 0) {
            val highLightStart = totalBarHeight * (highlightRange.x.toFloat() / max)
            val highLightEnd = totalBarHeight * (highlightRange.y.toFloat() / max)
            c.drawRect(0.toFloat(), highLightStart, width.toFloat(), highLightEnd, highlightPaint)
        }

        val halfTickSize = tickSize / 2
        val x0 = c.width.toFloat() / 2
        val y0 = halfTickSize
        c.drawLine(x0, y0, x0, y0 + totalBarHeight, linePaint)

        if (segments.isNotEmpty()) {
            for (segment in segments) {
                val y = y0 + totalBarHeight * (segment.toFloat() / max)
                c.drawLine(x0 - halfTickSize, y, x0 + halfTickSize, y, linePaint)
            }
        }

        // draw tick
        c.drawCircle(
            x0,
            y0 + totalBarHeight * (progress.toFloat() / max),
            tickSize / 2,
            if (isEnabled) tickEnabledPaint else tickDisabledPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownStart = event.y
                startDrag(event)
            }
            MotionEvent.ACTION_MOVE -> trackTouchEvent(event)
            MotionEvent.ACTION_UP -> {
                trackTouchEvent(event)
                isPressed = false
            }
            MotionEvent.ACTION_CANCEL -> isPressed = false
        }
        return true
    }

    fun setOnSeekBarChangeListener(listener: OnSeekBarChangeListener) {
        this.seekBarChangeListener = listener
    }

    fun setHighLightRange(start: Int, end: Int) {
        var s = Math.max(start, 0)
        var e = Math.max(end, 0)
        s = Math.min(max, s)
        e = Math.min(max, e)
        if (s != 0 || e != 0) {
            highlightRange.x = s
            highlightRange.y = e
        }
        invalidate()
    }

    fun setSegments(segments: Array<Int>) {
        this.segments = segments
        invalidate()
    }

    private fun onProgressChanged(newProgress: Int, fromUser: Boolean) {
        seekBarChangeListener?.invoke(this, newProgress, fromUser)
        invalidate()
    }

    private fun startDrag(event: MotionEvent) {
        isPressed = true
        parent.requestDisallowInterceptTouchEvent(true)
    }

    private fun trackTouchEvent(event: MotionEvent) {
        val y = Math.round(event.y)
        val availableHeight: Int = height - paddingTop - paddingBottom

        val scale: Float = when {
            y < paddingTop -> 0.0f
            y > height - paddingBottom -> 1.0f
            else -> (y - paddingTop) / availableHeight.toFloat()
        }
        val range = max - MIN

        val oldProgress = _progress
        _progress = (scale * range + MIN).toInt()

        if (oldProgress != _progress) {
            onProgressChanged(_progress, true)
        }
    }

    private fun Float.dpToPx() = this * toPixelUnit
}
