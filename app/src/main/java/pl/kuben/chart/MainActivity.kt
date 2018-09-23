package pl.kuben.chart

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import pl.kuben.chartview.view.Entry

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        chart.entries = listOf(
                Entry(2, System.currentTimeMillis()),
                Entry(4, System.currentTimeMillis() + 1000 * 60 * 60 * 24),
                Entry(5, System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 2),
                Entry(1, System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 3)
        )
    }
}
