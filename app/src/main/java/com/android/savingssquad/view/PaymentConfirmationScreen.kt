package com.android.savingssquad.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text

// ✅ Firebase Authentication

// ✅ Android + Kotlin
import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material3.Icon
import androidx.navigation.NavController
import com.android.savingssquad.model.PaymentsDetails


import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.PaymentSubType
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.LoaderManager
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.ToastManager
import com.android.savingssquad.viewmodel.ToastType

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun PaymentConfirmationView(
    navController: NavController,
    squadViewModel: SquadViewModel,
) {
    var payment by remember { mutableStateOf<PaymentsDetails?>(null) }
    var isProcessing by rememberSaveable { mutableStateOf(false) }
    var didCopyReference by remember { mutableStateOf(false) }
    var didCopyUPI by remember { mutableStateOf(false) }
    var showSuccessScreen by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val context = LocalContext.current
    val activity = LocalContext.current as Activity

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    LaunchedEffect(didCopyReference) {

        if (didCopyReference) {

            delay(1400.milliseconds)

            didCopyReference = false

        }

    }

    LaunchedEffect(didCopyUPI) {

        if (didCopyUPI) {

            delay(1400.milliseconds)

            didCopyUPI = false

        }

    }

    LaunchedEffect(Unit) {
        payment = UserDefaultsManager.getPendingPayment()
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        )
        {

            //====================================================
            // Header
            //====================================================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Payment Confirmation",
                    style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                    color = AppColors.headerText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }

            //====================================================
            // Scrollable Content
            //====================================================

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                StatusHero(
                    payment = payment,
                    pulseScale = pulseScale,
                    pulseAlpha = pulseAlpha
                )

                ReferenceCard(
                    payment = payment,
                    didCopyReference = didCopyReference,
                    didCopyUPI = didCopyUPI,

                    onCopyReference = {
                        clipboardManager.setText(
                            AnnotatedString(payment?.id.orEmpty())
                        )
                        didCopyReference = true
                    },

                    onCopyUPI = {
                        clipboardManager.setText(
                            AnnotatedString(payment?.upiID.orEmpty())
                        )
                        didCopyUPI = true
                    }
                )

                StepTracker(payment = payment)

                DetailsCard(payment = payment)

                TrustLine(payment = payment)

                // Keeps content above bottom bar when scrolling
                Spacer(modifier = Modifier.height(24.dp))
            }

            //====================================================
            // Fixed Bottom Bar
            //====================================================

            Surface(

                modifier = Modifier.fillMaxWidth(),

                color = Color.Transparent,

                shadowElevation = 12.dp

            ) {

                BottomActionBar(
                    isProcessing = isProcessing,

                    onCompletePayment = {

                        isProcessing = true

                        squadViewModel.savePayments(
                            showUPIIntent = false,
                            activity = activity,
                            context = context,
                            squadID = squadViewModel.squad.value?.squadID.orEmpty(),
                            payment = listOf(payment!!)
                        ) { success, error ->

                            isProcessing = false
                            LoaderManager.shared.hideLoader()

                            if (success) {

                                UserDefaultsManager.clearPendingPayment()
                                showSuccessScreen = true

                            } else {

                                ToastManager.show(
                                    title = SquadStrings.appName,
                                    message = error ?: "Something went wrong",
                                    type = ToastType.ERROR
                                )
                            }
                        }
                    },

                    onPaymentNotMade = {

                        isProcessing = false
                        UserDefaultsManager.clearPendingPayment()
                        navController.popBackStack()
                    }
                )
            }
        }

        if (showSuccessScreen && payment != null) {
            PaymentSuccessView(
                payment = payment!!,
                onDone = {
                    showSuccessScreen = false
                    navController.popBackStack()
                }
            )
        }
    }
}

// small helper so the "Copied" state auto-resets, without needing LaunchedEffect
// imported at the top (kept local to avoid an extra import line clutter)
@Composable
private fun LaunchedEffectCopyReset(key: Boolean, onReset: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(key) {
        if (key) {
            delay(1400)
            onReset()
        }
    }
}

// =========================================================
//  STATUS HERO
// =========================================================

@Composable
private fun StatusHero(payment: PaymentsDetails?, pulseScale: Float, pulseAlpha: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {

        Box(
            modifier = Modifier.size(78.dp),
            contentAlignment = Alignment.Center
        ) {
            // pulsing outer ring — expands and fades, signals a live/pending state
            Box(
                modifier = Modifier
                    .size((60 * pulseScale).dp)
                    .clip(CircleShape)
                    .border(2.dp, AppColors.warningAccent.copy(alpha = 0.35f * pulseAlpha), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(AppColors.warningAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = AppColors.warningAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "AMOUNT TO PAY",
                style = AppFont.ibmPlexSans(10, FontWeight.SemiBold),
                color = AppColors.secondaryText.copy(alpha = 0.7f)
            )

            Text(
                text = payment?.transferReferenceId ?: "",
                style = AppFont.ibmPlexSans(10, FontWeight.SemiBold),
                color = AppColors.secondaryText.copy(alpha = 0.7f)
            )

            if (payment?.paymentSubType == PaymentSubType.EMI_AMOUNT) {

                Text(
                    text = "₹${payment.amount + payment.intrestAmount}",
                    style = AppFont.ibmPlexSans(36, FontWeight.Bold),
                    color = AppColors.headerText
                )
            }
            else {

                Text(
                    text = "₹${payment?.amount}",
                    style = AppFont.ibmPlexSans(36, FontWeight.Bold),
                    color = AppColors.headerText
                )
            }


        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = paymentTitle(payment),
                style = AppFont.ibmPlexSans(12, FontWeight.Medium),
                color = AppColors.secondaryText
            )

            Box(
                modifier = Modifier
                    .size(3.dp)
                    .background(AppColors.secondaryText.copy(alpha = 0.4f), CircleShape)
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = AppColors.primaryBrand,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = directionText(payment),
                    style = AppFont.ibmPlexSans(12, FontWeight.Medium),
                    color = AppColors.primaryBrand
                )
            }
        }
    }
}

// =========================================================
//  REFERENCE CARD
// =========================================================

@Composable
private fun ReferenceCard(
    payment: PaymentsDetails?,
    didCopyReference: Boolean,
    didCopyUPI: Boolean,
    onCopyReference: () -> Unit,
    onCopyUPI: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.background.copy(alpha = 0.5f))
            .border(
                1.dp,
                AppColors.border.copy(alpha = 0.6f),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        //========================
        // Reference ID
        //========================

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "REFERENCE ID",
                    style = AppFont.ibmPlexSans(
                        9,
                        FontWeight.SemiBold
                    ),
                    color = AppColors.secondaryText.copy(alpha = 0.6f)
                )

                Text(
                    text = payment?.id.orEmpty(),
                    style = AppFont.ibmPlexSans(
                        11,
                        FontWeight.Medium
                    ).copy(fontFamily = FontFamily.Monospace),
                    color = AppColors.headerText,
                    maxLines = 1
                )
            }

            CopyChip(
                copied = didCopyReference,
                onClick = onCopyReference
            )
        }

        HorizontalDivider(color = AppColors.border)

        //========================
        // UPI ID
        //========================

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "UPI ID",
                    style = AppFont.ibmPlexSans(
                        9,
                        FontWeight.SemiBold
                    ),
                    color = AppColors.secondaryText.copy(alpha = 0.6f)
                )

                Text(
                    text = payment?.upiID.orEmpty(),
                    style = AppFont.ibmPlexSans(
                        13,
                        FontWeight.Medium
                    ).copy(fontFamily = FontFamily.Monospace),
                    color = AppColors.primaryBrand,
                    maxLines = 1
                )
            }

            CopyChip(
                copied = didCopyUPI,
                onClick = onCopyUPI
            )
        }

        HorizontalDivider(color = AppColors.border)

        Row {

            Text(
                text = formattedTimestamp(),
                style = AppFont.ibmPlexSans(
                    10,
                    FontWeight.Medium
                ),
                color = AppColors.secondaryText
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CopyChip(
    copied: Boolean,
    onClick: () -> Unit
) {

    val chipColor =
        if (copied)
            AppColors.successAccent
        else
            AppColors.primaryBrand

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(chipColor.copy(alpha = 0.10f))
            .clickable(
                indication = null,
                interactionSource = remember {
                    MutableInteractionSource()
                }
            ) {
                onClick()
            }
            .padding(
                horizontal = 9.dp,
                vertical = 5.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        Icon(
            imageVector =
                if (copied)
                    Icons.Default.Check
                else
                    Icons.Default.ContentCopy,
            contentDescription = null,
            tint = chipColor,
            modifier = Modifier.size(12.dp)
        )

        Text(
            text =
                if (copied)
                    "Copied"
                else
                    "Copy",
            style = AppFont.ibmPlexSans(
                11,
                FontWeight.SemiBold
            ),
            color = chipColor
        )
    }
}

// =========================================================
//  STEP TRACKER
// =========================================================

@Composable
private fun StepTracker(payment: PaymentsDetails?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.background.copy(alpha = 0.5f))
            .border(1.dp, AppColors.border.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {

        Text(
            text = "HOW IT WORKS",
            style = AppFont.ibmPlexSans(10, FontWeight.SemiBold),
            color = AppColors.secondaryText.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        StepRow(
            number = "1",
            title = "Open your UPI app",
            subtitle = "Pay via GPay, PhonePe, Paytm or any UPI app",
            isDone = true,
            isLast = false
        )

        StepRow(
            number = "2",
            title = "Complete the payment",
            subtitle = "Send ₹${payment?.amount} using the reference above",
            isDone = true,
            isLast = false
        )

        StepRow(
            number = "3",
            title = "Confirm below",
            subtitle = "Tap \"I Completed Payment\" once done",
            isDone = false,
            isLast = true
        )
    }
}

@Composable
private fun StepRow(number: String, title: String, subtitle: String, isDone: Boolean, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth()) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isDone) AppColors.primaryBrand else AppColors.surface),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                } else {
                    Text(
                        text = number,
                        style = AppFont.ibmPlexSans(10, FontWeight.Bold),
                        color = AppColors.primaryBrand
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .defaultMinSize(minHeight = 22.dp)
                        .background(AppColors.border)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 10.dp)) {
            Text(
                text = title,
                style = AppFont.ibmPlexSans(13, FontWeight.SemiBold),
                color = AppColors.headerText
            )
            Text(
                text = subtitle,
                style = AppFont.ibmPlexSans(11, FontWeight.Normal),
                color = AppColors.secondaryText
            )
        }
    }
}

// =========================================================
//  DETAILS CARD
// =========================================================

@Composable
private fun DetailsCard(payment: PaymentsDetails?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.background.copy(alpha = 0.5f))
            .border(1.dp, AppColors.border.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(vertical = 2.dp)
    ) {
        InfoRow(icon = Icons.Default.Person, title = "Member", value = payment?.memberName ?: "")
        HorizontalDivider(
            color = AppColors.border.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 42.dp)
        )
        InfoRow(icon = Icons.Default.Sell, title = "Type", value = paymentTitle(payment))
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(AppColors.primaryBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.primaryBrand,
                modifier = Modifier.size(11.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = title,
            style = AppFont.ibmPlexSans(12, FontWeight.Medium),
            color = AppColors.secondaryText
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = value,
            style = AppFont.ibmPlexSans(12, FontWeight.SemiBold),
            color = AppColors.headerText,
            maxLines = 1
        )
    }
}

// =========================================================
//  TRUST LINE
// =========================================================

@Composable
private fun TrustLine(payment: PaymentsDetails?) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = AppColors.secondaryText,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = infoMessage(payment),
            style = AppFont.ibmPlexSans(11, FontWeight.Medium),
            color = AppColors.secondaryText
        )
    }
}

// =========================================================
//  STICKY BOTTOM ACTION BAR
// =========================================================

@Composable
fun BottomActionBar(
    isProcessing: Boolean,
    onCompletePayment: () -> Unit,
    onPaymentNotMade: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surface)
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 16.dp)
    ) {

        // ================= PRIMARY BUTTON =================
        Button(
            onClick = onCompletePayment,
            enabled = !isProcessing,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.primaryBrand,
                disabledContainerColor = AppColors.primaryBrand.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {

                if (isProcessing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (isProcessing) "Processing..." else "I Completed Payment",
                    style = AppFont.ibmPlexSans(15, FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ================= SECONDARY BUTTON =================
        TextButton(
            onClick = onPaymentNotMade,
            enabled = !isProcessing,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = "Payment Not Made",
                style = AppFont.ibmPlexSans(13, FontWeight.SemiBold),
                color = AppColors.errorAccent,
                modifier = Modifier.alpha(if (isProcessing) 0.5f else 1f)
            )
        }
    }
}

// =========================================================
//  Helpers — payment title / direction / info message / timestamp
// =========================================================

private fun paymentTitle(payment: PaymentsDetails?): String = when (payment?.paymentSubType) {
    PaymentSubType.CONTRIBUTION_AMOUNT -> "Contribution"
    PaymentSubType.EMI_AMOUNT -> "EMI Amount"
    PaymentSubType.LOAN_AMOUNT -> "Loan Disbursement"
    PaymentSubType.OTHERS_AMOUNT -> "Payment"
    PaymentSubType.INTEREST_AMOUNT -> ""
    else -> {""}
}

private fun directionText(payment: PaymentsDetails?): String = when (payment?.paymentSubType) {
    PaymentSubType.CONTRIBUTION_AMOUNT, PaymentSubType.EMI_AMOUNT -> "Member → Manager"
    PaymentSubType.LOAN_AMOUNT -> "Manager → Member"
    PaymentSubType.OTHERS_AMOUNT -> if (payment.paymentType == PaymentType.PAYMENT_CREDIT) "Credit to Squad" else "Debit from Squad"
    PaymentSubType.INTEREST_AMOUNT -> "Transaction"
    else -> {""}
}

private fun infoMessage(payment: PaymentsDetails?): String = when (payment?.paymentSubType) {
    PaymentSubType.CONTRIBUTION_AMOUNT -> "Contribution will be verified after payment confirmation."
    PaymentSubType.EMI_AMOUNT -> "EMI will be automatically reconciled once payment is completed."
    PaymentSubType.LOAN_AMOUNT -> "Loan disbursement will be confirmed after successful transfer."
    PaymentSubType.OTHERS_AMOUNT -> "This transaction will be verified before final approval."
    PaymentSubType.INTEREST_AMOUNT -> ""
    else -> {""}
}

// Timestamp the confirmation flow started. Swap for a real field on
// PaymentsDetails (e.g. payment.initiatedAt) if one exists.
private fun formattedTimestamp(): String =
    SimpleDateFormat("dd MMM, h:mm a", Locale.ENGLISH).format(Date())

// =========================================================
//  PREVIEW
// =========================================================

//@Preview(
//    name = "Payment Confirmation",
//    showBackground = true,
//    showSystemUi = true
//)
//@Composable
//fun PaymentConfirmationViewPreview() {
//
//    val payment = PaymentsDetails(
//        memberId = "MB-1232",
//        memberName = "Mithun",
//        paymentPhone = "9597143577",
//        paymentEmail = "mithun@gmail.com",
//        userType = SquadUserType.SQUAD_MEMBER,
//        amount = 1000,
//        intrestAmount = 0,
//        paymentEntryType = PaymentEntryType.AUTOMATIC_ENTRY,
//        paymentType = PaymentType.PAYMENT_CREDIT,
//        paymentSubType = PaymentSubType.CONTRIBUTION_AMOUNT,
//        description = "Contribution",
//        squadId = "SQM-EWEW-23232",
//        contributionId = "CONTRI-231",
//        loanId = "",
//        installmentId = "",
//        transferReferenceId = "JAN 2022"
//    )
//
//
//        PaymentConfirmationView(
//            payment = payment,
//
//            onCompletePayment = { onResult ->
//                // Simulate success for preview
//                onResult(true)
//            },
//
//            onPaymentNotMade = {
//                // Preview only
//            },
//
//            onClose = {
//                // Preview only
//            }
//        )
//
//}