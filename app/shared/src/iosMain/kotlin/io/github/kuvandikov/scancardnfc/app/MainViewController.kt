package io.github.kuvandikov.scancardnfc.app

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import io.github.kuvandikov.scancardnfc.scanner.IosNfcScanner
import platform.UIKit.UIViewController

@Suppress("FunctionName", "unused") // Called from Swift
fun MainViewController(): UIViewController = ComposeUIViewController {
    App(remember { IosNfcScanner() })
}
