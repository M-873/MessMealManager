package com.example.messmealmanager.util

import com.example.messmealmanager.model.DailyEntry
import com.example.messmealmanager.model.Deposit
import com.example.messmealmanager.model.Member

data class SheetSummary(
    val totalMeals: Int = 0,
    val totalDeposit: Double = 0.0,
    val totalCost: Double = 0.0,
    val currentBalance: Double = 0.0,
    val perMealCost: Double = 0.0
)

data class MemberBalance(
    val member: Member,
    val totalMeals: Int = 0,
    val totalDeposit: Double = 0.0,
    val individualExpense: Double = 0.0,
    val dueBalance: Double = 0.0 // Deposit - Individual Expense (negative = owes money, positive = owed money)
)

object Calculations {

    /**
     * Calculates the overall summary for a sheet:
     * - Total Meals: sum across all members, all dates.
     * - Total Deposit: sum of all deposits.
     * - Total Cost/Expense: sum of all bazar entries (empty/null and 0 both treated as no expense).
     * - Current Balance = Total Deposit - Total Cost (0 if books balance).
     * - Per Meal Cost = Total Cost / Total Meals (0.0 if Total Meals is 0).
     */
    fun calculateSummary(
        dailyEntries: List<DailyEntry>,
        deposits: List<Deposit>
    ): SheetSummary {
        val totalMeals = dailyEntries.sumOf { entry ->
            entry.meals.values.sum()
        }
        val totalDeposit = deposits.sumOf { it.amount }
        val totalCost = dailyEntries.sumOf { entry ->
            entry.bazarAmount ?: 0.0
        }
        val currentBalance = totalDeposit - totalCost
        val perMealCost = if (totalMeals > 0) totalCost / totalMeals else 0.0

        return SheetSummary(
            totalMeals = totalMeals,
            totalDeposit = totalDeposit,
            totalCost = totalCost,
            currentBalance = currentBalance,
            perMealCost = perMealCost
        )
    }

    /**
     * Calculates individual balance per member:
     * - Their Total Meals (sum of just their entries)
     * - Their Deposit (sum of just their deposits)
     * - Their Individual Expense (Total Meals * Per Meal Cost, where Per Meal Cost = Total Cost / Total Meals)
     * - Their Due/Final Balance = Deposit - Individual Expense
     */
    fun calculateMemberBalances(
        members: List<Member>,
        dailyEntries: List<DailyEntry>,
        deposits: List<Deposit>
    ): List<MemberBalance> {
        val summary = calculateSummary(dailyEntries, deposits)

        return members.map { member ->
            val memberMeals = dailyEntries.sumOf { entry ->
                entry.meals[member.userId] ?: 0
            }
            val memberDeposit = deposits
                .filter { it.memberId == member.userId }
                .sumOf { it.amount }
            val memberExpense = memberMeals * summary.perMealCost
            val dueBalance = memberDeposit - memberExpense

            MemberBalance(
                member = member,
                totalMeals = memberMeals,
                totalDeposit = memberDeposit,
                individualExpense = memberExpense,
                dueBalance = dueBalance
            )
        }
    }
}
