package com.bagolysz.arsheeps

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener {
            (ar_fragment as? EnhancedArFragment)?.onFabClick()
        }
    }

    fun setFabText(text: String) {
        fab.text = text
    }

    fun setFabEnabled(enabled: Boolean) {
        fab.isEnabled = enabled
    }

    fun setStartVisibility(visible: Boolean) {
        ready_text.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setFinishedVisibility(visible: Boolean) {
        finished_text.visibility = if (visible) View.VISIBLE else View.GONE
    }
}
