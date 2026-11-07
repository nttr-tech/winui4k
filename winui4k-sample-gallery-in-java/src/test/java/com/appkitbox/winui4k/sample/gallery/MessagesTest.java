package com.appkitbox.winui4k.sample.gallery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the contents of messages.properties (English) and messages_ja.properties (Japanese), and how Messages chooses the language. */
class MessagesTest {
    private static final Pattern KEY_PATTERN = Pattern.compile("[a-z0-9]+(\\.[a-z0-9]+)+");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)");
    private static final Pattern USAGE = Pattern.compile("Messages\\.get\\(\\s*(\"([^\"]*)\")?");
    private static final String RESOURCE_DIR = "src/main/resources/com/appkitbox/winui4k/sample/gallery/";

    private final TreeMap<String, String> en = load("messages.properties");
    private final TreeMap<String, String> ja = load("messages_ja.properties");

    @Test
    @DisplayName("the English and Japanese key sets are the same")
    void sameKeys() {
        assertEquals(en.keySet(), ja.keySet());
    }

    @Test
    @DisplayName("no value is empty")
    void nonEmptyValues() {
        List<String> blank = new ArrayList<>();
        en.forEach((key, value) -> addIf(blank, value.trim().isEmpty(), "en: " + key));
        ja.forEach((key, value) -> addIf(blank, value.trim().isEmpty(), "ja: " + key));
        assertEquals(Collections.emptyList(), blank);
    }

    @Test
    @DisplayName("all keys used by Messages.get in the source are in the properties, and the keys are written as string literals")
    void usedKeysExist() throws IOException {
        List<String> usages = sourceUsages(new File("src/main/java").toPath());
        assertFalse(usages.isEmpty());
        Set<String> wrong = new TreeSet<>();
        for (String key : usages) {
            if (!KEY_PATTERN.matcher(key).matches() || !en.containsKey(key)) {
                wrong.add(key);
            }
        }
        assertEquals(Collections.emptySet(), wrong);
    }

    @Test
    @DisplayName("the argument numbers such as {0} match in English and Japanese")
    void samePlaceholders() {
        List<String> wrong = new ArrayList<>();
        en.forEach((key, value) -> addIf(wrong, !placeholders(value).equals(placeholders(ja.getOrDefault(key, ""))), key));
        assertEquals(Collections.emptyList(), wrong);
    }

    @Test
    @DisplayName("messages with arguments write ' as '', and messages without arguments do not use ''")
    void apostrophes() {
        List<String> wrong = new ArrayList<>();
        en.forEach((key, value) -> addIf(wrong, !apostrophesFit(value), "en: " + key));
        ja.forEach((key, value) -> addIf(wrong, !apostrophesFit(value), "ja: " + key));
        assertEquals(Collections.emptyList(), wrong);
    }

    @Test
    @DisplayName("returns Japanese for the Japanese locale, and English for English and other languages (fr)")
    void localeSelection() {
        ResourceBundle jaBundle = Messages.bundleFor(Locale.JAPAN);
        ResourceBundle enBundle = Messages.bundleFor(Locale.US);
        ResourceBundle frBundle = Messages.bundleFor(Locale.FRANCE);
        List<String> wrong = new ArrayList<>();
        for (String key : en.keySet()) {
            addIf(wrong, !jaBundle.getString(key).equals(ja.get(key)), "ja: " + key);
            addIf(wrong, !enBundle.getString(key).equals(en.get(key)), "en: " + key);
            addIf(wrong, !frBundle.getString(key).equals(en.get(key)), "fr: " + key);
        }
        assertEquals(Collections.emptyList(), wrong);
    }

    @Test
    @DisplayName("messages with arguments filled in, and the key itself for a missing key")
    void formatAndMissingKey() {
        String key = en.keySet().stream().filter(k -> !placeholders(en.get(k)).isEmpty()).findFirst().get();
        Set<String> numbers = placeholders(en.get(key));
        Object[] args = new Object[numbers.stream().mapToInt(Integer::parseInt).max().getAsInt() + 1];
        for (int i = 0; i < args.length; i++) {
            args[i] = "<" + i + ">";
        }
        String filled = Messages.format(Messages.bundleFor(Locale.US), key, args);
        for (String number : numbers) {
            assertTrue(filled.contains("<" + number + ">"), filled);
        }
        assertEquals("no.such.key", Messages.get("no.such.key"));
    }

    @Test
    @DisplayName("the properties have the same contents as the Kotlin Gallery's")
    void sameAsKotlinGallery() throws IOException {
        for (String name : new String[] {"messages.properties", "messages_ja.properties"}) {
            byte[] mine = Files.readAllBytes(new File(RESOURCE_DIR + name).toPath());
            byte[] kotlin = Files.readAllBytes(new File("../winui4k-sample-gallery/" + RESOURCE_DIR + name).toPath());
            assertEquals(new String(kotlin, StandardCharsets.UTF_8), new String(mine, StandardCharsets.UTF_8), name);
        }
    }

    private static TreeMap<String, String> load(String name) {
        String path = "com/appkitbox/winui4k/sample/gallery/" + name;
        Properties properties = new Properties();
        try (InputStream stream = MessagesTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException(path + " is missing");
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        TreeMap<String, String> map = new TreeMap<>();
        for (String key : properties.stringPropertyNames()) {
            map.put(key, properties.getProperty(key));
        }
        return map;
    }

    /** The first argument of Messages.get( in the source. Non-string-literal arguments are returned as "<dynamic>" (they fail the key format check). */
    private static List<String> sourceUsages(Path dir) throws IOException {
        List<String> usages = new ArrayList<>();
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList())) {
                Matcher matcher = USAGE.matcher(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
                while (matcher.find()) {
                    usages.add(matcher.group(2) != null ? matcher.group(2) : "<dynamic in " + file.getFileName() + ">");
                }
            }
        }
        return usages;
    }

    private static Set<String> placeholders(String value) {
        Set<String> numbers = new TreeSet<>();
        Matcher matcher = PLACEHOLDER.matcher(value);
        while (matcher.find()) {
            numbers.add(matcher.group(1));
        }
        return numbers;
    }

    private static boolean apostrophesFit(String value) {
        if (placeholders(value).isEmpty()) {
            return !value.contains("''");
        }
        return !value.replace("''", "").contains("'");
    }

    private static void addIf(List<String> list, boolean condition, String item) {
        if (condition) {
            list.add(item);
        }
    }
}
