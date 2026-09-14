package com.example.messmealmanager.model

import com.google.firebase.Timestamp

data class Sheet(
    val id: String = "",
    val sheetNumber: Int = 0, // Auto-increment logic will be handled in repository
    val sheetName: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val editors: List<String> = emptyList(),
    val viewers: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp = Timestamp.now() // Set to createdAt + 60 days in repository
)

enum class MemberRole {
    OWNER,
    EDITOR,
    VIEWER
}

data class Member(
    val userId: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val email: String = ""
)

data class DailyEntry(
    val id: String = "",
    val sheetId: String = "",
    val date: String = "", // e.g., "YYYY-MM-DD"
    val meals: Map<String, Int> = emptyMap(), // memberId to count
    val bazarAmount: Double? = null,
    val bazarBy: String = "" // memberId
)

data class Deposit(
    val id: String = "",
    val sheetId: String = "",
    val memberId: String = "",
    val amount: Double = 0.0,
    val date: String = "", // e.g., "YYYY-MM-DD"
    val remark: String = ""
)
