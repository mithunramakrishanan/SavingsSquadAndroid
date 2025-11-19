package com.android.savingssquad.view

import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.R
import com.android.savingssquad.model.BeneficiaryResult
import com.android.savingssquad.model.ContributionDetail
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import kotlinx.coroutines.launch
import java.util.Date
import com.android.savingssquad.model.Squad
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.Member
import com.android.savingssquad.model.MemberLoan
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.model.unpaidMonths
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.SquadActivityType
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.PaidStatus
import com.android.savingssquad.singleton.PaymentEntryType
import com.android.savingssquad.singleton.PaymentStatus
import com.android.savingssquad.singleton.PaymentSubType
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.asTimestamp
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.singleton.displayText
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.viewmodel.AppDestination
import com.android.savingssquad.viewmodel.FirebaseFunctionsManager
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DuesScreenView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared
) {
    // replace these collects with actual flows/state from your ViewModel
    val squadPayments by remember { derivedStateOf { squadViewModel.squadPayments } } // placeholder
    var payments by remember { mutableStateOf(listOf<PaymentsDetails>()) }

    var currentOverDueContribution by remember { mutableStateOf(listOf<ContributionDetail>()) }
    var currentOverDueInstallments by remember { mutableStateOf(listOf<Installment>()) }

    var selectedSegment by remember { mutableStateOf("Contribution") }
    var isLoading by remember { mutableStateOf(true) }

    val screenType =
        if (UserDefaultsManager.getSquadManagerLogged())
            SquadUserType.SQUAD_MANAGER
        else
            SquadUserType.SQUAD_MEMBER

    // On first composition load data (mimics SwiftUI onAppear)
    LaunchedEffect(Unit) {
        // Payments
        squadViewModel.fetchPayments(showLoader = true) { success, _ ->
            payments = getCurrentMonthPayments(squadViewModel.squadPayments.value)
                .filter { it.paymentStatus == PaymentStatus.SUCCESS }

            if (screenType == SquadUserType.SQUAD_MEMBER) {
                val id = squadViewModel.currentMember.value?.id
                payments = payments.filter { it.memberId == id }
            }
        }

        // Dues
        fetchDueContributionsAndInstallments(squadViewModel) { contributions, installments ->

            CoroutineScope(Dispatchers.Main).launch {
                Log.d("DuesScreen", "🟡 Contributions Due = ${contributions.size}")
                Log.d("DuesScreen", "🟡 Installments Due = ${installments.size}")

                currentOverDueContribution = contributions
                currentOverDueInstallments = installments
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background gradient (AppBackgroundGradient equivalent)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFF8F9FB), Color(0xFFFFFFFF))
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize().padding(vertical = 16.dp)) {
            // Top nav
            SSNavigationBar(title = "Current & Over Dues", navController = navController, showBackButton = true)

            Spacer(modifier = Modifier.height(12.dp))

            if (payments.size == 0) {

                DuesCardView(
                    title = "All Due’s Paid",
                    subtitle = "Squad all caught up!",
                    icon = Icons.Default.CheckCircle,
                    iconColor = Color(0xFF4CAF50),
                    gradientColors = listOf(
                        Color(0xFF4CAF50).copy(alpha = 0.08f),
                        Color(0xFF4CAF50).copy(alpha = 0.15f)
                    ),
                    showChevron = false
                )

                Text("No payments yet",
                    style = AppFont.ibmPlexSans(15, FontWeight.Medium),
                    color = AppColors.secondaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(1f))
            }
            else {

                if (screenType == SquadUserType.SQUAD_MEMBER) {
                    DuesCardView(
                        title = "All Due’s Paid",
                        subtitle = "Squad all caught up!",
                        icon = Icons.Default.CheckCircle,
                        iconColor = Color(0xFF4CAF50),
                        gradientColors = listOf(
                            Color(0xFF4CAF50).copy(alpha = 0.08f),
                            Color(0xFF4CAF50).copy(alpha = 0.15f)
                        ),
                        showChevron = false
                    )

                    SectionView(title = "Recent Payments") {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(payments) { payment ->
                                PaymentRow(
                                    payment = payment,
                                    showPaymentStatusRow = true,
                                    showPayoutStatusRow = false,
                                    squadViewModel = squadViewModel
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                else {
                    // manager or default
                    if (currentOverDueContribution.isEmpty() && currentOverDueInstallments.isEmpty()) {
                        DuesCardView(
                            title = "All Due’s Paid",
                            subtitle = "Squad all caught up!",
                            icon = Icons.Default.CheckCircle,
                            iconColor = Color(0xFF4CAF50),
                            gradientColors = listOf(
                                Color(0xFF4CAF50).copy(alpha = 0.08f),
                                Color(0xFF4CAF50).copy(alpha = 0.15f)
                            ),
                            showChevron = false
                        )

                        SectionView(title = "Recent Payments") {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                items(payments) { payment ->
                                    PaymentRow(
                                        payment = payment,
                                        showPaymentStatusRow = true,
                                        showPayoutStatusRow = true,
                                        squadViewModel = squadViewModel
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        // Segmented control
                        ModernSegmentedPickerView(
                            segments = listOf("Contribution", "EMI"),
                            selectedSegment = selectedSegment,
                            onSegmentSelected = { selectedSegment = it }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Content
                        if (selectedSegment == "Contribution") {
                            if (currentOverDueContribution.isEmpty()) {
                                DuesCardView(
                                    title = "All Contributions Paid",
                                    subtitle = "Squad all caught up!",
                                    icon = Icons.Default.CheckCircle,
                                    iconColor = Color(0xFF4CAF50),
                                    gradientColors = listOf(
                                        Color(0xFF4CAF50).copy(alpha = 0.08f),
                                        Color(0xFF4CAF50).copy(alpha = 0.15f)
                                    ),
                                    showChevron = false
                                )
                            } else {
                                SectionView(title = "$selectedSegment Dues") {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        items(currentOverDueContribution) { contribution ->
                                            contribution.dueDate?.let {
                                                CommonFunctions.dateToString(
                                                    it.toDate(), format = "MMM dd yyyy")
                                            }?.let {
                                                PaymentDetailRow(
                                                    title = "Contribution ${contribution.monthYear}",
                                                    amount = "₹${contribution.amount}",
                                                    date = it,
                                                    status = if (contribution.paidStatus == PaidStatus.PAID) "PAID" else "PENDING",
                                                    memberName = contribution.memberName
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            if (currentOverDueInstallments.isEmpty()) {

                                DuesCardView(
                                    title = "All EMI's Paid",
                                    subtitle = "Squad all caught up!",
                                    icon = Icons.Default.CheckCircle,
                                    iconColor = Color(0xFF4CAF50),
                                    gradientColors = listOf(
                                        Color(0xFF4CAF50).copy(alpha = 0.08f),
                                        Color(0xFF4CAF50).copy(alpha = 0.15f)
                                    ),
                                    showChevron = false
                                )
                            } else {
                                SectionView(title = "$selectedSegment Dues") {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        items(currentOverDueInstallments) { installment ->
                                            installment. dueDate?.let {
                                                CommonFunctions.dateToString(
                                                    it.toDate(), format = "MMM dd yyyy")
                                            }?.let {
                                                PaymentDetailRow(
                                                    title = "${installment.installmentNumber} (${installment.loanNumber})",
                                                    amount = "₹${installment.installmentAmount + installment.interestAmount}",
                                                    date = it,
                                                    status = if (installment.status == EMIStatus.PAID) "PAID" else "PENDING",
                                                    memberName = installment.memberName
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }


        }
    }
}

@Composable
fun PaymentDetailRow(
    title: String,
    amount: String,
    date: String,
    status: String,
    memberName: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // left stripe
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(56.dp)
                .background(if (status == "PENDING" || status == "FAILED") Color.Red.copy(alpha = 0.8f) else Color(0xFF2FB55F))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = memberName,
                    style = AppFont.ibmPlexSans(16, FontWeight.SemiBold).copy(color = AppColors.headerText),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = status,
                    style = AppFont.ibmPlexSans(12, FontWeight.Medium),
                    color = if (status.contains("PENDING") || status.contains("FAILED")) Color.Red else Color(0xFF2FB55F),
                    modifier = Modifier
                        .background(
                            color = if (status.contains("PENDING") || status.contains("FAILED")) Color.Red.copy(alpha = 0.12f)
                            else Color(0xFF2FB55F).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                style = AppFont.ibmPlexSans(14, FontWeight.Normal).copy(color = AppColors.secondaryText),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // rupee + amount
                Text(
                    text = amount,
                    style = AppFont.ibmPlexSans(13, FontWeight.SemiBold).copy(color = AppColors.headerText)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = date,
                    style = AppFont.ibmPlexSans(13, FontWeight.Normal).copy(color = AppColors.secondaryText)
                )
            }
        }
    }
}

// ----------------------
// Helper functions (mirrors SwiftUI versions)
// ----------------------

/**
 * Filter payments for the current month and only allowed subtypes
 */
@RequiresApi(Build.VERSION_CODES.O)
fun getCurrentMonthPayments(allPayments: List<PaymentsDetails>): List<PaymentsDetails> {
    // Current date
    val now = LocalDate.now()

    // Start of current month in epoch millis
    val startOfMonth = now
        .withDayOfMonth(1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    // Start of next month in epoch millis
    val startOfNextMonth = now
        .plusMonths(1)
        .withDayOfMonth(1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    // Allowed payment subtypes for current month filtering
    val allowedSubTypes = setOf(
        PaymentSubType.INTEREST_AMOUNT,
        PaymentSubType.EMI_AMOUNT,
        PaymentSubType.CONTRIBUTION_AMOUNT
    )

    return allPayments.filter { payment ->
        val recordMs = payment.recordDate.toDate().time
        payment.paymentType == PaymentType.PAYMENT_CREDIT &&
                payment.paymentSubType in allowedSubTypes &&
                recordMs in startOfMonth until startOfNextMonth
    }
}

/**
 * Fetch due contributions and installments.
 * This calls your ViewModel's fetchDueContributionsAndInstallments method.
 * Replace with your real implementation as required.
 */

fun fetchDueContributionsAndInstallments(
    squadViewModel: SquadViewModel,
    onComplete: (List<ContributionDetail>, List<Installment>) -> Unit
) {
    val squadID = squadViewModel.squad.value?.squadID ?: run {
        Log.e("DuesScreen", "❌ squadID is NULL — Cannot fetch dues")
        return
    }

    Log.d("DuesScreen", "🔍 Fetching dues for SquadID: $squadID")

    squadViewModel.fetchDueContributionsAndInstallments(
        squadID)
        { contributions, installments ->
            Log.d("DuesScreen", "🟡 Contributions Due = ${contributions.size}")
            Log.d("DuesScreen", "🟡 Installments Due = ${installments.size}")

            onComplete(contributions, installments)
        }

}