package com.android.savingssquad.SquadSubscription

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.savingssquad.model.NotificationRecipientType
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.model.PushNotificationRequest
import com.android.savingssquad.model.ReminderRequest
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.LoaderManager
import com.android.savingssquad.singleton.NotificationService
import com.android.savingssquad.singleton.PaymentApproveStatus
import com.android.savingssquad.singleton.PaymentEntryType
import com.android.savingssquad.singleton.PaymentStatus
import com.android.savingssquad.singleton.PaymentSubType
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.PayoutStatus
import com.android.savingssquad.singleton.SquadActivityType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.SquadStringsEnglish
import com.android.savingssquad.singleton.SquadStringsEnglishDesc
import com.android.savingssquad.singleton.SquadStringsHindi
import com.android.savingssquad.singleton.SquadStringsHindiDesc
import com.android.savingssquad.singleton.SquadStringsTamil
import com.android.savingssquad.singleton.SquadStringsTamilDesc
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.view.SSAlert
import com.android.savingssquad.view.SSLoaderView
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.ToastManager
import com.android.savingssquad.viewmodel.ToastType
import com.google.firebase.Timestamp
import com.yourapp.utils.CommonFunctions
import com.yourapp.utils.IDGenerator
import java.util.Date

fun SubscriptionModel.Plan.label(): String {
    return when (this) {
        SubscriptionModel.Plan.FREE -> SquadStrings.freePlan
        SubscriptionModel.Plan.BASIC -> SquadStrings.basicPlan
        SubscriptionModel.Plan.BUSINESS -> SquadStrings.businessPlan
    }
}

@Composable
fun UpgradeSuccessScreen(
    plan: SubscriptionModel.Plan,
    viewModel: SubscriptionManager = SubscriptionManager.shared,
    squadViewModel: SquadViewModel,
    onDone: () -> Unit
) {
    val remoteConfig by viewModel.remoteConfig.collectAsState()
    val sub by viewModel.subscription.collectAsState()

    val activity = LocalContext.current as Activity
    val appContext = LocalContext.current.applicationContext

    val billingPeriod =
        sub?.billingPeriod ?: SubscriptionModel.BillingPeriod.MONTHLY

    val features = remoteConfig.features(plan)
    val maxMembers = remoteConfig.maxMembers(plan)
    val price = remoteConfig.priceText(plan, billingPeriod)

    // Success animation
    val successScale = remember { Animatable(0.65f) }
    val successAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        successAlpha.animateTo(1f)
        successScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.62f,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    )
    {
        // MARK: - Ambient Background Accents

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 30.dp, end = 10.dp)
                .size(180.dp)
                .blur(70.dp)
                .background(
                    AppColors.successAccent.copy(alpha = 0.13f),
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, bottom = 50.dp)
                .size(160.dp)
                .blur(70.dp)
                .background(
                    AppColors.primaryBrand.copy(alpha = 0.10f),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            // MARK: - Premium Success Badge

            Box(
                modifier = Modifier
                    .size(148.dp)
                    .scale(successScale.value),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(148.dp)
                        .background(
                            AppColors.successAccent.copy(alpha = 0.07f),
                            CircleShape
                        )
                )

                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .border(
                            width = 1.dp,
                            color = AppColors.successAccent.copy(alpha = 0.20f),
                            shape = CircleShape
                        )
                )

                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    AppColors.successAccent.copy(alpha = 0.25f),
                                    AppColors.successAccent.copy(alpha = 0.08f)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Upgrade successful",
                        tint = AppColors.successAccent,
                        modifier = Modifier
                            .size(58.dp)
                            .scale(successScale.value)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // MARK: - Success Message

            Text(
                text = "YOU'RE ALL SET",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp,
                color = AppColors.successAccent,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = SquadStrings.upgradeSuccessful,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.headerText,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = SquadStrings.youAreNowOnPlan(plan.label()),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.secondaryText,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(30.dp))

            // MARK: - Premium Membership Card

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(25.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = AppColors.border.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(25.dp)
                        )
                        .padding(22.dp)
                ) {
                    // Membership header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "YOUR MEMBERSHIP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = AppColors.secondaryText
                            )

                            Spacer(modifier = Modifier.height(7.dp))

                            Text(
                                text = plan.label(),
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.headerText
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(
                                    AppColors.successAccent.copy(alpha = 0.10f),
                                    RoundedCornerShape(17.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Premium membership",
                                tint = AppColors.successAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                AppColors.border.copy(alpha = 0.7f)
                            )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "PLAN BENEFITS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        color = AppColors.secondaryText
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    PremiumFeatureRow(
                        title = "Billing Period",
                        value = billingPeriod.localizedName,
                        enabled = true
                    )

                    Spacer(modifier = Modifier.height(17.dp))

                    PremiumFeatureRow(
                        title = SquadStrings.contribution,
                        value = if (features.contribution) {
                            "Included"
                        } else {
                            "Unavailable"
                        },
                        enabled = features.contribution
                    )

                    Spacer(modifier = Modifier.height(17.dp))

                    PremiumFeatureRow(
                        title = "Loan Access",
                        value = if (features.loan) {
                            "Included"
                        } else {
                            "Unavailable"
                        },
                        enabled = features.loan
                    )

                    Spacer(modifier = Modifier.height(17.dp))

                    PremiumFeatureRow(
                        title = SquadStrings.maxMembers(""),
                        value = maxMembers.toString(),
                        enabled = true
                    )

                    Spacer(modifier = Modifier.height(17.dp))

                    PremiumFeatureRow(
                        title = SquadStrings.price(""),
                        value = price,
                        enabled = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // MARK: - Premium Done Button

            Button(
                onClick = {

                    val subscription = SubscriptionManager.shared.subscription.value

                    val remoteConfig = SubscriptionManager.shared.remoteConfig.value

                    val amountInt = remoteConfig.price(

                        subscription!!.plan,

                        subscription.billingPeriod

                    ) + if (subscription.loanAddon) {

                        remoteConfig.addon_loan_price

                    } else {

                        0

                    }

                    AlertManager.shared.showAlert(

                        title = SquadStrings.confirmDeduction,

                        message = SquadStrings.doYouWantToDeduct(

                            amountInt.toString()

                        ),

                        primaryButtonTitle = SquadStrings.ok,

                        primaryAction = {

                            val squad = squadViewModel.squad.value

                            if (squad == null) {

                                ToastManager.show(

                                    title = "Error",

                                    message = "Squad details not found.",

                                    type = ToastType.ERROR

                                )

                                return@showAlert

                            }

                            LoaderManager.shared.showLoader()

                            val paymentId = IDGenerator.generatePaymentID(

                                squadId = squad.squadID

                            )

                            val newPayment = PaymentsDetails(

                                id = paymentId,

                                paymentUpdatedDate = Timestamp(Date()),

                                memberId = "",

                                memberName = SquadStringsEnglish.squadManager,

                                memberNameHindi = SquadStringsHindi.squadManager,

                                memberNameTamil = SquadStringsTamil.squadManager,

                                memberNameEnglish = SquadStringsEnglish.squadManager,

                                paymentPhone = "",

                                paymentEmail = "",

                                userType = SquadUserType.SQUAD_MANAGER,

                                amount = amountInt,

                                intrestAmount = 0,

                                paymentEntryType = PaymentEntryType.MANUAL_ENTRY,

                                paymentType = PaymentType.PAYMENT_DEBIT,

                                paymentSubType = PaymentSubType.OTHERS_AMOUNT,

                                paymentStatus = PaymentStatus.SUCCESS,

                                payoutStatus = PayoutStatus.PAYOUT_SUCCESS,

                                paymentApproveStatus = PaymentApproveStatus.ACCEPTED,

                                description = SquadStringsEnglish.squadSubscriptionAmountDeducted,

                                squadId = squad.squadID,

                                order_id = paymentId,

                                contributionId = "",

                                loanId = "",

                                installmentId = "",

                                paymentSuccess = true,

                                payoutSuccess = true,

                                transferReferenceId = "",

                                descriptionTamil = SquadStringsTamil.squadSubscriptionAmountDeducted,

                                descriptionHindi = SquadStringsHindi.squadSubscriptionAmountDeducted

                            )

                            squadViewModel.savePayments(
                                activity = activity,
                                context = appContext,
                                squadID = squad.squadID,
                                payment = listOf(newPayment)
                            ) { success, error ->

                                if (!success) {

                                    LoaderManager.shared.hideLoader()

                                    ToastManager.show(

                                        title = "Error",

                                        message = error ?: "Unable to record the payment.",

                                        type = ToastType.ERROR

                                    )

                                    return@savePayments

                                }

                                squadViewModel.createSquadActivity(

                                    activityType = SquadActivityType.AMOUNT_CREDIT,

                                    memberName = SquadStringsEnglish.squadManager,

                                    memberNameHindi = SquadStringsHindi.squadManager,

                                    memberNameTamil = SquadStringsTamil.squadManager,

                                    memberNameEnglish = SquadStringsEnglish.squadManager,

                                    memberId = newPayment.memberId,

                                    amount = amountInt,

                                    description = SquadStringsEnglishDesc.recordedPayment(

                                        amount = amountInt.currencyFormattedWithCommas(),

                                        note = SquadStringsEnglish.squadSubscriptionAmountDeducted

                                    ),

                                    descriptionTamil = SquadStringsTamilDesc.recordedPayment(

                                        amount = amountInt.currencyFormattedWithCommas(),

                                        note = SquadStringsTamil.squadSubscriptionAmountDeducted

                                    ),

                                    descriptionHindi = SquadStringsHindiDesc.recordedPayment(

                                        amount = amountInt.currencyFormattedWithCommas(),

                                        note = SquadStringsHindi.squadSubscriptionAmountDeducted

                                    )

                                ) { _, _ ->

                                    LoaderManager.shared.hideLoader()

                                    onDone()

                                    ToastManager.show(

                                        title = SquadStrings.paymentRecorded,

                                        message = SquadStrings.paymentRecordedSuccessfully(

                                            amountInt.currencyFormattedWithCommas()

                                        ),

                                        type = ToastType.SUCCESS

                                    )

                                    NotificationService.shared.sendMemberReminder(
                                        request = PushNotificationRequest(
                                            squadId = squad.squadID,
                                            title = "New Payment Recorded",
                                            message = "A payment of ${
                                                amountInt.currencyFormattedWithCommas()
                                            } has been recorded by the squad manager. Note: ${
                                                SquadStringsEnglish.squadSubscriptionAmountDeducted
                                            }",
                                            data = mapOf(
                                                "navigate" to "PAYMENT",
                                                "notificationType" to "PAYMENT"
                                            ),
                                            recipientType = NotificationRecipientType.ALL
                                        ),
                                        onSuccess = { response ->
                                            println("✅ Notification response: ${response.success ?: false}")
                                        },
                                        onError = { error ->
                                            println("❌ Notification failed: $error")
                                        }
                                    )

                                }

                            }

                        },

                        secondaryButtonTitle = SquadStrings.no,

                        secondaryAction = {

                            onDone()

                        }

                    )

                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primaryBrand
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp
                )
            )
            {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = SquadStrings.done,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        SSAlert()
        SSLoaderView()
    }
}

// MARK: - Premium Feature Row

@Composable
private fun PremiumFeatureRow(
    title: String,
    value: String,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (enabled) {
                Icons.Default.CheckCircleOutline
            } else {
                Icons.Default.Close
            },
            contentDescription = null,
            tint = if (enabled) {
                AppColors.successAccent
            } else {
                AppColors.secondaryText
            },
            modifier = Modifier.size(19.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.headerText,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) {
                AppColors.successAccent
            } else {
                AppColors.secondaryText
            },
            textAlign = TextAlign.End
        )
    }
}