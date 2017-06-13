package io.github.landerlyoung.droidstreamer

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById(R.id.stream_button)?.setOnClickListener {
            val intent = Intent(this, StreamingService::class.java)
            startService(intent)
        }
    }
}
