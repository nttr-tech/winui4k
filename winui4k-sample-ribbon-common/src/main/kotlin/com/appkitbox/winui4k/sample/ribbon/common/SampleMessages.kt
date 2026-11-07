package com.appkitbox.winui4k.sample.ribbon.common

import java.io.InputStreamReader
import java.text.MessageFormat
import java.util.Locale
import java.util.PropertyResourceBundle
import java.util.ResourceBundle

/**
 * Reads the strings shown on screen (ResourceBundle's `messages.properties` = English, `messages_ja.properties` = Japanese)
 * by the OS display language ([Locale.Category.DISPLAY]). Languages other than Japanese get English.
 * A missing key does not throw and returns the key itself (so it is noticed on screen).
 * If [parent] is given, keys missing here are read from [parent] (from the app's strings to the strings shared by the ribbon demos).
 */
open class SampleMessages(
    /** The bundle name (e.g. `com.appkitbox.winui4k.sample.ribbon.word.messages`). */
    val baseName: String,
    /** Where keys missing here are read from. */
    private val parent: SampleMessages? = null,
    /** The display language. */
    val locale: Locale = Locale.getDefault(Locale.Category.DISPLAY),
) {
    private val bundle: ResourceBundle by lazy {
        ResourceBundle.getBundle(baseName, locale, SampleMessages::class.java.classLoader, Utf8Control)
    }

    /** Whether [key] exists (here or in [parent]). */
    fun contains(key: String): Boolean = bundle.containsKey(key) || parent?.contains(key) == true

    /** The string for [key]. */
    fun get(key: String): String = if (bundle.containsKey(key)) bundle.getString(key) else parent?.get(key) ?: key

    /** The string for [key] with [args] filled into `{0}` etc. (the [MessageFormat] syntax; write `'` as `''`). */
    fun get(key: String, vararg args: Any?): String {
        val pattern = get(key)
        return if (args.isEmpty()) pattern else MessageFormat(pattern, locale).format(args)
    }

    /** Reads properties as UTF-8 (Java 8's default is ISO-8859-1) and falls back to English (the default bundle) without being pulled to the OS default locale. */
    private object Utf8Control : ResourceBundle.Control() {
        override fun getFormats(baseName: String): List<String> = FORMAT_PROPERTIES

        override fun getFallbackLocale(baseName: String, locale: Locale): Locale? = null

        override fun newBundle(baseName: String, locale: Locale, format: String, loader: ClassLoader, reload: Boolean): ResourceBundle? {
            val name = toResourceName(toBundleName(baseName, locale), "properties")
            val stream = loader.getResourceAsStream(name) ?: return null
            return InputStreamReader(stream, Charsets.UTF_8).use { PropertyResourceBundle(it) }
        }
    }
}

/** The strings shared by the ribbon demos (Word / Excel / PowerPoint / CAD / tools). Used as the [SampleMessages.parent] of each app's Messages. */
object RibbonSampleMessages : SampleMessages("com.appkitbox.winui4k.sample.ribbon.common.messages")
