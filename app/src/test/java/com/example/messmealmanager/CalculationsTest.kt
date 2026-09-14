package com.example.messmealmanager

import com.example.messmealmanager.model.DailyEntry
import com.example.messmealmanager.model.Deposit
import com.example.messmealmanager.model.Member
import com.example.messmealmanager.util.Calculations
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculationsTest {

    @Test
    fun testDataset1_August() {
        val mahfuz = Member(userId = "mahfuz", name = "Mahfuz")
        val rakib = Member(userId = "rakib", name = "Rakib")
        val bipu = Member(userId = "bipu", name = "Bipu")
        val zehad = Member(userId = "zehad", name = "Zehad")
        val ishfaq = Member(userId = "ishfaq", name = "Ishfaq")
        val mt = Member(userId = "mt", name = "MT")

        val members = listOf(mahfuz, rakib, bipu, zehad, ishfaq, mt)

        // Meals across dates
        val dailyEntries = listOf(
            DailyEntry(
                id = "e1",
                sheetId = "s1",
                date = "2026-08-01",
                meals = mapOf(
                    "mahfuz" to 25,
                    "rakib" to 15,
                    "bipu" to 36,
                    "zehad" to 81,
                    "ishfaq" to 0,
                    "mt" to 0
                ),
                bazarAmount = 12982.0
            )
        )

        val deposits = listOf(
            Deposit(id = "d1", sheetId = "s1", memberId = "mahfuz", amount = 3121.0, date = "2026-08-01"),
            Deposit(id = "d2", sheetId = "s1", memberId = "rakib", amount = 1770.0, date = "2026-08-01"),
            Deposit(id = "d3", sheetId = "s1", memberId = "bipu", amount = 3471.0, date = "2026-08-01"),
            Deposit(id = "d4", sheetId = "s1", memberId = "zehad", amount = 4620.0, date = "2026-08-01"),
            Deposit(id = "d5", sheetId = "s1", memberId = "ishfaq", amount = 0.0, date = "2026-08-01"),
            Deposit(id = "d6", sheetId = "s1", memberId = "mt", amount = 0.0, date = "2026-08-01")
        )

        val summary = Calculations.calculateSummary(dailyEntries, deposits)

        assertEquals(157, summary.totalMeals)
        assertEquals(12982.0, summary.totalCost, 0.001)
        assertEquals(12982.0, summary.totalDeposit, 0.001)
        assertEquals(0.0, summary.currentBalance, 0.001)
        assertEquals(82.6879, summary.perMealCost, 0.0001)

        val balances = Calculations.calculateMemberBalances(members, dailyEntries, deposits)
            .associateBy { it.member.userId }

        // Mahfuz ~1053.80 (~1054)
        assertEquals(1053.80, balances["mahfuz"]!!.dueBalance, 0.01)
        // Rakib ~529.68
        assertEquals(529.68, balances["rakib"]!!.dueBalance, 0.01)
        // Bipu ~494.24
        assertEquals(494.24, balances["bipu"]!!.dueBalance, 0.01)
        // Zehad ~ -2077.72
        assertEquals(-2077.72, balances["zehad"]!!.dueBalance, 0.01)
        // Ishfaq 0.0
        assertEquals(0.0, balances["ishfaq"]!!.dueBalance, 0.01)
        // MT 0.0
        assertEquals(0.0, balances["mt"]!!.dueBalance, 0.01)
    }

    @Test
    fun testDataset2_September() {
        val mahfuz = Member(userId = "mahfuz", name = "Mahfuz")
        val rakib = Member(userId = "rakib", name = "Rakib")
        val bipu = Member(userId = "bipu", name = "Bipu")
        val zehad = Member(userId = "zehad", name = "Zehad")
        val ishfaq = Member(userId = "ishfaq", name = "Ishfaq")
        val mt = Member(userId = "mt", name = "MT")

        val members = listOf(mahfuz, rakib, bipu, zehad, ishfaq, mt)

        val dailyEntries = listOf(
            DailyEntry(
                id = "e1",
                sheetId = "s2",
                date = "2026-09-01",
                meals = mapOf(
                    "mahfuz" to 14,
                    "rakib" to 4,
                    "bipu" to 13,
                    "zehad" to 22,
                    "ishfaq" to 10,
                    "mt" to 0
                ),
                bazarAmount = 8150.0
            )
        )

        val deposits = listOf(
            Deposit(id = "d1", sheetId = "s2", memberId = "mahfuz", amount = 3010.0, date = "2026-09-01"),
            Deposit(id = "d2", sheetId = "s2", memberId = "rakib", amount = 740.0, date = "2026-09-01"),
            Deposit(id = "d3", sheetId = "s2", memberId = "bipu", amount = 760.0, date = "2026-09-01"),
            Deposit(id = "d4", sheetId = "s2", memberId = "zehad", amount = 3640.0, date = "2026-09-01"),
            Deposit(id = "d5", sheetId = "s2", memberId = "ishfaq", amount = 0.0, date = "2026-09-01"),
            Deposit(id = "d6", sheetId = "s2", memberId = "mt", amount = 0.0, date = "2026-09-01")
        )

        val summary = Calculations.calculateSummary(dailyEntries, deposits)

        assertEquals(63, summary.totalMeals)
        assertEquals(8150.0, summary.totalCost, 0.001)
        assertEquals(8150.0, summary.totalDeposit, 0.001)
        assertEquals(0.0, summary.currentBalance, 0.001)
        assertEquals(129.3651, summary.perMealCost, 0.0001)

        val balances = Calculations.calculateMemberBalances(members, dailyEntries, deposits)
            .associateBy { it.member.userId }

        // Mahfuz ~1198.89
        assertEquals(1198.89, balances["mahfuz"]!!.dueBalance, 0.01)
        // Rakib ~222.54
        assertEquals(222.54, balances["rakib"]!!.dueBalance, 0.01)
        // Bipu ~ -921.75
        assertEquals(-921.75, balances["bipu"]!!.dueBalance, 0.01)
        // Zehad ~793.97
        assertEquals(793.97, balances["zehad"]!!.dueBalance, 0.01)
        // Ishfaq ~ -1293.65
        assertEquals(-1293.65, balances["ishfaq"]!!.dueBalance, 0.01)
        // MT 0.0
        assertEquals(0.0, balances["mt"]!!.dueBalance, 0.01)
    }

    @Test
    fun testEmptyAndZeroHandling() {
        val members = listOf(Member(userId = "m1", name = "Test"))
        // Both null and 0 bazarAmount treated as 0
        val entries = listOf(
            DailyEntry(id = "e1", sheetId = "s", date = "2026-09-01", meals = emptyMap(), bazarAmount = null),
            DailyEntry(id = "e2", sheetId = "s", date = "2026-09-02", meals = emptyMap(), bazarAmount = 0.0)
        )
        val summary = Calculations.calculateSummary(entries, emptyList())
        assertEquals(0, summary.totalMeals)
        assertEquals(0.0, summary.totalCost, 0.001)
        assertEquals(0.0, summary.perMealCost, 0.001)
        assertEquals(0.0, summary.currentBalance, 0.001)

        val balances = Calculations.calculateMemberBalances(members, entries, emptyList())
        assertEquals(0.0, balances.first().dueBalance, 0.001)
    }
}
