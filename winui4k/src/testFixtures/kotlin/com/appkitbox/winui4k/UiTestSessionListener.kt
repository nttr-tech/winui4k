package com.appkitbox.winui4k

import org.junit.platform.launcher.LauncherSession
import org.junit.platform.launcher.LauncherSessionListener

/**
 * Calls [UiTestHarness.shutdown] when the entire JUnit Platform run (including both the Kotest and JUnit Jupiter engines)
 * finishes. Registered in META-INF/services/org.junit.platform.launcher.LauncherSessionListener.
 *
 * The WinUI message loop can be started only once per JVM. Closing it in per-engine teardown (such as Kotest's
 * afterProject) would leave tests in engines that run later unable to use WinUI, so it is closed at session end, which
 * runs after all engines.
 */
class UiTestSessionListener : LauncherSessionListener {
    override fun launcherSessionClosed(session: LauncherSession) {
        UiTestHarness.shutdown()
    }
}
