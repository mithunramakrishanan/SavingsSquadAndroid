package com.android.savingssquad.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Text

// ✅ Firebase Authentication
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

// ✅ Android + Kotlin
import android.app.Activity
import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import com.android.savingssquad.R
import com.android.savingssquad.model.PaymentsDetails
import java.util.concurrent.TimeUnit


import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.PaymentSubType
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.viewmodel.AppDestination
import com.android.savingssquad.viewmodel.FirestoreManager
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.viewmodel.SquadViewModel
import com.google.firebase.auth.PhoneAuthOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
@Composable
fun PaymentConfirmationView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager
) {

    var payment by remember { mutableStateOf<PaymentsDetails?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val activity = LocalContext.current as Activity
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        payment = UserDefaultsManager.getPendingPayment()
    }

    if (payment == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No pending payment found")
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ✅ Background (iOS match)
        AppBackgroundGradient()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(20.dp))

            // ---------------- HERO ----------------
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Icon(
                    imageVector = Icons.Default.CurrencyRupee,
                    contentDescription = null,
                    modifier = Modifier.size(90.dp),
                    tint = AppColors.successAccent
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = paymentTitle(payment!!.paymentSubType),
                    style = AppFont.ibmPlexSans(22, FontWeight.Bold),
                    color = AppColors.headerText
                )

                Text(
                    text = directionText(payment!!.paymentSubType, payment!!.paymentType),
                    style = AppFont.ibmPlexSans(14, FontWeight.Medium),
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ---------------- GLASS CARD ----------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.4f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Complete your payment in UPI app",
                    style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                    color = AppColors.headerText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "₹${payment!!.amount}",
                    style = AppFont.ibmPlexSans(32, FontWeight.Bold),
                    color = AppColors.successAccent
                )

                Spacer(modifier = Modifier.height(14.dp))
                Divider()
                Spacer(modifier = Modifier.height(14.dp))

                InfoRow("Member", payment!!.memberName)
                InfoRow("Reference", payment!!.transferReferenceId)
                InfoRow("Type", paymentTitle(payment!!.paymentSubType))

                Spacer(modifier = Modifier.height(12.dp))

                // ✅ iOS-like 2-line behavior
                Text(
                    text = infoMessage(payment!!.paymentSubType),
                    style = AppFont.ibmPlexSans(13, FontWeight.Normal),
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ---------------- BUTTON ----------------
                SSButton(
                    isButtonLoading = isLoading,
                    title = "I Completed Payment"
                ) {

                    isLoading = true
                    loaderManager.showLoader()

                    squadViewModel.savePayments(
                        activity = activity,
                        context = context,
                        squadID = squadViewModel.squad.value?.squadID ?: "",
                        payment = listOf(payment!!)
                    ) { success, error ->

                        isLoading = false
                        loaderManager.hideLoader()

                        if (success) {
                            UserDefaultsManager.clearPendingPayment()

                            AlertManager.shared.showAlert(
                                title = SquadStrings.appName,
                                message = "Payment updated successfully",
                                primaryButtonTitle = "OK",
                                primaryAction = {
                                    navController.popBackStack()
                                }
                            )
                        } else {
                            AlertManager.shared.showAlert(
                                title = SquadStrings.appName,
                                message = error ?: "Something went wrong"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ---------------- CANCEL ----------------
                Text(
                    text = "Payment Not Made",
                    style = AppFont.ibmPlexSans(15, FontWeight.SemiBold),
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            UserDefaultsManager.clearPendingPayment()
                            navController.popBackStack()
                        }
                        .padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        SSLoaderView()
        SSAlert()
    }
}

@Composable
fun InfoRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = AppFont.ibmPlexSans(13, FontWeight.Medium),
            color = Color.Gray
        )

        Text(
            text = value,
            style = AppFont.ibmPlexSans(13, FontWeight.SemiBold),
            color = AppColors.headerText
        )
    }
}

fun paymentTitle(type: PaymentSubType): String {
    return when (type) {
        PaymentSubType.CONTRIBUTION_AMOUNT -> "Contribution"
        PaymentSubType.EMI_AMOUNT -> "EMI Amount"
        PaymentSubType.LOAN_AMOUNT -> "Loan Disbursement"
        PaymentSubType.OTHERS_AMOUNT -> "Payment"
        PaymentSubType.INTEREST_AMOUNT -> ""
    }
}

fun directionText(type: PaymentSubType, paymentType: PaymentType): String {
    return when (type) {

        PaymentSubType.CONTRIBUTION_AMOUNT,
        PaymentSubType.EMI_AMOUNT -> {
            "Member → Manager"
        }

        PaymentSubType.LOAN_AMOUNT -> {
            "Manager → Member"
        }

        PaymentSubType.OTHERS_AMOUNT -> {
            if (paymentType == PaymentType.PAYMENT_CREDIT) {
                "Credit to Squad"
            } else {
                "Debit from Squad"
            }
        }

        PaymentSubType.INTEREST_AMOUNT -> {
            "Transaction"
        }
    }
}

fun infoMessage(type: PaymentSubType): String {
    return when (type) {

        PaymentSubType.CONTRIBUTION_AMOUNT -> {
            "Contribution will be verified after payment confirmation."
        }

        PaymentSubType.EMI_AMOUNT -> {
            "EMI will be automatically reconciled once payment is completed."
        }

        PaymentSubType.LOAN_AMOUNT -> {
            "Loan disbursement will be confirmed after successful transfer."
        }

        PaymentSubType.OTHERS_AMOUNT -> {
            "This transaction will be verified before final approval."
        }

        PaymentSubType.INTEREST_AMOUNT -> {
            ""
        }
    }
}