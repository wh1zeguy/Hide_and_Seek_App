package com.example.hide_and_seek

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Connect buttons from the layout
        val playButton = findViewById<Button>(R.id.startButton)
        val setupButton = findViewById<Button>(R.id.setupButton)
        val quitButton = findViewById<Button>(R.id.exitButton)

        playButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }
        setupButton.setOnClickListener {
            val intent = Intent(this, SetupActivity::class.java)
            startActivity(intent)
        }

        /*setupButton.setOnClickListener {
            val intent = Intent(this, SetupActivity::class.java)
            startActivity(intent)
        }*/

        quitButton.setOnClickListener {
            finishAffinity()
        }
    }
}
