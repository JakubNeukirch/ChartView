package pl.kuben.chart

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import pl.kuben.chartview.view.Entry

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val dates = listOf(
                System.currentTimeMillis(),
                System.currentTimeMillis() + 1000 * 60 * 60 * 24,
                System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 2,
                System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 3
        )
        chart.fromDate = dates[1]
        chart.toDate = dates[3]
        chart.entries = listOf(
                Entry(2, dates[0]),
                Entry(4, dates[1]),
                Entry(5, dates[2]),
                Entry(1, dates[3])
        )
    }
}
