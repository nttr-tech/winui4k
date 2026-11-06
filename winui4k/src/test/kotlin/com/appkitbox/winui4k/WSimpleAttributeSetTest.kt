package com.appkitbox.winui4k

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for [WSimpleAttributeSet] and [WStyleConstants] (no window is used).
 * The expected values are the values from the same operations with Swing's SimpleAttributeSet and StyleConstants.
 */
@Tags("Pure")
class WSimpleAttributeSetTest : FunSpec() {
    init {
        context("Defaults") {
            test("a newly created attribute collection is empty, its count is 0, and no key is defined") {
                val attrs = WSimpleAttributeSet()
                attrs.isEmpty.shouldBeTrue()
                attrs.attributeCount shouldBe 0
                attrs.isDefined(WStyleConstants.Bold).shouldBeFalse()
                attrs.getAttribute(WStyleConstants.Bold).shouldBeNull()
                attrs.attributeNames.hasMoreElements().shouldBeFalse()
            }

            test("reading keys without values with the StyleConstants getters gives the same defaults as Swing (false, 12, Monospaced, black)") {
                val attrs = WSimpleAttributeSet()
                WStyleConstants.isBold(attrs).shouldBeFalse()
                WStyleConstants.isItalic(attrs).shouldBeFalse()
                WStyleConstants.isUnderline(attrs).shouldBeFalse()
                WStyleConstants.getFontSize(attrs) shouldBe 12
                WStyleConstants.getFontFamily(attrs) shouldBe "Monospaced"
                WStyleConstants.getForeground(attrs) shouldBe WColor.BLACK
            }

            test("EMPTY is an empty, unmodifiable collection (not a MutableAttributeSet, as in Swing), and isEqual to an empty collection") {
                WSimpleAttributeSet.EMPTY.attributeCount shouldBe 0
                (WSimpleAttributeSet.EMPTY is WMutableAttributeSet).shouldBeFalse()
                WSimpleAttributeSet.EMPTY.isEqual(WSimpleAttributeSet()).shouldBeTrue()
                WSimpleAttributeSet().isEqual(WSimpleAttributeSet.EMPTY).shouldBeTrue()
            }
        }

        context("Reading and writing") {
            test("values put with the StyleConstants setters read back with the getters and getAttribute") {
                val attrs = WSimpleAttributeSet()
                WStyleConstants.setBold(attrs, true)
                WStyleConstants.setItalic(attrs, true)
                WStyleConstants.setUnderline(attrs, true)
                WStyleConstants.setForeground(attrs, WColor.RED)
                WStyleConstants.setFontSize(attrs, 20)
                WStyleConstants.setFontFamily(attrs, "Consolas")

                WStyleConstants.isBold(attrs).shouldBeTrue()
                WStyleConstants.isItalic(attrs).shouldBeTrue()
                WStyleConstants.isUnderline(attrs).shouldBeTrue()
                WStyleConstants.getForeground(attrs) shouldBe WColor.RED
                WStyleConstants.getFontSize(attrs) shouldBe 20
                WStyleConstants.getFontFamily(attrs) shouldBe "Consolas"
                attrs.getAttribute(WStyleConstants.FontSize) shouldBe 20
                attrs.attributeCount shouldBe 6
                attrs.attributeNames.toList().shouldContainExactlyInAnyOrder(
                    WStyleConstants.Bold,
                    WStyleConstants.Italic,
                    WStyleConstants.Underline,
                    WStyleConstants.Foreground,
                    WStyleConstants.FontSize,
                    WStyleConstants.FontFamily,
                )
            }

            test("putting a value again for the same key replaces it without increasing the count, and removeAttribute removes it") {
                val attrs = WSimpleAttributeSet()
                WStyleConstants.setBold(attrs, true)
                WStyleConstants.setBold(attrs, false)
                attrs.attributeCount shouldBe 1
                attrs.getAttribute(WStyleConstants.Bold) shouldBe false
                attrs.removeAttribute(WStyleConstants.Bold)
                attrs.isDefined(WStyleConstants.Bold).shouldBeFalse()
            }

            test("addAttributes adds all values of another collection, and removeAttributes(collection) removes only those with the same values") {
                val source = WSimpleAttributeSet().also {
                    WStyleConstants.setBold(it, true)
                    WStyleConstants.setFontSize(it, 18)
                }
                val attrs = WSimpleAttributeSet()
                WStyleConstants.setItalic(attrs, true)
                attrs.addAttributes(source)
                attrs.attributeCount shouldBe 3

                val toRemove = WSimpleAttributeSet().also {
                    WStyleConstants.setBold(it, true) // Same value → removed
                    WStyleConstants.setFontSize(it, 99) // Different value → kept
                }
                attrs.removeAttributes(toRemove)
                attrs.isDefined(WStyleConstants.Bold).shouldBeFalse()
                attrs.getAttribute(WStyleConstants.FontSize) shouldBe 18
            }

            test("removeAttributes(an Enumeration of names) removes those names regardless of their values") {
                val attrs = WSimpleAttributeSet()
                WStyleConstants.setBold(attrs, true)
                WStyleConstants.setItalic(attrs, true)
                attrs.removeAttributes(java.util.Collections.enumeration(listOf(WStyleConstants.Bold)))
                attrs.attributeCount shouldBe 1
                attrs.isDefined(WStyleConstants.Italic).shouldBeTrue()
            }

            test("the copy constructor and copyAttributes create a separate collection, and changing the original does not change the copy") {
                val attrs = WSimpleAttributeSet()
                WStyleConstants.setBold(attrs, true)
                val copy = WSimpleAttributeSet(attrs)
                val copied = attrs.copyAttributes()
                WStyleConstants.setBold(attrs, false)
                WStyleConstants.isBold(copy).shouldBeTrue()
                WStyleConstants.isBold(copied).shouldBeTrue()
            }

            test("containsAttribute, containsAttributes, isEqual and equals compare down to the values") {
                val a = WSimpleAttributeSet().also {
                    WStyleConstants.setBold(it, true)
                    WStyleConstants.setFontSize(it, 16)
                }
                val b = WSimpleAttributeSet().also {
                    WStyleConstants.setFontSize(it, 16)
                    WStyleConstants.setBold(it, true)
                }
                val sub = WSimpleAttributeSet().also { WStyleConstants.setBold(it, true) }
                a.containsAttribute(WStyleConstants.Bold, true).shouldBeTrue()
                a.containsAttribute(WStyleConstants.Bold, false).shouldBeFalse()
                a.containsAttributes(sub).shouldBeTrue()
                sub.containsAttributes(a).shouldBeFalse()
                a.isEqual(b).shouldBeTrue()
                a shouldBe b
                a.hashCode() shouldBe b.hashCode()
                a shouldNotBe sub
            }
        }

        context("Boundaries and exceptions") {
            test("reading a key whose value has a different type with a StyleConstants getter throws IllegalArgumentException") {
                val attrs = WSimpleAttributeSet()
                attrs.addAttribute(WStyleConstants.FontSize, "large")
                shouldThrow<IllegalArgumentException> { WStyleConstants.getFontSize(attrs) }
            }
        }

        context("Swing compatibility") {
            test("the toString of the keys returns the same names as Swing's StyleConstants") {
                WStyleConstants.Bold.toString() shouldBe "bold"
                WStyleConstants.Italic.toString() shouldBe "italic"
                WStyleConstants.Underline.toString() shouldBe "underline"
                WStyleConstants.Foreground.toString() shouldBe "foreground"
                WStyleConstants.FontSize.toString() shouldBe "size"
                WStyleConstants.FontFamily.toString() shouldBe "family"
            }
        }
    }
}
