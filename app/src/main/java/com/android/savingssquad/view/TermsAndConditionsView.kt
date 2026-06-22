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

@Composable
private fun TermsContent(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        SectionTitle("1. About Savings Squad")

        SectionText(
            "Savings Squad is a digital platform that helps trusted groups manage savings, contributions, loan records, member activities, and financial tracking.\n\n" +
                    "Savings Squad is not a bank, NBFC, lending institution, investment advisor, or financial service provider. The app only provides tools for recording and managing information entered by users."
        )

        SectionTitle("2. Eligibility")

        BulletSection(
            listOf(
                "You must be at least 18 years old",
                "You must provide accurate information",
                "You are responsible for maintaining account security",
                "You must comply with local laws and regulations"
            )
        )

        SectionTitle("3. Squad Management")

        SectionText(
            "Squads are created and managed by Squad Managers.\n\n" +
                    "Managers are solely responsible for:\n" +
                    "• Adding and removing members\n" +
                    "• Collecting contributions\n" +
                    "• Issuing loans\n" +
                    "• Maintaining records\n" +
                    "• Resolving disputes\n\n" +
                    "Savings Squad does not verify the identity, credibility, or financial standing of any member."
        )

        SectionTitle("4. Contributions & Loans")

        SectionText(
            "All contributions, loans, repayments, and financial arrangements are conducted directly between squad members.\n\n" +
                    "Savings Squad does not:\n" +
                    "• Hold funds\n" +
                    "• Facilitate lending decisions\n" +
                    "• Guarantee repayments\n" +
                    "• Recover outstanding loans\n" +
                    "• Participate in financial transactions between members\n\n" +
                    "All financial decisions are made entirely at the users' own discretion and risk."
        )

        SectionTitle("5. Payments & Subscriptions")

        SectionText(
            "Subscription purchases and payment processing may be handled by Apple, Google, Razorpay, or other third-party payment providers.\n\n" +
                    "Subscription fees provide access to app features and are generally non-refundable except where required by applicable law or platform policies.\n\n" +
                    "Savings Squad does not store card information or banking credentials used during payment processing."
        )

        SectionTitle("6. Data Storage")

        SectionText(
            "We store information necessary to operate the application, including:\n\n" +
                    "• Name\n" +
                    "• Phone number\n" +
                    "• Squad information\n" +
                    "• Member records\n" +
                    "• Contribution records\n" +
                    "• Loan records\n" +
                    "• Payment records\n" +
                    "• Device notification tokens\n\n" +
                    "This information is stored securely using cloud infrastructure providers."
        )

        SectionTitle("7. Privacy")

        SectionText(
            "We do not sell personal information to third parties.\n\n" +
                    "Your information may only be used for:\n" +
                    "• Providing app functionality\n" +
                    "• Account management\n" +
                    "• Customer support\n" +
                    "• Security and fraud prevention\n" +
                    "• Legal compliance\n\n" +
                    "Please review our Privacy Policy for complete details."
        )

        SectionTitle("8. Prohibited Activities")

        BulletSection(
            listOf(
                "Creating fake squads",
                "Fraudulent financial activity",
                "Harassment or abusive behavior",
                "Attempting unauthorized access",
                "Using the app for illegal purposes",
                "Uploading malicious content"
            )
        )

        SectionTitle("9. Service Availability")

        SectionText(
            "We strive to keep the service available at all times; however, we do not guarantee uninterrupted operation.\n\n" +
                    "Temporary downtime may occur due to:\n" +
                    "• Maintenance\n" +
                    "• Platform updates\n" +
                    "• Third-party outages\n" +
                    "• Internet connectivity issues"
        )

        SectionTitle("10. Limitation of Liability")

        SectionText(
            "Savings Squad shall not be responsible for:\n\n" +
                    "• Financial losses\n" +
                    "• Loan defaults\n" +
                    "• Missed contributions\n" +
                    "• Disputes between members\n" +
                    "• Payment gateway failures\n" +
                    "• Data loss caused by third-party services\n" +
                    "• Decisions made by Squad Managers or Members\n\n" +
                    "Users assume full responsibility for their use of the platform."
        )

        SectionTitle("11. Account Suspension")

        SectionText(
            "We reserve the right to suspend or terminate accounts that violate these Terms, engage in fraudulent activity, abuse the platform, or pose security risks."
        )

        SectionTitle("12. Changes to Terms")

        SectionText(
            "We may update these Terms and Conditions from time to time.\n\n" +
                    "Continued use of Savings Squad after updates become effective constitutes acceptance of the revised Terms."
        )

        SectionTitle("13. Contact Us")

        SectionText(
            "For questions, support requests, or legal inquiries, please contact the Savings Squad support team through the application."
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