import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        MainViewControllerKt.registerNativeBridge(bridge: NativeBridge.shared)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onChange(of: scenePhase) { _, newPhase in
                    if newPhase == .active {
                        MainViewControllerKt.notifyAppForeground()
                    }
                }
        }
    }
}
