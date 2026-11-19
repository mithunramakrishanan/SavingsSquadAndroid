package com.android.savingssquad.view

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.model.Member
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.PaymentFilter
import com.android.savingssquad.singleton.PaymentStatus
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.SquadLanguages
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.viewmodel.SquadViewModel
import com.yourapp.utils.CommonFunctions
import java.util.Date
import androidx.compose.runtime.collectAsState
import com.android.savingssquad.viewmodel.SSToast

private fun accountSummaryLabel(key: String): String {
    val english = mapOf(
        "currentSquadBalance" to "CURRENT SQUAD BALANCE", "memberAccountBalance" to "MEMBER ACCOUNT BALANCE",
        "moneyIn" to "Money In", "moneyOut" to "Money Out", "intoSquad" to "Into squad", "fromSquad" to "From squad",
        "intoSquadFromMember" to "Into squad from member", "fromSquadToMember" to "From squad to member",
        "totalAmount" to "TOTAL AMOUNT", "interestPaid" to "INTEREST PAID", "amountPlusInterest" to "AMOUNT + INTEREST",
        "repaymentReceived" to "Repayment Received", "squadAccount" to "SQUAD ACCOUNT",
        "moneyInAndMoneyOut" to "Money In & Money Out", "squadPosition" to "CURRENT SQUAD POSITION",
        "memberPosition" to "MEMBER POSITION", "netPosition" to "Net Position", "availableBalance" to "Available Balance"
    )
    val hindi = mapOf(
        "currentSquadBalance" to "वर्तमान समूह शेष", "memberAccountBalance" to "सदस्य खाते की राशि",
        "moneyIn" to "जमा राशि", "moneyOut" to "खर्च राशि", "intoSquad" to "समूह में जमा", "fromSquad" to "समूह से खर्च",
        "intoSquadFromMember" to "सदस्य से समूह में", "fromSquadToMember" to "समूह से सदस्य को",
        "totalAmount" to "कुल राशि", "interestPaid" to "दिया गया ब्याज", "amountPlusInterest" to "राशि + ब्याज",
        "repaymentReceived" to "भुगतान प्राप्त", "squadAccount" to "समूह खाता",
        "moneyInAndMoneyOut" to "जमा और खर्च", "squadPosition" to "वर्तमान समूह स्थिति",
        "memberPosition" to "सदस्य की स्थिति", "netPosition" to "शुद्ध स्थिति", "availableBalance" to "उपलब्ध शेष"
    )
    val tamil = mapOf(
        "currentSquadBalance" to "தற்போதைய குழு இருப்பு", "memberAccountBalance" to "உறுப்பினர் கணக்கு இருப்பு",
        "moneyIn" to "வரவு", "moneyOut" to "செலவு", "intoSquad" to "குழுவிற்கு வரவு", "fromSquad" to "குழுவிலிருந்து செலவு",
        "intoSquadFromMember" to "உறுப்பினரிடமிருந்து குழுவிற்கு", "fromSquadToMember" to "குழுவிலிருந்து உறுப்பினருக்கு",
        "totalAmount" to "மொத்த தொகை", "interestPaid" to "செலுத்திய வட்டி", "amountPlusInterest" to "தொகை + வட்டி",
        "repaymentReceived" to "திருப்பிச் செலுத்தல் பெறப்பட்டது", "squadAccount" to "குழு கணக்கு",
        "moneyInAndMoneyOut" to "வரவு மற்றும் செலவு", "squadPosition" to "தற்போதைய குழு நிலை",
        "memberPosition" to "உறுப்பினர் நிலை", "netPosition" to "நிகர நிலை", "availableBalance" to "கிடைக்கும் இருப்பு"
    )
    return when (SquadStrings.currentLanguage) {
        SquadLanguages.TAMIL -> tamil[key] ?: english[key] ?: key
        SquadLanguages.HINDI -> hindi[key] ?: english[key] ?: key
        SquadLanguages.ENGLISH -> english[key] ?: key
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun AccountSummaryView(
    navController: NavController,
    squadViewModel: SquadViewModel
) {

    val payments = squadViewModel.squadPayments.collectAsStateWithLifecycle()
    val squad by squadViewModel.squad.collectAsStateWithLifecycle()
    val currentMember by squadViewModel.currentMember.collectAsStateWithLifecycle()
    val isManager = squadViewModel.loginMember?.role == SquadUserType.SQUAD_MANAGER

    var selectedMember by remember { mutableStateOf<Member?>(null) }
    var showMemberPicker by remember { mutableStateOf(false) }
    var selectedSegment by remember { mutableStateOf(false) }

    var selectedFilter by remember {
        mutableStateOf(PaymentFilter.ALL)
    }

    fun reloadPayments() {

        squadViewModel.resetPaymentsPagination()

        squadViewModel.fetchPayments(
            showLoader = true,
            memberId = if (isManager) selectedMember?.id else currentMember?.id,
            filterType = selectedFilter,
            showRejected = false
        ) { _, error ->

            if (error != null) {
                println("❌ $error")
            }
        }
    }

    LaunchedEffect(selectedMember?.id, currentMember?.id, isManager, selectedFilter) {
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

            if (isManager) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .appShadow(AppShadows.card, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .background(AppColors.surface)
                        .border(1.dp, AppColors.border, RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = SquadStrings.selectMember,
                        style = AppFont.ibmPlexSans(13, FontWeight.SemiBold),
                        color = AppColors.headerText
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .background(AppColors.secondaryText.copy(alpha = 0.06f))
                            .clickable { showMemberPicker = true }
                            .padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AppColors.successAccent)
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = selectedMember?.localizedMemberName ?: SquadStrings.selectMember,
                            modifier = Modifier.weight(1f),
                            style = AppFont.ibmPlexSans(14, FontWeight.Medium),
                            color = AppColors.headerText,
                            maxLines = 1
                        )
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppColors.secondaryText)
                    }
                    if (selectedMember != null) {
                        Text(
                            text = SquadStrings.all,
                            modifier = Modifier.clickable { selectedMember = null }.padding(vertical = 2.dp),
                            style = AppFont.ibmPlexSans(12, FontWeight.SemiBold),
                            color = AppColors.successAccent
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(AppColors.secondaryText.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AccountSummarySegmentButton(SquadStrings.accountView, !selectedSegment, Modifier.weight(1f)) { selectedSegment = false }
                AccountSummarySegmentButton(SquadStrings.transactionView, selectedSegment, Modifier.weight(1f)) { selectedSegment = true }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!selectedSegment) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    item {
                        AccountsSummaryHeaderView(
                            squadViewModel = squadViewModel,
                            selectedMember = if (isManager) selectedMember else currentMember
                        )
                    }
                    item {
                        AccountMoneyFlowView(
                            squadViewModel = squadViewModel,
                            selectedMember = if (isManager) selectedMember else currentMember
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(
                        top = 4.dp,
                        bottom = 20.dp
                    )
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(SquadStrings.recentTransactions, style = AppFont.ibmPlexSans(18, FontWeight.Bold), color = AppColors.headerText)
                            Spacer(Modifier.weight(1f))
                            Text("${payments.value.size} ${SquadStrings.records}", style = AppFont.ibmPlexSans(12, FontWeight.Medium), color = AppColors.secondaryText)
                        }
                    }
                    item {
                        PaymentFilterSegmentedControl(selectedFilter = selectedFilter) { filter ->
                            selectedFilter = filter
                        }
                    }

                    if (payments.value.isEmpty() && !squadViewModel.paymentsIsLoadingMore) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 52.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(SquadStrings.noPaymentsYet, style = AppFont.ibmPlexSans(15, FontWeight.Medium), color = AppColors.secondaryText)
                            }
                        }
                    } else {
                        items(items = payments.value, key = { it.id ?: "" }) { payment ->
                            AccountsSummaryCellView(payment = payment)
                            LaunchedEffect(payment.id) {
                                squadViewModel.loadMorePaymentsIfNeeded(
                                    currentPayment = payment,
                                    filterType = selectedFilter,
                                    memberId = if (isManager) selectedMember?.id else currentMember?.id
                                )
                            }
                        }
                    }

                    if (squadViewModel.paymentsIsLoadingMore && payments.value.isNotEmpty()) {
                        item {
                            ShimmerLoader()
                        }
                    }
                }
            }
        }

        MemberNameSelectionPopup(
            showPopup = showMemberPicker,
            squadId = squad?.squadID.orEmpty(),
            squadViewModel = squadViewModel,
            onDismiss = { showMemberPicker = false },
            onSelect = { member -> selectedMember = member },
            showSelectAll = true,
            onSelectAll = { selectedMember = null }
        )
    }


}

@Composable
private fun AccountSummarySegmentButton(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) AppColors.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = AppFont.ibmPlexSans(12, FontWeight.SemiBold),
            color = if (selected) AppColors.successAccent else AppColors.secondaryText,
            textAlign = TextAlign.Center
        )
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
                        text = filter.localizedName.lowercase()
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
fun AccountsSummaryHeaderView(squadViewModel: SquadViewModel, selectedMember: Member? = null) {

    val squad by squadViewModel.squad.collectAsState()
    val totals = accountSummaryTotals(squad, selectedMember)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .appShadow(AppShadows.card, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF103D35), Color(0xFF176B58), Color(0xFF15856C))))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (selectedMember == null) accountSummaryLabel("currentSquadBalance") else accountSummaryLabel("memberAccountBalance"),
                    style = AppFont.ibmPlexSans(10, FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.72f)
                )
                Text(
                    totals.available.currencyFormattedWithCommas(),
                    style = AppFont.ibmPlexSans(30, FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    selectedMember?.localizedMemberName ?: squad?.localizedSquadName.orEmpty(),
                    style = AppFont.ibmPlexSans(13, FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.84f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                AccountHeroMetric(accountSummaryLabel("moneyIn"), "+${totals.credit.currencyFormattedWithCommas()}", if (selectedMember == null) accountSummaryLabel("intoSquad") else accountSummaryLabel("intoSquadFromMember"), Color(0xFFB9F4D3), Modifier.weight(1f))
                Divider(modifier = Modifier.height(42.dp).width(1.dp), color = Color.White.copy(alpha = 0.22f))
                Spacer(Modifier.width(14.dp))
                AccountHeroMetric(accountSummaryLabel("moneyOut"), "−${totals.debit.currencyFormattedWithCommas()}", if (selectedMember == null) accountSummaryLabel("fromSquad") else accountSummaryLabel("fromSquadToMember"), Color(0xFFFFD2C8), Modifier.weight(1f))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().appShadow(AppShadows.card, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(AppColors.surface).border(1.dp, AppColors.border, RoundedCornerShape(18.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccountSmallMetric(accountSummaryLabel("totalAmount"), (totals.available - if (selectedMember == null) totals.interest else 0).currencyFormattedWithCommas(), Modifier.weight(1f))
            Divider(modifier = Modifier.height(38.dp).width(1.dp), color = AppColors.border)
            AccountSmallMetric(accountSummaryLabel("interestPaid"), "+${totals.interest.currencyFormattedWithCommas()}", Modifier.weight(1f), accent = true)
            Divider(modifier = Modifier.height(38.dp).width(1.dp), color = AppColors.border)
            AccountSmallMetric(accountSummaryLabel("amountPlusInterest"), (totals.available + if (selectedMember == null) 0 else totals.interest).currencyFormattedWithCommas(), Modifier.weight(1f))
        }
    }
}

private data class AccountSummaryTotals(
    val credit: Int,
    val debit: Int,
    val available: Int,
    val interest: Int,
    val contribution: Int,
    val loanReceived: Int,
    val repaymentReceived: Int,
    val loanSent: Int,
    val repaymentSent: Int,
    val settlementSent: Int,
    val loanPaid: Int,
    val repaymentPaid: Int,
    val loanBorrowed: Int,
    val memberRepaymentReceived: Int,
    val settlementReceived: Int,
    val memberCount: Int
)

private fun accountSummaryTotals(squad: com.android.savingssquad.model.Squad?, member: Member?): AccountSummaryTotals {
    if (member != null) {
        val credit = member.totalContributionPaid + member.totalLoanPaid + member.totalRepaymentPaid
        val debit = member.totalLoanBorrowed + member.totalRepaymentReceived + member.totalSettlementReceived
        return AccountSummaryTotals(credit, debit, kotlin.math.abs(debit - credit), member.totalInterestPaid, member.totalContributionPaid, 0, 0, 0, 0, 0, member.totalLoanPaid, member.totalRepaymentPaid, member.totalLoanBorrowed, member.totalRepaymentReceived, member.totalSettlementReceived, 1)
    }
    val value = squad
    val contribution = value?.totalContributionAmountReceived ?: 0
    val interest = value?.totalInterestAmountReceived ?: 0
    val loanReceived = value?.totalLoanAmountReceived ?: 0
    val repaymentReceived = value?.totalRepaymentReceived ?: 0
    val loanSent = value?.totalLoanAmountSent ?: 0
    val repaymentSent = value?.totalRepaymentSent ?: 0
    val settlementSent = value?.totalSettlementSent ?: 0
    return AccountSummaryTotals(
        credit = contribution + interest + loanReceived + repaymentReceived,
        debit = loanSent + repaymentSent + settlementSent,
        available = value?.currentAvailableAmount ?: 0,
        interest = interest,
        contribution = contribution,
        loanReceived = loanReceived,
        repaymentReceived = repaymentReceived,
        loanSent = loanSent,
        repaymentSent = repaymentSent,
        settlementSent = settlementSent,
        loanPaid = 0,
        repaymentPaid = 0,
        loanBorrowed = 0,
        memberRepaymentReceived = 0,
        settlementReceived = 0,
        memberCount = value?.totalMembers ?: 0
    )
}

@Composable
private fun AccountHeroMetric(title: String, amount: String, detail: String, tint: Color, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title.uppercase(), style = AppFont.ibmPlexSans(9, FontWeight.Bold), color = Color.White.copy(alpha = 0.7f), maxLines = 1)
        Text(amount, style = AppFont.ibmPlexSans(15, FontWeight.Bold), color = tint, maxLines = 1)
        Text(detail, style = AppFont.ibmPlexSans(9, FontWeight.Medium), color = Color.White.copy(alpha = 0.72f), maxLines = 1)
    }
}

@Composable
private fun AccountSmallMetric(title: String, value: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(modifier.padding(horizontal = 7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = AppFont.ibmPlexSans(8, FontWeight.Bold), color = AppColors.secondaryText, maxLines = 1)
        Text(value, style = AppFont.ibmPlexSans(12, FontWeight.Bold), color = if (accent) AppColors.successAccent else AppColors.headerText, maxLines = 1)
    }
}

@Composable
private fun AccountMoneyFlowView(squadViewModel: SquadViewModel, selectedMember: Member?) {
    val squad by squadViewModel.squad.collectAsState()
    val totals = accountSummaryTotals(squad, selectedMember)
    val moneyInRows = if (selectedMember != null) listOf(
        SquadStrings.contributionReceived to totals.contribution,
        SquadStrings.interestReceived to totals.interest,
        SquadStrings.loanAmount to totals.loanPaid,
        accountSummaryLabel("repaymentReceived") to totals.repaymentPaid
    ) else listOf(
        SquadStrings.contributionReceived to totals.contribution,
        SquadStrings.interestReceived to totals.interest,
        SquadStrings.loanAmount to totals.loanReceived,
        accountSummaryLabel("repaymentReceived") to totals.repaymentReceived
    )
    val moneyOutRows = if (selectedMember != null) listOf(
        SquadStrings.loanAmount to totals.loanBorrowed,
        accountSummaryLabel("repaymentReceived") to totals.memberRepaymentReceived,
        SquadStrings.settlement to totals.settlementReceived
    ) else listOf(
        SquadStrings.loanAmount to totals.loanSent,
        accountSummaryLabel("repaymentReceived") to totals.repaymentSent,
        SquadStrings.settlement to totals.settlementSent
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(24.dp)).background(AppColors.surface.copy(alpha = 0.78f)).border(1.dp, AppColors.border, RoundedCornerShape(24.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(selectedMember?.let { "${SquadStrings.member} • ${it.localizedMemberName.uppercase()}" } ?: accountSummaryLabel("squadAccount"), style = AppFont.ibmPlexSans(10, FontWeight.Bold), color = AppColors.secondaryText, maxLines = 1)
            Text(accountSummaryLabel("moneyInAndMoneyOut"), style = AppFont.ibmPlexSans(21, FontWeight.Bold), color = AppColors.headerText)
        }
        MoneyFlowSection(accountSummaryLabel("moneyIn"), totals.credit, true, moneyInRows)
        MoneyFlowSection(accountSummaryLabel("moneyOut"), totals.debit, false, moneyOutRows)
        CurrentPositionCard(selectedMember?.localizedMemberName ?: squad?.localizedSquadName.orEmpty(), totals, selectedMember != null)
    }
}

@Composable
private fun MoneyFlowSection(title: String, amount: Int, isCredit: Boolean, rows: List<Pair<String, Int>>) {
    val accent = if (isCredit) AppColors.successAccent else AppColors.errorAccent
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(AppColors.surface).border(1.dp, AppColors.border, RoundedCornerShape(18.dp)).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(title, style = AppFont.ibmPlexSans(16, FontWeight.Bold), color = AppColors.headerText)
            Spacer(Modifier.weight(1f))
            Text("${if (isCredit) "+" else "−"}${amount.currencyFormattedWithCommas()}", style = AppFont.ibmPlexSans(14, FontWeight.Bold), color = accent)
        }
        rows.forEach { (label, value) ->
            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = AppFont.ibmPlexSans(12, FontWeight.Medium), color = AppColors.secondaryText)
                Spacer(Modifier.weight(1f))
                Text(value.currencyFormattedWithCommas(), style = AppFont.ibmPlexSans(12, FontWeight.SemiBold), color = AppColors.headerText)
            }
        }
        Divider(modifier = Modifier.padding(top = 10.dp), color = AppColors.border)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (isCredit) SquadStrings.totalCredit else SquadStrings.totalDebit, style = AppFont.ibmPlexSans(10, FontWeight.Bold), color = AppColors.headerText)
            Spacer(Modifier.weight(1f))
            Text("${if (isCredit) "+" else "−"}${amount.currencyFormattedWithCommas()}", style = AppFont.ibmPlexSans(14, FontWeight.Bold), color = accent)
        }
    }
}

@Composable
private fun CurrentPositionCard(name: String, totals: AccountSummaryTotals, isMember: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Color(0xFF103D35), Color(0xFF176B58)))).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (isMember) accountSummaryLabel("memberPosition") else accountSummaryLabel("squadPosition"), style = AppFont.ibmPlexSans(9, FontWeight.Bold), color = Color.White.copy(alpha = 0.7f))
            Text(name, style = AppFont.ibmPlexSans(14, FontWeight.SemiBold), color = Color.White)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PositionMetric(accountSummaryLabel("moneyIn"), "+${totals.credit.currencyFormattedWithCommas()}", Modifier.weight(1f))
            PositionMetric(accountSummaryLabel("moneyOut"), "−${totals.debit.currencyFormattedWithCommas()}", Modifier.weight(1f))
        }
        Divider(color = Color.White.copy(alpha = 0.2f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PositionMetric(if (isMember) accountSummaryLabel("netPosition") else accountSummaryLabel("availableBalance"), totals.available.currencyFormattedWithCommas(), Modifier.weight(1f), true)
            if (!isMember) PositionMetric(SquadStrings.totalMembers, totals.memberCount.toString(), Modifier.weight(1f), true)
        }
    }
}

@Composable
private fun PositionMetric(title: String, value: String, modifier: Modifier = Modifier, prominent: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = AppFont.ibmPlexSans(if (prominent) 9 else 10, FontWeight.Medium), color = Color.White.copy(alpha = 0.68f), maxLines = 1)
        Text(value, style = AppFont.ibmPlexSans(if (prominent) 14 else 12, FontWeight.Bold), color = Color.White, maxLines = 1)
    }
}

@Composable
fun AccountsSummaryCellView(payment: PaymentsDetails) {
    val isCredit = payment.paymentType == PaymentType.PAYMENT_CREDIT
    val movementColor = if (isCredit) AppColors.successAccent else AppColors.secondaryAccent
    val amountPrefix = if (isCredit) "+ " else "− "

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .appShadow(AppShadows.card, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.surface)
            .border(1.dp, AppColors.border.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(movementColor.copy(alpha = 0.11f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = movementColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = payment.localizedMemberName,
                style = AppFont.ibmPlexSans(14, FontWeight.SemiBold),
                color = AppColors.headerText,
                maxLines = 1
            )
            Text(
                text = payment.localizedDescription,
                style = AppFont.ibmPlexSans(12, FontWeight.Normal),
                color = AppColors.secondaryText,
                maxLines = 1
            )
            Text(
                text = SquadStrings.localizedMonthInText(CommonFunctions.dateToString(payment.paymentUpdatedDate?.toDate() ?: Date())),
                style = AppFont.ibmPlexSans(10, FontWeight.Medium),
                color = AppColors.secondaryText.copy(alpha = 0.78f),
                maxLines = 1
            )
        }

        Text(
            text = amountPrefix + payment.amount.currencyFormattedWithCommas(),
            style = AppFont.ibmPlexSans(14, FontWeight.Bold),
            color = movementColor,
            maxLines = 1
        )
    }
}
