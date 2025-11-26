package com.android.savingssquad.view

import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.savingssquad.model.*
import com.android.savingssquad.singleton.*
import com.yourapp.utils.CommonFunctions
import com.android.savingssquad.viewmodel.LoaderManager
import java.util.*
import androidx.navigation.NavController
import com.android.savingssquad.viewmodel.FirestoreManager
import com.android.savingssquad.viewmodel.SquadViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.android.savingssquad.viewmodel.AlertManager

import com.google.firebase.auth.*

import kotlinx.coroutines.launch

@Composable
fun TermsAndConditionsView(
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.primaryBackground)
    ) {

        // 🔹 Top Bar
        SSNavigationBar("Terms & Conditions", navController)

        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 Content Card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .appShadow(AppShadows.card, RoundedCornerShape(20.dp))
                .background(Color.White)

        ) {
            TermsContent(navController)
        }
    }
}

@Composable
private fun TermsContent(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        SectionTitle("1. About the App")
        SectionText(
            "Savings Squad is a group-saving platform for trusted communities. " +
                    "We help squads manage monthly contributions and internal loans.\n\n" +
                    "Savings Squad is NOT a bank or financial institution."
        )

        SectionTitle("2. Usage Rules")
        BulletSection(
            listOf(
                "You are at least 18 years old",
                "You will use the app only for genuine group savings",
                "You will provide accurate information",
                "You will not misuse or attempt to hack the app"
            )
        )

        SectionTitle("3. Squad & Members")
        BulletSection(
            listOf(
                "Each Squad has one Manager and multiple Members",
                "All contributions and loans are internal to the squad",
                "Any dispute must be resolved internally"
            )
        )

        SectionTitle("4. Payments")
        SectionText(
            "• Payments may use third-party gateways.\n" +
                    "• Savings Squad does NOT hold your money.\n" +
                    "• Refunds or failures are handled by the payment provider."
        )

        SectionTitle("5. Loans")
        SectionText(
            "Loans are issued by Squad Managers only. Savings Squad provides " +
                    "calculation and record-keeping but is not responsible for disputes."
        )

        SectionTitle("6. Data & Privacy")
        SectionText(
            "We collect minimal information such as your name, mobile number, " +
                    "and transaction details. We never sell your data."
        )

        SectionTitle("7. Restrictions")
        BulletSection(
            listOf(
                "Creating fake squads",
                "Illegal activities",
                "Harassment or threats",
                "Fraudulent transactions"
            )
        )

        SectionTitle("8. Liability")
        SectionText(
            "Savings Squad is not responsible for financial losses, payment issues, " +
                    "or disputes between members. Use the app at your own risk."
        )

        SectionTitle("9. Account Suspension")
        SectionText(
            "We may suspend accounts involved in fraud, abuse, or illegal activities."
        )

        SectionTitle("10. Updates")
        SectionText(
            "We may update these Terms at any time. Continued use means you accept the latest Terms."
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {

                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("termsAccepted", true)

                navController.popBackStack()
            },
            colors = ButtonDefaults.buttonColors(AppColors.primaryButton),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Accept & Continue",
                style = AppFont.ibmPlexSans(15, FontWeight.Bold),
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = AppFont.ibmPlexSans(15, FontWeight.Bold),
        color = AppColors.headerText
    )
}

@Composable
fun SectionText(text: String) {
    Text(
        text = text,
        style = AppFont.ibmPlexSans(14, FontWeight.Normal),
        color = AppColors.secondaryText
    )
}

@Composable
fun BulletSection(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach {
            Row {
                Text("• ", style = AppFont.ibmPlexSans(14, FontWeight.Bold))
                Text(
                    text = it,
                    style = AppFont.ibmPlexSans(14, FontWeight.Normal),
                    color = AppColors.secondaryText
                )
            }
        }
    }
}