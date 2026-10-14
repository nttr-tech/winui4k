package com.appkitbox.winui4k

import org.w3c.dom.Element
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import java.io.IOException
import java.io.StringReader
import java.util.Locale
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/**
 * One `<path>` of an [SvgIcon].
 *
 * [data] is in the XAML path mini-language, already shifted so the viewBox origin is (0, 0), and starts with the fill
 * rule (`F1` = nonzero, SVG's default; `F0` = evenodd). A null [fillColor] / [strokeColor] (no color or `currentColor`)
 * means the foreground color of whoever draws the icon.
 */
data class SvgIconPath(
    /** Path data in the XAML mini-language with the fill rule prefix. */
    val data: String,
    /** Whether the path is filled (false for `fill="none"`). */
    val isFilled: Boolean = true,
    /** Fixed fill color (#RRGGBB / #AARRGGBB), or null for the foreground color. */
    val fillColor: String? = null,
    /** Whether the path is stroked. */
    val isStroked: Boolean = false,
    /** Fixed stroke color, or null for the foreground color. */
    val strokeColor: String? = null,
    /** Stroke width in design units. */
    val strokeWidth: Double = 1.0,
    /** Opacity (0..1; opacity × fill-opacity). */
    val opacity: Double = 1.0,
)

/**
 * A UI-independent vector icon read from an SVG document (such as Material Symbols or Fluent System Icons).
 *
 * Only `<path>` elements are read (also inside `<g>`); shapes, transforms, gradients and CSS are not supported.
 * [width] / [height] are the size of the design grid (the viewBox). Draw it with [WSvgIcon].
 */
data class SvgIcon(
    /** Width of the design grid. */
    val width: Double,
    /** Height of the design grid. */
    val height: Double,
    /** The paths in document order. */
    val paths: List<SvgIconPath>,
) {
    companion object {
        /**
         * Parses an SVG document. Throws [IllegalArgumentException] if it is not an SVG with a size, if the XML or
         * path data is malformed, or if it has a DOCTYPE (external entities are never resolved).
         */
        @JvmStatic
        fun parse(svg: String): SvgIcon {
            val root = readRoot(svg)
            require(root.localNameOrTag() == "svg") { "The root element is not <svg>: <${root.tagName}>" }
            val viewBox = root.getAttribute("viewBox").trim()
            val box = if (viewBox.isNotEmpty()) {
                val values = SvgPathData.numbers(viewBox)
                require(values.size == VIEW_BOX_VALUES) { "viewBox must have 4 numbers: $viewBox" }
                values
            } else {
                val width = parseLength(root.getAttribute("width"))
                val height = parseLength(root.getAttribute("height"))
                require(width != null && height != null) { "The SVG has neither a viewBox nor width / height" }
                listOf(0.0, 0.0, width, height)
            }
            require(box[2] > 0 && box[3] > 0) { "The viewBox size must be positive: $viewBox" }
            val paths = mutableListOf<SvgIconPath>()
            collectPaths(root, -box[0], -box[1], paths)
            return SvgIcon(box[2], box[3], paths)
        }

        /** Parses an SVG document in a classpath resource of [loader]. */
        @JvmStatic
        @JvmOverloads
        fun fromResource(path: String, loader: ClassLoader = SvgIcon::class.java.classLoader): SvgIcon {
            val stream = requireNotNull(loader.getResourceAsStream(path.removePrefix("/"))) { "SVG resource not found: $path" }
            return parse(stream.use { it.readBytes().toString(Charsets.UTF_8) })
        }

        private const val VIEW_BOX_VALUES = 4

        private fun readRoot(svg: String): Element {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            // An icon never needs a DTD; refusing it also rules out external entities (XXE)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
            val builder = factory.newDocumentBuilder()
            // Keep the default handler from printing "[Fatal Error]" to stderr; the exception carries the message
            builder.setErrorHandler(
                object : ErrorHandler {
                    override fun warning(exception: SAXParseException) = Unit

                    override fun error(exception: SAXParseException) = throw exception

                    override fun fatalError(exception: SAXParseException) = throw exception
                },
            )
            return try {
                builder.parse(InputSource(StringReader(svg))).documentElement
            } catch (e: SAXException) {
                throw IllegalArgumentException("Malformed SVG: ${e.message}", e)
            } catch (e: IOException) {
                throw IllegalArgumentException("Malformed SVG: ${e.message}", e)
            }
        }

        private fun collectPaths(element: Element, dx: Double, dy: Double, out: MutableList<SvgIconPath>) {
            val children = element.childNodes
            for (i in 0 until children.length) {
                val child = children.item(i) as? Element ?: continue
                when (child.localNameOrTag()) {
                    "g" -> collectPaths(child, dx, dy, out)
                    "path" -> readPath(child, dx, dy)?.let { out += it }
                }
            }
        }

        private fun readPath(element: Element, dx: Double, dy: Double): SvgIconPath? {
            val d = element.getAttribute("d").trim()
            if (d.isEmpty()) return null
            val rule = if (element.getAttribute("fill-rule").trim() == "evenodd") "F0" else "F1"
            val fill = element.getAttribute("fill").trim()
            val stroke = element.getAttribute("stroke").trim()
            val opacity = (parseOpacity(element.getAttribute("opacity")) * parseOpacity(element.getAttribute("fill-opacity")))
            return SvgIconPath(
                data = rule + " " + SvgPathData.translate(d, dx, dy),
                isFilled = fill != "none",
                fillColor = parseColor(fill),
                isStroked = stroke.isNotEmpty() && stroke != "none",
                strokeColor = parseColor(stroke),
                strokeWidth = parseLength(element.getAttribute("stroke-width")) ?: 1.0,
                opacity = opacity,
            )
        }

        private fun Element.localNameOrTag(): String = localName ?: tagName

        private fun parseLength(value: String): Double? = value.trim().removeSuffix("px").toDoubleOrNull()

        private fun parseOpacity(value: String): Double = value.trim().toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 1.0

        /** #RGB / #RRGGBB / #AARRGGBB as an upper-case #RRGGBB / #AARRGGBB; anything else (currentColor, names) is null. */
        private fun parseColor(value: String): String? {
            if (!value.startsWith("#")) return null
            val hex = value.substring(1)
            if (hex.any { Character.digit(it, 16) < 0 }) return null
            return when (hex.length) {
                3 -> "#" + hex.map { "$it$it" }.joinToString("").uppercase(Locale.ROOT)
                6, 8 -> "#" + hex.uppercase(Locale.ROOT)
                else -> null
            }
        }
    }
}

/** Parsing and shifting of SVG path data (the `d` attribute). */
internal object SvgPathData {
    private val NUMBER = Regex("[+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?")

    /** Number of parameters per command (lower case). */
    private val PARAMETER_COUNTS = mapOf(
        'm' to 2, 'l' to 2, 't' to 2, 'h' to 1, 'v' to 1, 'c' to 6, 's' to 4, 'q' to 4, 'a' to 7, 'z' to 0,
    )

    private const val ARC_LARGE_FLAG = 3
    private const val ARC_SWEEP_FLAG = 4
    private const val ARC_END_X = 5

    /** All numbers in [text] (for viewBox). */
    fun numbers(text: String): List<Double> = NUMBER.findAll(text).map { it.value.toDouble() }.toList()

    /**
     * Shifts every absolute coordinate in [data] by ([dx], [dy]) and writes it in the XAML mini-language
     * (relative commands are unchanged except a leading `m`, which SVG treats as absolute).
     */
    fun translate(data: String, dx: Double, dy: Double): String = Translator(Reader(data), dx, dy).run()

    /** Writes the shifted path data command by command. */
    private class Translator(private val reader: Reader, private val dx: Double, private val dy: Double) {
        private val out = StringBuilder()

        fun run(): String {
            var first = true
            while (reader.skipSeparators()) {
                val command = reader.next()
                if (out.isNotEmpty()) out.append(' ')
                out.append(command)
                appendParameters(command, first)
                first = false
            }
            return out.toString()
        }

        /** Reads the parameters of [command] (repeated as long as numbers follow), shifts them if absolute, and appends them. */
        private fun appendParameters(command: Char, first: Boolean) {
            val lower = command.lowercaseChar()
            val count = PARAMETER_COUNTS[lower] ?: throw IllegalArgumentException("Unknown path command '$command' in: ${reader.text}")
            if (count == 0) return
            // SVG treats a leading relative moveto as absolute (only its first pair; the implicit line-tos stay relative)
            val leadingMove = first && lower == 'm'
            var repeat = 0
            do {
                val values = DoubleArray(count) { index -> if (isArcFlag(lower, index)) reader.flag() else reader.number() }
                if (command.isUpperCase() || (leadingMove && repeat == 0)) shift(lower, values, dx, dy)
                if (repeat > 0) out.append(' ')
                out.append(values.joinToString(" ") { format(it) })
                repeat++
            } while (reader.hasNumber())
        }

        private fun isArcFlag(command: Char, index: Int): Boolean = command == 'a' && (index == ARC_LARGE_FLAG || index == ARC_SWEEP_FLAG)
    }

    private fun shift(command: Char, values: DoubleArray, dx: Double, dy: Double) {
        when (command) {
            'h' -> values[0] += dx
            'v' -> values[0] += dy
            'a' -> {
                values[ARC_END_X] += dx
                values[ARC_END_X + 1] += dy
            }
            else -> for (i in values.indices) values[i] += if (i % 2 == 0) dx else dy
        }
    }

    private fun format(value: Double): String = when {
        value == Math.rint(value) && Math.abs(value) < 1e15 -> value.toLong().toString()
        else -> String.format(Locale.ROOT, "%.4f", value).trimEnd('0').trimEnd('.')
    }

    /** A cursor over path data. */
    private class Reader(val text: String) {
        private var position = 0

        /** Skips whitespace and commas; returns whether anything is left. */
        fun skipSeparators(): Boolean {
            while (position < text.length && (text[position].isWhitespace() || text[position] == ',')) position++
            return position < text.length
        }

        fun next(): Char = text[position++]

        fun hasNumber(): Boolean = skipSeparators() && NUMBER.matchAt(text, position) != null

        fun number(): Double {
            skipSeparators()
            val match = NUMBER.matchAt(text, position) ?: throw IllegalArgumentException("A number is expected at $position in: $text")
            position = match.range.last + 1
            return match.value.toDouble()
        }

        /** An arc flag: a single 0 or 1, which may be written without a separator before the next number. */
        fun flag(): Double {
            skipSeparators()
            val c = text.getOrNull(position)
            require(c == '0' || c == '1') { "An arc flag (0 / 1) is expected at $position in: $text" }
            position++
            return if (c == '1') 1.0 else 0.0
        }
    }
}
