package com.appkitbox.winui4k

import io.kotest.core.config.AbstractProjectConfig

/**
 * Kotest's project config (auto-discovered via a classpath scan).
 * Closes [UiTestHarness]'s shared window after every spec finishes, ending the message loop.
 */
class ProjectConfig : AbstractProjectConfig() {
    override suspend fun afterProject() {
        UiTestHarness.shutdown()
    }
}
