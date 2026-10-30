package com.bekzodjon.voicereminder

import com.bekzodjon.voicereminder.domain.Recurrence
import com.bekzodjon.voicereminder.parser.UzbekReminderParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class UzbekReminderParserTest {
    // Friday 2026-10-30 08:00
    private val now = LocalDateTime.of(2026, 10, 30, 8, 0)
    private val p = UzbekReminderParser()

    @Test fun callToday() {
        val r = p.parse("Bugun soat 15:00 da Akmalga qo'ng'iroq qilishni eslat.", now)
        assertEquals(1, r.size)
        assertEquals("Akmalga qo'ng'iroq qilish", r[0].title)
        assertEquals(LocalDateTime.of(2026, 10, 30, 15, 0), r[0].dateTime)
    }

    @Test fun dailyMedicine() {
        val r = p.parse("Har kuni ertalab soat 7 da dori ichishni eslat.", now)
        assertEquals(1, r.size)
        assertEquals("Dori ichish", r[0].title)
        assertEquals(Recurrence.DAILY, r[0].recurrence)
        assertEquals(LocalDateTime.of(2026, 10, 31, 7, 0), r[0].dateTime)
    }

    @Test fun meetingTomorrow() {
        val r = p.parse("Ertaga soat 10 da direktor bilan uchrashuvim bor.", now)
        assertEquals("Direktor bilan uchrashuv", r[0].title)
        assertEquals(LocalDateTime.of(2026, 10, 31, 10, 0), r[0].dateTime)
    }

    @Test fun lessonPrep() {
        val r = p.parse("Bugun soat 18:00 da ingliz tili darsini tayyorlashim kerak.", now)
        assertEquals("Ingliz tili darsini tayyorlash", r[0].title)
        assertEquals(LocalDateTime.of(2026, 10, 30, 18, 0), r[0].dateTime)
    }

    @Test fun shoppingListIsOneUnscheduledTask() {
        val r = p.parse("Do'kondan non, sut va tuxum olishim kerak.", now)
        assertEquals(1, r.size)
        assertEquals("Do'kondan non, sut va tuxum olish", r[0].title)
        assertNull(r[0].dateTime)
    }

    @Test fun multipleTasksWithCommas() {
        val r = p.parse("Ertaga soat 9 da darsga borishim kerak, soat 12 da Akmalga qo'ng'iroq qilishim kerak, soat 5 da do'konga borishim kerak.", now)
        assertEquals(3, r.size)
        assertEquals("Darsga borish", r[0].title)
        assertEquals(LocalDateTime.of(2026, 10, 31, 9, 0), r[0].dateTime)
        assertEquals("Akmalga qo'ng'iroq qilish", r[1].title)
        assertEquals(LocalDateTime.of(2026, 10, 31, 12, 0), r[1].dateTime)
        assertEquals("Do'konga borish", r[2].title)
        assertEquals(LocalDateTime.of(2026, 10, 31, 17, 0), r[2].dateTime)
    }

    @Test fun multipleTasksWithoutPunctuation() {
        val r = p.parse("ertaga soat 9 da darsga borishim kerak soat 12 da akmalga qo'ng'iroq qilishim kerak soat 5 da do'konga borishim kerak", now)
        assertEquals(3, r.size)
        assertEquals(LocalDateTime.of(2026, 10, 31, 17, 0), r[2].dateTime)
    }

    @Test fun weeklyMonday() {
        val r = p.parse("Har dushanba soat 10 da yig'ilish.", now)
        assertEquals(Recurrence.WEEKLY, r[0].recurrence)
        assertEquals("Yig'ilish", r[0].title)
        assertEquals(LocalDateTime.of(2026, 11, 2, 10, 0), r[0].dateTime)
    }

    @Test fun monthlyDay5() {
        val r = p.parse("Har oyning 5-kuni to'lov qilish.", now)
        assertEquals(Recurrence.MONTHLY, r[0].recurrence)
        assertEquals("To'lov qilish", r[0].title)
        assertEquals(LocalDateTime.of(2026, 11, 5, 9, 0), r[0].dateTime)
    }

    @Test fun typographicApostrophes() {
        val r = p.parse("Ertaga soat 10 da Akmalga qo\u2019ng\u2019iroq qilishim kerak", now)
        assertEquals("Akmalga qo'ng'iroq qilish", r[0].title)
    }

    @Test fun timeOnlyInPastGoesToTomorrow() {
        val r = p.parse("Soat 7 da yugurish", now)
        assertEquals(LocalDateTime.of(2026, 10, 31, 7, 0), r[0].dateTime)
    }

    @Test fun relativeTime() {
        val r = p.parse("30 daqiqadan keyin choy qo'yish", now)
        assertEquals(LocalDateTime.of(2026, 10, 30, 8, 30), r[0].dateTime)
        assertEquals("Choy qo'yish", r[0].title)
    }

    @Test fun calendarDate() {
        val r = p.parse("15-noyabr soat 14:30 da shifokorga borish", now)
        assertEquals(LocalDateTime.of(2026, 11, 15, 14, 30), r[0].dateTime)
    }

    @Test fun spelledHour() {
        val r = p.parse("Ertaga soat uchda majlis", now)
        assertEquals(LocalDateTime.of(2026, 10, 31, 15, 0), r[0].dateTime)
    }

    @Test fun everyTwoDays() {
        val r = p.parse("Har 2 kunda gul sug'orish", now)
        assertEquals(Recurrence.DAILY, r[0].recurrence)
        assertEquals(2, r[0].interval)
    }

    @Test fun emptyInput() { assertEquals(0, p.parse("   ", now).size) }
}
