package com.example.util

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object UnitConverterEngine {

    enum class Category(val displayNameEn: String, val displayNameKn: String) {
        LENGTH("Length", "ಉದ್ದ"),
        WEIGHT("Weight", "ತೂಕ"),
        AREA("Area", "ವಿಸ್ತೀರ್ಣ"),
        VOLUME("Volume", "ಗಾತ್ರ"),
        TEMPERATURE("Temperature", "ತಾಪಮಾನ"),
        TIME("Time", "ಸಮಯ"),
        DIGITAL("Digital Storage", "ಡಿಜಿಟಲ್ ಸಂಗ್ರಹ"),
        SPEED("Speed", "ವೇಗ")
    }

    data class UnitItem(
        val id: String,
        val nameEn: String,
        val nameKn: String,
        val symbol: String,
        val toBaseRatio: Double // Ratio to base unit (e.g. meter, kg, sq.m, litre, sec, byte, m/s)
    )

    private val lengthUnits = listOf(
        UnitItem("mm", "Millimeter", "ಮಿಲಿಮೀಟರ್", "mm", 0.001),
        UnitItem("cm", "Centimeter", "ಸೆಂಟಿಮೀಟರ್", "cm", 0.01),
        UnitItem("m", "Meter", "ಮೀಟರ್", "m", 1.0),
        UnitItem("km", "Kilometer", "ಕಿಲೋಮೀಟರ್", "km", 1000.0),
        UnitItem("inch", "Inch", "ಇಂಚು", "in", 0.0254),
        UnitItem("ft", "Foot / Feet", "ಅಡಿ", "ft", 0.3048),
        UnitItem("yd", "Yard", "ಗಜ", "yd", 0.9144),
        UnitItem("mi", "Mile", "ಮೈಲಿ", "mi", 1609.344),
        UnitItem("nmi", "Nautical Mile", "ನಾಟಿಕಲ್ ಮೈಲಿ", "NM", 1852.0)
    )

    private val weightUnits = listOf(
        UnitItem("mg", "Milligram", "ಮಿಲಿಗ್ರಾಂ", "mg", 0.000001),
        UnitItem("g", "Gram", "ಗ್ರಾಂ", "g", 0.001),
        UnitItem("kg", "Kilogram", "ಕಿಲೋಗ್ರಾಂ", "kg", 1.0),
        UnitItem("quintal", "Quintal", "ಕ್ವಿಂಟಾಲ್", "q", 100.0),
        UnitItem("tonne", "Tonne / Metric Ton", "ಟನ್", "t", 1000.0),
        UnitItem("lb", "Pound", "ಪೌಂಡ್", "lb", 0.45359237),
        UnitItem("oz", "Ounce", "ಔನ್ಸ್", "oz", 0.028349523),
        UnitItem("tola", "Tola (Gold/Silver)", "ತೋಲ", "tola", 0.0116638)
    )

    private val areaUnits = listOf(
        UnitItem("sqft", "Square Feet", "ಚದರ ಅಡಿ", "sq.ft", 0.092903),
        UnitItem("sqm", "Square Meter", "ಚದರ ಮೀಟರ್", "sq.m", 1.0),
        UnitItem("sqyd", "Square Yard (Gaj)", "ಗಜ", "sq.yd", 0.836127),
        UnitItem("sqkm", "Square Kilometer", "ಚದರ ಕಿ.ಮೀ", "sq.km", 1000000.0),
        UnitItem("gunta", "Gunta (Indian)", "ಗುಂಟೆ", "gunta", 101.17141), // 1 Gunta = 1089 sq ft ≈ 101.17 sq m
        UnitItem("acre", "Acre", "ಎಕರೆ", "ac", 4046.8564), // 1 Acre = 40 Guntas
        UnitItem("hectare", "Hectare", "ಹೆಕ್ಟೇರ್", "ha", 10000.0),
        UnitItem("bigha", "Bigha (Standard)", "ವಿಘಾ", "bigha", 2529.285)
    )

    private val volumeUnits = listOf(
        UnitItem("ml", "Millilitre", "ಮಿಲಿಲೀಟರ್", "ml", 0.001),
        UnitItem("l", "Litre", "ಲೀಟರ್", "L", 1.0),
        UnitItem("gal_us", "Gallon (US)", "ಗ್ಯಾಲನ್ (US)", "gal", 3.78541),
        UnitItem("gal_uk", "Gallon (UK)", "ಗ್ಯಾಲನ್ (UK)", "gal(UK)", 4.54609),
        UnitItem("cum", "Cubic Metre", "ಘನ ಮೀಟರ್", "m³", 1000.0),
        UnitItem("cuft", "Cubic Feet", "ಘನ ಅಡಿ", "cu.ft", 28.3168),
        UnitItem("floz", "Fluid Ounce (US)", "ದ್ರವ ಔನ್ಸ್", "fl oz", 0.0295735)
    )

    private val temperatureUnits = listOf(
        UnitItem("c", "Celsius", "ಸೆಲ್ಸಿಯಸ್", "°C", 1.0),
        UnitItem("f", "Fahrenheit", "ಫ್ಯಾರನ್‌ಹೀಟ್", "°F", 1.0),
        UnitItem("k", "Kelvin", "ಕೆಲ್ವಿನ್", "K", 1.0)
    )

    private val timeUnits = listOf(
        UnitItem("ms", "Millisecond", "ಮಿಲಿಸೆಕೆಂಡ್", "ms", 0.001),
        UnitItem("s", "Second", "ಸೆಕೆಂಡ್", "s", 1.0),
        UnitItem("min", "Minute", "ನಿಮಿಷ", "min", 60.0),
        UnitItem("h", "Hour", "ಗಂಟೆ", "h", 3600.0),
        UnitItem("day", "Day", "ದಿನ", "d", 86400.0),
        UnitItem("week", "Week", "ವಾರ", "wk", 604800.0),
        UnitItem("month", "Month (30d)", "ತಿಂಗಳು", "mo", 2592000.0),
        UnitItem("yr", "Year (365d)", "ವರ್ಷ", "yr", 31536000.0)
    )

    private val digitalUnits = listOf(
        UnitItem("bit", "Bit", "ಬಿಟ್", "b", 0.125),
        UnitItem("b", "Byte", "ಬೈಟ್", "B", 1.0),
        UnitItem("kb", "Kilobyte", "ಕಿಲೋಬೈಟ್", "KB", 1024.0),
        UnitItem("mb", "Megabyte", "ಮೆಗಾಬೈಟ್", "MB", 1048576.0),
        UnitItem("gb", "Gigabyte", "ಗಿಗಾಬೈಟ್", "GB", 1073741824.0),
        UnitItem("tb", "Terabyte", "ಟೆರಾಬೈಟ್", "TB", 1099511627776.0),
        UnitItem("pb", "Petabyte", "ಪೆಟಾಬೈಟ್", "PB", 1125899906842624.0)
    )

    private val speedUnits = listOf(
        UnitItem("ms", "Meters per second", "ಮೀಟರ್/ಸೆಕೆಂಡ್", "m/s", 1.0),
        UnitItem("kmh", "Kilometers per hour", "ಕಿ.ಮೀ/ಗಂಟೆ", "km/h", 0.277778),
        UnitItem("mph", "Miles per hour", "ಮೈಲಿ/ಗಂಟೆ", "mph", 0.44704),
        UnitItem("knot", "Knot", "ನಾಟ್", "kn", 0.514444),
        UnitItem("fts", "Feet per second", "ಅಡಿ/ಸೆಕೆಂಡ್", "ft/s", 0.3048)
    )

    fun getUnitsForCategory(category: Category): List<UnitItem> {
        return when (category) {
            Category.LENGTH -> lengthUnits
            Category.WEIGHT -> weightUnits
            Category.AREA -> areaUnits
            Category.VOLUME -> volumeUnits
            Category.TEMPERATURE -> temperatureUnits
            Category.TIME -> timeUnits
            Category.DIGITAL -> digitalUnits
            Category.SPEED -> speedUnits
        }
    }

    fun convert(
        category: Category,
        fromUnitId: String,
        toUnitId: String,
        value: Double
    ): Double {
        if (fromUnitId == toUnitId) return value

        if (category == Category.TEMPERATURE) {
            // Temperature specific conversion
            val tempInCelsius = when (fromUnitId) {
                "c" -> value
                "f" -> (value - 32.0) * 5.0 / 9.0
                "k" -> value - 273.15
                else -> value
            }

            return when (toUnitId) {
                "c" -> tempInCelsius
                "f" -> (tempInCelsius * 9.0 / 5.0) + 32.0
                "k" -> tempInCelsius + 273.15
                else -> tempInCelsius
            }
        }

        val units = getUnitsForCategory(category)
        val fromUnit = units.firstOrNull { it.id == fromUnitId } ?: return value
        val toUnit = units.firstOrNull { it.id == toUnitId } ?: return value

        // Convert to base unit then to target unit
        val baseValue = value * fromUnit.toBaseRatio
        return baseValue / toUnit.toBaseRatio
    }

    fun formatConvertedValue(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        val bd = BigDecimal.valueOf(value).round(MathContext(8, RoundingMode.HALF_UP))
        return bd.stripTrailingZeros().toPlainString()
    }
}
