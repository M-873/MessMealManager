package com.example.messmealmanager.ui.screens.sheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.messmealmanager.auth.AuthManager
import com.example.messmealmanager.data.FirestoreRepository
import com.example.messmealmanager.model.DailyEntry
import com.example.messmealmanager.model.Deposit
import com.example.messmealmanager.model.Member
import com.example.messmealmanager.model.Sheet
import com.example.messmealmanager.util.Calculations
import com.example.messmealmanager.util.SheetSummary
import com.example.messmealmanager.util.MemberBalance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SheetDetailViewModel(
    val sheetId: String,
    private val firestoreRepository: FirestoreRepository,
    private val authManager: AuthManager
) : ViewModel() {

    val currentUserId = authManager.currentUser?.uid ?: ""

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Sheet real-time state
    private val _sheet = MutableStateFlow<Sheet?>(null)
    val sheet: StateFlow<Sheet?> = _sheet.asStateFlow()

    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner.asStateFlow()

    private val _canEdit = MutableStateFlow(false)
    val canEdit: StateFlow<Boolean> = _canEdit.asStateFlow()

    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    // Real-time daily entries stream
    val dailyEntries: StateFlow<List<DailyEntry>> = firestoreRepository
        .getDailyEntriesFlowForSheet(sheetId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val deposits: StateFlow<List<Deposit>> = firestoreRepository
        .getDepositsFlowForSheet(sheetId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val sheetSummary: StateFlow<SheetSummary> = combine(dailyEntries, deposits) { entries, deps ->
        Calculations.calculateSummary(entries, deps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SheetSummary())

    val memberBalances: StateFlow<List<MemberBalance>> = combine(members, dailyEntries, deposits) { mems, entries, deps ->
        Calculations.calculateMemberBalances(mems, entries, deps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily Entry Form State
    private val _selectedDate = MutableStateFlow(dateFormat.format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _mealCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val mealCounts: StateFlow<Map<String, Int>> = _mealCounts.asStateFlow()

    private val _bazarAmount = MutableStateFlow("")
    val bazarAmount: StateFlow<String> = _bazarAmount.asStateFlow()

    private val _bazarBy = MutableStateFlow("")
    val bazarBy: StateFlow<String> = _bazarBy.asStateFlow()

    // Deposit Form State
    private val _depositMemberId = MutableStateFlow("")
    val depositMemberId: StateFlow<String> = _depositMemberId.asStateFlow()

    private val _depositAmount = MutableStateFlow("")
    val depositAmount: StateFlow<String> = _depositAmount.asStateFlow()

    private val _depositRemark = MutableStateFlow("")
    val depositRemark: StateFlow<String> = _depositRemark.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadSheet()
        observeEntriesForCurrentDate()
    }

    private fun loadSheet() {
        viewModelScope.launch {
            firestoreRepository.getSheetFlow(sheetId).collect { loadedSheet ->
                _sheet.value = loadedSheet
                if (loadedSheet != null) {
                    val owner = loadedSheet.ownerId == currentUserId
                    val editor = loadedSheet.editors.contains(currentUserId)
                    _isOwner.value = owner
                    _canEdit.value = owner || editor

                    loadAllMembers(loadedSheet)
                }
            }
        }
    }

    private fun loadAllMembers(sheet: Sheet) {
        viewModelScope.launch {
            try {
                val memberIds = (sheet.editors + sheet.ownerId).distinct()
                val loadedMembers = firestoreRepository.getMembersByIds(memberIds)
                _members.value = loadedMembers

                // Default bazarBy to current user or owner if not set
                if (_bazarBy.value.isEmpty()) {
                    _bazarBy.value = if (memberIds.contains(currentUserId)) currentUserId else sheet.ownerId
                }
            } catch (e: Exception) {
                // Fallback minimal member objects
                _members.value = listOf(Member(userId = sheet.ownerId, name = sheet.ownerName.ifEmpty { "Owner" }))
            }
        }
    }

    private fun observeEntriesForCurrentDate() {
        viewModelScope.launch {
            dailyEntries.collect { entries ->
                val currentForDate = entries.find { it.date == _selectedDate.value }
                if (currentForDate != null && !_isSaving.value) {
                    _mealCounts.value = currentForDate.meals
                    _bazarAmount.value = currentForDate.bazarAmount?.takeIf { it > 0 }?.toString() ?: ""
                    _bazarBy.value = currentForDate.bazarBy
                }
            }
        }
    }

    fun onDateSelected(dateString: String) {
        _selectedDate.value = dateString
        // Load data for the newly selected date from existing entries
        val existingEntry = dailyEntries.value.find { it.date == dateString }
        if (existingEntry != null) {
            _mealCounts.value = existingEntry.meals
            _bazarAmount.value = existingEntry.bazarAmount?.takeIf { it > 0 }?.toString() ?: ""
            _bazarBy.value = existingEntry.bazarBy
        } else {
            // Reset fields for new entry date
            _mealCounts.value = emptyMap()
            _bazarAmount.value = ""
        }
    }

    fun onMealCountChanged(memberId: String, count: Int) {
        if (!_canEdit.value) return
        val current = _mealCounts.value.toMutableMap()
        if (count <= 0) {
            current.remove(memberId)
        } else {
            current[memberId] = count
        }
        _mealCounts.value = current
    }

    fun onBazarAmountChanged(amount: String) {
        if (!_canEdit.value) return
        // Allow numeric and decimal points
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d*$"))) {
            _bazarAmount.value = amount
        }
    }

    fun onBazarByChanged(memberId: String) {
        if (!_canEdit.value) return
        _bazarBy.value = memberId
    }

    fun loadEntryForEditing(entry: DailyEntry) {
        _selectedDate.value = entry.date
        _mealCounts.value = entry.meals
        _bazarAmount.value = entry.bazarAmount?.takeIf { it > 0 }?.toString() ?: ""
        _bazarBy.value = entry.bazarBy
    }

    fun saveMeals() {
        if (!_canEdit.value) return
        val date = _selectedDate.value
        _isSaving.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                firestoreRepository.saveMealsForDate(sheetId, date, _mealCounts.value)
                _successMessage.value = "Meals saved for $date"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to save meals"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun saveBazarCost() {
        if (!_canEdit.value) return
        val parsedAmount = _bazarAmount.value.toDoubleOrNull() // null if empty
        val date = _selectedDate.value
        val shopper = if (parsedAmount != null && parsedAmount > 0) _bazarBy.value else ""
        
        _isSaving.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                firestoreRepository.saveBazarCostForDate(sheetId, date, parsedAmount, shopper)
                _successMessage.value = "Bazar cost saved for $date"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to save bazar cost"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun onDepositMemberChanged(memberId: String) { _depositMemberId.value = memberId }
    fun onDepositAmountChanged(amount: String) { 
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d*$"))) {
            _depositAmount.value = amount 
        }
    }
    fun onDepositRemarkChanged(remark: String) { _depositRemark.value = remark }

    fun addDeposit() {
        if (!_canEdit.value) return
        val amount = _depositAmount.value.toDoubleOrNull() ?: 0.0
        if (amount <= 0.0) {
            _errorMessage.value = "Invalid deposit amount"
            return
        }
        val memberId = _depositMemberId.value.ifEmpty { currentUserId }
        val date = dateFormat.format(Date())
        
        _isSaving.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                firestoreRepository.addDeposit(Deposit(
                    sheetId = sheetId,
                    memberId = memberId,
                    amount = amount,
                    date = date,
                    remark = _depositRemark.value
                ))
                _depositAmount.value = ""
                _depositRemark.value = ""
                _successMessage.value = "Deposit added successfully"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to add deposit"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteDeposit(depositId: String) {
        if (!_canEdit.value) return
        viewModelScope.launch {
            try {
                firestoreRepository.deleteDeposit(depositId)
                _successMessage.value = "Deposit removed"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to remove deposit"
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
