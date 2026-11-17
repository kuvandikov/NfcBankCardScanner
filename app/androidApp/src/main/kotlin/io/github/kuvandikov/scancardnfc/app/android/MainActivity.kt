package io.github.kuvandikov.scancardnfc.app.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.kuvandikov.scancardnfc.app.App
import io.github.kuvandikov.scancardnfc.scanner.AndroidNfcScanner

class MainActivity : ComponentActivity() {

    private val scanner by lazy { AndroidNfcScanner(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { App(scanner) }
    }
}
