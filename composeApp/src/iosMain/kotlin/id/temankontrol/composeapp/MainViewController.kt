package id.temankontrol.composeapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import id.temankontrol.composeapp.data.KmpAppStore
import id.temankontrol.composeapp.di.initKoin
import id.temankontrol.composeapp.platform.IosNativeBridgeHolder
import id.temankontrol.composeapp.platform.IosNativeMediaAndShareBridge
import id.temankontrol.composeapp.ui.TemanKontrolCrossPlatformApp
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import platform.UIKit.UIViewController

private object IosAppHolder : KoinComponent {
    var initialized = false
    var externalLockRequested by mutableStateOf(false)

    fun ensureStore(): KmpAppStore {
        if (!initialized) {
            initKoin()
            initialized = true
        }
        return get()
    }
}

fun registerNativeBridge(bridge: IosNativeMediaAndShareBridge) {
    IosNativeBridgeHolder.bridge = bridge
}

fun notifyAppForeground() {
    if (IosAppHolder.initialized) {
        val store = IosAppHolder.ensureStore()
        store.onAppForeground()
        if (store.state.value.appLockEnabled) {
            IosAppHolder.externalLockRequested = true
        }
    }
}

fun MainViewController(): UIViewController {
    val store = IosAppHolder.ensureStore()
    return ComposeUIViewController {
        TemanKontrolCrossPlatformApp(
            store = store,
            isLockedExternal = IosAppHolder.externalLockRequested,
            onUnlockExternal = { IosAppHolder.externalLockRequested = false }
        )
    }
}
