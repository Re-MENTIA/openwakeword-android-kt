package com.rementia.openwakeword

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.rementia.openwakeword.lib.WakeWordEngine
import com.rementia.openwakeword.lib.model.WakeWordModel
import kotlinx.coroutines.launch

/**
 * Sample app demonstrating wake word detection using the OpenWakeWord library.
 */
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val PERMISSION_REQUEST_RECORD_AUDIO = 200
    }
    
    private lateinit var scoreTextView: TextView
    private lateinit var statusTextView: TextView
    private var wakeWordEngine: WakeWordEngine? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        scoreTextView = findViewById(R.id.scoreTextView)
        statusTextView = findViewById(R.id.statusTextView)
        
        if (checkAndRequestPermissions()) {
            startWakeWordDetection()
        }
    }
    
    private fun checkAndRequestPermissions(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_RECORD_AUDIO
            )
            false
        } else {
            true
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_RECORD_AUDIO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startWakeWordDetection()
                } else {
                    Toast.makeText(
                        this,
                        "Audio recording permission is required for wake word detection",
                        Toast.LENGTH_LONG
                    ).show()
                    statusTextView.text = "Permission denied"
                }
            }
        }
    }
    
    private fun startWakeWordDetection() {
        statusTextView.text = "Starting wake word detection..."
        
        // Configure wake word models
        val models = listOf(
            WakeWordModel(
                name = "Hello World",
                modelPath = "hello_world.onnx",
                threshold = 0.5f
            )
        )
        
        // Create and start the engine
        wakeWordEngine = WakeWordEngine(
            context = this,
            models = models,
            scope = lifecycleScope
        )
        
        // Collect detection events
        lifecycleScope.launch {
            wakeWordEngine?.detections?.collect { detection ->
                runOnUiThread {
                    scoreTextView.text = String.format("Score: %.5f", detection.score)
                    statusTextView.text = "Wake Word Detected!"
                    
                    Toast.makeText(
                        this@MainActivity,
                        "${detection.model.name} detected!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        
        // Start detection
        wakeWordEngine?.start()
        statusTextView.text = "Listening for wake words..."
    }
    
    override fun onDestroy() {
        super.onDestroy()
        wakeWordEngine?.release()
    }
}