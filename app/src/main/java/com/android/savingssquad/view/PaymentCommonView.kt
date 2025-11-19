package com.android.savingssquad.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.savingssquad.model.*
import com.android.savingssquad.model.PayoutStatus
import com.android.savingssquad.singleton.*
import com.android.savingssquad.viewmodel.SquadViewModel
import com.google.firebase.Timestamp
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.launch
import java.util.*

// --------------------------------------------------------------
// MARK: - Payment Row (Main Container)
// --------------------------------------------------------------
@Composable
fun PaymentRow(
    payment: PaymentsDetails,
    showPaymentStatusRow: Boolean,
    showPayoutStatusRow: Boolean,
    squadViewModel: SquadViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .appShadow(AppShadows.card, RoundedCornerShape(14.dp)) // shadow outside
            .clip(RoundedCornerShape(14.dp))                       // round corners
            .background(AppColors.surface)                        // card background
            .padding(12.dp)                                       // inner content padding
    ) {
        // 🔹 Name
        Text(
            text = if (payment.paymentType == PaymentType.PAYMENT_DEBIT)
                "Squad Manager" else payment.memberName,
            style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
            color = AppColors.headerText
        )

        // 🔹 Description
        Text(
            text = if (payment.paymentType == PaymentType.PAYMENT_DEBIT)
                "${payment.description} to ${payment.memberName}" else payment.description,
            style = AppFont.ibmPlexSans(13, FontWeight.Normal),
            color = AppColors.secondaryText,
            modifier = Modifier.padding(top = 4.dp)
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        if (showPaymentStatusRow) {
            PaymentStatusRowView(
                status = payment.paymentStatus,
                paymentDate = payment.paymentUpdatedDate,
                reason = payment.paymentResponseMessage,
                retryAction = { squadViewModel.retryPaymentAction(payment) },
                paymentType = payment.paymentType,
                memberId = payment.memberId,
                squadViewModel = squadViewModel
            )
        }

        if (showPayoutStatusRow) {
            PayoutStatusRowView(
                status = payment.payoutStatus,
                payoutDate = payment.payoutUpdatedDate,
                reason = payment.payoutResponseMessage,
                retryAction = { squadViewModel.retryPayoutAction(payment) },
                paymentType = payment.paymentType,
                memberId = payment.memberId,
                squadViewModel = squadViewModel
            )
        }
    }
}

// --------------------------------------------------------------
// MARK: - Payment Status Row
// --------------------------------------------------------------
@Composable
fun PaymentStatusRowView(
    status: PaymentStatus,
    paymentDate: Timestamp?,
    reason: String,
    retryAction: () -> Unit,
    paymentType: PaymentType,
    memberId: String,
    squadViewModel: SquadViewModel
) {
    var reasonData by remember { mutableStateOf<ReasonSheetData?>(null) }

    CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(color = AppColors.headerText)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    // ✅ Enables subpixel antialiasing for icons and text
                    renderEffect = null
                    clip = false
                }
                .padding(vertical = 2.dp)
        ) {
            // ✅ Larger icon (better vector clarity on HDPI)
            Icon(
                imageVector = when (status) {
                    PaymentStatus.SUCCESS -> Icons.Filled.CheckCircle
                    PaymentStatus.FAILED -> Icons.Filled.Error
                    else -> Icons.Filled.Info
                },
                contentDescription = null,
                tint = when (status) {
                    PaymentStatus.SUCCESS -> Color(0xFF2ECC71) // vibrant green
                    PaymentStatus.FAILED -> Color(0xFFE74C3C)  // bright red
                    else -> AppColors.infoAccent
                },
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.CenterVertically)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // ✅ Use Material3 typography for better anti-alias
                Text(
                    text = status.displayText,
                    style = AppFont.ibmPlexSans(14, FontWeight.SemiBold),
                    color = when (status) {
                        PaymentStatus.SUCCESS -> Color(0xFF2ECC71)
                        PaymentStatus.FAILED -> Color(0xFFE74C3C)
                        else -> AppColors.infoAccent
                    }
                )

                paymentDate?.let {
                    Text(
                        text = CommonFunctions.dateToString(it.toDate()),
                        style = AppFont.ibmPlexSans(12, FontWeight.Normal),
                        color = AppColors.secondaryText
                    )
                }

                if (reason.isNotEmpty()) {
                    Text(
                        text = "More Details",
                        style = AppFont.ibmPlexSans(11, FontWeight.Medium),
                        color = Color(0xFF007AFF),
                        modifier = Modifier.clickable {
                            reasonData = ReasonSheetData(
                                title = if (status == PaymentStatus.SUCCESS)
                                    "Payment Success" else "Payment Issue",
                                message = reason
                            )
                        }
                    )
                }
            }

            if (canShowPaymentAction(status, paymentType, memberId, squadViewModel)) {
                Button(
                    onClick = retryAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71).copy(alpha = 0.15f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                ) {
                    Text(
                        "Retry",
                        style = AppFont.ibmPlexSans(11, FontWeight.Medium),
                        color = Color(0xFF2ECC71)
                    )
                }
            }
        }

        reasonData?.let {
            ReasonSheet(it) { reasonData = null }
        }
    }
}

private fun canShowPaymentAction(
    status: PaymentStatus,
    paymentType: PaymentType,
    memberId: String,
    squadViewModel: SquadViewModel
): Boolean {
    return status == PaymentStatus.FAILED &&
            ((paymentType == PaymentType.PAYMENT_CREDIT &&
                    memberId == squadViewModel.currentMember.value?.id) ||
                    (paymentType == PaymentType.PAYMENT_DEBIT &&
                            UserDefaultsManager.getSquadManagerLogged()))
}

// --------------------------------------------------------------
// MARK: - Payout Status Row
// --------------------------------------------------------------
@Composable
fun PayoutStatusRowView(
    status: PayoutStatus,
    payoutDate: Timestamp?,
    reason: String,
    retryAction: () -> Unit,
    paymentType: PaymentType,
    memberId: String,
    squadViewModel: SquadViewModel
) {
    var reasonData by remember { mutableStateOf<ReasonSheetData?>(null) }

    CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(color = AppColors.headerText)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    // ✅ Ensure sharp edges & text clarity
                    renderEffect = null
                    clip = false
                }
                .padding(vertical = 2.dp)
        ) {
            // 🔹 Crisp Vector Icon (slightly larger)
            Icon(
                imageVector = when (status) {
                    PayoutStatus.SUCCESS -> Icons.Filled.CheckCircle
                    PayoutStatus.FAILED -> Icons.Filled.Error
                    PayoutStatus.PENDING -> Icons.Filled.HourglassEmpty
                    PayoutStatus.IN_PROGRESS -> Icons.Filled.Autorenew
                    else -> Icons.Filled.Info
                },
                contentDescription = null,
                tint = when (status) {
                    PayoutStatus.SUCCESS -> Color(0xFF2ECC71)  // bright green
                    PayoutStatus.FAILED -> Color(0xFFE74C3C)   // red
                    PayoutStatus.PENDING,
                    PayoutStatus.INITIATED,
                    PayoutStatus.IN_PROGRESS -> Color(0xFFFFA500) // orange
                    else -> AppColors.infoAccent
                },
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.CenterVertically)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 🔹 Status Text
                Text(
                    text = status.displayText,
                    style = AppFont.ibmPlexSans(14, FontWeight.SemiBold),
                    color = when (status) {
                        PayoutStatus.SUCCESS -> Color(0xFF2ECC71)
                        PayoutStatus.FAILED -> Color(0xFFE74C3C)
                        PayoutStatus.PENDING,
                        PayoutStatus.INITIATED,
                        PayoutStatus.IN_PROGRESS -> Color(0xFFFFA500)
                        else -> AppColors.secondaryText
                    }
                )

                // 🔹 Date Text
                payoutDate?.let {
                    Text(
                        text = CommonFunctions.dateToString(it.toDate()),
                        style = AppFont.ibmPlexSans(12, FontWeight.Normal),
                        color = AppColors.secondaryText
                    )
                }

                // 🔹 Reason (More Details)
                if (reason.isNotEmpty()) {
                    Text(
                        text = "More Details",
                        style = AppFont.ibmPlexSans(11, FontWeight.Medium),
                        color = Color(0xFF007AFF),
                        modifier = Modifier.clickable {
                            reasonData = ReasonSheetData(
                                title = if (status == PayoutStatus.SUCCESS)
                                    "Payout Success" else "Payout Issue",
                                message = reason
                            )
                        }
                    )
                }
            }

            // 🔹 Retry / Verify / Refresh Button
            if (canShowPayoutAction(status, paymentType, memberId, squadViewModel)) {
                Button(
                    onClick = retryAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71).copy(alpha = 0.15f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                ) {
                    Text(
                        text = when (status) {
                            PayoutStatus.FAILED -> "Retry"
                            PayoutStatus.PENDING,
                            PayoutStatus.INITIATED,
                            PayoutStatus.IN_PROGRESS -> "Verify"
                            else -> "Refresh"
                        },
                        style = AppFont.ibmPlexSans(11, FontWeight.Medium),
                        color = Color(0xFF2ECC71)
                    )
                }
            }
        }

        // 🔹 Crisp Reason Sheet Popup
        reasonData?.let {
            ReasonSheet(it) { reasonData = null }
        }
    }
}

private fun canShowPayoutAction(
    status: PayoutStatus,
    paymentType: PaymentType,
    memberId: String,
    squadViewModel: SquadViewModel
): Boolean {
    return status != PayoutStatus.SUCCESS &&
            ((paymentType == PaymentType.PAYMENT_CREDIT &&
                    UserDefaultsManager.getSquadManagerLogged()) ||
                    (paymentType == PaymentType.PAYMENT_DEBIT &&
                            memberId == squadViewModel.currentMember.value?.id))
}

// --------------------------------------------------------------
// MARK: - Reason Sheet
// --------------------------------------------------------------
@Composable
fun ReasonSheet(data: ReasonSheetData, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text(data.title, style = AppFont.ibmPlexSans(18, FontWeight.SemiBold)) },
        text = {
            Text(
                text = if (data.message.isEmpty()) "No details available." else data.message,
                style = AppFont.ibmPlexSans(13, FontWeight.Normal),
                color = AppColors.secondaryText
            )
        }
    )
}

// --------------------------------------------------------------
// MARK: - Model
// --------------------------------------------------------------
data class ReasonSheetData(
    val title: String,
    val message: String
)
