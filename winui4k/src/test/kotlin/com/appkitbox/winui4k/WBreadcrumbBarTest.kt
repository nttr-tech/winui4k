package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Tests verifying WBreadcrumbBar's hierarchy setting (ItemClicked is excluded, since it originates from user interaction). */
class WBreadcrumbBarTest : FunSpec() {
    init {
        test("items returns exactly the hierarchy that was set via setItems") {
            onUiThreadGet {
                val breadcrumbBar = WBreadcrumbBar()
                breadcrumbBar.setItems(listOf("C:", "Users", "hisano"))
                breadcrumbBar.items
            } shouldBe listOf("C:", "Users", "hisano")
        }

        test("calling setItems again replaces the hierarchy, and items reflects the replacement") {
            onUiThreadGet {
                val breadcrumbBar = WBreadcrumbBar()
                breadcrumbBar.setItems(listOf("C:", "Users"))
                breadcrumbBar.setItems(listOf("D:"))
                breadcrumbBar.items
            } shouldBe listOf("D:")
        }
    }
}
