package com.android.savingssquad.view

import androidx.activity.compose.BackHandler
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
import com.android.savingssquad.model.GroupFund
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.MemberLoan
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.GroupFundUserType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.asTimestamp
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.singleton.displayText
import com.android.savingssquad.viewmodel.AlertManager
import com.yourapp.utils.CommonFunctions
import java.util.Calendar

@Composable
fun LoanDetailsView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared
) {
    val groupFund = squadViewModel.groupFund
    val screenType =
        if (UserDefaultsManager.getGroupFundManagerLogged())
            GroupFundUserType.GROUP_FUND_MANAGER
        else
            GroupFundUserType.GROUP_FUND_MEMBER

    val currentMember = squadViewModel.currentMember.collectAsState().value

    var groupFundLoans by remember { mutableStateOf<List<MemberLoan>?>(null) }
    var selectedStatus by remember { mutableStateOf<EMIStatus?>(null) }
    var selectedMember by remember { mutableStateOf("All") }

    // ===== FILTERED LOANS =====
    val filteredLoans = remember(groupFundLoans, selectedStatus, selectedMember) {
        (groupFundLoans ?: emptyList())
            .filter { loan ->
                (selectedStatus == null || loan.loanStatus == selectedStatus) &&
                        (selectedMember == "All" || loan.memberName == selectedMember)
            }
    }

    // ===== MEMBER LIST =====
    val memberList = remember(groupFundLoans) {
        val names = groupFundLoans?.map { it.memberName }?.toSet() ?: emptySet()
        listOf("All") + names.sorted()
    }

    // ===== LOAD LOANS =====
    LaunchedEffect(screenType, currentMember?.id, groupFundLoans) {
        val groupFundID = groupFund.value?.groupFundID ?: return@LaunchedEffect

        if (screenType == GroupFundUserType.GROUP_FUND_MEMBER && currentMember?.id != null) {
            squadViewModel.fetchMemberLoans(true, currentMember.id!!) { success, _ ->
                groupFundLoans = squadViewModel.memberLoans.value
            }
        } else {
            squadViewModel.fetchAllLoansInGroupFund(true, groupFundID) { success, loans, _ ->
                if (success) groupFundLoans = loans
            }
        }
    }

    // ==== UI BODY ====
    AppBackgroundGradient()

    Column(modifier = Modifier.fillMaxSize()) {

        // NAV BAR
        SSNavigationBar("Loan Details",navController)

        Spacer(modifier = Modifier.height(16.dp))

        // FILTER SECTION
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (screenType != GroupFundUserType.GROUP_FUND_MEMBER) {
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
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            when {
                // 🔵 Loading State
                groupFundLoans == null -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Loading...",
                                style = AppFont.ibmPlexSans(14),
                                color = AppColors.secondaryText
                            )
                        }
                    }
                }

                // 🔴 Empty State
                filteredLoans.isEmpty() -> {
                    item {
                        Text(
                            text = "No loans found",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            style = AppFont.ibmPlexSans(15),
                            color = AppColors.secondaryText
                        )
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

@Composable
fun DropdownMenuPicker(
    label: String,
    selected: String,
    items: List<String>,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = AppFont.ibmPlexSans(12),
            color = AppColors.secondaryText,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .appShadow(AppShadows.card, RoundedCornerShape(12.dp))
                .background(AppColors.surface, RoundedCornerShape(12.dp))
                .border(1.dp, AppColors.border, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(12.dp)
        ) {
            Text(
                text = selected,
                style = AppFont.ibmPlexSans(14),
                color = AppColors.headerText
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item,
                            style = AppFont.ibmPlexSans(14),
                            color = AppColors.headerText
                        )
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

@Composable
fun LoanCard(loan: MemberLoan) {
    var showInstallments by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .appShadow(AppShadows.card, RoundedCornerShape(16.dp))
            .background(AppColors.surface, RoundedCornerShape(16.dp))
            .border(0.5.dp, AppColors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {

        // HEADER
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    "Loan #${loan.loanNumber}",
                    style = AppFont.ibmPlexSans(17, FontWeight.Bold),
                    color = AppColors.headerText
                )
                Text(
                    "Member: ${loan.memberName}",
                    style = AppFont.ibmPlexSans(14),
                    color = AppColors.secondaryText
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                loan.loanStatus.displayText,
                style = AppFont.ibmPlexSans(12, FontWeight.Bold),
                modifier = Modifier
                    .background(statusColor(loan.loanStatus).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = statusColor(loan.loanStatus)
            )
        }

        Divider(color = AppColors.border, thickness = 1.dp)

        Row(Modifier.padding(vertical = 6.dp)) {
            InfoColumn("Loan Amount", loan.loanAmount.currencyFormattedWithCommas())
            Spacer(modifier = Modifier.weight(1f))
            InfoColumn("Interest", "${loan.interest}%")
            Spacer(modifier = Modifier.weight(1f))
            InfoColumn("Months", "${loan.loanMonth}")
        }

        Row {
            InfoColumn("Start Date", CommonFunctions.dateToString(loan.amountSentDate?.toDate() ?: Date()))
            Spacer(modifier = Modifier.weight(1f))
            InfoColumn("Close Date", CommonFunctions.dateToString(loan.loanClosedDate?.toDate() ?: Date()))
        }

        // INSTALLMENTS
        if (loan.installments.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showInstallments = !showInstallments }
            ) {
                Text(
                    if (showInstallments) "Hide Installments" else "View Installments",
                    color = AppColors.infoAccent,
                    style = AppFont.ibmPlexSans(13, FontWeight.Bold)
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (showInstallments) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppColors.infoAccent
                )
            }

            if (showInstallments) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    loan.installments.forEach { inst ->
                        InstallmentRow(installment = inst)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoColumn(title: String, value: String) {
    Column {
        Text(title, style = AppFont.ibmPlexSans(12), color = AppColors.secondaryText)
        Text(value, style = AppFont.ibmPlexSans(14, FontWeight.SemiBold), color = AppColors.headerText)
    }
}

private fun statusColor(status: EMIStatus): Color = when (status) {
    EMIStatus.PAID -> AppColors.successAccent
    EMIStatus.PENDING -> AppColors.warningAccent
    EMIStatus.OVERDUE -> AppColors.errorAccent
    EMIStatus.FAILED -> AppColors.errorAccent
}

@Composable
fun InstallmentRow(installment: Installment) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appShadow(AppShadows.card, RoundedCornerShape(14.dp))
            .background(AppColors.surface, RoundedCornerShape(14.dp))
            .border(0.5.dp, AppColors.border.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {

        Row {
            InfoColumn("Due Date", CommonFunctions.dateToString(installment.dueDate?.toDate() ?: Date()))
            Spacer(modifier = Modifier.weight(1f))
            InfoColumn("Paid Date", CommonFunctions.dateToString(installment.duePaidDate?.toDate() ?: Date()))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row {
            InfoColumn("Amount", installment.installmentAmount.currencyFormattedWithCommas())
            Spacer(modifier = Modifier.weight(1f))
            InfoColumn("Interest", installment.interestAmount.currencyFormattedWithCommas())
            Spacer(modifier = Modifier.weight(1f))

            Column {
                Text("Status", style = AppFont.ibmPlexSans(12), color = AppColors.secondaryText)
                Text(
                    installment.status.value,
                    style = AppFont.ibmPlexSans(14, FontWeight.Bold),
                    color = statusColor(installment.status),
                    modifier = Modifier
                        .background(statusColor(installment.status).copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}