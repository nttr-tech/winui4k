package com.appkitbox.winui4k.extension.ribbon.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/** Tests for item size definitions (RibbonSizeDefinition). Ported and extended from RibbonSpace's SimplifiedLayoutTests.Size_definition_parsing. */
class RibbonSizeDefinitionTest : FunSpec() {
    init {
        test("parses \"Large, Middle, Small\" and the like, filling missing values with the last one") {
            RibbonSizeDefinition.parse("Large, Middle, Small") shouldBe RibbonSizeDefinition.LARGE_MEDIUM_SMALL
            RibbonSizeDefinition.parse("large") shouldBe RibbonSizeDefinition.ALWAYS_LARGE
            RibbonSizeDefinition.parse("Large,Small") shouldBe
                RibbonSizeDefinition(RibbonItemSize.LARGE, RibbonItemSize.SMALL, RibbonItemSize.SMALL)
            RibbonSizeDefinition.parse("Medium;Small") shouldBe RibbonSizeDefinition.MEDIUM_SMALL
        }

        test("an invalid string yields null from tryParse and throws from parse") {
            RibbonSizeDefinition.tryParse("Huge").shouldBeNull()
            RibbonSizeDefinition.tryParse("Large, Medium, Small, Small").shouldBeNull()
            RibbonSizeDefinition.tryParse(" ").shouldBeNull()
            RibbonSizeDefinition.tryParse(null).shouldBeNull()
            shouldThrow<IllegalArgumentException> { RibbonSizeDefinition.parse("Huge") }
        }

        test("the size for a group state is large when collapsed (inside the popup)") {
            RibbonSizeDefinition.LARGE_MEDIUM_SMALL.getSize(RibbonGroupState.LARGE) shouldBe RibbonItemSize.LARGE
            RibbonSizeDefinition.LARGE_MEDIUM_SMALL.getSize(RibbonGroupState.MEDIUM) shouldBe RibbonItemSize.MEDIUM
            RibbonSizeDefinition.LARGE_MEDIUM_SMALL.getSize(RibbonGroupState.SMALL) shouldBe RibbonItemSize.SMALL
            RibbonSizeDefinition.LARGE_MEDIUM_SMALL.getSize(RibbonGroupState.COLLAPSED) shouldBe RibbonItemSize.LARGE
        }

        test("default size definitions for preferred sizes") {
            RibbonSizeDefinition.forPreferredSize(RibbonItemSize.LARGE) shouldBe RibbonSizeDefinition.LARGE_MEDIUM_SMALL
            RibbonSizeDefinition.forPreferredSize(RibbonItemSize.MEDIUM) shouldBe RibbonSizeDefinition.MEDIUM_SMALL
            RibbonSizeDefinition.forPreferredSize(RibbonItemSize.SMALL) shouldBe RibbonSizeDefinition.ALWAYS_SMALL
        }

        test("the string form matches RibbonSpace and parses back to the original") {
            RibbonSizeDefinition.LARGE_MEDIUM_SMALL.toString() shouldBe "Large, Medium, Small"
            RibbonSizeDefinition.parse(RibbonSizeDefinition.ALWAYS_MEDIUM.toString()) shouldBe RibbonSizeDefinition.ALWAYS_MEDIUM
        }
    }
}
