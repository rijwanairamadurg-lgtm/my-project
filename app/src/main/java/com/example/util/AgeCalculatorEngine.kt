package com.example.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object AgeCalculatorEngine {

    data class UpcomingBirthday(
        val date: LocalDate,
        val dayOfWeek: String,
        val age: Int
    )

    data class AgeResult(
        val years: Int,
        val months: Int,
        val days: Int,
        val totalMonths: Long,
        val totalWeeks: Long,
        val totalDays: Long,
        val totalHours: Long,
        val totalMinutes: Long,
        val nextBirthdayDate: LocalDate,
        val daysUntilNextBirthday: Long,
        val monthsUntilNextBirthday: Int,
        val daysAfterMonthsUntilNextBirthday: Int,
        val nextBirthdayDayOfWeek: String,
        val upcomingBirthdays: List<UpcomingBirthday>,
        val zodiacSign: String
    )

    fun calculateAge(dob: LocalDate, targetDate: LocalDate = LocalDate.now()): AgeResult? {
        if (dob.isAfter(targetDate)) {
            return null
        }

        val period = Period.between(dob, targetDate)
        val years = period.years
        val months = period.months
        val days = period.days

        val totalDays = ChronoUnit.DAYS.between(dob, targetDate)
        val totalWeeks = totalDays / 7
        val totalMonths = ChronoUnit.MONTHS.between(dob, targetDate)
        val totalHours = totalDays * 24
        val totalMinutes = totalHours * 60

        // Calculate next birthday
        var nextBday = dob.withYear(targetDate.year)
        if (nextBday.isBefore(targetDate) || nextBday.isEqual(targetDate)) {
            nextBday = dob.withYear(targetDate.year + 1)
        }

        val daysUntilNext = ChronoUnit.DAYS.between(targetDate, nextBday)
        val nextBdayPeriod = Period.between(targetDate, nextBday)
        val nextBdayDayOfWeek = nextBday.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

        // Upcoming 5 birthdays
        val upcoming = mutableListOf<UpcomingBirthday>()
        for (i in 0 until 5) {
            val upcomingDate = dob.withYear(nextBday.year + i)
            val dayName = upcomingDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            val ageAtBday = Period.between(dob, upcomingDate).years
            upcoming.add(UpcomingBirthday(upcomingDate, dayName, ageAtBday))
        }

        val zodiac = getZodiacSign(dob.dayOfMonth, dob.monthValue)

        return AgeResult(
            years = years,
            months = months,
            days = days,
            totalMonths = totalMonths,
            totalWeeks = totalWeeks,
            totalDays = totalDays,
            totalHours = totalHours,
            totalMinutes = totalMinutes,
            nextBirthdayDate = nextBday,
            daysUntilNextBirthday = daysUntilNext,
            monthsUntilNextBirthday = nextBdayPeriod.months,
            daysAfterMonthsUntilNextBirthday = nextBdayPeriod.days,
            nextBirthdayDayOfWeek = nextBdayDayOfWeek,
            upcomingBirthdays = upcoming,
            zodiacSign = zodiac
        )
    }

    private fun getZodiacSign(day: Int, month: Int): String {
        return when (month) {
            1 -> if (day < 20) "Capricorn ♑ (ಮಕರ)" else "Aquarius ♒ (ಕುಂಭ)"
            2 -> if (day < 19) "Aquarius ♒ (ಕುಂಭ)" else "Pisces ♓ (ಮೀನ)"
            3 -> if (day < 21) "Pisces ♓ (ಮೀನ)" else "Aries ♈ (ಮೇಷ)"
            4 -> if (day < 20) "Aries ♈ (ಮೇಷ)" else "Taurus ♉ (ವೃಷಭ)"
            5 -> if (day < 21) "Taurus ♉ (ವೃಷಭ)" else "Gemini ♊ (ಮಿಥುನ)"
            6 -> if (day < 21) "Gemini ♊ (ಮಿಥುನ)" else "Cancer ♋ (ಕರ್ಕಾಟಕ)"
            7 -> if (day < 23) "Cancer ♋ (ಕರ್ಕಾಟಕ)" else "Leo ♌ (ಸಿಂಹ)"
            8 -> if (day < 23) "Leo ♌ (ಸಿಂಹ)" else "Virgo ♍ (ಕನ್ಯಾ)"
            9 -> if (day < 23) "Virgo ♍ (ಕನ್ಯಾ)" else "Libra ♎ (ತುಲಾ)"
            10 -> if (day < 23) "Libra ♎ (ತುಲಾ)" else "Scorpio ♏ (ವೃಶ್ಚಿಕ)"
            11 -> if (day < 22) "Scorpio ♏ (ವೃಶ್ಚಿಕ)" else "Sagittarius ♐ (ಧನು)"
            12 -> if (day < 22) "Sagittarius ♐ (ಧನು)" else "Capricorn ♑ (ಮಕರ)"
            else -> "Unknown"
        }
    }
}
