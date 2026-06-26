package com.android.savingssquad.view

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.android.savingssquad.model.ContributionDetail
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import kotlinx.coroutines.launch
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.model.ReminderRequest
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.NotificationService
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.PaidStatus
import com.android.savingssquad.singleton.PaymentApproveStatus
import com.android.savingssquad.singleton.PaymentStatus
import com.android.savingssquad.singleton.PaymentSubType
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.viewmodel.SSToast
import com.android.savingssquad.viewmodel.ToastManager
import com.android.savingssquad.viewmodel.ToastType
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate
import java.time.ZoneId


/*
NotificationService.shared.sendMemberReminder(
    request = ReminderRequest(
        squadId = "SQD123",
        memberIds = listOf("MEM001", "MEM002"),
        title = "Payment Reminder",
        message = "Please complete your payment",
        data = mapOf(
            "screen" to "REMINDER"
        )
    ),
    onSuccess = { response ->
        Log.d("FCM", "Sent to: ${response.sentTo}")
    },
    onError = { error ->
        Log.e("FCM", "Error sending reminder", error)
    }
)
 */

@SuppressLint("UnrememberedMutableState")
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

    var contributionRemaindCount by mutableStateOf(0)
    var installmentRemaindCount by mutableStateOf(0)

    var contributionRemaindIds by mutableStateOf(listOf<String>())
    var installmentRemaindIds by mutableStateOf(listOf<String>())

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
                .filter { it.paymentStatus == PaymentStatus.SUCCESS && it.paymentApproveStatus == PaymentApproveStatus.ACCEPTED }

            if (screenType == SquadUserType.SQUAD_MEMBER) {
                val id = squadViewModel.currentMember.value?.id
                payments = payments.filter { it.memberId == id && it.paymentApproveStatus == PaymentApproveStatus.ACCEPTED }
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


                contributionRemaindIds = currentOverDueContribution
                    .map { it.memberID }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()

                contributionRemaindCount = contributionRemaindIds.size


                installmentRemaindIds = currentOverDueInstallments
                    .map { it.memberID }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()

                installmentRemaindCount = installmentRemaindIds.size

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

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp)) {
            // Top nav
            SSNavigationBar(title = SquadStrings.currentOverDues, navController = navController, showBackButton = true)

            Spacer(modifier = Modifier.height(12.dp))

            if (screenType == SquadUserType.SQUAD_MANAGER) {

                // manager or default
                if (currentOverDueContribution.isEmpty() && currentOverDueInstallments.isEmpty()) {
                    AllCaughUPView(
                        title = "All Due’s Paid",
                        subtitle = "Squad all caught up!",
                        icon = Icons.Default.CheckCircle,
                        iconColor = Color(0xFF4CAF50),
                        showChevron = false
                    )

                    if (payments.size != 0) {

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
                                        showPayoutStatusRow = false,
                                        squadViewModel = squadViewModel
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    }


                }
                else {
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
                            AllCaughUPView(
                                title = "All Contributions Paid",
                                subtitle = "Squad all caught up!",
                                icon = Icons.Default.CheckCircle,
                                iconColor = Color(0xFF4CAF50),
                                showChevron = false
                            )
                        } else {
                            SectionView(title = "$selectedSegment Dues") {

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RemindAllButton(contributionRemaindCount) {

                                        NotificationService.shared.sendMemberReminder(

                                            request = ReminderRequest(

                                                squadId = squadViewModel.squad.value?.squadID
                                                    ?: "",

                                                memberIds = contributionRemaindIds,

                                                title = "Contribution Reminder",

                                                message = "Please complete the Contribution Due(s)",

                                                data = mapOf(

                                                    "screen" to "PAYMENT"

                                                )

                                            ),

                                            onSuccess = { response ->
                                                LoaderManager.shared.hideLoader()
                                                ToastManager.show(
                                                    title = "Reminder Sent",
                                                    message = "Notification sent to ${response.sentTo} member(s)",
                                                    type = ToastType.SUCCESS
                                                )
                                            },

                                            onError = { error ->
                                                LoaderManager.shared.hideLoader()
                                                ToastManager.show(
                                                    title = "Failed",
                                                    message = error.localizedMessage ?: "Unable to send reminder.",
                                                    type = ToastType.ERROR
                                                )
                                            }

                                        )

                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),

                                    verticalArrangement = Arrangement.spacedBy(12.dp),

                                    contentPadding = PaddingValues(

                                        start = 16.dp,

                                        end = 16.dp,

                                        top = 8.dp,

                                        bottom = 8.dp

                                    )
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
                                                memberName = contribution.memberName,
                                                onRemind = {
                                                    LoaderManager.shared.showLoader()
                                                    NotificationService.shared.sendMemberReminder(

                                                        request = ReminderRequest(

                                                            squadId = squadViewModel.squad.value?.squadID
                                                                ?: "",

                                                            memberIds = listOf(contribution.memberID),

                                                            title = "Contribution Reminder",

                                                            message = "Please complete the contribution for ${contribution.monthYear}",

                                                            data = mapOf(

                                                                "screen" to "PAYMENT"

                                                            )

                                                        ),

                                                        onSuccess = { response ->
                                                            LoaderManager.shared.hideLoader()
                                                            ToastManager.show(
                                                                title = "Reminder Sent",
                                                                message = "Notification sent to ${contribution.memberName}",
                                                                type = ToastType.SUCCESS
                                                            )
                                                        },

                                                        onError = { error ->
                                                            LoaderManager.shared.hideLoader()
                                                            ToastManager.show(
                                                                title = "Failed",
                                                                message = error.localizedMessage ?: "Unable to send reminder.",
                                                                type = ToastType.ERROR
                                                            )
                                                        }

                                                    )

                                                }

                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else {
                        if (currentOverDueInstallments.isEmpty()) {

                            AllCaughUPView(
                                title = "All EMI's Paid",
                                subtitle = "Squad all caught up!",
                                icon = Icons.Default.CheckCircle,
                                iconColor = Color(0xFF4CAF50),
                                showChevron = false
                            )
                        }
                        else {
                            SectionView(title = "$selectedSegment Dues") {

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RemindAllButton(installmentRemaindCount) {

                                        NotificationService.shared.sendMemberReminder(

                                            request = ReminderRequest(

                                                squadId = squadViewModel.squad.value?.squadID
                                                    ?: "",

                                                memberIds = installmentRemaindIds,

                                                title = "EMI Reminder",

                                                message = "Please complete the EMI Due(s)",

                                                data = mapOf(

                                                    "screen" to "PAYMENT"

                                                )

                                            ),

                                            onSuccess = { response ->
                                                LoaderManager.shared.hideLoader()
                                                ToastManager.show(
                                                    title = "Reminder Sent",
                                                    message = "Notification sent to ${response.sentTo} member(s)",
                                                    type = ToastType.SUCCESS
                                                )
                                            },

                                            onError = { error ->
                                                LoaderManager.shared.hideLoader()
                                                ToastManager.show(
                                                    title = "Failed",
                                                    message = error.localizedMessage ?: "Unable to send reminder.",
                                                    type = ToastType.ERROR
                                                )
                                            }

                                        )
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),

                                    verticalArrangement = Arrangement.spacedBy(12.dp),

                                    contentPadding = PaddingValues(

                                        start = 16.dp,

                                        end = 16.dp,

                                        top = 8.dp,

                                        bottom = 8.dp

                                    )
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
                                                memberName = installment.memberName,
                                                onRemind = {

                                                    LoaderManager.shared.showLoader()
                                                    NotificationService.shared.sendMemberReminder(

                                                        request = ReminderRequest(

                                                            squadId = squadViewModel.squad.value?.squadID ?: "",

                                                            memberIds = listOf(installment.memberID),

                                                            title = "EMI Reminder",

                                                            message = "Please complete the ${installment.installmentNumber} of ${installment.loanNumber}",

                                                            data = mapOf(
                                                                "screen" to "PAYMENT"
                                                            )
                                                        ),

                                                        onSuccess = { response ->
                                                            LoaderManager.shared.hideLoader()
                                                            ToastManager.show(
                                                                title = "Reminder Sent",
                                                                message = "Notification sent to ${installment.memberName}",
                                                                type = ToastType.SUCCESS
                                                            )
                                                        },

                                                        onError = { error ->
                                                            LoaderManager.shared.hideLoader()
                                                            ToastManager.show(
                                                                title = "Failed",
                                                                message = error.localizedMessage ?: "Unable to send reminder.",
                                                                type = ToastType.ERROR
                                                            )
                                                        }
                                                    )

                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else {

                AllCaughUPView(
                    title = "All Due’s Paid",
                    subtitle = "Squad all caught up!",
                    icon = Icons.Default.CheckCircle,
                    iconColor = Color(0xFF4CAF50),
                    showChevron = false
                )

                if (payments.size != 0) {

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


            }


        }
    }
}

private fun uniqueMemberIDs(list: List<String>): Set<String> {
    return list.filter { it.isNotBlank() }.toSet()
}

@Composable
fun PaymentDetailRow(
    title: String,
    amount: String,
    date: String,
    status: String,
    memberName: String,
    onRemind: (() -> Unit)? = null
) {

    val isPending = status == "PENDING"
    val statusColor = if (isPending) Color.Red else Color(0xFF2FB55F)

    Row(
        modifier = Modifier

            .fillMaxWidth()

            .padding(vertical = 6.dp)

            .shadow(

                elevation = 6.dp,

                shape = RoundedCornerShape(16.dp)

            )

            .background(

                color = AppColors.surface,

                shape = RoundedCornerShape(16.dp)

            )

            .padding(14.dp),

        verticalAlignment = Alignment.Top
    ) {

        // 🔹 Status Dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(statusColor, CircleShape)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            // MARK: Top Section
            Row(
                verticalAlignment = Alignment.Top
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = memberName,
                        style = AppFont.ibmPlexSans(
                            16,
                            FontWeight.SemiBold
                        ).copy(
                            color = AppColors.headerText
                        ),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = title,
                        style = AppFont.ibmPlexSans(
                            13,
                            FontWeight.Normal
                        ).copy(
                            color = AppColors.secondaryText
                        ),
                        maxLines = 1
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {

                    // 🔹 Status Badge
                    Text(
                        text = status,
                        style = AppFont.ibmPlexSans(
                            9,
                            FontWeight.SemiBold
                        ),
                        color = statusColor.copy(alpha = 0.9f),
                        modifier = Modifier
                            .background(
                                statusColor.copy(alpha = 0.10f),
                                RoundedCornerShape(50)
                            )
                            .padding(
                                horizontal = 8.dp,
                                vertical = 3.dp
                            )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 🔔 Remind Button
                    if (isPending && onRemind != null) {

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(AppColors.primaryButton)
                                .clickable {
                                    onRemind()
                                }
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = "Remind",
                                style = AppFont.ibmPlexSans(
                                    11,
                                    FontWeight.SemiBold
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // MARK: Bottom Section
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.CurrencyRupee,
                        contentDescription = null,
                        tint = AppColors.headerText,
                        modifier = Modifier.size(14.dp)
                    )

                    Text(
                        text = amount,
                        style = AppFont.ibmPlexSans(
                            13,
                            FontWeight.Medium
                        ).copy(
                            color = AppColors.headerText
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = AppColors.secondaryText,
                        modifier = Modifier.size(13.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = date,
                        style = AppFont.ibmPlexSans(
                            12,
                            FontWeight.Normal
                        ).copy(
                            color = AppColors.secondaryText
                        )
                    )
                }
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