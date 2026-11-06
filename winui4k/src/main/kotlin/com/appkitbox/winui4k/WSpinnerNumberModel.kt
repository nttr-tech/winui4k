package com.appkitbox.winui4k

/**
 * SpinnerNumberModel-style: a numeric [WSpinnerModel] that steps between a minimum ([minimum]) and a maximum ([maximum]) by a step ([stepSize]).
 *
 * As in Swing, the value type (Integer, Long, Double and so on) is kept. For an Integer value, [nextValue] / [previousValue] also return an Integer
 * (Integer, Long, Short and Byte are added as integers, Double and Float as decimals). If [minimum] / [maximum] is null, there is no limit on that side.
 * - Setting [value] does not clamp it to the range (values outside the range are kept as is). Null or non-numeric values throw [IllegalArgumentException]
 * - [nextValue] / [previousValue] are null when the result goes beyond the range
 * - Notifications are sent only when the value, minimum, maximum or step actually changed (compared with equals)
 *
 * Difference from Swing: Swing's minimum and maximum are Comparable, and comparing numbers of different types (an Integer maximum with a Double value and so on) throws ClassCastException,
 * but here the minimum and maximum are [Number]s and are compared as numbers even if the types differ (as long if both are integers, otherwise as double).
 *
 * @throws IllegalArgumentException if the constructor does not satisfy `minimum <= value <= maximum`
 */
class WSpinnerNumberModel @JvmOverloads constructor(
    value: Number = 0,
    minimum: Number? = null,
    maximum: Number? = null,
    stepSize: Number = 1,
) : WAbstractSpinnerModel() {
    /** Like SpinnerNumberModel(int, int, int, int): creates it with an Integer value, minimum, maximum and step. */
    constructor(value: Int, minimum: Int, maximum: Int, stepSize: Int) :
        this(value as Number, minimum as Number?, maximum as Number?, stepSize as Number)

    /** Like SpinnerNumberModel(double, double, double, double): creates it with a Double value, minimum, maximum and step. */
    constructor(value: Double, minimum: Double, maximum: Double, stepSize: Double) :
        this(value as Number, minimum as Number?, maximum as Number?, stepSize as Number)

    private var current: Number = value

    init {
        require((minimum == null || compareNumbers(minimum, value) <= 0) && (maximum == null || compareNumbers(maximum, value) >= 0)) {
            "(minimum <= value <= maximum) is false"
        }
    }

    /** The minimum of the range (no lower limit if null). Changing it does not clamp [value] (same as Swing). */
    var minimum: Number? = minimum
        set(n) {
            if (n == field) return
            field = n
            fireStateChanged()
        }

    /** The maximum of the range (no upper limit if null). Changing it does not clamp [value] (same as Swing). */
    var maximum: Number? = maximum
        set(n) {
            if (n == field) return
            field = n
            fireStateChanged()
        }

    /** The step added and subtracted by [nextValue] / [previousValue]. For integer values it is added as a long (0.5 becomes 0). */
    var stepSize: Number = stepSize
        set(n) {
            if (n == field) return
            field = n
            fireStateChanged()
        }

    /** Like SpinnerNumberModel.getNumber: the current value (returns the same thing as [value] as a [Number]). */
    val number: Number get() = current

    /**
     * The current value (a [Number]). Values outside the range are kept without clamping.
     * @throws IllegalArgumentException when null or a non-numeric value is set
     */
    override var value: Any?
        get() = current
        set(v) {
            require(v is Number) { "illegal value" }
            if (v == current) return
            current = v
            fireStateChanged()
        }

    /** [value] plus [stepSize] (the same type as [value]). Null when it goes beyond [maximum]. */
    override val nextValue: Any? get() = increment(1)

    /** [value] minus [stepSize] (the same type as [value]). Null when it goes below [minimum]. */
    override val previousValue: Any? get() = increment(-1)

    private fun increment(direction: Int): Number? {
        val value = current
        val step = stepSize
        val next: Number = if (value is Double || value is Float) {
            val sum = value.toDouble() + step.toDouble() * direction
            if (value is Double) sum else sum.toFloat()
        } else {
            val sum = value.toLong() + step.toLong() * direction
            when (value) {
                is Long -> sum
                is Int -> sum.toInt()
                is Short -> sum.toInt().toShort()
                else -> sum.toInt().toByte() // As in Swing, other Numbers are treated as Byte
            }
        }
        val max = maximum
        val min = minimum
        return when {
            max != null && compareNumbers(max, next) < 0 -> null
            min != null && compareNumbers(min, next) > 0 -> null
            else -> next
        }
    }

    internal companion object {
        /** Compares two numbers. As long if both are integers (Byte, Short, Int, Long), otherwise as double. */
        @JvmSynthetic
        fun compareNumbers(a: Number, b: Number): Int =
            if (a.isIntegral() && b.isIntegral()) a.toLong().compareTo(b.toLong()) else a.toDouble().compareTo(b.toDouble())

        /** Whether it is an integer type (Byte, Short, Int, Long). */
        @JvmSynthetic
        fun Number.isIntegral(): Boolean = this is Int || this is Long || this is Short || this is Byte
    }
}
