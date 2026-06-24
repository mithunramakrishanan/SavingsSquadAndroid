package com.android.savingssquad.view

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import com.android.savingssquad.model.Squad
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.MemberLoan
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.asTimestamp
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.singleton.displayText
import com.android.savingssquad.viewmodel.AlertManager
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.flow.map
import java.util.Calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.savingssquad.singleton.PaymentApproveStatus
import com.android.savingssquad.singleton.PaymentSubType
import com.android.savingssquad.viewmodel.SSToast
import com.android.savingssquad.viewmodel.ToastManager
import com.android.savingssquad.viewmodel.ToastType

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun PaymentApprovalView(
    navController: NavController,
    squadViewModel: SquadViewModel
) {

    var selectedUser by remember { mutableStateOf("All") }

    val screenType =
        if (UserDefaultsManager.getSquadManagerLogged())
            SquadUserType.SQUAD_MANAGER
        else
            SquadUserType.SQUAD_MEMBER

    val squadMembers by squadViewModel.squadMembers.collectAsStateWithLifecycle()
    val pendingPayments by squadViewModel.pendingApprovalPayments.collectAsStateWithLifecycle()

    val userList = remember(squadMembers) {
        listOf("All") + squadMembers.map { it.name }.distinct()
    }

    // ❌ NO FILTERING HERE (Firestore already does it)
    val pendingApprovals = pendingPayments

    LaunchedEffect(Unit) {

        UserDefaultsManager.saveIsFromnotification(false)

        val memberId =
            if (screenType == SquadUserType.SQUAD_MEMBER)
                squadViewModel.currentMember.value?.id
            else null

        squadViewModel.fetchPendingApprovalPayments(

            showLoader = true,

            screenType = screenType,

            memberId = memberId

        ) { success, error ->

            if (!success) {

                println("Error: $error")

            }

        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        AppBackgroundGradient()

        Column(modifier = Modifier.fillMaxSize()) {

            SSNavigationBar(
                title = "Verify Payments",
                navController = navController
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Manager only dropdown (UI only, NOT filtering)
            if (screenType == SquadUserType.SQUAD_MANAGER) {
                DropdownMenuPicker(
                    label = "",
                    selected = selectedUser,
                    items = userList
                ) {
                    selectedUser = it

                    val memberId = if (it == "All") null else {
                        squadMembers.firstOrNull { m -> m.name == it }?.id
                    }

                    squadViewModel.fetchPendingApprovalPayments(

                        showLoader = true,

                        screenType = screenType,

                        memberId = memberId

                    ) { success, error ->

                        if (!success) {

                            println("Error: $error")

                        }

                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (pendingApprovals.isEmpty()) {

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = AppColors.successAccent.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "No Payments Pending Verification",
                        style = AppFont.ibmPlexSans(15, FontWeight.Medium),
                        color = AppColors.secondaryText
                    )
                }

            } else {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    items(
                        pendingApprovals,
                        key = { it.id ?: "" }
                    ) { payment ->

                        PaymentApprovalRow(
                            approval = payment,
                            onApprove = {

                                squadViewModel.updatePaymentApproveStatus(
                                    squadID = payment.squadId,
                                    paymentID = payment.id ?: "",
                                    status = PaymentApproveStatus.ACCEPTED
                                ) { success, _ ->

                                    if (success) {

                                        ToastManager.show(

                                            message = "Payment Status Updated",

                                            type = ToastType.SUCCESS

                                        )

                                        val memberId =
                                            if (screenType == SquadUserType.SQUAD_MEMBER)
                                                squadViewModel.currentMember.value?.id
                                            else null
                                        squadViewModel.fetchPendingApprovalPayments(

                                            showLoader = true,

                                            screenType = screenType,

                                            memberId = memberId

                                        ) { success, error ->

                                            if (!success) {

                                                println("Error: $error")

                                            }

                                        }
                                    }
                                }
                            },
                            onReject = {

                                squadViewModel.updatePaymentApproveStatus(
                                    squadID = payment.squadId,
                                    paymentID = payment.id ?: "",
                                    status = PaymentApproveStatus.REJECTED
                                ) { success, _ ->

                                    if (success) {

                                        ToastManager.show(

                                            message = "Payment Status Updated",

                                            type = ToastType.SUCCESS

                                        )

                                        val memberId =
                                            if (screenType == SquadUserType.SQUAD_MEMBER)
                                                squadViewModel.currentMember.value?.id
                                            else null

                                        squadViewModel.fetchPendingApprovalPayments(

                                            showLoader = true,

                                            screenType = screenType,

                                            memberId = memberId

                                        ) { success, error ->

                                            if (!success) {

                                                println("Error: $error")

                                            }

                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        SSAlert()
        SSLoaderView()
        SSToast()
    }
}


@Composable
fun PaymentApprovalRow(
    approval: PaymentsDetails,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.surface)
            .padding(16.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = approval.memberName,
                    style = AppFont.ibmPlexSans(
                        16,
                        FontWeight.SemiBold
                    ),
                    color = AppColors.headerText
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = getPaymentLabel(approval.paymentSubType),
                    style = AppFont.ibmPlexSans(
                        13,
                        FontWeight.Normal
                    ),
                    color = AppColors.secondaryText
                )
            }

            Text(
                text = "₹${approval.amount}",
                style = AppFont.ibmPlexSans(
                    18,
                    FontWeight.Bold
                ),
                color = AppColors.primaryBrand
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp)
        )

        if (approval.description.isNotEmpty()) {

            Text(
                text = "Description",
                style = AppFont.ibmPlexSans(
                    12,
                    FontWeight.Medium
                ),
                color = AppColors.secondaryText
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = approval.description,
                style = AppFont.ibmPlexSans(
                    14,
                    FontWeight.Normal
                ),
                color = AppColors.headerText
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = Color(0xFFFF9800)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "Pending Verification",
                style = AppFont.ibmPlexSans(
                    12,
                    FontWeight.Medium
                ),
                color = Color(0xFFFF9800)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Button(
            onClick = onApprove,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.successAccent
            ),
            shape = RoundedCornerShape(10.dp)
        ) {

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Amount Received",
                style = AppFont.ibmPlexSans(
                    15,
                    FontWeight.SemiBold
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onReject,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red
            ),
            shape = RoundedCornerShape(10.dp)
        ) {

            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Amount Not Received",
                style = AppFont.ibmPlexSans(
                    15,
                    FontWeight.SemiBold
                )
            )
        }
    }
}

fun getPaymentLabel(type: PaymentSubType): String {
    return when (type) {

        PaymentSubType.CONTRIBUTION_AMOUNT -> "Contribution"

        PaymentSubType.LOAN_AMOUNT  -> "Loan"

        PaymentSubType.EMI_AMOUNT  -> "EMI"

        else -> "Payment"
    }
}