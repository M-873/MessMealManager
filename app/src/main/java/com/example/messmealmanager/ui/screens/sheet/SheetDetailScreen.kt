package com.example.messmealmanager.ui.screens.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.messmealmanager.model.DailyEntry
import com.example.messmealmanager.model.Deposit
import com.example.messmealmanager.model.Member
import com.example.messmealmanager.model.Sheet
import com.example.messmealmanager.util.MemberBalance
import com.example.messmealmanager.util.SheetSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetDetailScreen(
    viewModel: SheetDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToManageAccess: (String) -> Unit
) {
    val sheet by viewModel.sheet.collectAsState()
    val isOwner by viewModel.isOwner.collectAsState()
    val canEdit by viewModel.canEdit.collectAsState()
    val members by viewModel.members.collectAsState()
    val dailyEntries by viewModel.dailyEntries.collectAsState()
    val deposits by viewModel.deposits.collectAsState()
    val sheetSummary by viewModel.sheetSummary.collectAsState()
    val memberBalances by viewModel.memberBalances.collectAsState()

    val selectedDate by viewModel.selectedDate.collectAsState()
    val mealCounts by viewModel.mealCounts.collectAsState()
    val bazarAmount by viewModel.bazarAmount.collectAsState()
    val bazarBy by viewModel.bazarBy.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    
    val depositMemberId by viewModel.depositMemberId.collectAsState()
    val depositAmount by viewModel.depositAmount.collectAsState()
    val depositRemark by viewModel.depositRemark.collectAsState()

    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Expense", "Deposit", "Summary", "Individual Balance")
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = sheet?.sheetName ?: "Sheet Details",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        if (sheet != null) {
                            Text(
                                text = "Sheet #${sheet?.sheetNumber} • ${if (canEdit) (if (isOwner) "Owner" else "Editor") else "Viewer"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isOwner && sheet != null) {
                        IconButton(onClick = { onNavigateToManageAccess(sheet!!.id) }) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Manage Access",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (sheet == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> ExpenseTab(
                    selectedDate = selectedDate,
                    mealCounts = mealCounts,
                    bazarAmount = bazarAmount,
                    bazarBy = bazarBy,
                    canEdit = canEdit,
                    members = members,
                    dailyEntries = dailyEntries,
                    isSaving = isSaving,
                    onDateSelected = { viewModel.onDateSelected(it) },
                    onMealCountChanged = { memberId, count -> viewModel.onMealCountChanged(memberId, count) },
                    onBazarAmountChanged = { viewModel.onBazarAmountChanged(it) },
                    onBazarByChanged = { viewModel.onBazarByChanged(it) },
                    onSaveMeals = { viewModel.saveMeals() },
                    onSaveBazarCost = { viewModel.saveBazarCost() },
                    onLoadEntry = { viewModel.loadEntryForEditing(it) }
                )
                1 -> DepositTab(
                    canEdit = canEdit,
                    members = members,
                    deposits = deposits,
                    depositMemberId = depositMemberId,
                    depositAmount = depositAmount,
                    depositRemark = depositRemark,
                    isSaving = isSaving,
                    onMemberChanged = { viewModel.onDepositMemberChanged(it) },
                    onAmountChanged = { viewModel.onDepositAmountChanged(it) },
                    onRemarkChanged = { viewModel.onDepositRemarkChanged(it) },
                    onAddDeposit = { viewModel.addDeposit() },
                    onDeleteDeposit = { viewModel.deleteDeposit(it) }
                )
                2 -> SummaryTab(summary = sheetSummary)
                3 -> IndividualBalanceTab(balances = memberBalances)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTab(
    selectedDate: String,
    mealCounts: Map<String, Int>,
    bazarAmount: String,
    bazarBy: String,
    canEdit: Boolean,
    members: List<Member>,
    dailyEntries: List<DailyEntry>,
    isSaving: Boolean,
    onDateSelected: (String) -> Unit,
    onMealCountChanged: (String, Int) -> Unit,
    onBazarAmountChanged: (String) -> Unit,
    onBazarByChanged: (String) -> Unit,
    onSaveMeals: () -> Unit,
    onSaveBazarCost: () -> Unit,
    onLoadEntry: (DailyEntry) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!canEdit) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View Only Mode: You are viewing this sheet without editor privileges.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Date Selector Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Entry Date",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedDate,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Button(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Pick Date")
                    }
                }
            }
        }

        // Member Meals Counter Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Member Meals for $selectedDate",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (members.isEmpty()) {
                        Text(
                            text = "No members registered on this sheet yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        members.forEach { member ->
                            val currentCount = mealCounts[member.userId] ?: 0
                            MemberMealRow(
                                member = member,
                                count = currentCount,
                                canEdit = canEdit,
                                onCountChange = { newCount -> onMealCountChanged(member.userId, newCount) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (canEdit) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onSaveMeals,
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Meals")
                        }
                    }
                }
            }
        }

        // Bazar Expense Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bazar Cost for $selectedDate",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = bazarAmount,
                        onValueChange = onBazarAmountChanged,
                        label = { Text("Bazar Amount (৳)") },
                        placeholder = { Text("0.00 or empty") },
                        enabled = canEdit,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    BazarByDropdown(
                        selectedMemberId = bazarBy,
                        members = members,
                        canEdit = canEdit,
                        onMemberSelected = onBazarByChanged
                    )

                    if (canEdit) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onSaveBazarCost,
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Bazar Cost")
                        }
                    }
                }
            }
        }

        // History / Previous Days List
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Previous Recorded Days (${dailyEntries.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        if (dailyEntries.isEmpty()) {
            item {
                Text(
                    text = "No entries recorded yet for this sheet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val sortedEntries = dailyEntries.sortedByDescending { it.date }
            items(sortedEntries, key = { it.id }) { entry ->
                val shopperMember = members.find { it.userId == entry.bazarBy }
                val shopperName = shopperMember?.name ?: entry.bazarBy.ifEmpty { "None" }
                val totalMeals = entry.meals.values.sum()

                DailyEntryHistoryCard(
                    entry = entry,
                    totalMeals = totalMeals,
                    shopperName = shopperName,
                    isSelected = entry.date == selectedDate,
                    onClick = { onLoadEntry(entry) }
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            onDateSelected(formatter.format(Date(selectedMillis)))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun DepositTab(
    canEdit: Boolean,
    members: List<Member>,
    deposits: List<Deposit>,
    depositMemberId: String,
    depositAmount: String,
    depositRemark: String,
    isSaving: Boolean,
    onMemberChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onRemarkChanged: (String) -> Unit,
    onAddDeposit: () -> Unit,
    onDeleteDeposit: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (canEdit) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Add Deposit",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        BazarByDropdown(
                            selectedMemberId = depositMemberId,
                            members = members,
                            canEdit = true,
                            onMemberSelected = onMemberChanged
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = depositAmount,
                            onValueChange = onAmountChanged,
                            label = { Text("Amount (৳)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = depositRemark,
                            onValueChange = onRemarkChanged,
                            label = { Text("Remark (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onAddDeposit,
                            enabled = !isSaving && depositAmount.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Add Deposit")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Deposit History (${deposits.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (deposits.isEmpty()) {
            item {
                Text(
                    text = "No deposits recorded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val sortedDeposits = deposits.sortedByDescending { it.date }
            items(sortedDeposits, key = { it.id }) { deposit ->
                val memberName = members.find { it.userId == deposit.memberId }?.name ?: "Unknown"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = memberName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${deposit.date} ${if (deposit.remark.isNotBlank()) "• ${deposit.remark}" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "৳${deposit.amount}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (canEdit) {
                            IconButton(onClick = { onDeleteDeposit(deposit.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Deposit", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryTab(summary: SheetSummary) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SummaryCard(title = "Total Meals", value = "${summary.totalMeals}")
        SummaryCard(title = "Total Deposit", value = "৳${String.format(Locale.getDefault(), "%.2f", summary.totalDeposit)}")
        SummaryCard(title = "Total Cost", value = "৳${String.format(Locale.getDefault(), "%.2f", summary.totalCost)}")
        SummaryCard(title = "Per Meal Cost", value = "৳${String.format(Locale.getDefault(), "%.4f", summary.perMealCost)}")
        SummaryCard(title = "Current Balance (Deposit - Cost)", value = "৳${String.format(Locale.getDefault(), "%.2f", summary.currentBalance)}")
    }
}

@Composable
fun SummaryCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun IndividualBalanceTab(balances: List<MemberBalance>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(balances, key = { it.member.userId }) { balance ->
            var expanded by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = balance.member.name.ifEmpty { "Unknown" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val color = if (balance.dueBalance >= 0) Color(0xFF388E3C) else MaterialTheme.colorScheme.error
                            Text(
                                text = "৳${String.format(Locale.getDefault(), "%.2f", balance.dueBalance)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = color
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Divider(modifier = Modifier.padding(bottom = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Meals", style = MaterialTheme.typography.bodyMedium)
                                Text("${balance.totalMeals}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Deposit", style = MaterialTheme.typography.bodyMedium)
                                Text("৳${String.format(Locale.getDefault(), "%.2f", balance.totalDeposit)}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Individual Expense", style = MaterialTheme.typography.bodyMedium)
                                Text("৳${String.format(Locale.getDefault(), "%.2f", balance.individualExpense)}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val isOwed = balance.dueBalance >= 0
                            Text(
                                text = if (isOwed) "Gets Back: ৳${String.format(Locale.getDefault(), "%.2f", balance.dueBalance)}"
                                       else "Owes: ৳${String.format(Locale.getDefault(), "%.2f", balance.dueBalance.absoluteValue)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isOwed) Color(0xFF388E3C) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

// Reuse the same helper components
@Composable
fun MemberMealRow(
    member: Member,
    count: Int,
    canEdit: Boolean,
    onCountChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = member.name.take(1).uppercase().ifEmpty { "?" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = member.name.ifEmpty { "Member" },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        if (canEdit) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(
                    onClick = { if (count > 0) onCountChange(count - 1) },
                    enabled = count > 0,
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                }

                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 14.dp)
                )

                FilledIconButton(
                    onClick = { onCountChange(count + 1) },
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "$count meals",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BazarByDropdown(
    selectedMemberId: String,
    members: List<Member>,
    canEdit: Boolean,
    onMemberSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedMember = members.find { it.userId == selectedMemberId }
    val displayText = selectedMember?.name ?: "Select Member"

    ExposedDropdownMenuBox(
        expanded = expanded && canEdit,
        onExpandedChange = { if (canEdit) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Member") },
            trailingIcon = {
                if (canEdit) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            enabled = canEdit,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            members.forEach { member ->
                DropdownMenuItem(
                    text = { Text(member.name.ifEmpty { member.email }) },
                    onClick = {
                        onMemberSelected(member.userId)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DailyEntryHistoryCard(
    entry: DailyEntry,
    totalMeals: Int,
    shopperName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = entry.date,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                val bazarText = if ((entry.bazarAmount ?: 0.0) > 0) "৳${entry.bazarAmount} (by $shopperName)" else "No Bazar"
                Text(
                    text = "Bazar: $bazarText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "$totalMeals Meals",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
