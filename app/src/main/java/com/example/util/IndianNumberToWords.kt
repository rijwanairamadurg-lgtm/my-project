package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode

object IndianNumberToWords {

    private val unitsEn = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    )

    private val tensEn = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    private val unitsKn = arrayOf(
        "", "ಒಂದು", "ಎರಡು", "ಮೂರು", "ನಾಲ್ಕು", "ಐದು", "ಆರು", "ಏಳು", "ಎಂಟು", "ಒಂಬತ್ತು",
        "ಹತ್ತು", "ಹನ್ನೊಂದು", "ಹನ್ನೆರಡು", "ಹದಿಮೂರು", "ಹದಿನಾಲ್ಕು", "ಹದಿನೈದು", "ಹದಿನಾರು",
        "ಹದಿನೇಳು", "ಹದಿನೆಂಟು", "ಹತ್ತೊಂಬತ್ತು"
    )

    private val tensKn = arrayOf(
        "", "", "ಇಪ್ಪತ್ತು", "ಮೂವತ್ತು", "ನಲವತ್ತು", "ಐವತ್ತು", "ಅರವತ್ತು", "ಎಪ್ಪತ್ತು", "ಎಂಬತ್ತು", "ತೊಂಬತ್ತು"
    )

    /**
     * Converts a Rupee amount (string, e.g. "125500.50" or "12,50,000") to English Words.
     */
    fun convertToEnglish(amountStr: String): String {
        val clean = amountStr.replace("₹", "").replace(",", "").trim()
        if (clean.isEmpty()) return ""

        val bd = try {
            BigDecimal(clean).setScale(2, RoundingMode.HALF_UP)
        } catch (e: Exception) {
            return "Invalid Amount"
        }

        if (bd.compareTo(BigDecimal.ZERO) == 0) {
            return "Zero Rupees Only"
        }

        val isNegative = bd < BigDecimal.ZERO
        val absBd = bd.abs()

        val rupeePart = absBd.toBigInteger().toLong()
        val paisePart = absBd.remainder(BigDecimal.ONE).multiply(BigDecimal(100)).toInt()

        val rupeeWords = convertRupeesToEnglish(rupeePart)
        val paiseWords = if (paisePart > 0) convertTwoDigitsEn(paisePart) + " Paise" else ""

        val builder = StringBuilder()
        if (isNegative) builder.append("Minus ")
        builder.append("Rupees ")

        if (rupeePart == 0L) {
            if (paisePart > 0) {
                return (if (isNegative) "Minus " else "") + "$paiseWords Only"
            }
            return "Zero Rupees Only"
        }

        builder.append(rupeeWords)
        if (paiseWords.isNotEmpty()) {
            builder.append(" and ").append(paiseWords)
        }
        builder.append(" Only")

        return builder.toString().trim()
    }

    private fun convertRupeesToEnglish(amount: Long): String {
        if (amount == 0L) return ""

        val crore = amount / 10000000L
        var rem = amount % 10000000L

        val lakh = rem / 100000L
        rem %= 100000L

        val thousand = rem / 1000L
        rem %= 1000L

        val hundred = rem / 100L
        val lastTwo = (rem % 100L).toInt()

        val sb = StringBuilder()

        if (crore > 0) {
            sb.append(convertRupeesToEnglish(crore)).append(" Crore ")
        }
        if (lakh > 0) {
            sb.append(convertTwoDigitsEn(lakh.toInt())).append(" Lakh ")
        }
        if (thousand > 0) {
            sb.append(convertTwoDigitsEn(thousand.toInt())).append(" Thousand ")
        }
        if (hundred > 0) {
            sb.append(unitsEn[hundred.toInt()]).append(" Hundred ")
        }
        if (lastTwo > 0) {
            sb.append(convertTwoDigitsEn(lastTwo)).append(" ")
        }

        return sb.toString().trim()
    }

    private fun convertTwoDigitsEn(n: Int): String {
        return when {
            n < 20 -> unitsEn[n]
            n % 10 == 0 -> tensEn[n / 10]
            else -> tensEn[n / 10] + " " + unitsEn[n % 10]
        }
    }

    /**
     * Converts a Rupee amount to Kannada Words.
     */
    fun convertToKannada(amountStr: String): String {
        val clean = amountStr.replace("₹", "").replace(",", "").trim()
        if (clean.isEmpty()) return ""

        val bd = try {
            BigDecimal(clean).setScale(2, RoundingMode.HALF_UP)
        } catch (e: Exception) {
            return "ಅಮಾನ್ಯ ಮೊತ್ತ"
        }

        if (bd.compareTo(BigDecimal.ZERO) == 0) {
            return "ಶೂನ್ಯ ರೂಪಾಯಿ ಮಾತ್ರ"
        }

        val isNegative = bd < BigDecimal.ZERO
        val absBd = bd.abs()

        val rupeePart = absBd.toBigInteger().toLong()
        val paisePart = absBd.remainder(BigDecimal.ONE).multiply(BigDecimal(100)).toInt()

        val rupeeWords = convertRupeesToKannada(rupeePart)
        val paiseWords = if (paisePart > 0) convertTwoDigitsKn(paisePart) + " ಪೈಸೆ" else ""

        val builder = StringBuilder()
        if (isNegative) builder.append("ಋಣಾತ್ಮಕ ")
        builder.append("ರೂಪಾಯಿ ")

        if (rupeePart == 0L) {
            if (paisePart > 0) {
                return (if (isNegative) "ಋಣಾತ್ಮಕ " else "") + "$paiseWords ಮಾತ್ರ"
            }
            return "ಶೂನ್ಯ ರೂಪಾಯಿ ಮಾತ್ರ"
        }

        builder.append(rupeeWords)
        if (paiseWords.isNotEmpty()) {
            builder.append(" ಮತ್ತು ").append(paiseWords)
        }
        builder.append(" ಮಾತ್ರ")

        return builder.toString().trim()
    }

    private fun convertRupeesToKannada(amount: Long): String {
        if (amount == 0L) return ""

        val crore = amount / 10000000L
        var rem = amount % 10000000L

        val lakh = rem / 100000L
        rem %= 100000L

        val thousand = rem / 1000L
        rem %= 1000L

        val hundred = rem / 100L
        val lastTwo = (rem % 100L).toInt()

        val sb = StringBuilder()

        if (crore > 0) {
            sb.append(convertRupeesToKannada(crore)).append(" ಕೋಟಿ ")
        }
        if (lakh > 0) {
            sb.append(convertTwoDigitsKn(lakh.toInt())).append(" ಲಕ್ಷ ")
        }
        if (thousand > 0) {
            sb.append(convertTwoDigitsKn(thousand.toInt())).append(" ಸಾವಿರದ ")
        }
        if (hundred > 0) {
            val hText = when (hundred.toInt()) {
                1 -> "ನೂರು"
                2 -> "ಇನ್ನೂರು"
                3 -> "ಮುನ್ನೂರು"
                4 -> "ನಾನೂರು"
                5 -> "ಐನೂರು"
                6 -> "ಆರುನೂರು"
                7 -> "ಏಳುನೂರು"
                8 -> "ಎಂಟು ನೂರು"
                9 -> "ಒಂಬೈನೂರು"
                else -> "${unitsKn[hundred.toInt()]} ನೂರು"
            }
            sb.append(hText).append(" ")
        }
        if (lastTwo > 0) {
            sb.append(convertTwoDigitsKn(lastTwo)).append(" ")
        }

        return sb.toString().trim()
    }

    private fun convertTwoDigitsKn(n: Int): String {
        return when {
            n < 20 -> unitsKn[n]
            n % 10 == 0 -> tensKn[n / 10]
            else -> tensKn[n / 10] + " " + unitsKn[n % 10]
        }
    }
}
