package com.copincomics.copinapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class FActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_factivity)
        val finishButton = findViewById<Button>(R.id.button_finish)
        finishButton.setOnClickListener {
            finish();
        }
    }
}