package com.appkitbox.winui4k.sample.gallery;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * The strings shown on the Gallery's screens (messages.properties = English, messages_ja.properties = Japanese).
 * The language is decided by the OS display language ({@code Locale.getDefault(Locale.Category.DISPLAY)})
 * and can be changed with {@code -Duser.language=en} etc. Languages other than Japanese get English.
 */
final class Messages {
    private static final String BUNDLE_NAME = "com.appkitbox.winui4k.sample.gallery.messages";

    private static ResourceBundle bundle;

    private Messages() {
    }

    /** The string for key. When the key is missing, returns the key itself so it is noticed on screen. */
    static String get(String key) {
        return lookup(defaultBundle(), key);
    }

    /** Fills args into {0} etc. of the string for key (the java.text.MessageFormat syntax). */
    static String get(String key, Object... args) {
        return format(defaultBundle(), key, args);
    }

    /** The bundle for the language of locale. Also used by the tests to check the contents per language. */
    static ResourceBundle bundleFor(Locale locale) {
        return ResourceBundle.getBundle(BUNDLE_NAME, locale, new Utf8Control());
    }

    /** Looks up key in bundle and fills in args if any. */
    static String format(ResourceBundle bundle, String key, Object... args) {
        String pattern = lookup(bundle, key);
        if (args == null || args.length == 0) {
            return pattern;
        }
        return new MessageFormat(pattern, bundle.getLocale()).format(args);
    }

    private static synchronized ResourceBundle defaultBundle() {
        if (bundle == null) {
            bundle = bundleFor(Locale.getDefault(Locale.Category.DISPLAY));
        }
        return bundle;
    }

    private static String lookup(ResourceBundle bundle, String key) {
        return bundle.containsKey(key) ? bundle.getString(key) : key;
    }

    /**
     * Reads properties as UTF-8 (Java 8's ResourceBundle reads them as ISO-8859-1).
     * So that an OS in a language other than Japanese gets English (messages.properties) without being pulled to the OS default locale,
     * no fallback locale is searched.
     */
    private static final class Utf8Control extends ResourceBundle.Control {
        @Override
        public List<String> getFormats(String baseName) {
            return FORMAT_PROPERTIES;
        }

        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            return null;
        }

        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IOException {
            String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
            InputStream stream = loader.getResourceAsStream(resourceName);
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return new PropertyResourceBundle(reader);
            }
        }
    }
}
