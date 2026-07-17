package com.android.savingssquad.view//
//  PaymentSuccessView.kt
//  SavingsSquad
//

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradient
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.savingssquad.model.PaymentsDetails
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material3.Surface
import androidx.compose.ui.input.pointer.pointerInput
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.ShadowStyle
import com.android.savingssquad.singleton.appShadow

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.Login
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.viewmodel.SquadViewModel
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.delay
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import java.util.Date
import java.util.concurrent.TimeUnit
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.android.savingssquad.R
import com.android.savingssquad.SquadSubscription.SubscriptionManager
import com.android.savingssquad.model.ForceCloseSummary
import com.android.savingssquad.model.InterestType
import com.android.savingssquad.model.Member
import com.android.savingssquad.model.MemberLoan
import com.android.savingssquad.model.forceCloseSummary
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.LoaderManager
import com.android.savingssquad.singleton.PaymentSubType
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.RecordStatus
import com.android.savingssquad.singleton.color
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.singleton.displayText
import com.android.savingssquad.viewmodel.AppDestination
import com.google.firebase.auth.PhoneAuthOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PaymentSuccessView(
    payment: PaymentsDetails,
    onDone: () -> Unit
) {
    var checkAppeared by remember { mutableStateOf(false) }
    var contentAppeared by remember { mutableStateOf(false) }
    var cardAppeared by remember { mutableStateOf(false) }
    var badgeAppeared by remember { mutableStateOf(false) }
    var glowAppeared by remember { mutableStateOf(false) }

    // Infinite expanding ring pulses
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Scale"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Alpha"
    )
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, delayMillis = 400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2Scale"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, delayMillis = 400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2Alpha"
    )

    LaunchedEffect(Unit) {
        glowAppeared = true
        delay(50)
        checkAppeared = true
        delay(200)
        contentAppeared = true
        delay(100)
        cardAppeared = true
        delay(150)
        badgeAppeared = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.surface)
    ) {

        // Ambient radial glow behind icon
        val glowAlpha by animateFloatAsState(
            targetValue = if (glowAppeared) 0.16f else 0f,
            animationSpec = tween(500),
            label = "glowAlpha"
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 60.dp)
                .size(340.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AppColors.successAccent.copy(alpha = glowAlpha),
                            AppColors.successAccent.copy(alpha = 0f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.weight(1f))

            // MARK: - Success Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(148.dp)
            ) {

                // Outer expanding rings
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(ring1Scale)
                        .border(1.5.dp, AppColors.successAccent.copy(alpha = ring1Alpha), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(ring2Scale)
                        .border(1.5.dp, AppColors.successAccent.copy(alpha = ring2Alpha), CircleShape)
                )

                val iconScale by animateFloatAsState(
                    targetValue = if (checkAppeared) 1f else 0.6f,
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMedium),
                    label = "iconScale"
                )
                val iconAlpha by animateFloatAsState(
                    targetValue = if (checkAppeared) 1f else 0f,
                    animationSpec = tween(300),
                    label = "iconAlpha"
                )

                // Frosted glass halo
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .scale(iconScale)
                        .alpha(iconAlpha)
                        .background(AppColors.surface.copy(alpha = 0.55f), CircleShape)
                        .border(1.dp, AppColors.successAccent.copy(alpha = 0.2f), CircleShape)
                )

                // Core gradient badge
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(iconScale)
                        .alpha(iconAlpha)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    AppColors.successAccent,
                                    AppColors.successAccent.copy(alpha = 0.78f)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(30.dp)
                        .scale(iconScale)
                        .alpha(iconAlpha)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // MARK: - Title Block
            val contentAlpha by animateFloatAsState(
                targetValue = if (contentAppeared) 1f else 0f,
                animationSpec = tween(450),
                label = "contentAlpha"
            )
            val contentOffset by animateFloatAsState(
                targetValue = if (contentAppeared) 0f else 12f,
                animationSpec = tween(450),
                label = "contentOffset"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(contentAlpha)
                    .offset(y = contentOffset.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(AppColors.successAccent, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PAYMENT SUBMITTED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        color = AppColors.secondaryText.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "₹${displayAmount(payment)}",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.headerText
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = paymentTitle(payment),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.secondaryText
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // MARK: - Verification Card
            val cardScale by animateFloatAsState(
                targetValue = if (cardAppeared) 1f else 0.94f,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
                label = "cardScale"
            )
            val cardAlpha by animateFloatAsState(
                targetValue = if (cardAppeared) 1f else 0f,
                animationSpec = tween(400),
                label = "cardAlpha"
            )
            val badgeScale by animateFloatAsState(
                targetValue = if (badgeAppeared) 1f else 0.7f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                label = "badgeScale"
            )
            val badgeAlpha by animateFloatAsState(
                targetValue = if (badgeAppeared) 1f else 0f,
                animationSpec = tween(300),
                label = "badgeAlpha"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(cardScale)
                    .alpha(cardAlpha)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.background.copy(alpha = 0.6f))
                    .border(1.dp, AppColors.border.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .scale(badgeScale)
                            .alpha(badgeAlpha)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        AppColors.primaryBrand.copy(alpha = 0.18f),
                                        AppColors.primaryBrand.copy(alpha = 0.08f)
                                    )
                                ),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = AppColors.primaryBrand,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Awaiting Verification",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.headerText
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            StatusPill()
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = verificationMessage(payment),
                            fontSize = 12.5.sp,
                            color = AppColors.secondaryText,
                            lineHeight = 17.sp
                        )
                    }
                }

                HorizontalDivider(color = AppColors.border.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        tint = AppColors.secondaryText.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = payment.transferReferenceId,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.secondaryText.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = formattedTimestamp(),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.secondaryText.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // MARK: - Bottom Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contentAlpha)
                    .offset(y = contentOffset.dp)
                    .padding(bottom = 18.dp, top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        AppColors.primaryBrand,
                                        AppColors.primaryBrand.copy(alpha = 0.85f)
                                    )
                                ),
                                shape = RoundedCornerShape(17.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Done",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = AppColors.secondaryText.copy(alpha = 0.7f),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Your transaction is securely recorded",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.secondaryText.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill() {
    Text(
        text = "PENDING",
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        color = AppColors.warningAccent,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AppColors.warningAccent.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

// =========================================================
//  DATA HELPERS
// =========================================================

private fun displayAmount(payment: PaymentsDetails): Int {
    return if (payment.paymentSubType == PaymentSubType.EMI_AMOUNT)
        payment.amount + payment.intrestAmount
    else
        payment.amount
}

private fun paymentTitle(payment: PaymentsDetails): String {
    return when (payment.paymentSubType) {
        PaymentSubType.CONTRIBUTION_AMOUNT -> "Contribution Payment"
        PaymentSubType.EMI_AMOUNT -> "EMI Payment"
        PaymentSubType.LOAN_AMOUNT -> "Loan Disbursement"
        PaymentSubType.OTHERS_AMOUNT -> "Payment"
        PaymentSubType.INTEREST_AMOUNT -> "Interest Payment"
    }
}

private fun verificationMessage(payment: PaymentsDetails): String {
    return if (payment.paymentType == PaymentType.PAYMENT_CREDIT) {
        "Your payment of ₹${displayAmount(payment)} has been recorded and will reflect once your manager verifies it. A notification has been sent for review."
    } else {
        "${payment.memberName} will verify this payment. You'll be notified as soon as it's confirmed."
    }
}

private fun formattedTimestamp(): String {
    val formatter = SimpleDateFormat("dd MMM, h:mm a", Locale.US)
    return formatter.format(Date())
}