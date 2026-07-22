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
import com.android.savingssquad.singleton.*
import com.android.savingssquad.viewmodel.SquadViewModel
import com.google.firebase.Timestamp
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.launch
import java.util.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

// MARK: - Currency helper

private val inrFormatter: NumberFormat by lazy {
    NumberFormat.getNumberInstance(Locale("en", "IN"))
}

private fun formatINR(amount: Int): String = "₹${inrFormatter.format(amount)}"


// MARK: - Payment History Row (GPay style)

@Composable
fun PaymentRow(
    payment: PaymentsDetails
) {

    var showDetails by remember { mutableStateOf(false) }

    val isCredit = payment.paymentType == PaymentType.PAYMENT_CREDIT
    val displayName = if (payment.paymentType == PaymentType.PAYMENT_DEBIT) "Squad Manager" else payment.memberName
    val totalAmount = if (payment.paymentSubType == PaymentSubType.EMI_AMOUNT)
        payment.amount + payment.intrestAmount else payment.amount
    val amountText = (if (isCredit) "+ " else "- ") + formatINR(totalAmount)

    val iconColor = if (payment.paymentType == PaymentType.PAYMENT_DEBIT)
        AppColors.secondaryAccent else AppColors.primaryBrand

    val iconVector: ImageVector = when (payment.paymentSubType) {
        PaymentSubType.CONTRIBUTION_AMOUNT -> Icons.Default.Groups
        PaymentSubType.EMI_AMOUNT -> Icons.Default.EventRepeat
        PaymentSubType.LOAN_AMOUNT -> Icons.Default.AccountBalanceWallet
        PaymentSubType.OTHERS_AMOUNT -> if (payment.paymentType == PaymentType.PAYMENT_DEBIT)
            Icons.AutoMirrored.Filled.CallMade else Icons.AutoMirrored.Filled.CallReceived
        PaymentSubType.INTEREST_AMOUNT -> Icons.Default.Percent
        else -> {Icons.Default.Percent}
    }

    val statusColor = statusColorFor(payment.paymentStatus)
    val statusIcon = statusIconFor(payment.paymentStatus)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .appShadow(AppShadows.card, RoundedCornerShape(18.dp))
            .background(color = AppColors.surface, shape = RoundedCornerShape(18.dp))
            .border(width = 1.dp, color = AppColors.border.copy(alpha = 0.6f), shape = RoundedCornerShape(18.dp))
            .clickable { showDetails = true }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // MARK: Icon

        Box(
            modifier = Modifier
                .size(42.dp)
                .background(color = iconColor.copy(alpha = 0.12f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(19.dp)
            )
        }

        // MARK: Name + reference

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {

            Text(
                text = displayName,
                style = AppFont.ibmPlexSans(size = 14, weight = FontWeight.Bold),
                color = AppColors.headerText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = payment.transferReferenceId,
                style = AppFont.ibmPlexSans(size = 11, weight = FontWeight.Normal),
                color = AppColors.secondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            payment.paymentUpdatedDate?.let { date ->
                Text(
                    text = CommonFunctions.dateToString(date.toDate()),
                    style = AppFont.ibmPlexSans(size = 10, weight = FontWeight.Medium),
                    color = AppColors.placeholderText
                )
            }
        }

        // MARK: Amount + status

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {

            Text(
                text = amountText,
                style = AppFont.ibmPlexSans(size = 15, weight = FontWeight.Bold),
                color = if (isCredit) AppColors.successAccent else AppColors.headerText,
                maxLines = 1
            )

            StatusBadge(
                text = payment.paymentStatus.displayText,
                icon = statusIcon,
                color = statusColor
            )
        }
    }

    if (showDetails) {
        PaymentDetailSheet(
            payment = payment,
            onDismiss = { showDetails = false }
        )
    }
}


@Composable
private fun StatusBadge(text: String, icon: ImageVector, color: Color) {
    Row(
        modifier = Modifier
            .background(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(10.dp))
        Text(
            text = text,
            style = AppFont.ibmPlexSans(size = 10, weight = FontWeight.Bold),
            color = color,
            maxLines = 1
        )
    }
}


// MARK: - Payment Detail Sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailSheet(
    payment: PaymentsDetails,
    onDismiss: () -> Unit
) {

    val clipboard = LocalClipboardManager.current
    var didCopy by remember { mutableStateOf(false) }

    LaunchedEffect(didCopy) {
        if (didCopy) {
            delay(1200.milliseconds)
            didCopy = false
        }
    }

    val isCredit = payment.paymentType == PaymentType.PAYMENT_CREDIT
    val totalAmount = if (payment.paymentSubType == PaymentSubType.EMI_AMOUNT)
        payment.amount + payment.intrestAmount else payment.amount
    val amountText = (if (isCredit) "+ " else "- ") + formatINR(totalAmount)

    val statusColor = statusColorFor(payment.paymentStatus)
    val statusIcon = statusIconFor(payment.paymentStatus)

    val typeTitle = when (payment.paymentSubType) {
        PaymentSubType.CONTRIBUTION_AMOUNT -> "Contribution"
        PaymentSubType.EMI_AMOUNT -> "EMI Amount"
        PaymentSubType.LOAN_AMOUNT -> "Loan Disbursement"
        PaymentSubType.OTHERS_AMOUNT -> "Payment"
        PaymentSubType.INTEREST_AMOUNT -> "Interest"
        PaymentSubType.RE_PAYMENT -> "Repayment"
        PaymentSubType.SETTLEMENT -> "Settlement"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // MARK: Hero

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(color = statusColor.copy(alpha = 0.12f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(28.dp))
                }

                Text(
                    text = amountText,
                    style = AppFont.ibmPlexSans(size = 28, weight = FontWeight.Bold),
                    color = AppColors.headerText
                )

                Text(
                    text = payment.paymentStatus.displayText,
                    style = AppFont.ibmPlexSans(size = 12, weight = FontWeight.SemiBold),
                    color = statusColor
                )
            }

            // MARK: Description

            if (payment.description.isNotEmpty()) {
                Text(
                    text = payment.description,
                    style = AppFont.ibmPlexSans(size = 13, weight = FontWeight.Normal),
                    color = AppColors.secondaryText,
                    textAlign = TextAlign.Center
                )
            }

            // MARK: Response message

            if (payment.paymentResponseMessage.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = statusColor.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = payment.paymentResponseMessage,
                        style = AppFont.ibmPlexSans(size = 12, weight = FontWeight.Medium),
                        color = AppColors.headerText
                    )
                }
            }

            // MARK: Details card

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = AppColors.background.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
                    .border(width = 1.dp, color = AppColors.border.copy(alpha = 0.6f), shape = RoundedCornerShape(16.dp))
                    .padding(vertical = 2.dp)
            ) {

                DetailRow(icon = Icons.Default.Person, title = "Member", value = payment.memberName)
                RowDivider()

                DetailRow(icon = Icons.Default.Sell, title = "Type", value = typeTitle)
                RowDivider()

                DetailRow(
                    icon = Icons.Default.Tag,
                    title = "Payment ID",
                    value = payment.id ?: "—",
                    monospaced = true,
                    copyable = true,
                    didCopy = didCopy,
                    onCopy = {
                        clipboard.setText(AnnotatedString(payment.id ?: ""))
                        didCopy = true
                    }
                )

                if (payment.upiID.isNotEmpty()) {
                    RowDivider()
                    DetailRow(icon = Icons.Default.AlternateEmail, title = "UPI ID", value = payment.upiID, monospaced = true)
                }

                RowDivider()
                DetailRow(
                    icon = Icons.Default.SwapHoriz,
                    title = "Reference",
                    value = payment.transferReferenceId,
                    monospaced = true
                )

                payment.paymentUpdatedDate?.let { date ->
                    RowDivider()
                    DetailRow(
                        icon = Icons.Default.Schedule,
                        title = "Date",
                        value = CommonFunctions.dateToString(date.toDate())
                    )
                }
            }
        }
    }
}


@Composable
private fun DetailRow(
    icon: ImageVector,
    title: String,
    value: String,
    monospaced: Boolean = false,
    copyable: Boolean = false,
    didCopy: Boolean = false,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(color = AppColors.primaryBackground, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = AppColors.primaryBrand, modifier = Modifier.size(11.dp))
        }

        Text(
            text = title,
            style = AppFont.ibmPlexSans(size = 12, weight = FontWeight.Medium),
            color = AppColors.secondaryText
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = value,
            style = if (monospaced)
                AppFont.ibmPlexSans(size = 12, weight = FontWeight.SemiBold).copy(fontFamily = FontFamily.Monospace)
            else
                AppFont.ibmPlexSans(size = 12, weight = FontWeight.SemiBold),
            color = AppColors.headerText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (copyable && onCopy != null) {
            Icon(
                imageVector = if (didCopy) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = if (didCopy) AppColors.successAccent else AppColors.primaryBrand,
                modifier = Modifier
                    .size(15.dp)
                    .clickable { onCopy() }
            )
        }
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 42.dp)
            .height(1.dp)
            .background(AppColors.border.copy(alpha = 0.5f))
    )
}


// MARK: - Shared status helpers

private fun statusColorFor(status: PaymentStatus): Color = when (status) {
    PaymentStatus.SUCCESS -> AppColors.successAccent
    PaymentStatus.FAILED -> AppColors.errorAccent
    PaymentStatus.INVERIFICATION -> AppColors.warningAccent
    else -> AppColors.infoAccent
}

private fun statusIconFor(status: PaymentStatus): ImageVector = when (status) {
    PaymentStatus.SUCCESS -> Icons.Default.CheckCircle
    PaymentStatus.FAILED -> Icons.Default.Cancel
    PaymentStatus.INVERIFICATION -> Icons.Default.Schedule
    else -> Icons.Default.Info
}
