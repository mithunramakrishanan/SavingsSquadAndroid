package com.android.savingssquad.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.model.ContributionDetail
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.PaidStatus
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.viewmodel.SquadViewModel
import com.yourapp.utils.CommonFunctions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ContributionDetailsView(
    navController: NavController,
    squadViewModel: SquadViewModel
) {
    var selectedSegment by remember {
        mutableStateOf(SquadStrings.all)
    }

    var memberContributions by remember {
        mutableStateOf(listOf<ContributionDetail>())
    }

    var isSummaryExpanded by remember {
        mutableStateOf(false)
    }

    val squadState by squadViewModel.squad.collectAsStateWithLifecycle()

    val screenType = if (UserDefaultsManager.getSquadManagerLogged()) {
        SquadUserType.SQUAD_MANAGER
    } else {
        SquadUserType.SQUAD_MEMBER
    }

    val memberState = if (screenType == SquadUserType.SQUAD_MANAGER) {
        squadViewModel.selectedMember.collectAsStateWithLifecycle()
    } else {
        squadViewModel.currentMember.collectAsStateWithLifecycle()
    }

    val member = memberState.value

    // Initial contribution amount.
    // Convert to Int so all summary calculations use Int.
    val initialContributionAmount = remember(member) {
        member?.memberCreatedContribuionAmount?.toInt() ?: 0
    }

    // Contribution counts.
    val subtitleContributions by remember(
        memberContributions,
        selectedSegment
    ) {
        derivedStateOf {
            val paidCount = memberContributions.count {
                it.paidStatus == PaidStatus.PAID
            }

            val unpaidCount = memberContributions.count {
                it.paidStatus == PaidStatus.NOT_PAID
            }

            val total = memberContributions.size

            when (selectedSegment) {
                SquadStrings.paidFilter ->
                    "$paidCount ${SquadStrings.paid}, ${total - paidCount} ${SquadStrings.remaining}"

                SquadStrings.unpaidFilter ->
                    "$unpaidCount ${SquadStrings.unpaid}, ${total - unpaidCount} ${SquadStrings.paid}"

                else ->
                    "$paidCount ${SquadStrings.paid}, $unpaidCount ${SquadStrings.unpaid}"
            }
        }
    }

    // Sort and filter contribution records.
    val filteredContributions by remember(
        memberContributions,
        selectedSegment
    ) {
        derivedStateOf {
            val formatter = SimpleDateFormat(
                "MMM yyyy",
                Locale.ENGLISH
            )

            memberContributions
                .sortedBy {
                    runCatching {
                        formatter.parse(it.monthYear)
                    }.getOrNull()
                }
                .filter { contribution ->
                    when (selectedSegment) {
                        SquadStrings.paidFilter ->
                            contribution.paidStatus == PaidStatus.PAID

                        SquadStrings.unpaidFilter ->
                            contribution.paidStatus == PaidStatus.NOT_PAID

                        else -> true
                    }
                }
        }
    }

    // Total paid amount.
    val totalPaidAmount by remember(
        filteredContributions
    ) {
        derivedStateOf {
            filteredContributions
                .filter {
                    it.paidStatus == PaidStatus.PAID
                }
                .sumOf {
                    it.amount.toInt()
                }
        }
    }

    // Total unpaid amount.
    val totalUnpaidAmount by remember(
        filteredContributions
    ) {
        derivedStateOf {
            filteredContributions
                .filter {
                    it.paidStatus == PaidStatus.NOT_PAID
                }
                .sumOf {
                    it.amount.toInt()
                }
        }
    }

    // Fetch member contributions.
    LaunchedEffect(
        squadState?.squadID,
        member?.id
    ) {
        val squad = squadState
        val memberID = member?.id

        if (squad != null && memberID != null) {
            squadViewModel.fetchContributionsForMember(
                showLoader = true,
                squadID = squad.squadID,
                memberID = memberID
            ) { list, _ ->
                memberContributions = list ?: emptyList()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        AppBackgroundGradient()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Navigation bar.
            SSNavigationBar(
                title = member?.memberName.orEmpty(),
                navController = navController
            )

            // Segmented picker.
            ModernSegmentedPickerView(
                segments = listOf(
                    SquadStrings.all,
                    SquadStrings.paidFilter,
                    SquadStrings.unpaidFilter
                ),
                selectedSegment = selectedSegment,
                onSegmentSelected = {
                    selectedSegment = it
                }
            )

            // Expandable contribution summary.
            ContributionSummarySection(
                isExpanded = isSummaryExpanded,
                onToggle = {
                    isSummaryExpanded = !isSummaryExpanded
                },
                initialContributionAmount = initialContributionAmount,
                totalPaidAmount = totalPaidAmount + initialContributionAmount,
                totalUnpaidAmount = totalUnpaidAmount,
                selectedSegment = selectedSegment
            )

            // Contribution list.
            SectionView(
                title = SquadStrings.contribution,
                subtitle = subtitleContributions
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(
                        vertical = 8.dp
                    )
                ) {
                    if (filteredContributions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = SquadStrings.noContributions,
                                    color = AppColors.secondaryText,
                                    modifier = Modifier.padding(top = 20.dp),
                                    style = AppFont.ibmPlexSans(
                                        14,
                                        FontWeight.Normal
                                    )
                                )
                            }
                        }
                    } else {
                        items(
                            items = filteredContributions,
                            key = { it.monthYear }
                        ) { fund ->
                            ContributionDetailCell(
                                fund = fund
                            )
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Expandable Contribution Summary

@Composable
private fun ContributionSummarySection(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    initialContributionAmount: Int,
    totalPaidAmount: Int,
    totalUnpaidAmount: Int,
    selectedSegment: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .animateContentSize(
                animationSpec = tween(durationMillis = 250)
            )
    ) {

        // Compact summary header.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .appShadow(
                    AppShadows.card,
                    RoundedCornerShape(12.dp)
                )
                .background(
                    AppColors.surface,
                    RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = AppColors.border.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onToggle)
                .padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = null,
                tint = AppColors.headerText
            )

            Text(
                text = SquadStrings.summary,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                style = AppFont.ibmPlexSans(
                    14,
                    FontWeight.SemiBold
                ),
                color = AppColors.headerText
            )

            Icon(
                imageVector = if (isExpanded) {
                    Icons.Default.ExpandLess
                } else {
                    Icons.Default.ExpandMore
                },
                contentDescription = if (isExpanded) {
                    "Collapse summary"
                } else {
                    "Expand summary"
                },
                tint = AppColors.secondaryText
            )
        }

        // Summary cards are hidden initially.
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(
                animationSpec = tween(200)
            ) + expandVertically(
                animationSpec = tween(250)
            ),
            exit = fadeOut(
                animationSpec = tween(150)
            ) + shrinkVertically(
                animationSpec = tween(250)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // Initial contribution.
                if (initialContributionAmount > 0) {
                    ContributionSummaryItem(
                        title = SquadStrings.initialContribution,
                        amount = initialContributionAmount,
                        color = AppColors.headerText
                    )
                }

                // Paid and unpaid amounts.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedSegment != SquadStrings.unpaidFilter) {
                        ContributionSummaryItem(
                            title = SquadStrings.totalPaid,
                            amount = totalPaidAmount,
                            color = AppColors.successAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (selectedSegment != SquadStrings.paidFilter) {
                        ContributionSummaryItem(
                            title = SquadStrings.totalUnpaid,
                            amount = totalUnpaidAmount,
                            color = AppColors.errorAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// MARK: - Summary Item

@Composable
private fun ContributionSummaryItem(
    title: String,
    amount: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .appShadow(
                AppShadows.card,
                RoundedCornerShape(12.dp)
            )
            .background(
                AppColors.surface,
                RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = AppColors.border.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = AppFont.ibmPlexSans(
                12,
                FontWeight.Medium
            ),
            color = AppColors.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = amount.currencyFormattedWithCommas(),
            style = AppFont.ibmPlexSans(
                16,
                FontWeight.Bold
            ),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// MARK: - Contribution Detail Cell

@Composable
fun ContributionDetailCell(
    fund: ContributionDetail
) {
    val isPaid = fund.paidStatus == PaidStatus.PAID

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .appShadow(
                AppShadows.card,
                RoundedCornerShape(16.dp)
            )
            .background(
                AppColors.surface,
                RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = AppColors.border.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Left: Month and amount.
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = SquadStrings.localizedMonthInText(
                    fund.monthYear
                ),
                style = AppFont.ibmPlexSans(
                    16,
                    FontWeight.SemiBold
                ),
                color = AppColors.headerText
            )

            Text(
                text = fund.amount.currencyFormattedWithCommas(),
                style = AppFont.ibmPlexSans(
                    17,
                    FontWeight.Bold
                ),
                color = AppColors.headerText
            )
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )

        // Right: Payment status and date.
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.End
        ) {

            Text(
                text = if (isPaid) {
                    SquadStrings.listPaid
                } else {
                    SquadStrings.listUnpaid
                },
                color = Color.White,
                style = AppFont.ibmPlexSans(
                    13,
                    FontWeight.SemiBold
                ),
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (isPaid) {
                                listOf(
                                    AppColors.successAccent.copy(
                                        alpha = 0.85f
                                    ),
                                    AppColors.successAccent
                                )
                            } else {
                                listOf(
                                    AppColors.errorAccent.copy(
                                        alpha = 0.85f
                                    ),
                                    AppColors.errorAccent
                                )
                            }
                        ),
                        shape = CircleShape
                    )
                    .padding(
                        horizontal = 14.dp,
                        vertical = 6.dp
                    )
            )

            if (isPaid) {
                val paidDate = CommonFunctions.dateToString(
                    fund.paidOn?.toDate() ?: Date()
                )

                val localizedPaidDate =
                    SquadStrings.localizedMonthInText(paidDate)

                Text(
                    text = "${SquadStrings.paidOn} $localizedPaidDate",
                    style = AppFont.ibmPlexSans(
                        12,
                        FontWeight.Normal
                    ),
                    color = AppColors.secondaryText
                )
            }
        }
    }
}