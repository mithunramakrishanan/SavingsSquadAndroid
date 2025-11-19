package com.android.savingssquad.view

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.R
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import kotlinx.coroutines.launch
import java.util.Date
import com.android.savingssquad.model.Squad
import com.android.savingssquad.model.Member
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.asTimestamp
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.viewmodel.AlertManager
import com.yourapp.utils.CommonFunctions
import java.util.Calendar
import androidx.compose.foundation.lazy.items
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.viewmodel.AppDestination


@Composable
fun MembersListView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared
) {
    var didFetchMembers by remember { mutableStateOf(false) }

    val members by squadViewModel.squadMembers.collectAsStateWithLifecycle()
    val showAddMemberPopup by squadViewModel.showAddMemberPopup.collectAsStateWithLifecycle()

    val screenType =
        if (UserDefaultsManager.getSquadManagerLogged())
            SquadUserType.SQUAD_MANAGER
        else
            SquadUserType.SQUAD_MEMBER

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        AppBackgroundGradient()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.Top
        ) {

            SSNavigationBar(
                title = SquadStrings.squadMembersTitle,
                navController = navController,
                showBackButton = true,
                rightButtonDrawable = if (screenType == SquadUserType.SQUAD_MANAGER)
                    R.drawable.add_member_icon
                else null
            ) {
                if (UserDefaultsManager.getSquadManagerLogged()) {
                    squadViewModel.setShowAddMemberPopup(true)
                }
            }

            if (members.isEmpty()) {
                EmptyMembersView()
            } else {
                MembersListContent(
                    members = members,
                    navController = navController,
                    squadViewModel = squadViewModel
                )
            }
        }

        if (showAddMemberPopup) {
            OverlayBackgroundView(
                showPopup = remember { mutableStateOf(true) },
                onDismiss = { squadViewModel.setShowAddMemberPopup(false) }
            ) {
                AddMemberPopup(
                    squadViewModel = squadViewModel,
                    onDismiss = { squadViewModel.setShowAddMemberPopup(false) }
                )
            }
        }

        SSAlert()
        SSLoaderView()

    }

    LaunchedEffect(Unit) {
        if (!didFetchMembers) {
            didFetchMembers = true
            squadViewModel.fetchMembers(showLoader = true) { success, _, error ->
                if (success) {
                    println("✅ Members fetched successfully!")
                } else {
                    println("❌ Fetch error: ${error ?: "Unknown error"}")
                }
            }
        }
    }
}

@Composable
fun EmptyMembersView() {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            tint = AppColors.secondaryText.copy(alpha = 0.6f),
            modifier = Modifier.size(80.dp)
        )

        Text(
            text = "No squad members yet!",
            style = AppFont.ibmPlexSans(16, FontWeight.Normal),
            color = AppColors.headerText
        )
    }
}

@Composable
fun MembersListContent(
    members: List<Member>,
    navController: NavController,
    squadViewModel: SquadViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {

        items(members, key = { it.phoneNumber }) { member ->

            if (UserDefaultsManager.getSquadManagerLogged()) {
                Row(
                    modifier = Modifier.clickable {
                        squadViewModel.setSelectedMember(member)
                        navController.navigate(AppDestination.OPEN_MEMBER_PROFILE.route)
                    }
                ) {
                    MembersListCellView(member)
                }
            } else {
                MembersListCellView(member)
            }
        }
    }
}

@Composable
fun MembersListCellView(member: Member) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .appShadow(AppShadows.card, RoundedCornerShape(15.dp))
            .background(AppColors.surface, RoundedCornerShape(15.dp))
            .border(0.5.dp, AppColors.border, RoundedCornerShape(15.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Avatar
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(AppColors.primaryButton.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = member.name.first().toString(),
                style = AppFont.ibmPlexSans(20, FontWeight.Bold),
                color = AppColors.primaryButton
            )
        }

        // Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = member.name,
                style = AppFont.ibmPlexSans(18, FontWeight.SemiBold),
                color = AppColors.headerText,
                maxLines = 1
            )
            Text(
                text = "${SquadStrings.phonePrefix} ${member.phoneNumber}",
                style = AppFont.ibmPlexSans(14, FontWeight.Normal),
                color = AppColors.secondaryText,
                maxLines = 1
            )
            Text(
                text = "${SquadStrings.contributionPrefix}${member.totalContributionPaid.currencyFormattedWithCommas()}",
                style = AppFont.ibmPlexSans(14, FontWeight.SemiBold),
                color = AppColors.successAccent,
                maxLines = 1
            )
        }
    }
}