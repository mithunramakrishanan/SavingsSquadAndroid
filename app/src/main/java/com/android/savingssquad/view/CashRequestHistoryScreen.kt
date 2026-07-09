package com.android.savingssquad.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.pointer.pointerInput
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.ShadowStyle
import com.android.savingssquad.singleton.appShadow

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.android.savingssquad.R
import com.android.savingssquad.SquadSubscription.SubscriptionManager
import com.android.savingssquad.model.CashRequest
import com.android.savingssquad.model.CashRequestStatus
import com.android.savingssquad.model.EMIConfiguration
import com.android.savingssquad.model.Member
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.RecordStatus
import com.android.savingssquad.singleton.color
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.singleton.displayText
import com.android.savingssquad.viewmodel.AppDestination
import com.google.firebase.auth.PhoneAuthOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashRequestHistoryScreen(
    navController: NavController,
    squadViewModel: SquadViewModel
) {

    var selectedUser by remember { mutableStateOf("All") }
    var selectedMemberId by remember { mutableStateOf<String?>(null) }

    val screenType =
        if (UserDefaultsManager.getSquadManagerLogged())
            SquadUserType.SQUAD_MANAGER
        else
            SquadUserType.SQUAD_MEMBER

    val cashRequests by squadViewModel.squadCashRequests
    val members by squadViewModel.squadMembers.collectAsState()

    val userList = remember(members) {
        buildList {
            add("All")
            addAll(members.map { it.name }.distinct())
        }
    }

    var hasLoaded by rememberSaveable { mutableStateOf(false) }

    fun configureInitialFilter() {

        if (screenType == SquadUserType.SQUAD_MEMBER) {

            selectedMemberId = squadViewModel.currentMember.value?.id
            selectedUser = squadViewModel.currentMember.value?.name ?: ""

        } else {

            selectedUser = "All"
            selectedMemberId = null
        }
    }

    fun reloadCashRequests() {

        squadViewModel.resetCashRequestsPagination()

        squadViewModel.fetchCashRequests(
            showLoader = true,
            memberId = selectedMemberId
        ) { success, error ->

            if (!success) {
                Log.e("CashRequest", error ?: "")
            }
        }
    }

    LaunchedEffect(Unit) {

        if (!hasLoaded) {

            hasLoaded = true

            configureInitialFilter()

            reloadCashRequests()
        }
    }

    Scaffold(
        topBar = {

            SSNavigationBar(
                title = SquadStrings.cashRequests,
                navController = navController
            )
        },
        containerColor = Color.Transparent
    ) { padding ->

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            AppBackgroundGradient()

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                //----------------------------------------------------
                // User Filter
                //----------------------------------------------------

                if (screenType != SquadUserType.SQUAD_MEMBER) {

                    DropdownMenuPicker(
                        label = "",
                        selected = selectedUser,
                        items = userList,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                    ) { newUser ->

                            selectedUser = newUser

                            selectedMemberId =
                                if (newUser == "All") {
                                    null
                                } else {
                                    members.firstOrNull {
                                        it.name == newUser
                                    }?.id
                                }

                            reloadCashRequests()
                        }

                }

                //----------------------------------------------------
                // Empty State
                //----------------------------------------------------

                if (
                    cashRequests.isEmpty() &&
                    !squadViewModel.cashRequestsIsLoadingMore
                ) {

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Icon(
                            Icons.Default.Payments,
                            null,
                            modifier = Modifier.size(72.dp),
                            tint = Color.Gray.copy(alpha = 0.6f)
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "No cash requests yet",
                            style = AppFont.ibmPlexSans(
                                15,
                                FontWeight.Medium
                            ),
                            color = AppColors.secondaryText
                        )
                    }

                }

                //----------------------------------------------------
                // List
                //----------------------------------------------------

                else {

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {

                        items(
                            cashRequests,
                            key = { it.id ?: "" }
                        ) { cashRequest ->

                            CashRequestRow(

                                cashRequest = cashRequest,

                                showActions =
                                    screenType != SquadUserType.SQUAD_MEMBER &&
                                            cashRequest.cashRequestStatus ==
                                            CashRequestStatus.CREATED,

                                onAccept = {

                                    squadViewModel.updateCashRequestStatus(
                                        cashRequest,
                                        CashRequestStatus.ACCEPTED
                                    ) { _, _ ->

                                        reloadCashRequests()
                                    }
                                },

                                onReject = {

                                    squadViewModel.updateCashRequestStatus(
                                        cashRequest,
                                        CashRequestStatus.REJECTED
                                    ) { _, _ ->

                                        reloadCashRequests()
                                    }
                                }
                            )

                            LaunchedEffect(cashRequest.id) {

                                squadViewModel.loadMoreCashRequestsIfNeeded(
                                    currentCashRequest = cashRequest,
                                    memberId = selectedMemberId
                                )
                            }
                        }

                        if (
                            squadViewModel.cashRequestsIsLoadingMore &&
                            cashRequests.isNotEmpty()
                        ) {

                            item {

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {

                                    CircularProgressIndicator()
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
fun CashRequestRow(
    cashRequest: CashRequest,
    showActions: Boolean = false,
    onAccept: () -> Unit = {},
    onReject: () -> Unit = {}
) {

    val statusColor = when (cashRequest.cashRequestStatus) {
        CashRequestStatus.CREATED -> AppColors.warningAccent
        CashRequestStatus.ACCEPTED -> AppColors.successAccent
        CashRequestStatus.REJECTED -> AppColors.errorAccent
    }

    val formattedDate = remember(cashRequest.requestedOn) {
        cashRequest.requestedOn?.toDate()?.let {
            SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).format(it)
        } ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.surface)
            .border(
                1.dp,
                AppColors.border.copy(alpha = 0.45f),
                RoundedCornerShape(16.dp)
            )
            .appShadow(
                style = AppShadows.card,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        //---------------------------------------------------------
        // Header
        //---------------------------------------------------------

        Row(
            verticalAlignment = Alignment.Top
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = cashRequest.requestedByName,
                    style = AppFont.ibmPlexSans(
                        15,
                        FontWeight.SemiBold
                    ),
                    color = AppColors.headerText
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = formattedDate,
                    style = AppFont.ibmPlexSans(
                        12,
                        FontWeight.Normal
                    ),
                    color = AppColors.secondaryText
                )
            }

            StatusChip(
                text = cashRequest.cashRequestStatus.name,
                color = statusColor
            )
        }

        //---------------------------------------------------------
        // EMI Details
        //---------------------------------------------------------

        cashRequest.requestedEMIConfig?.let { emi ->

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {

                    Text(
                        "Amount",
                        style = AppFont.ibmPlexSans(
                            11,
                            FontWeight.Normal
                        ),
                        color = AppColors.secondaryText
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        "₹${emi.loanAmount}",
                        style = AppFont.ibmPlexSans(
                            18,
                            FontWeight.Bold
                        ),
                        color = AppColors.headerText
                    )
                }

                Spacer(Modifier.weight(1f))

                if (emi.emiInterestRate > 0) {

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {

                        Text(
                            "Interest",
                            style = AppFont.ibmPlexSans(
                                11,
                                FontWeight.Normal
                            ),
                            color = AppColors.secondaryText
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            "${emi.emiInterestRate}%",
                            style = AppFont.ibmPlexSans(
                                18,
                                FontWeight.Bold
                            ),
                            color = AppColors.headerText
                        )
                    }
                }
            }

            HorizontalDivider(
                color = AppColors.border
            )

            Row {

                InfoItem(
                    title = "Months",
                    value = "${emi.emiMonths}"
                )

                Spacer(Modifier.weight(1f))

                InfoItem(
                    title = "Monthly EMI",
                    value = "₹${emi.emiAmount}"
                )

                Spacer(Modifier.weight(1f))

                InfoItem(
                    title = "Interest",
                    value = "₹${emi.interestAmount}"
                )
            }
        }
        if (showActions) {

            HorizontalDivider(
                color = AppColors.border
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AppColors.errorAccent.copy(alpha = .08f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        AppColors.errorAccent.copy(alpha = .25f)
                    )
                ) {

                    Text(
                        "Reject",
                        style = AppFont.ibmPlexSans(
                            13,
                            FontWeight.SemiBold
                        ),
                        color = AppColors.errorAccent
                    )
                }

                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.successAccent
                    )
                ) {

                    Text(
                        "Accept",
                        style = AppFont.ibmPlexSans(
                            13,
                            FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    color: Color
) {

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = .12f))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {

        Text(
            text = text.replaceFirstChar { it.uppercase() },
            style = AppFont.ibmPlexSans(
                11,
                FontWeight.SemiBold
            ),
            color = color
        )
    }
}

@Composable
private fun InfoItem(
    title: String,
    value: String
) {

    Column {

        Text(
            title,
            style = AppFont.ibmPlexSans(
                11,
                FontWeight.Normal
            ),
            color = AppColors.secondaryText
        )

        Spacer(Modifier.height(2.dp))

        Text(
            value,
            style = AppFont.ibmPlexSans(
                14,
                FontWeight.Bold
            ),
            color = AppColors.headerText
        )
    }
}

@Composable
fun CashRequestEmptyView() {

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = AppColors.secondaryText.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Cash Requests Yet",
            style = AppFont.ibmPlexSans(
                16,
                FontWeight.SemiBold
            ),
            color = AppColors.headerText
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Cash requests will appear here.",
            style = AppFont.ibmPlexSans(
                13,
                FontWeight.Medium
            ),
            color = AppColors.secondaryText
        )
    }
}

@Composable
fun RequestCashEMIListView(
    title: String = "Request Cash",
    emiConfigs: List<EMIConfiguration>,
    onRequestCash: (EMIConfiguration) -> Unit = {},
    onDismiss: () -> Unit
) {

    var appeared by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .width(330.dp)
            .heightIn(max = 380.dp)
            .background(
                color = AppColors.surface,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = AppColors.border.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            )
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp)
            )
    ) {


        // MARK: Header

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 14.dp,
                    bottom = 10.dp
                )
        ) {

            Column(
                modifier = Modifier.align(
                    Alignment.Center
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = title,
                    style = AppFont.ibmPlexSans(
                        size = 17,
                        weight = FontWeight.Bold
                    ),
                    color = AppColors.headerText
                )


                Text(
                    text = "${emiConfigs.size} option${if (emiConfigs.size == 1) "" else "s"}",
                    style = AppFont.ibmPlexSans(
                        size = 11,
                        weight = FontWeight.Medium
                    ),
                    color = AppColors.secondaryText
                )
            }


            IconButton(
                onClick = {
                    onDismiss()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp)
                    .size(28.dp)
            ) {

                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = AppColors.secondaryText,
                    modifier = Modifier.size(14.dp)
                )
            }
        }


        HorizontalDivider(
            color = AppColors.secondaryText.copy(alpha = 0.15f)
        )


        // MARK: EMI List

        LazyColumn(
            modifier = Modifier
                .weight(1f, fill = false)
                .heightIn(max = 260.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(
                vertical = 10.dp,
                horizontal = 12.dp
            )
        ) {

            itemsIndexed(
                items = emiConfigs,
                key = { _, item -> item.id!! }
            ) { index, emi ->


                AnimatedVisibility(
                    visible = appeared,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = 250,
                            delayMillis = index * 30
                        )
                    ) + slideInVertically(
                        initialOffsetY = { 5 }
                    )
                ) {

                    EMIRequestRow(
                        emi = emi,
                        onRequestCash = {

                            onRequestCash(emi)
                            onDismiss()

                        }
                    )
                }
            }
        }


        HorizontalDivider(
            color = AppColors.secondaryText.copy(alpha = 0.15f)
        )


        // MARK: Footer

        TextButton(
            onClick = {
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = "Close",
                style = AppFont.ibmPlexSans(
                    size = 13,
                    weight = FontWeight.SemiBold
                ),
                color = AppColors.secondaryText
            )
        }


        LaunchedEffect(Unit) {
            appeared = true
        }
    }
}



@Composable
private fun EMIRequestRow(
    emi: EMIConfiguration,
    onRequestCash: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AppColors.background,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {


        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {

                Text(
                    text = "Loan Amount",
                    style = AppFont.ibmPlexSans(
                        size = 10
                    ),
                    color = AppColors.secondaryText
                )


                Text(
                    text = "₹${emi.loanAmount}",
                    style = AppFont.ibmPlexSans(
                        size = 17,
                        weight = FontWeight.Bold
                    ),
                    color = AppColors.headerText
                )
            }


            Spacer(
                modifier = Modifier.weight(1f)
            )


            Button(
                onClick = onRequestCash,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primaryBrand
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(
                    horizontal = 12.dp,
                    vertical = 7.dp
                )
            ) {

                Text(
                    text = "Request",
                    style = AppFont.ibmPlexSans(
                        size = 12,
                        weight = FontWeight.SemiBold
                    ),
                    color = Color.White
                )
            }
        }


        HorizontalDivider(
            color = AppColors.border.copy(alpha = 0.3f)
        )


        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            EMIInfoView(
                title = "Months",
                value = "${emi.emiMonths}"
            )


            Spacer(
                modifier = Modifier.weight(1f)
            )


            EMIInfoView(
                title = "EMI",
                value = "₹${emi.emiAmount}"
            )


            Spacer(
                modifier = Modifier.weight(1f)
            )


            EMIInfoView(
                title = "Interest",
                value = "₹${emi.interestAmount}"
            )
        }
    }
}



@Composable
private fun EMIInfoView(
    title: String,
    value: String
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {

        Text(
            text = title,
            style = AppFont.ibmPlexSans(
                size = 10
            ),
            color = AppColors.secondaryText
        )


        Text(
            text = value,
            style = AppFont.ibmPlexSans(
                size = 12,
                weight = FontWeight.Bold
            ),
            color = AppColors.headerText
        )
    }
}

//@Preview(showBackground = true)
//@Composable
//fun RequestCashEMIListViewPreview() {
//
//    RequestCashEMIListView(
//        emiConfigs = listOf(
//
//            EMIConfiguration(
//                id = "1",
//                loanAmount = 100000,
//                emiMonths = 12,
//                emiInterestRate = 10.0,
//                emiAmount = 9500,
//                interestAmount = 14000
//            ),
//
//            EMIConfiguration(
//                id = "2",
//                loanAmount = 50000,
//                emiMonths = 6,
//                emiInterestRate = 8.0,
//                emiAmount = 9000,
//                interestAmount = 4000
//            ),
//
//            EMIConfiguration(
//                id = "3",
//                loanAmount = 50000,
//                emiMonths = 6,
//                emiInterestRate = 8.0,
//                emiAmount = 9000,
//                interestAmount = 4000
//            ),
//            EMIConfiguration(
//                id = "4",
//                loanAmount = 50000,
//                emiMonths = 6,
//                emiInterestRate = 8.0,
//                emiAmount = 9000,
//                interestAmount = 4000
//            )
//        ),
//        onRequestCash = { emi ->
//            println("Request Cash: ${emi.loanAmount}")
//        },
//        title = "",
//        isShowing = true
//    )
//}