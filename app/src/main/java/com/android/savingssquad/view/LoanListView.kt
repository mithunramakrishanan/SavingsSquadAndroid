package com.android.savingssquad.view

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.MemberLoan
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.singleton.LoaderManager
import com.yourapp.utils.CommonFunctions
import java.text.NumberFormat
import java.util.Locale

// =========================================================================
// MAIN SCREEN — list shows only summary details, tap opens the full detail
// =========================================================================

@Composable
fun LoanListView(
    navController: NavController,
    squadViewModel: SquadViewModel
) {
    val squad = squadViewModel.squad

    val screenType = if (UserDefaultsManager.getSquadManagerLogged()) {
        SquadUserType.SQUAD_MANAGER
    } else {
        SquadUserType.SQUAD_MEMBER
    }

    val currentMember by if (screenType == SquadUserType.SQUAD_MANAGER) {
        squadViewModel.selectedMember.collectAsStateWithLifecycle()
    } else {
        squadViewModel.currentMember.collectAsStateWithLifecycle()
    }

    var squadLoans by remember { mutableStateOf<List<MemberLoan>?>(null) }
    var selectedStatus by remember { mutableStateOf(SquadStrings.all) }
    var selectedMember by remember { mutableStateOf(SquadStrings.all) }

    // The loan currently pushed into the full-detail screen (null = showing the list)
    var openedLoan by remember { mutableStateOf<MemberLoan?>(null) }

    // Back press closes the detail screen first, instead of leaving LoanDetailsView
    BackHandler(enabled = openedLoan != null) {
        openedLoan = null
    }

    val filteredLoans = remember(squadLoans, selectedStatus, selectedMember) {
        (squadLoans ?: emptyList())
            .filter { loan ->
                (loan.loanStatus.localizedName == selectedStatus) &&
                        (selectedMember == SquadStrings.all || loan.memberName == selectedMember)
            }
    }

    val memberList = remember(squadLoans) {
        val names = squadLoans?.map { it.memberName }?.toSet() ?: emptySet()
        listOf(SquadStrings.all) + names.sorted()
    }

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
    ) {
        AppBackgroundGradient()

        if (openedLoan == null) {
            // ===== LIST =====
            Column(modifier = Modifier.fillMaxSize()) {

                SSNavigationBar(SquadStrings.loanDetails, navController)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (screenType != SquadUserType.SQUAD_MEMBER) {
                        DropdownMenuPicker(
                            selected = selectedMember,
                            items = memberList,
                            icon = Icons.Default.Group,
                            modifier = Modifier.weight(1f)
                        ) { selectedMember = it }
                    }

                    DropdownMenuPicker(
                        selected = selectedStatus,
                        items = listOf(SquadStrings.all, SquadStrings.pending, SquadStrings.paid,SquadStrings.overdue),
                        icon = Icons.Default.Tune,
                        modifier = Modifier.weight(1f)
                    ) {

                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when {
                        squadLoans == null -> {
                            item { EmptyStateText("Loading loans...") }
                        }
                        filteredLoans.isEmpty() -> {
                            item { EmptyStateText(SquadStrings.noLoansYet) }
                        }
                        else -> {
                            items(
                                items = filteredLoans,
                                key = { it.id ?: it.hashCode().toString() }
                            ) { loan ->
                                LoanSummaryCard(loan = loan, onClick = { openedLoan = loan })
                            }
                        }
                    }
                }
            }
        } else {
            // ===== FULL DETAIL (pushed on top of the list) =====
            LoanFullDetailView(
                loan = openedLoan!!,
                navController = navController,
                onBack = { openedLoan = null }
            )
        }
    }
}

@Composable
private fun EmptyStateText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = AppColors.secondaryText,
            modifier = Modifier.padding(top = 20.dp),
            style = AppFont.ibmPlexSans(14, FontWeight.Normal)
        )
    }
}

// =========================================================================
// DROPDOWN PICKER (unchanged)
// =========================================================================

@Composable
fun DropdownMenuPicker(
    selected: String,
    items: List<String>,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.surface, RoundedCornerShape(14.dp))
                .border(1.dp, AppColors.border.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.primaryBrand,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = selected,
                style = AppFont.ibmPlexSans(size = 13, weight = FontWeight.Medium),
                color = AppColors.headerText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = AppColors.secondaryText,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AppColors.surface, RoundedCornerShape(14.dp))
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item,
                                style = AppFont.ibmPlexSans(size = 13, weight = FontWeight.Medium),
                                modifier = Modifier.weight(1f)
                            )
                            if (item == selected) {
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
                        expanded = false
                        onSelected(item)
                    }
                )
            }
        }
    }
}

private val inrFormatter: NumberFormat by lazy { NumberFormat.getNumberInstance(Locale("en", "IN")) }
private fun Int.currencyFormattedWithCommas(): String = "₹${inrFormatter.format(this)}"

private fun statusColorFor(status: EMIStatus): Color = when (status) {
    EMIStatus.PAID, EMIStatus.CREATED -> AppColors.successAccent
    EMIStatus.PENDING, EMIStatus.INVERIFICATION -> AppColors.warningAccent
    EMIStatus.OVERDUE, EMIStatus.FAILED -> AppColors.errorAccent
}

// =========================================================================
// LOAN SUMMARY CARD — shown in the list. Only the "main" details.
// =========================================================================

@Composable
fun LoanSummaryCard(loan: MemberLoan, onClick: () -> Unit) {

    val statusColor = statusColorFor(loan.loanStatus)
    val paidCount = loan.installments.count { it.status == EMIStatus.PAID }
    val progressFraction = if (loan.installments.isEmpty()) 0f
    else paidCount.toFloat() / loan.installments.size.toFloat()
    val progressColor = if (loan.loanStatus == EMIStatus.PAID) AppColors.successAccent else AppColors.primaryBrand

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appShadow(AppShadows.card, RoundedCornerShape(20.dp))
            .background(color = AppColors.surface, shape = RoundedCornerShape(20.dp))
            .border(width = 1.dp, color = AppColors.border.copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // Header: loan #, member, status
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color = statusColor.copy(alpha = 0.12f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Loan #${loan.loanNumber}",
                    style = AppFont.ibmPlexSans(size = 14, weight = FontWeight.Bold),
                    color = AppColors.headerText
                )
                Text(
                    text = loan.memberName,
                    style = AppFont.ibmPlexSans(size = 12, weight = FontWeight.Medium),
                    color = AppColors.secondaryText,
                    maxLines = 1
                )
            }

            StatusBadge(text = loan.loanStatus.localizedName, color = statusColor)
        }

        // Main numbers only: amount, tenure, paid count
        Row(modifier = Modifier.fillMaxWidth()) {
            StatColumn(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CurrencyRupee,
                title = SquadStrings.loanAmount,
                value = loan.loanAmount.currencyFormattedWithCommas()
            )
            StatDivider()
            StatColumn(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CalendarMonth,
                title = "Tenure",
                value = "${loan.loanMonth} mo"
            )
            StatDivider()
            StatColumn(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.TaskAlt,
                title = SquadStrings.paid,
                value = "$paidCount/${loan.installments.size}"
            )
        }

        // Progress bar
        if (loan.installments.isNotEmpty()) {
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

        // Tap hint
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = SquadStrings.viewDetails,
                style = AppFont.ibmPlexSans(size = 11, weight = FontWeight.Bold),
                color = AppColors.primaryBrand
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AppColors.primaryBrand,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// =========================================================================
// LOAN FULL DETAIL VIEW — everything else lives here
// =========================================================================

@Composable
fun LoanFullDetailView(
    loan: MemberLoan,
    navController: NavController,
    onBack: () -> Unit
) {
    var showInstallments by remember { mutableStateOf(true) }

    val statusColor = statusColorFor(loan.loanStatus)
    val paidCount = loan.installments.count { it.status == EMIStatus.PAID }
    val canForeclose = paidCount >= 1 && loan.loanStatus != EMIStatus.PAID && !loan.isForceClosed

    val totalInterestPaid = if (loan.isForceClosed) {
        loan.installments.filter { it.status == EMIStatus.PAID }.sumOf { it.interestAmount } +
                loan.forceCloseSummary.recalculatedInterest
    } else {
        loan.installments.filter { it.status == EMIStatus.PAID }.sumOf { it.interestAmount }
    }

    // Unified timeline: real installments, plus (if foreclosed) a synthetic
    // "settlement" step in place of the un-needed remaining installments —
    // so the story reads naturally instead of showing confusing "pending" rows
    // for a loan that's already closed.
    val timelineSteps: List<TimelineStep> = remember(loan) {
        if (loan.isForceClosed) {
            val paidOnes = loan.installments
                .mapIndexed { index, installment -> index to installment }
                .filter { it.second.status == EMIStatus.PAID }
                .map { TimelineStep.InstallmentStep(it.second, number = it.first + 1) }
            paidOnes + TimelineStep.ForeclosureStep
        } else {
            loan.installments.mapIndexed { index, installment ->
                TimelineStep.InstallmentStep(installment, number = index + 1)
            }
        }
    }

    val chevronRotation by animateFloatAsState(
        targetValue = if (showInstallments) 180f else 0f,
        label = "chevronRotation"
    )

    Column(modifier = Modifier.fillMaxSize()) {

        // Simple top bar with back action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = AppColors.headerText,
                modifier = Modifier
                    .size(22.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Loan #${loan.loanNumber}",
                style = AppFont.ibmPlexSans(size = 16, weight = FontWeight.Bold),
                color = AppColors.headerText
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // MARK: Header card

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = AppColors.surface, shape = RoundedCornerShape(20.dp))
                    .border(width = 1.dp, color = AppColors.border.copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp))
                    .appShadow(AppShadows.card, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(color = statusColor.copy(alpha = 0.12f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Loan #${loan.loanNumber}",
                        style = AppFont.ibmPlexSans(size = 16, weight = FontWeight.Bold),
                        color = AppColors.headerText
                    )
                    Text(
                        text = loan.memberName,
                        style = AppFont.ibmPlexSans(size = 13, weight = FontWeight.Medium),
                        color = AppColors.secondaryText
                    )
                }

                StatusBadge(text = loan.loanStatus.localizedName, color = statusColor)
            }

            // MARK: Story card — plain-language explanation of what's going on

            LoanStoryCard(loan = loan, paidCount = paidCount)

            // MARK: Stats grid (amount, interest, tenure)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = AppColors.surface, shape = RoundedCornerShape(20.dp))
                    .border(width = 1.dp, color = AppColors.border.copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                StatColumn(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CurrencyRupee,
                    title = SquadStrings.loanAmount,
                    value = loan.loanAmount.currencyFormattedWithCommas()
                )
                StatDivider()
                StatColumn(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Percent,
                    title = "(${loan.emiConfiguration?.interestType?.name ?: ""})",
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = AppColors.surface, shape = RoundedCornerShape(20.dp))
                    .border(width = 1.dp, color = AppColors.border.copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
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
                    value = if (loan.loanStatus == EMIStatus.PAID || loan.isForceClosed)
                        CommonFunctions.dateToString(loan.loanClosedDate?.toDate() ?: java.util.Date())
                    else "—",
                    small = true
                )
            }

            // MARK: Total Interest Paid

            if (totalInterestPaid > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = AppColors.surface, shape = RoundedCornerShape(20.dp))
                        .border(width = 1.dp, color = AppColors.border.copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp))
                        .padding(16.dp),
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

            // MARK: Foreclose button (only offered when not already foreclosed)

            if (!loan.isForceClosed && canForeclose) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO: hook up your foreclose action, e.g. onForeclose(loan) */ }
                        .background(color = AppColors.primaryBrand.copy(alpha = 0.08f), shape = RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = AppColors.primaryBrand,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Foreclose Loan",
                        style = AppFont.ibmPlexSans(size = 12, weight = FontWeight.Bold),
                        color = AppColors.primaryBrand,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = AppColors.primaryBrand,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // MARK: Timeline (installments + foreclosure settlement, if applicable)

            if (timelineSteps.isNotEmpty()) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showInstallments = !showInstallments }
                        .background(color = AppColors.surface, shape = RoundedCornerShape(20.dp))
                        .border(width = 1.dp, color = AppColors.border.copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showInstallments) "Hide Timeline" else "View Timeline (${timelineSteps.size})",
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
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        timelineSteps.forEachIndexed { index, step ->
                            val isLast = index == timelineSteps.size - 1
                            when (step) {
                                is TimelineStep.InstallmentStep -> InstallmentRow(
                                    installment = step.installment,
                                    index = step.number,
                                    isLast = isLast
                                )
                                TimelineStep.ForeclosureStep -> ForecloseSettlementRow(
                                    loan = loan,
                                    isLast = isLast
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// A step in the loan's timeline — either a real installment, or (for a
// foreclosed loan) the synthetic settlement step that replaces the
// remaining, never-actually-paid installments.
private sealed class TimelineStep {
    data class InstallmentStep(val installment: Installment, val number: Int) : TimelineStep()
    object ForeclosureStep : TimelineStep()
}

// =========================================================================
// LOAN STORY CARD — plain-language explanation of what's going on
// =========================================================================

@Composable
private fun LoanStoryCard(loan: MemberLoan, paidCount: Int) {

    val total = loan.installments.size

    val (icon, color, title) = when {
        loan.isForceClosed -> Triple(Icons.Default.Bolt, AppColors.primaryBrand, "Closed Early (Foreclosed)")
        loan.loanStatus == EMIStatus.PAID -> Triple(Icons.Default.CheckCircle, AppColors.successAccent, "Fully Repaid")
        loan.loanStatus == EMIStatus.OVERDUE || loan.loanStatus == EMIStatus.FAILED ->
            Triple(Icons.Default.Warning, AppColors.errorAccent, "Payment Overdue")
        else -> Triple(Icons.Default.Info, AppColors.warningAccent, "In Progress")
    }

    val message: String = when {
        loan.isForceClosed -> {
            val asOf = CommonFunctions.dateToString(loan.forceCloseSummary.asOfDate)
            "${loan.memberName} paid off this loan early. After completing $paidCount of $total monthly installments, " +
                    "the remaining balance was settled in one payment on $asOf — " +
                    "₹${loan.forceCloseSummary.outstandingPrincipal} principal plus ₹${loan.forceCloseSummary.recalculatedInterest} " +
                    "in recalculated interest, totalling ${loan.forceCloseSummary.totalPayable.currencyFormattedWithCommas()}."
        }
        loan.loanStatus == EMIStatus.PAID -> {
            val closeDate = CommonFunctions.dateToString(loan.loanClosedDate?.toDate() ?: java.util.Date())
            "${loan.memberName} has fully repaid this loan across all $total monthly installments. " +
                    "The final payment was made on $closeDate."
        }
        loan.loanStatus == EMIStatus.OVERDUE || loan.loanStatus == EMIStatus.FAILED -> {
            val overdueInstallment = loan.installments.firstOrNull {
                it.status == EMIStatus.OVERDUE || it.status == EMIStatus.FAILED
            }
            if (overdueInstallment != null) {
                val due = CommonFunctions.dateToString(overdueInstallment.dueDate?.toDate() ?: java.util.Date())
                "An installment of ${overdueInstallment.installmentAmount.currencyFormattedWithCommas()} was due on $due " +
                        "and hasn't been paid yet. $paidCount of $total installments are paid so far."
            } else {
                "This loan has an overdue payment. $paidCount of $total installments are paid so far."
            }
        }
        paidCount == 0 -> {
            val firstInstallment = loan.installments.firstOrNull()
            if (firstInstallment != null) {
                val due = CommonFunctions.dateToString(firstInstallment.dueDate?.toDate() ?: java.util.Date())
                "${loan.memberName} hasn't made a payment yet. The first installment of " +
                        "${firstInstallment.installmentAmount.currencyFormattedWithCommas()} is due on $due."
            } else {
                "${loan.memberName} hasn't made a payment yet."
            }
        }
        else -> {
            val nextDue = loan.installments.firstOrNull { it.status == EMIStatus.PENDING }
            val emi = loan.emiConfiguration?.emiAmount ?: 0
            if (nextDue != null) {
                val due = CommonFunctions.dateToString(nextDue.dueDate?.toDate() ?: java.util.Date())
                "${loan.memberName} is repaying this loan in $total monthly installments of ${emi.currencyFormattedWithCommas()} each. " +
                        "So far $paidCount of $total have been paid. The next payment is due on $due."
            } else {
                "${loan.memberName} is repaying this loan in $total monthly installments. $paidCount of $total have been paid so far."
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = color.copy(alpha = 0.08f), shape = RoundedCornerShape(16.dp))
            .border(width = 1.dp, color = color.copy(alpha = 0.25f), shape = RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(color = color.copy(alpha = 0.15f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }

        Column {
            Text(
                text = title,
                style = AppFont.ibmPlexSans(size = 12, weight = FontWeight.Bold),
                color = color
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = message,
                style = AppFont.ibmPlexSans(size = 12, weight = FontWeight.Medium),
                color = AppColors.headerText
            )
        }
    }
}

// =========================================================================
// FORECLOSURE SETTLEMENT ROW
// Rendered as the final step in the timeline when a loan was force-closed —
// makes it read like "the loan was paid off here" instead of leaving the
// remaining scheduled installments dangling as "pending".
// =========================================================================

@Composable
fun ForecloseSettlementRow(loan: MemberLoan, isLast: Boolean) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color = AppColors.primaryBrand, shape = CircleShape)
                    .border(width = 1.5.dp, color = AppColors.primaryBrand.copy(alpha = 0.4f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
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

        Column(
            modifier = Modifier
                .weight(1f)
                .background(color = AppColors.primaryBrand.copy(alpha = 0.06f), shape = RoundedCornerShape(14.dp))
                .border(width = 1.dp, color = AppColors.primaryBrand.copy(alpha = 0.3f), shape = RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Row(modifier = Modifier.fillMaxWidth()) {
                LabeledValue(
                    modifier = Modifier.weight(1f),
                    title = "Settled On",
                    value = CommonFunctions.dateToString(loan.forceCloseSummary.asOfDate)
                )
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text(
                        text = SquadStrings.status,
                        style = AppFont.ibmPlexSans(size = 10, weight = FontWeight.Medium),
                        color = AppColors.secondaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Foreclosed",
                        style = AppFont.ibmPlexSans(size = 10, weight = FontWeight.Bold),
                        color = AppColors.primaryBrand,
                        modifier = Modifier
                            .background(color = AppColors.primaryBrand.copy(alpha = 0.12f), shape = RoundedCornerShape(50))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = AppColors.border.copy(alpha = 0.5f))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                LabeledValue(
                    modifier = Modifier.weight(1f),
                    title = "Remaining Principal",
                    value = loan.forceCloseSummary.outstandingPrincipal.currencyFormattedWithCommas()
                )
                LabeledValue(
                    modifier = Modifier.weight(1f),
                    title = SquadStrings.interest,
                    value = loan.forceCloseSummary.recalculatedInterest.currencyFormattedWithCommas()
                )
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Paid",
                        style = AppFont.ibmPlexSans(size = 10, weight = FontWeight.Medium),
                        color = AppColors.secondaryText
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = loan.forceCloseSummary.totalPayable.currencyFormattedWithCommas(),
                        style = AppFont.ibmPlexSans(size = 12, weight = FontWeight.Bold),
                        color = AppColors.primaryBrand
                    )
                }
            }

            Text(
                text = "Instead of continuing with the remaining monthly installments, the full outstanding balance was paid off here in a single payment.",
                style = AppFont.ibmPlexSans(size = 11, weight = FontWeight.Medium),
                color = AppColors.secondaryText
            )
        }
    }
}

@Composable
private fun StatColumn(
    modifier: Modifier = Modifier,
    icon: ImageVector,
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

// =========================================================================
// INSTALLMENT ROW (unchanged, used inside the full detail screen)
// =========================================================================

@Composable
fun InstallmentRow(
    installment: Installment,
    index: Int,
    isLast: Boolean
) {
    val statusColor = statusColorFor(installment.status)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

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
                        text = installment.status.localizedName,
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