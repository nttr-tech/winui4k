package com.appkitbox.winui4k.extension.ribbon.model

import io.kotest.core.spec.style.FunSpec
import java.util.concurrent.Executor

/**
 * For the duration of this spec, delivers model change notifications synchronously on the thread that made the change.
 * Even if views from E2E tests that ran earlier in the same JVM have set [RibbonNotifications.dispatcher], which
 * routes notifications to the UI thread, model-only tests can receive and verify notifications off the UI thread.
 */
fun FunSpec.synchronousModelNotifications() {
    var saved: Executor? = null
    beforeSpec {
        saved = RibbonNotifications.dispatcher
        RibbonNotifications.dispatcher = null
    }
    afterSpec { RibbonNotifications.dispatcher = saved }
}
