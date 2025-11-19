package com.android.savingssquad.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.PaymentStatus
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.viewmodel.SquadViewModel
import com.yourapp.utils.CommonFunctions
import java.util.Date

@Composable
fun AccountSummaryView(
    navController: NavController,
    squadViewModel: SquadViewModel
) {
    val payments = squadViewModel.squadPayments.collectAsState().value

    LaunchedEffect(Unit) {
        squadViewModel.fetchPayments(showLoader = true) { success, error ->
            if (!success) println("❌ Error: $error")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background) // Your gradient
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {

            // 🔹 Navigation Bar
            SSNavigationBar(title = "Account Summary", navController = navController)

            // 🔹 Totals Header
            AccountsSummaryHeaderView(payments)

            // 🔹 Payments List
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 50.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                val successful = payments.filter { it.paymentStatus == PaymentStatus.SUCCESS }

                if (successful.isEmpty()) {
                    item {
                        Text(
                            text = "No payments yet",
                            style = AppFont.ibmPlexSans(14, FontWeight.Medium),
                            color = AppColors.secondaryText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    items(successful) { payment ->
                        AccountsSummaryCellView(payment)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountsSummaryHeaderView(payments: List<PaymentsDetails>) {

    val totalCredit = payments
        .filter { it.paymentType == PaymentType.PAYMENT_CREDIT && it.paymentStatus == PaymentStatus.SUCCESS }
        .sumOf { it.amount }

    val totalDebit = payments
        .filter { it.paymentType == PaymentType.PAYMENT_DEBIT && it.paymentStatus == PaymentStatus.SUCCESS }
        .sumOf { it.amount }

    val balance = totalCredit - totalDebit

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .appShadow(AppShadows.card, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.surface)
            .padding(16.dp)
    ) {

        Row {
            Text(
                "Total Credit",
                style = AppFont.ibmPlexSans(16, FontWeight.Medium),
                color = AppColors.headerText
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                totalCredit.currencyFormattedWithCommas(),
                style = AppFont.ibmPlexSans(16, FontWeight.Medium),
                color = AppColors.successAccent
            )
        }

        Row {
            Text(
                "Total Debit",
                style = AppFont.ibmPlexSans(16, FontWeight.Medium),
                color = AppColors.headerText
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                totalDebit.currencyFormattedWithCommas(),
                style = AppFont.ibmPlexSans(16, FontWeight.Medium),
                color = AppColors.errorAccent
            )
        }

        Divider(color = AppColors.border)

        Row {
            Text(
                "Balance",
                style = AppFont.ibmPlexSans(18, FontWeight.Bold),
                color = AppColors.headerText
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                balance.currencyFormattedWithCommas(),
                style = AppFont.ibmPlexSans(18, FontWeight.Bold),
                color = if (balance >= 0) AppColors.successAccent else AppColors.errorAccent
            )
        }
    }
}

@Composable
fun AccountsSummaryCellView(payment: PaymentsDetails) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .appShadow(AppShadows.card, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${payment.memberName} - ${payment.description}",
                style = AppFont.ibmPlexSans(14, FontWeight.Medium),
                color = AppColors.headerText
            )

            Text(
                text = CommonFunctions.dateToString(payment.paymentUpdatedDate?.toDate() ?: Date()),
                style = AppFont.ibmPlexSans(12, FontWeight.Normal),
                color = AppColors.secondaryText
            )
        }

        Text(
            text = payment.amount.currencyFormattedWithCommas(),
            style = AppFont.ibmPlexSans(14, FontWeight.SemiBold),
            color = if (payment.paymentType == PaymentType.PAYMENT_CREDIT)
                AppColors.successAccent else AppColors.errorAccent
        )
    }
}