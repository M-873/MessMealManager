package com.example.messmealmanager.data

import com.example.messmealmanager.model.DailyEntry
import com.example.messmealmanager.model.Deposit
import com.example.messmealmanager.model.Member
import com.example.messmealmanager.model.Sheet
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()

    // --- Sheets ---
    suspend fun createSheet(sheetName: String, ownerId: String, ownerName: String): Sheet {
        val sheetsRef = db.collection("sheets")
        val counterRef = db.collection("counters").document("sheets")
        val docRef = sheetsRef.document()

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 60)
        val expiresAt = Timestamp(calendar.time)
        val createdAt = Timestamp.now()

        val newSheet = db.runTransaction { transaction ->
            val counterSnapshot = transaction.get(counterRef)
            val currentCount = counterSnapshot.getLong("lastSheetNumber") ?: 0L
            val nextNumber = (currentCount + 1).toInt()

            transaction.set(counterRef, mapOf("lastSheetNumber" to nextNumber), SetOptions.merge())

            val sheet = Sheet(
                id = docRef.id,
                sheetNumber = nextNumber,
                sheetName = sheetName,
                ownerId = ownerId,
                ownerName = ownerName,
                editors = listOf(ownerId),
                viewers = emptyList(),
                createdAt = createdAt,
                expiresAt = expiresAt
            )
            transaction.set(docRef, sheet)
            sheet
        }.await()

        return newSheet
    }

    /**
     * Real-time stream of a single sheet.
     */
    fun getSheetFlow(sheetId: String): Flow<Sheet?> = callbackFlow {
        val listener = db.collection("sheets").document(sheetId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val sheet = snapshot?.toObject(Sheet::class.java)
                trySend(sheet)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getSheet(sheetId: String): Sheet? {
        val doc = db.collection("sheets").document(sheetId).get().await()
        return doc.toObject(Sheet::class.java)
    }

    /**
     * Real-time stream of sheets where the user is an owner, editor, or viewer.
     */
    fun getSheetsFlowForUser(userId: String): Flow<List<Sheet>> {
        val editorsFlow: Flow<List<Sheet>> = callbackFlow {
            val listener = db.collection("sheets")
                .whereArrayContains("editors", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val sheets = snapshot?.toObjects(Sheet::class.java) ?: emptyList()
                    trySend(sheets)
                }
            awaitClose { listener.remove() }
        }

        val viewersFlow: Flow<List<Sheet>> = callbackFlow {
            val listener = db.collection("sheets")
                .whereArrayContains("viewers", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val sheets = snapshot?.toObjects(Sheet::class.java) ?: emptyList()
                    trySend(sheets)
                }
            awaitClose { listener.remove() }
        }

        return combine(editorsFlow, viewersFlow) { editors, viewers ->
            (editors + viewers).distinctBy { it.id }.sortedByDescending { it.sheetNumber }
        }
    }

    suspend fun getSheetsForUser(userId: String): List<Sheet> {
        val editorsSnapshot = db.collection("sheets")
            .whereArrayContains("editors", userId)
            .get()
            .await()
        val viewersSnapshot = db.collection("sheets")
            .whereArrayContains("viewers", userId)
            .get()
            .await()

        val all = editorsSnapshot.toObjects(Sheet::class.java) + viewersSnapshot.toObjects(Sheet::class.java)
        return all.distinctBy { it.id }.sortedByDescending { it.sheetNumber }
    }

    suspend fun getNextSheetNumber(): Int {
        return try {
            val counterDoc = db.collection("counters").document("sheets").get().await()
            val currentCount = counterDoc.getLong("lastSheetNumber") ?: 0L
            (currentCount + 1).toInt()
        } catch (e: Exception) {
            1
        }
    }

    suspend fun isSheetNameTaken(sheetName: String, ownerId: String): Boolean {
        val trimmed = sheetName.trim()
        if (trimmed.isEmpty()) return false
        val snapshot = db.collection("sheets")
            .whereEqualTo("ownerId", ownerId)
            .whereEqualTo("sheetName", trimmed)
            .get()
            .await()
        return !snapshot.isEmpty
    }

    suspend fun searchSheets(query: String): List<Sheet> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val numberQuery = trimmed.toIntOrNull()
        if (numberQuery != null) {
            val numberSnapshot = db.collection("sheets")
                .whereEqualTo("sheetNumber", numberQuery)
                .get()
                .await()
            val byNumber = numberSnapshot.toObjects(Sheet::class.java)
            if (byNumber.isNotEmpty()) return byNumber
        }

        val nameSnapshot = db.collection("sheets")
            .whereGreaterThanOrEqualTo("sheetName", trimmed)
            .whereLessThanOrEqualTo("sheetName", trimmed + "\uf8ff")
            .limit(20)
            .get()
            .await()
        return nameSnapshot.toObjects(Sheet::class.java)
    }

    suspend fun addEditorToSheet(sheetId: String, editorUserId: String) {
        setUserSheetRole(sheetId, editorUserId, isEditor = true)
    }

    suspend fun removeEditorFromSheet(sheetId: String, editorUserId: String) {
        setUserSheetRole(sheetId, editorUserId, isEditor = false)
    }

    suspend fun setUserSheetRole(sheetId: String, userId: String, isEditor: Boolean) {
        val sheetRef = db.collection("sheets").document(sheetId)
        if (isEditor) {
            sheetRef.update(
                mapOf(
                    "editors" to FieldValue.arrayUnion(userId),
                    "viewers" to FieldValue.arrayRemove(userId)
                )
            ).await()
        } else {
            sheetRef.update(
                mapOf(
                    "viewers" to FieldValue.arrayUnion(userId),
                    "editors" to FieldValue.arrayRemove(userId)
                )
            ).await()
        }
    }

    suspend fun removeUserFromSheet(sheetId: String, userId: String) {
        db.collection("sheets").document(sheetId)
            .update(
                mapOf(
                    "editors" to FieldValue.arrayRemove(userId),
                    "viewers" to FieldValue.arrayRemove(userId)
                )
            ).await()
    }

    // --- Members ---
    suspend fun getMember(userId: String): Member? {
        val doc = db.collection("members").document(userId).get().await()
        return doc.toObject(Member::class.java)
    }

    suspend fun findMemberByEmail(email: String): Member? {
        val trimmed = email.trim().lowercase()
        if (trimmed.isEmpty()) return null
        val snapshot = db.collection("members")
            .whereEqualTo("email", trimmed)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.toObject(Member::class.java)
    }

    suspend fun getMembersByIds(userIds: List<String>): List<Member> {
        if (userIds.isEmpty()) return emptyList()
        val members = mutableListOf<Member>()
        // Firestore whereIn supports up to 30 elements
        val chunks = userIds.chunked(30)
        for (chunk in chunks) {
            val snapshot = db.collection("members")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()
            members.addAll(snapshot.toObjects(Member::class.java))
        }
        return members
    }

    suspend fun addOrUpdateMember(member: Member) {
        db.collection("members")
            .document(member.userId)
            .set(member.copy(email = member.email.lowercase()), SetOptions.merge())
            .await()
    }

    /**
     * Real-time stream of a single member.
     */
    fun getMemberFlow(userId: String): Flow<Member?> = callbackFlow {
        val listener = db.collection("members").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val member = snapshot?.toObject(Member::class.java)
                trySend(member)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateMemberName(userId: String, newName: String) {
        db.collection("members").document(userId)
            .update("name", newName)
            .await()
    }

    // --- Daily Entries ---
    suspend fun addDailyEntry(entry: DailyEntry) {
        val docId = if (entry.id.isNotBlank()) entry.id else "${entry.sheetId}_${entry.date}"
        val docRef = db.collection("dailyEntries").document(docId)
        docRef.set(entry.copy(id = docId), SetOptions.merge()).await()
    }

    /**
     * Atomically saves or updates a daily entry within a Firestore transaction
     * to prevent concurrent overwrite collisions for the same date.
     */
    suspend fun saveDailyEntryTransactional(entry: DailyEntry): DailyEntry {
        val docId = if (entry.id.isNotBlank()) entry.id else "${entry.sheetId}_${entry.date}"
        val docRef = db.collection("dailyEntries").document(docId)

        return db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val updatedEntry = entry.copy(id = docId)
            transaction.set(docRef, updatedEntry, SetOptions.merge())
            updatedEntry
        }.await()
    }

    /**
     * Real-time stream of daily entries for a specific sheet.
     */
    fun getDailyEntriesFlowForSheet(sheetId: String): Flow<List<DailyEntry>> = callbackFlow {
        val listener = db.collection("dailyEntries")
            .whereEqualTo("sheetId", sheetId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val entries = snapshot?.toObjects(DailyEntry::class.java) ?: emptyList()
                trySend(entries)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getDailyEntriesForSheet(sheetId: String): List<DailyEntry> {
        val snapshot = db.collection("dailyEntries")
            .whereEqualTo("sheetId", sheetId)
            .get()
            .await()
        return snapshot.toObjects(DailyEntry::class.java)
    }

    suspend fun saveMealsForDate(sheetId: String, date: String, meals: Map<String, Int>): DailyEntry {
        val docId = "${sheetId}_${date}"
        val docRef = db.collection("dailyEntries").document(docId)

        return db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val existing = snapshot.toObject(DailyEntry::class.java)
            val updated = if (existing != null) {
                existing.copy(meals = meals)
            } else {
                DailyEntry(
                    id = docId,
                    sheetId = sheetId,
                    date = date,
                    meals = meals,
                    bazarAmount = null,
                    bazarBy = ""
                )
            }
            transaction.set(docRef, updated, SetOptions.merge())
            updated
        }.await()
    }

    suspend fun saveBazarCostForDate(sheetId: String, date: String, bazarAmount: Double?, bazarBy: String): DailyEntry {
        val docId = "${sheetId}_${date}"
        val docRef = db.collection("dailyEntries").document(docId)

        return db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val existing = snapshot.toObject(DailyEntry::class.java)
            val updated = if (existing != null) {
                existing.copy(bazarAmount = bazarAmount, bazarBy = bazarBy)
            } else {
                DailyEntry(
                    id = docId,
                    sheetId = sheetId,
                    date = date,
                    meals = emptyMap(),
                    bazarAmount = bazarAmount,
                    bazarBy = bazarBy
                )
            }
            transaction.set(docRef, updated, SetOptions.merge())
            updated
        }.await()
    }

    // --- Deposits ---
    suspend fun addDeposit(deposit: Deposit) {
        val docRef = db.collection("deposits").document()
        docRef.set(deposit.copy(id = docRef.id)).await()
    }

    suspend fun deleteDeposit(depositId: String) {
        db.collection("deposits").document(depositId).delete().await()
    }

    /**
     * Real-time stream of deposits for a specific sheet.
     */
    fun getDepositsFlowForSheet(sheetId: String): Flow<List<Deposit>> = callbackFlow {
        val listener = db.collection("deposits")
            .whereEqualTo("sheetId", sheetId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val deposits = snapshot?.toObjects(Deposit::class.java) ?: emptyList()
                trySend(deposits)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getDepositsForSheet(sheetId: String): List<Deposit> {
        val snapshot = db.collection("deposits")
            .whereEqualTo("sheetId", sheetId)
            .get()
            .await()
        return snapshot.toObjects(Deposit::class.java)
    }
}
