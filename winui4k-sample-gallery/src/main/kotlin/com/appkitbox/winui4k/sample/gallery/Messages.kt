package com.appkitbox.winui4k.sample.gallery

import java.io.InputStreamReader
import java.text.MessageFormat
import java.util.Locale
import java.util.PropertyResourceBundle
import java.util.ResourceBundle

/**
 * The strings shown on the Gallery's screens (messages.properties = English, messages_ja.properties = Japanese).
 * The language is decided by the OS display language (`Locale.getDefault(Locale.Category.DISPLAY)`)
 * and can be changed with `-Duser.language=en` etc. Languages other than Japanese get English.
 */
internal object Messages {
    private const val BUNDLE_NAME = "com.appkitbox.winui4k.sample.gallery.messages"

    private val bundle: ResourceBundle by lazy { bundleFor(Locale.getDefault(Locale.Category.DISPLAY)) }

    /** The string for [key]. When the key is missing, returns the key itself so it is noticed on screen. */
    fun get(key: String): String = lookup(bundle, key)

    /** Fills [args] into `{0}` etc. of the string for [key] (the java.text.MessageFormat syntax). */
    fun get(key: String, vararg args: Any?): String = format(bundle, key, args)

    /** The bundle for the language of [locale]. Also used by the tests to check the contents per language. */
    fun bundleFor(locale: Locale): ResourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, locale, Utf8Control)

    /** Looks up [key] in [bundle] and fills in [args] if any. */
    fun format(bundle: ResourceBundle, key: String, args: Array<out Any?>): String {
        val pattern = lookup(bundle, key)
        if (args.isEmpty()) return pattern
        return MessageFormat(pattern, bundle.locale).format(args)
    }

    private fun lookup(bundle: ResourceBundle, key: String): String = if (bundle.containsKey(key)) bundle.getString(key) else key

    /**
     * Reads properties as UTF-8 (Java 8's ResourceBundle reads them as ISO-8859-1).
     * So that an OS in a language other than Japanese gets English (messages.properties) without being pulled to the OS default locale,
     * no fallback locale is searched.
     */
    private object Utf8Control : ResourceBundle.Control() {
        override fun getFormats(baseName: String): List<String> = FORMAT_PROPERTIES

        override fun getFallbackLocale(baseName: String, locale: Locale): Locale? = null

        override fun newBundle(
            baseName: String,
            locale: Locale,
            format: String,
            loader: ClassLoader,
            reload: Boolean,
        ): ResourceBundle? {
            val resourceName = toResourceName(toBundleName(baseName, locale), "properties")
            val stream = loader.getResourceAsStream(resourceName) ?: return null
            return InputStreamReader(stream, Charsets.UTF_8).use { PropertyResourceBundle(it) }
        }
    }
}
