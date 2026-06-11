package com.autoclock

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class UsageGuideActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage_guide)

        supportActionBar?.apply {
            title = "使用说明"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
