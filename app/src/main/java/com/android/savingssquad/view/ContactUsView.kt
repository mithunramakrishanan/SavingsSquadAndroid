package com.android.savingssquad.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.android.savingssquad.R
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.SquadStrings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.viewmodel.ToastManager
import com.android.savingssquad.viewmodel.ToastType
import androidx.core.net.toUri
import androidx.navigation.compose.rememberNavController


@Composable
fun ContactUsView(
    navController: NavController
) {

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {

        AppBackgroundGradient()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            SSNavigationBar(
                title = "Contact Us",
                navController = navController
            )

            HorizontalDivider(color = AppColors.border)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height(20.dp))

                Image(
                    painter = painterResource(R.drawable.app_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(95.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .appShadow(
                            style = AppShadows.elevated,
                            shape = RoundedCornerShape(24.dp)
                        )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Savings Squad",
                    style = AppFont.ibmPlexSans(
                        28,
                        FontWeight.Bold
                    ),
                    color = AppColors.headerText
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Need help or have feedback?\nWe're always happy to hear from you.",
                    style = AppFont.ibmPlexSans(15),
                    color = AppColors.secondaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                ContactCard(
                    icon = R.drawable.instagram_icon,
                    title = "Instagram",
                    subtitle = SquadStrings.instagramName,
                    tint = Color(0xFFE4405F)
                ) {
                    openLink(context, SquadStrings.instagramLink)
                }

                Spacer(modifier = Modifier.height(18.dp))

                ContactCard(
                    icon = R.drawable.whatsapp_icon,
                    title = "WhatsApp",
                    subtitle = SquadStrings.whatsappName,
                    tint = Color(0xFF25D366)
                ) {
                    openLink(context, SquadStrings.whatsappLink)
                }

                Spacer(modifier = Modifier.height(18.dp))

                ContactCard(
                    icon = R.drawable.gmail_icon,
                    title = "Email",
                    subtitle = SquadStrings.gmailName,
                    tint = Color(0xFFEA4335)
                ) {
                    openLink(context, SquadStrings.gmailLink)
                }

                Spacer(modifier = Modifier.height(34.dp))

                Text(
                    text = SquadStrings.app_version,
                    style = AppFont.ibmPlexSans(
                        13,
                        FontWeight.Medium
                    ),
                    color = AppColors.secondaryText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Saving Smarter, Together.",
                    style = AppFont.ibmPlexSans(
                        13,
                        FontWeight.Medium
                    ),
                    color = AppColors.secondaryText
                )

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun ContactCard(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .appShadow(
                style = AppShadows.card,
                shape = RoundedCornerShape(22.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.surface
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(34.dp)
            )

            Spacer(modifier = Modifier.width(18.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    style = AppFont.ibmPlexSans(
                        size = 18,
                        weight = FontWeight.SemiBold
                    ),
                    color = AppColors.headerText
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = AppFont.ibmPlexSans(14),
                    color = AppColors.secondaryText
                )
            }

            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun openLink(
    context: Context,
    url: String
) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    } catch (e: Exception) {

        ToastManager.show(message = "Unable to open link" , type = ToastType.ERROR)
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun ContactUsViewPreview() {

    ContactUsView(
        navController = rememberNavController()
    )
}