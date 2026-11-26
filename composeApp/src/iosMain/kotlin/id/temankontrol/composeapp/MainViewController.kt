package id.temankontrol.composeapp

import androidx.compose.ui.window.ComposeUIViewController
import id.temankontrol.composeapp.di.initKoin
import id.temankontrol.composeapp.ui.TemanKontrolSharedPreviewApp
import platform.UIKit.UIViewController

private var koinStarted = false

fun MainViewController(): UIViewController {
    if (!koinStarted) {
        initKoin()
        koinStarted = true
    }
    return ComposeUIViewController {
        TemanKontrolSharedPreviewApp()
    }
}
