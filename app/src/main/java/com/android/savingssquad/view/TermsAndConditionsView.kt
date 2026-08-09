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
import com.android.savingssquad.singleton.LoaderManager
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

    Box(
        modifier = Modifier

            .fillMaxSize()

            .windowInsetsPadding(WindowInsets.safeDrawing)
    )
    {
        AppBackgroundGradient()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.primaryBackground)
        )
        {

            // 🔹 Top Bar
            SSNavigationBar(SquadStrings.termsConditions, navController)

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


}

@Composable
private fun TermsContent(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    )
    {

        SectionTitle(SquadStrings.termsAboutSavingsSquad)

        SectionText(SquadStrings.termsAboutSavingsSquadDescription)

        SectionTitle(SquadStrings.termsEligibility)

        BulletSection(
            listOf(
                SquadStrings.termsEligibility18,
                SquadStrings.termsEligibilityAccurateInfo,
                SquadStrings.termsEligibilitySecurity,
                SquadStrings.termsEligibilityLaws
            )
        )

        SectionTitle(SquadStrings.termsSquadManagement)

        SectionText(SquadStrings.termsSquadManagementDescription)

        SectionTitle(SquadStrings.termsContributionsLoans)

        SectionText(SquadStrings.termsContributionsLoansDescription)

        SectionTitle(SquadStrings.termsPaymentsSubscriptions)

        SectionText(SquadStrings.termsPaymentsSubscriptionsDescription)

        SectionTitle(SquadStrings.termsDataStorage)

        SectionText(SquadStrings.termsDataStorageDescription)

        SectionTitle(SquadStrings.termsPrivacy)

        SectionText(SquadStrings.termsPrivacyDescription)

        SectionTitle(SquadStrings.termsProhibitedActivities)

        BulletSection(
            listOf(
                SquadStrings.termsFakeSquads,
                SquadStrings.termsFraudulentActivity,
                SquadStrings.termsHarassment,
                SquadStrings.termsUnauthorizedAccess,
                SquadStrings.termsIllegalUse,
                SquadStrings.termsMaliciousContent
            )
        )

        SectionTitle(SquadStrings.termsServiceAvailability)

        SectionText(SquadStrings.termsServiceAvailabilityDescription)

        SectionTitle(SquadStrings.termsLimitationOfLiability)

        SectionText(SquadStrings.termsLimitationOfLiabilityDescription)

        SectionTitle(SquadStrings.termsAccountSuspension)

        SectionText(SquadStrings.termsAccountSuspensionDescription)

        SectionTitle(SquadStrings.termsChangesToTerms)

        SectionText(SquadStrings.termsChangesToTermsDescription)

        SectionTitle(SquadStrings.termsContactUs)

        SectionText(SquadStrings.termsContactUsDescription)

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
                text = SquadStrings.acceptAndContinue,
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