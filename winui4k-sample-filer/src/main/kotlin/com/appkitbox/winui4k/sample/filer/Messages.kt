package com.appkitbox.winui4k.sample.filer

import java.io.InputStreamReader
import java.text.MessageFormat
import java.util.Locale
import java.util.PropertyResourceBundle
import java.util.ResourceBundle

/**
 * Looks up the strings shown on screen by the OS display language. Japanese uses messages_ja.properties,
 * and other languages use the English messages.properties (can be changed with `-Duser.language=en` etc.).
 */
internal object Messages {
    private const val BUNDLE_NAME = "com.appkitbox.winui4k.sample.filer.messages"

    private val bundle: ResourceBundle by lazy { bundleFor(Locale.getDefault(Locale.Category.DISPLAY)) }

    /** The string for [key]. A missing key does not throw and returns the key itself (so it is noticed on screen). */
    fun get(key: String): String = text(bundle, key)

    /** Fills [args] into `{0}` etc. of the string for [key] (java.text.MessageFormat; write the character `'` as `''`). */
    fun get(key: String, vararg args: Any?): String = format(bundle, key, *args)

    /** The bundle of strings for the language of [locale]. Languages other than Japanese get English. */
    fun bundleFor(locale: Locale): ResourceBundle =
        ResourceBundle.getBundle(BUNDLE_NAME, locale, Messages::class.java.classLoader, Utf8Control)

    /** Looks up the string for [key] in [bundle]. A missing key gives the key itself. */
    fun text(bundle: ResourceBundle, key: String): String = if (bundle.containsKey(key)) bundle.getString(key) else key

    /** Fills [args] into the string for [key] in [bundle]. Without arguments it does not go through MessageFormat. */
    fun format(bundle: ResourceBundle, key: String, vararg args: Any?): String {
        val pattern = text(bundle, key)
        if (args.isEmpty()) return pattern
        return MessageFormat(pattern, Locale.getDefault(Locale.Category.FORMAT)).format(args)
    }

    /**
     * Reads properties as UTF-8 (Java 8's ResourceBundle reads them as ISO-8859-1).
     * It does not fall back to the OS default locale, so languages other than Japanese get the English messages.properties.
     */
    private object Utf8Control : ResourceBundle.Control() {
        override fun getFormats(baseName: String): List<String> = ResourceBundle.Control.FORMAT_PROPERTIES

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
