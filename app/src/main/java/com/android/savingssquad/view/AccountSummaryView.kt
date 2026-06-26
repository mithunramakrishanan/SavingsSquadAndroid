package com.android.savingssquad.view

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.PaymentFilter
import com.android.savingssquad.singleton.PaymentStatus
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.viewmodel.SquadViewModel
import com.yourapp.utils.CommonFunctions
import java.util.Date
import androidx.compose.runtime.collectAsState
import com.android.savingssquad.viewmodel.SSToast

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun AccountSummaryView(
    navController: NavController,
    squadViewModel: SquadViewModel
) {

    val payments = squadViewModel.squadPayments.collectAsStateWithLifecycle()

    var selectedFilter by remember {
        mutableStateOf(PaymentFilter.ALL)
    }

    var hasLoaded by remember {
        mutableStateOf(false)
    }

    fun reloadPayments() {

        squadViewModel.resetPaymentsPagination()

        squadViewModel.fetchPayments(
            showLoader = true,
            filterType = selectedFilter
        ) { _, error ->

            if (error != null) {
                println("❌ $error")
            }
        }
    }

    // MARK: - Initial Load
    LaunchedEffect(Unit) {

        if (hasLoaded) return@LaunchedEffect

        hasLoaded = true

        reloadPayments()
    }

    Box(
        modifier = Modifier

            .fillMaxSize()

            .windowInsetsPadding(WindowInsets.safeDrawing)
    )
    {
        AppBackgroundGradient()
        Column(
            modifier = Modifier.fillMaxSize()
        )
        {

            SSNavigationBar(
                title = SquadStrings.accountSummary,
                navController = navController
            )

            Spacer(modifier = Modifier.height(12.dp))

            AccountsSummaryHeaderView(
                squadViewModel = squadViewModel
            )

            Spacer(modifier = Modifier.height(12.dp))

            PaymentFilterSegmentedControl(
                selectedFilter = selectedFilter
            ) { filter ->

                selectedFilter = filter

                reloadPayments()
            }

            Spacer(modifier = Modifier.height(8.dp))

            // MARK: - Empty State
            if (
                payments.value.isEmpty() &&
                !squadViewModel.paymentsIsLoadingMore
            ) {

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = Color.Gray.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = SquadStrings.noPaymentsYet,
                        style = AppFont.ibmPlexSans(
                            15,
                            FontWeight.Medium
                        ),
                        color = AppColors.secondaryText
                    )
                }
            }

            // MARK: - List
            else {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = 20.dp
                    )
                ) {

                    items(
                        items = payments.value,
                        key = { it.id ?: "" }
                    ) { payment ->

                        AccountsSummaryCellView(
                            payment = payment
                        )

                        // MARK: - Pagination
                        LaunchedEffect(payment.id) {

                            squadViewModel.loadMorePaymentsIfNeeded(
                                currentPayment = payment,
                                filterType = selectedFilter
                            )
                        }
                    }

                    // MARK: - Pagination Loader
                    if (
                        squadViewModel.paymentsIsLoadingMore &&
                        payments.value.isNotEmpty()
                    ) {

                        item {
                            ShimmerLoader()
                        }
                    }
                }
            }
        }
    }


}

@Composable
fun PaymentFilterSegmentedControl(
    selectedFilter: PaymentFilter,
    onFilterSelected: (PaymentFilter) -> Unit
) {

    val filters = PaymentFilter.values()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                color = AppColors.secondaryText.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(4.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            filters.forEach { filter ->

                val isSelected = filter == selectedFilter

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) AppColors.primaryButton
                            else Color.Transparent
                        )
                        .clickable { onFilterSelected(filter) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = filter.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        style = AppFont.ibmPlexSans(13, FontWeight.Medium),
                        color = if (isSelected)
                            AppColors.primaryButtonText
                        else
                            AppColors.secondaryText
                    )
                }
            }
        }
    }
}
@Composable
fun AccountsSummaryHeaderView(squadViewModel: SquadViewModel) {

    val squad by squadViewModel.squad.collectAsState()
    val totalCredit = squad?.currentCreditAmount ?: 0
    val totalDebit = squad?.currentDebitAmount ?: 0
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