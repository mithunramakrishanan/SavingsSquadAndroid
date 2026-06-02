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

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun PaymentApprovalView(
    navController: NavController,
    squadViewModel: SquadViewModel
) {

    var selectedUser by remember { mutableStateOf("All") }

    val userList = remember(
        squadViewModel.squadMembers.value
    ) {
        listOf("All") +
                squadViewModel.squadMembers.value
                    .map { it.name }
                    .distinct()
    }

    val pendingPayments by squadViewModel.pendingApprovalPayments.collectAsStateWithLifecycle()

    val pendingApprovals = pendingPayments.filter {
        it.paymentApproveStatus == PaymentApproveStatus.REQUESTED
    }.let { list ->
        if (selectedUser == "All") list
        else list.filter { it.memberName == selectedUser }
    }

    LaunchedEffect(Unit) {
        squadViewModel.fetchPendingApprovalPayments { _, _ -> }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        AppBackgroundGradient()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            SSNavigationBar(
                title = "Verify Payments",
                navController = navController
            )

            Spacer(modifier = Modifier.height(12.dp))

            DropdownMenuPicker(
                label = "",
                selected = selectedUser,
                items = userList
            ) {
                selectedUser = it
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (pendingApprovals.isEmpty()) {

                Column(
                    modifier = Modifier
                        .fillMaxSize(),
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
                        style = AppFont.ibmPlexSans(
                            15,
                            FontWeight.Medium
                        ),
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
                                ) { success, error ->

                                    if (success) {

                                        AlertManager.shared.showAlert(
                                            title = SquadStrings.appName,
                                            message = "Payment verified successfully",
                                            primaryButtonTitle = "OK"
                                        ) {}
                                    }
                                }
                            },
                            onReject = {

                                squadViewModel.updatePaymentApproveStatus(
                                    squadID = payment.squadId,
                                    paymentID = payment.id ?: "",
                                    status = PaymentApproveStatus.REJECTED
                                ) { success, error ->

                                    if (success) {

                                        AlertManager.shared.showAlert(
                                            title = SquadStrings.appName,
                                            message = "Payment rejected successfully",
                                            primaryButtonTitle = "OK"
                                        ) {}
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
                    text = approval.paymentSubType.value,
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