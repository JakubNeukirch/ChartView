package pl.kuben.chartview.view

/**
 * @param count - the value to be added in chart
 * @param date - the timestamp value of adding date
 */
data class Entry(
        val count: Int,
        val date: Long
)