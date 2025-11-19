package com.android.savingssquad.view

import android.app.Activity
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.ShadowStyle
import com.android.savingssquad.singleton.appShadow

import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.Login
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.viewmodel.SquadViewModel
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.delay
import com.android.savingssquad.viewmodel.LoaderManager
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import java.util.Date
import java.util.concurrent.TimeUnit
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.savingssquad.R
import com.android.savingssquad.model.ContributionDetail
import com.android.savingssquad.model.Squad
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.PaidStatus
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.viewmodel.AppDestination
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ContributionDetailsView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared,
) {
    val context = LocalContext.current
    val dismiss = { navController.popBackStack() }

    var selectedSegment by remember { mutableStateOf(SquadStrings.all) }
    var memberContributions by remember { mutableStateOf(listOf<ContributionDetail>()) }

    val squadState by squadViewModel.squad.collectAsStateWithLifecycle()

    val screenType = if (UserDefaultsManager.getSquadManagerLogged()) {
        SquadUserType.SQUAD_MANAGER
    } else {
        SquadUserType.SQUAD_MEMBER
    }

// Get the member depending on the screen type
    val member by if (screenType == SquadUserType.SQUAD_MANAGER) {
        squadViewModel.selectedMember.collectAsStateWithLifecycle()
    } else {
        squadViewModel.currentMember.collectAsStateWithLifecycle()
    }

    // Subtitle text
    val subtitleContributions by remember(memberContributions, selectedSegment) {
        derivedStateOf {
            val paidCount = memberContributions.count { it.paidStatus == PaidStatus.PAID }
            val unpaidCount = memberContributions.count { it.paidStatus == PaidStatus.NOT_PAID }
            val total = memberContributions.size

            when (selectedSegment) {
                SquadStrings.paidFilter -> "$paidCount Paid, ${total - paidCount} Remaining"
                SquadStrings.unpaidFilter -> "$unpaidCount Unpaid, ${total - unpaidCount} Paid"
                else -> "$paidCount Paid, $unpaidCount Unpaid"
            }
        }
    }

    // Filtered + sorted list
    val filteredContributions by remember(memberContributions, selectedSegment) {
        derivedStateOf {
            val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())

            memberContributions
                .sortedBy { formatter.parse(it.monthYear) }
                .filter {
                    when (selectedSegment) {
                        SquadStrings.paidFilter -> it.paidStatus == PaidStatus.PAID
                        SquadStrings.unpaidFilter -> it.paidStatus == PaidStatus.NOT_PAID
                        else -> true
                    }
                }
        }
    }

    LaunchedEffect(member!!.id) {
        squadState?.let { gf ->
            member!!.id?.let {
                squadViewModel.fetchContributionsForMember(
                    showLoader = true,
                    squadID = gf.squadID,
                    memberID = it
                ) { list, _ ->
                    memberContributions = list ?: emptyList()
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AppBackgroundGradient()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SSNavigationBar(
                title = member!!.name,
                navController = navController
            )

            // Segmented Picker
            ModernSegmentedPickerView(
                segments = listOf(
                    SquadStrings.all,
                    SquadStrings.paidFilter,
                    SquadStrings.unpaidFilter
                ),
                selectedSegment = selectedSegment,
                onSegmentSelected = { selectedSegment = it }
            )

            SectionView(
                title = "Contribution",
                subtitle = subtitleContributions
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (filteredContributions.isEmpty()) {
                        item {
                            Text(
                                "No contributions found",
                                color = AppColors.secondaryText,
                                modifier = Modifier.padding(top = 20.dp),
                                style = AppFont.ibmPlexSans(14, FontWeight.Normal)
                            )
                        }
                    } else {
                        items(filteredContributions, key = { it.monthYear }) { fund ->
                            ContributionDetailCell(fund = fund)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ContributionDetailCell(fund: ContributionDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .appShadow(AppShadows.card, RoundedCornerShape(16.dp))
            .background(AppColors.surface, RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = AppColors.border.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = fund.monthYear,
                style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                color = AppColors.headerText
            )

            Text(
                text = fund.amount.currencyFormattedWithCommas(),
                style = AppFont.ibmPlexSans(17, FontWeight.Bold),
                color = AppColors.headerText
            )
        }

        Spacer(Modifier.weight(1f))

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.End
        ) {

            val isPaid = fund.paidStatus == PaidStatus.PAID
            val badgeColor = if (isPaid) AppColors.successAccent else AppColors.errorAccent

            Text(
                text = if (isPaid) SquadStrings.listPaid else SquadStrings.listUnpaid,
                color = Color.White,
                style = AppFont.ibmPlexSans(13, FontWeight.SemiBold),
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (isPaid)
                                listOf(
                                    AppColors.successAccent.copy(alpha = 0.85f),
                                    AppColors.successAccent
                                )
                            else
                                listOf(
                                    AppColors.errorAccent.copy(alpha = 0.85f),
                                    AppColors.errorAccent
                                ),
                        ),
                        shape = CircleShape
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )

            if (isPaid) {
                Text(
                    text = "${SquadStrings.paidOn} ${
                        CommonFunctions.dateToString(fund.paidOn?.toDate() ?: Date())
                    }",
                    style = AppFont.ibmPlexSans(12, FontWeight.Normal),
                    color = AppColors.secondaryText
                )
            }
        }
    }
}