package com.onpointinfo.transrouter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Required to be a default SMS app.
 */
class ComposeSmsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This activity handles the "send to" intent. 
        // For a router app, it might just finish or show a simple UI.
        finish()
    }
}