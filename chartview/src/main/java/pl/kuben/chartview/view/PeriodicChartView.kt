package pl.kuben.chartview.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import pl.kuben.chartview.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

private const val GROUP_BY_DAY: Long = 3600000 * 24
private const val GROUP_BY_WEEK: Long = 3600000 * 24 * 7
private const val GROUP_BY_MONTH: Long = 3600000 * 24 * 30L
private const val GROUP_BY_SECOND: Long = 1000L
private const val MARGIN_HEIGHT_DIVIDER = 8
private const val FRAME_STROKE_WIDTH = 3f
private const val PROGRESS_STROKE_WIDTH = 8f
private const val POINTS_COUNT = 8
private const val LINES_COUNT = 6
private const val TEXT_MARGIN = 10

private const val NO_LIMITS = -1L

class PeriodicChartView : View {

    @SuppressLint("SimpleDateFormat")
    var dateFormat = SimpleDateFormat("dd.MM")
        set(value) {
            field = value
            redraw()
        }

    var entries: List<Entry> = listOf()
        set(value) {
            field = value
            redraw()
        }
    var fromDate = NO_LIMITS
        set(value) {
            field = value
            redraw()
        }
    var toDate = NO_LIMITS
        set(value) {
            field = value
            redraw()
        }

    var valueText: String = context.getString(R.string.value)
        set(value) {
            field = value
            redraw()
        }
    var dateText: String = context.getString(R.string.date)
        set(value) {
            field = value
            redraw()
        }

    var progressColor = Color.BLUE
        set(value) {
            field = value
            progressPaint.color = value
            fillPaint.color = value
            redraw()
        }

    var padding = 10
        set(value) {
            field = value
            redraw()
        }

    var showText = true
        set(value) {
            field = value
            redraw()
        }

    private var points = listOf<Point>()

    private var progressBackgroundColor: Int = ContextCompat.getColor(context, R.color.standardBackground)

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            .apply {
                color = ContextCompat.getColor(context, R.color.frame)
                style = Paint.Style.STROKE
                strokeWidth = FRAME_STROKE_WIDTH
            }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            .apply {
                color = progressColor
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                strokeMiter = 5f
                strokeWidth = PROGRESS_STROKE_WIDTH
            }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            .apply {
                color = Color.WHITE
                strokeWidth = 0f
                style = Paint.Style.FILL
            }

    private val clearPaint = Paint()
            .apply {
                color = progressBackgroundColor
            }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            .apply {
                textSize = context.resources.getDimension(R.dimen.text_small_size)
                color = ContextCompat.getColor(context, R.color.textColor)
            }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            .apply {
                color = Color.BLACK
                strokeWidth = 2f
                style = Paint.Style.STROKE
                xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            }

    private var groupBy = GROUP_BY_SECOND

    private var bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progressPath = Path()

    private var pointRadius = 7f
    private var maxValue = 0
    private var space: Float = 0f
    private var verticalSpace = 0f

    private var innerMarginLeft = 0
    private var innerMarginBottom = 50
    private var progressMarginTop = 50

    private var framePadding = 5f
    private var canvasBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    private var canvas = Canvas(canvasBitmap)
    private var bitmapMatrix = Matrix()

    private var groupedEntries = hashMapOf<Long, List<Entry>>()
    private var displayEntries = hashMapOf<Long, List<Entry>>()

    private var shouldInvalidate = false

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val array = context.obtainStyledAttributes(attrs, R.styleable.PeriodicChartView, 0, 0)
        valueText = array.getString(R.styleable.PeriodicChartView_valueText) ?: context.getString(R.string.value)
        dateText = array.getString(R.styleable.PeriodicChartView_dateText) ?: context.getString(R.string.date)
        progressColor = array.getColor(R.styleable.PeriodicChartView_progressColor, Color.BLUE)
        padding = array.getDimensionPixelSize(R.styleable.PeriodicChartView_chartPadding, 10)
        showText = array.getBoolean(R.styleable.PeriodicChartView_showText, true)
        groupBy = when (array.getInt(R.styleable.PeriodicChartView_dateInterval, 1)) {
            1 -> GROUP_BY_DAY
            2 -> GROUP_BY_WEEK
            3 -> GROUP_BY_MONTH
            else -> GROUP_BY_DAY
        }
        array.recycle()
        shouldInvalidate = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(canvasBitmap, bitmapMatrix, bitmapPaint)
    }

    private fun redraw() {
        invalidateData()
        canvas.drawColor(progressBackgroundColor)
        drawProgress()
        drawPoints()
        drawFrame()
        drawScale()
        drawLabels()
        if (shouldInvalidate) {
            invalidate()
        }
    }

    private fun drawFrame() {
        val path = Path()
        val halfFrameStroke = framePaint.strokeWidth / 2
        val left = innerMarginLeft - halfFrameStroke
        val bottom = canvas.height - (innerMarginBottom - halfFrameStroke)
        with(path) {
            moveTo(left, framePadding)
            lineTo(left, bottom)
            lineTo(canvas.width - framePadding, bottom)
        }
        val fillPath = Path(path).apply {
            lineTo(canvas.width.toFloat(), bottom)
            lineTo(canvas.width.toFloat(), canvas.height.toFloat())
            lineTo(0f, canvas.height.toFloat())
            lineTo(0f, 0f)
            lineTo(left, 0f)
            lineTo(left, framePadding)
        }
        canvas.drawPath(fillPath, clearPaint)
        canvas.drawPath(path, framePaint)
    }

    private fun drawScale() {
        var y: Float
        if (showText) {
            val bounds = Rect()
            textPaint.getTextBounds(valueText, 0, valueText.length, bounds)
            canvas.drawText(valueText, padding.toFloat(), padding + bounds.height().toFloat(), textPaint)
        }
        getValues().forEachIndexed { index, value ->
            y = canvas.height - (index * verticalSpace) - innerMarginBottom
            if (showText) {
                canvas.drawText(value, padding.toFloat(), y, textPaint)
            }
            canvas.drawLine(innerMarginLeft.toFloat(), y, canvas.width.toFloat(), y, linePaint)
        }
    }

    private fun calculateMargin() {
        val bounds = Rect()
        var max: Int = getValues().map {
            textPaint.getTextBounds(it, 0, it.length, bounds)
            bounds.width()
        }
                .max() ?: 0

        textPaint.getTextBounds(valueText, 0, valueText.length, bounds)
        max = if (max < bounds.width()) bounds.width() else max
        max += padding + TEXT_MARGIN
        innerMarginLeft = if (innerMarginLeft < max) max else innerMarginLeft
    }

    private fun getValues(): List<String> {
        val spaceValue = maxValue / LINES_COUNT.toDouble()
        val list = arrayListOf<String>()
        var value: String
        for (i in 0..(LINES_COUNT)) {
            value = "${Math.round(spaceValue * i * 10) / 10.0}"
            list.add(value)
        }
        return list
    }

    private fun drawLabels() {
        if (displayEntries.isNotEmpty() && showText) {
            var x: Float
            val y = canvas.height.toFloat()
            val bounds = Rect()
            var text: String
            for (i in 0..(min(displayEntries.size, POINTS_COUNT) - 1)) {
                text = (displayEntries.keys.toList()[i] * groupBy).toDateString()
                textPaint.getTextBounds(text, 0, text.length, bounds)
                x = innerMarginLeft + (i * space)
                canvas.drawText(
                        text,
                        x - (bounds.width() / 2),
                        y,
                        textPaint
                )
            }
            textPaint.getTextBounds(dateText, 0, dateText.length, bounds)
            canvas.drawText(dateText, canvas.width - bounds.width().toFloat(), y, textPaint)
        }
    }

    private fun drawPoints() {
        points.forEach {
            canvas.drawCircle(it.x, it.y, pointRadius, fillPaint)
            canvas.drawCircle(it.x, it.y, pointRadius + 1f, progressPaint)
        }
    }

    private fun drawProgress() {
        canvas.drawPath(progressPath, progressPaint)
    }

    private fun invalidateData() {
        val limited = toDate != NO_LIMITS && fromDate != NO_LIMITS
        groupedEntries = entries.asSequence()
                .filter { !limited || it.date in fromDate..toDate }
                .groupBy { it.date / groupBy } as HashMap<Long, List<Entry>>
        calculateDisplayEntries()
        val entriesCount = countEntries()
        maxValue = entriesCount.max() ?: 0
        if (showText) {
            calculateMargin()
        }
        points = calculatePoints(entriesCount)
        calculateProgressPath()
    }

    private fun calculateDisplayEntries() {
        if (groupedEntries.size <= POINTS_COUNT) {
            displayEntries = groupedEntries
        } else {
            val count = groupedEntries.size / POINTS_COUNT
            var rest = groupedEntries.size % POINTS_COUNT
            displayEntries = hashMapOf()
            var add = 0
            var entryItem: Pair<Long, List<Entry>>
            var toAddItem = listOf<Entry>()
            for (i in 0 until groupedEntries.size) {
                entryItem = groupedEntries.getAt(i)
                if (add == 0) {
                    toAddItem = entryItem.second
                }
                toAddItem.toMutableList().addAll(entryItem.second)
                add++
                if (add == count) {
                    if (rest > 0) {
                        rest--
                    } else {
                        displayEntries[entryItem.first] = toAddItem
                        add = 0
                    }
                } else if (add > count) {
                    displayEntries[entryItem.first] = toAddItem
                    add = 0
                }
            }
        }
    }

    private fun countEntries(): List<Int> {
        return displayEntries
                .map {
                    it.value.sumBy { entry ->
                        entry.count
                    }
                }
    }

    private fun calculatePoints(entriesCount: List<Int>): List<Point> {
        var count = 0
        var partOfMax: Float
        var bottom: Int
        var height: Int
        return entriesCount
                .asSequence()
                .map {
                    partOfMax = (it / maxValue.toFloat())
                    bottom = (canvasBitmap.height - innerMarginBottom)
                    height = bottom - progressMarginTop
                    Point(
                            innerMarginLeft + (count++ * space),
                            bottom - (partOfMax * height)
                    )
                }.toList()
    }

    private fun calculateProgressPath() {
        progressPath = Path()
        if (points.isNotEmpty()) {
            progressPath.moveTo(points.first().x, points.first().y)
            points.forEach {
                progressPath.lineTo(it.x, it.y)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w - 2 * padding, h - 2 * padding, Bitmap.Config.ARGB_8888)
        canvas = Canvas(canvasBitmap)
        bitmapMatrix.setRectToRect(
                RectF(0f, 0f, canvasBitmap.width.toFloat(), canvasBitmap.height.toFloat()),
                RectF(padding.toFloat(), padding.toFloat(), width - 2f * padding, height - 2f * padding),
                Matrix.ScaleToFit.START
        )
        space = (canvasBitmap.width - innerMarginLeft) / POINTS_COUNT.toFloat()
        val height = (canvasBitmap.height - innerMarginBottom)
        progressMarginTop = Math.round(height.toFloat() / MARGIN_HEIGHT_DIVIDER)
        verticalSpace = (height - progressMarginTop) / LINES_COUNT.toFloat()
        redraw()
    }

    private fun HashMap<Long, List<Entry>>.getAt(position: Int): Pair<Long, List<Entry>> {
        val key = this.keys.toList()[position]
        return Pair(key, this[key] ?: listOf())
    }

    @SuppressLint("SwitchIntDef")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = context.resources.getDimensionPixelSize(R.dimen.minimum_chart_size)
        val desiredHeight = context.resources.getDimensionPixelSize(R.dimen.minimum_chart_size)

        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> Math.min(desiredWidth, widthSize)
            else -> desiredWidth
        }
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> Math.min(desiredHeight, heightSize)
            else -> desiredHeight
        }
        setMeasuredDimension(width, height)
    }

    private fun Long.toDateString(): String {
        val date = Date()
        date.time = this
        return dateFormat.format(date)
    }
}