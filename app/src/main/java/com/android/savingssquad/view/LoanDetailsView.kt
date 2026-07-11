package com.android.savingssquad.view

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import kotlinx.coroutines.launch
import java.util.Date
import com.android.savingssquad.model.Squad
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.MemberLoan
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.asTimestamp
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.singleton.displayText
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.viewmodel.SSToast
import com.yourapp.utils.CommonFunctions
import java.util.Calendar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp

@Composable
fun LoanDetailsView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared
) {
    val squad = squadViewModel.squad

    val screenType = if (UserDefaultsManager.getSquadManagerLogged()) {
        SquadUserType.SQUAD_MANAGER
    } else {
        SquadUserType.SQUAD_MEMBER
    }

// Get the member depending on the screen type
    val currentMember by if (screenType == SquadUserType.SQUAD_MANAGER) {
        squadViewModel.selectedMember.collectAsStateWithLifecycle()
    } else {
        squadViewModel.currentMember.collectAsStateWithLifecycle()
    }

    var squadLoans by remember { mutableStateOf<List<MemberLoan>?>(null) }
    var selectedStatus by remember { mutableStateOf<EMIStatus?>(null) }
    var selectedMember by remember { mutableStateOf("All") }

    // ===== FILTERED LOANS =====
    val filteredLoans = remember(squadLoans, selectedStatus, selectedMember) {
        (squadLoans ?: emptyList())
            .filter { loan ->
                (selectedStatus == null || loan.loanStatus == selectedStatus) &&
                        (selectedMember == "All" || loan.memberName == selectedMember)
            }
    }

    // ===== MEMBER LIST =====
    val memberList = remember(squadLoans) {
        val names = squadLoans?.map { it.memberName }?.toSet() ?: emptySet()
        listOf("All") + names.sorted()
    }

    // ===== LOAD LOANS =====
    LaunchedEffect(Unit) {
        val squadID = squad.value?.squadID ?: return@LaunchedEffect

        if (screenType == SquadUserType.SQUAD_MEMBER && currentMember?.id != null) {
            squadViewModel.fetchMemberLoans(true, currentMember!!.id!!) { success, _ ->
                squadLoans = squadViewModel.memberLoans.value
                LoaderManager.shared.hideLoader()
            }
        } else {
            squadViewModel.fetchAllLoansInSquad(true, squadID) { success, loans, _ ->
                if (success) squadLoans = loans
                LoaderManager.shared.hideLoader()
            }
        }
    }

    Box(
        modifier = Modifier

            .fillMaxSize()

            .windowInsetsPadding(WindowInsets.safeDrawing)
    )
    {
        AppBackgroundGradient()
        Column(modifier = Modifier.fillMaxSize()) {

            // NAV BAR
            SSNavigationBar(SquadStrings.loanDetails,navController)

            Spacer(modifier = Modifier.height(16.dp))

            // FILTER SECTION
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (screenType != SquadUserType.SQUAD_MEMBER) {
                    DropdownMenuPicker(
                        label = "Member",
                        selected = selectedMember,
                        items = memberList,
                        modifier = Modifier.weight(1f)
                    ) { selectedMember = it }
                }

                DropdownMenuPicker(
                    label = "Status",
                    selected = selectedStatus?.value ?: "All",
                    items = listOf("All", "Pending", "Paid", "Overdue"),
                    modifier = Modifier.weight(1f)
                ) {
                    selectedStatus = when (it.lowercase()) {
                        "pending" -> EMIStatus.PENDING
                        "paid" -> EMIStatus.PAID
                        "overdue" -> EMIStatus.OVERDUE
                        else -> null
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ===== LOAN LIST =====
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                when {
                    // 🔵 Loading State
                    squadLoans == null -> {
                        item {

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No loans yet",
                                    color = AppColors.secondaryText,
                                    modifier = Modifier.padding(top = 20.dp),
                                    style = AppFont.ibmPlexSans(14, FontWeight.Normal)
                                )
                            }
                        }
                    }

                    // 🔴 Empty State
                    filteredLoans.isEmpty() -> {
                        item {

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No loans yet",
                                    color = AppColors.secondaryText,
                                    modifier = Modifier.padding(top = 20.dp),
                                    style = AppFont.ibmPlexSans(14, FontWeight.Normal)
                                )
                            }
                        }
                    }

                    // 🟢 Loan List
                    else -> {
                        items(
                            items = filteredLoans,
                            key = { it.id ?: it.hashCode().toString() }   // Safe key
                        ) { loan ->
                            LoanCard(loan = loan)
                        }
                    }
                }
            }
        }

    }


}

@Composable
fun DropdownMenuPicker(
    label: String,
    selected: String,
    items: List<String>,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val borderColor = if (expanded) AppColors.primaryBrand else AppColors.border
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevronRotation"
    )

    Column(modifier = modifier) {

        Text(
            text = label.uppercase(),
            style = AppFont.ibmPlexSans(size = 10, weight = FontWeight.SemiBold),
            color = AppColors.secondaryText,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        Box {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .appShadow(AppShadows.card, RoundedCornerShape(14.dp))
                    .background(AppColors.surface, RoundedCornerShape(14.dp))
                    .border(
                        width = if (expanded) 1.5.dp else 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selected,
                    style = AppFont.ibmPlexSans(size = 14, weight = FontWeight.SemiBold),
                    color = AppColors.headerText,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (expanded) AppColors.primaryBrand else AppColors.secondaryText,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(AppColors.surface, RoundedCornerShape(14.dp))
                    .border(1.dp, AppColors.border.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            ) {
                items.forEach { item ->

                    val isSelected = item == selected

                    DropdownMenuItem(
                        modifier = Modifier
                            .background(
                                if (isSelected) AppColors.primaryBackground else androidx.compose.ui.graphics.Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 4.dp),
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item,
                                    style = AppFont.ibmPlexSans(
                                        size = 14,
                                        weight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) AppColors.primaryBrand else AppColors.headerText,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = AppColors.primaryBrand,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelected(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


private val inrFormatter: NumberFormat by lazy { NumberFormat.getNumberInstance(Locale("en", "IN")) }
private fun Int.currencyFormattedWithCommas(): String = "₹${inrFormatter.format(this)}"


// MARK: - Shared status color mapping

private fun statusColorFor(status: EMIStatus): Color = when (status) {
    EMIStatus.PAID, EMIStatus.CREATED -> AppColors.successAccent
    EMIStatus.PENDING, EMIStatus.INVERIFICATION -> AppColors.warningAccent
    EMIStatus.OVERDUE, EMIStatus.FAILED -> AppColors.errorAccent
}


// MARK: - Loan Card

@Composable
fun LoanCard(loan: MemberLoan) {

    var showInstallments by remember { mutableStateOf(false) }

    val statusColor = statusColorFor(loan.loanStatus)
    val paidCount = loan.installments.count { it.status == EMIStatus.PAID }
    val progressFraction = if (loan.installments.isEmpty()) 0f
    else paidCount.toFloat() / loan.installments.size.toFloat()
    val progressColor = if (loan.loanStatus == EMIStatus.PAID) AppColors.successAccent else AppColors.primaryBrand

    val chevronRotation by animateFloatAsState(
        targetValue = if (showInstallments) 180f else 0f,
        label = "chevronRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appShadow(AppShadows.card, RoundedCornerShape(20.dp))
            .background(color = AppColors.surface, shape = RoundedCornerShape(20.dp))
            .border(width = 1.dp, color = AppColors.border.copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // MARK: Header

        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(color = statusColor.copy(alpha = 0.12f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Loan #${loan.loanNumber}",
                    style = AppFont.ibmPlexSans(size = 15, weight = FontWeight.Bold),
                    color = AppColors.headerText
                )
                Text(
                    text = loan.memberName,
                    style = AppFont.ibmPlexSans(size = 12, weight = FontWeight.Medium),
                    color = AppColors.secondaryText,
                    maxLines = 1
                )
            }

            StatusBadge(text = loan.loanStatus.displayText, color = statusColor)
        }

        // MARK: Progress

        if (loan.installments.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Repayment Progress",
                        style = AppFont.ibmPlexSans(size = 11, weight = FontWeight.SemiBold),
                        color = AppColors.secondaryText,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$paidCount of ${loan.installments.size} paid",
                        style = AppFont.ibmPlexSans(size = 11, weight = FontWeight.Bold),
                        color = AppColors.headerText
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(color = AppColors.border.copy(alpha = 0.4f), shape = RoundedCornerShape(50))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progressFraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(color = progressColor, shape = RoundedCornerShape(50))
                    )
                }
            }
        }

        // MARK: Total Interest Paid

        val totalInterestPaid = loan.installments
            .filter { it.status == EMIStatus.PAID }
            .sumOf { it.interestAmount }

        if (totalInterestPaid > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = AppColors.primaryBackground, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color = AppColors.primaryBrand.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Percent,
                        contentDescription = null,
                        tint = AppColors.primaryBrand,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Text(
                    text = "Total Interest Paid",
                    style = AppFont.ibmPlexSans(size = 12, weight = FontWeight.Medium),
                    color = AppColors.secondaryText,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = totalInterestPaid.currencyFormattedWithCommas(),
                    style = AppFont.ibmPlexSans(size = 14, weight = FontWeight.Bold),
                    color = AppColors.primaryBrand
                )
            }
        }

        HorizontalDivider(color = AppColors.border.copy(alpha = 0.6f))

        // MARK: Stats

        Row(modifier = Modifier.fillMaxWidth()) {
            StatColumn(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CurrencyRupee,
                title = "Loan Amount",
                value = loan.loanAmount.currencyFormattedWithCommas()
            )
            StatDivider()
            StatColumn(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Percent,
                title = "Interest",
                value = String.format("%.2f%%", loan.interest)
            )
            StatDivider()
            StatColumn(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CalendarMonth,
                title = "Tenure",
                value = "${loan.loanMonth} mo"
            )
        }

        // MARK: Dates

        Row(modifier = Modifier.fillMaxWidth()) {
            StatColumn(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ArrowUpward,
                title = "Start Date",
                value = CommonFunctions.dateToString(loan.amountSentDate?.toDate() ?: java.util.Date()),
                small = true
            )
            StatDivider()
            StatColumn(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Verified,
                title = "Close Date",
                value = if (loan.loanStatus == EMIStatus.PAID)
                    CommonFunctions.dateToString(loan.loanClosedDate?.toDate() ?: java.util.Date())
                else "—",
                small = true
            )
        }

        // MARK: Installment Toggle

        if (loan.installments.isNotEmpty()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showInstallments = !showInstallments }
                    .background(color = AppColors.primaryBackground, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showInstallments) "Hide Installments" else "View Installments (${loan.installments.size})",
                    style = AppFont.ibmPlexSans(size = 12, weight = FontWeight.Bold),
                    color = AppColors.primaryBrand,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppColors.primaryBrand,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(chevronRotation)
                )
            }

            AnimatedVisibility(
                visible = showInstallments,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    loan.installments.forEachIndexed { index, installment ->
                        InstallmentRow(
                            installment = installment,
                            index = index + 1,
                            isLast = index == loan.installments.size - 1
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun StatColumn(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    small: Boolean = false
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (small) AppColors.secondaryText else AppColors.primaryBrand,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = title,
                style = AppFont.ibmPlexSans(size = 10, weight = FontWeight.Medium),
                color = AppColors.secondaryText
            )
        }
        Text(
            text = value,
            style = AppFont.ibmPlexSans(size = if (small) 12 else 13, weight = FontWeight.Bold),
            color = AppColors.headerText,
            maxLines = 1
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .padding(vertical = 2.dp)
            .background(AppColors.border.copy(alpha = 0.5f))
    )
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Row(
        modifier = Modifier
            .background(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color = color, shape = CircleShape)
        )
        Text(
            text = text,
            style = AppFont.ibmPlexSans(size = 11, weight = FontWeight.Bold),
            color = color
        )
    }
}


// MARK: - Installment Row (timeline style)

@Composable
fun InstallmentRow(
    installment: Installment,
    index: Int,
    isLast: Boolean
) {

    val statusColor = statusColorFor(installment.status)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (index == 1) 12.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // MARK: Timeline marker

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = if (installment.status == EMIStatus.PAID) AppColors.successAccent else AppColors.surface,
                        shape = CircleShape
                    )
                    .border(width = 1.5.dp, color = statusColor.copy(alpha = 0.4f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (installment.status == EMIStatus.PAID) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                } else {
                    Text(
                        text = "$index",
                        style = AppFont.ibmPlexSans(size = 10, weight = FontWeight.Bold),
                        color = statusColor
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f, fill = false)
                        .defaultMinSize(minHeight = 44.dp)
                        .background(AppColors.border.copy(alpha = 0.6f))
                )
            }
        }

        // MARK: Content card

        Column(
            modifier = Modifier
                .weight(1f)
                .background(color = AppColors.background.copy(alpha = 0.5f), shape = RoundedCornerShape(14.dp))
                .border(width = 1.dp, color = AppColors.border.copy(alpha = 0.5f), shape = RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Row(modifier = Modifier.fillMaxWidth()) {
                LabeledValue(
                    modifier = Modifier.weight(1f),
                    title = SquadStrings.dueDate,
                    value = CommonFunctions.dateToString(installment.dueDate?.toDate() ?: java.util.Date())
                )
                LabeledValue(
                    modifier = Modifier.weight(1f),
                    title = SquadStrings.paidDate,
                    value = installment.duePaidDate?.let { CommonFunctions.dateToString(it.toDate()) } ?: "—",
                    alignEnd = true
                )
            }

            HorizontalDivider(color = AppColors.border.copy(alpha = 0.5f))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                LabeledValue(
                    modifier = Modifier.weight(1f),
                    title = SquadStrings.amount,
                    value = installment.installmentAmount.currencyFormattedWithCommas()
                )
                LabeledValue(
                    modifier = Modifier.weight(1f),
                    title = SquadStrings.interest,
                    value = installment.interestAmount.currencyFormattedWithCommas()
                )
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text(
                        text = SquadStrings.status,
                        style = AppFont.ibmPlexSans(size = 10, weight = FontWeight.Medium),
                        color = AppColors.secondaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = installment.status.displayText,
                        style = AppFont.ibmPlexSans(size = 10, weight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier
                            .background(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(50))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LabeledValue(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    alignEnd: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(
            text = title,
            style = AppFont.ibmPlexSans(size = 10, weight = FontWeight.Medium),
            color = AppColors.secondaryText
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = AppFont.ibmPlexSans(size = 12, weight = FontWeight.Bold),
            color = AppColors.headerText
        )
    }
}