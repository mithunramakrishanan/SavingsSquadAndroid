package com.android.savingssquad.view

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import java.util.Date
import com.android.savingssquad.model.SquadActivity
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.yourapp.utils.CommonFunctions
import com.android.savingssquad.singleton.SquadActivityType
import com.android.savingssquad.singleton.SquadStrings

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun SquadActivityView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared,
) {

    var selectedUser by remember { mutableStateOf("All") }
    var selectedMemberId by remember { mutableStateOf<String?>(null) }
    var hasLoaded by remember { mutableStateOf(false) }

    val squadActivities by squadViewModel
        .squadActivities
        .collectAsStateWithLifecycle()

    val squadMembers by squadViewModel
        .squadMembers
        .collectAsStateWithLifecycle()

    val currentMember by squadViewModel
        .currentMember
        .collectAsStateWithLifecycle()

    val screenType =
        if (UserDefaultsManager.getSquadManagerLogged())
            SquadUserType.SQUAD_MANAGER
        else
            SquadUserType.SQUAD_MEMBER

    val userList = remember(squadMembers) {
        listOf("All") + squadMembers
            .map { it.name }
            .distinct()
    }

    // Initial Load
    LaunchedEffect(Unit) {

        if (hasLoaded) return@LaunchedEffect

        hasLoaded = true

        if (screenType == SquadUserType.SQUAD_MEMBER) {

//            selectedMemberId = currentMember?.id
//            selectedUser = currentMember?.name ?: ""

            selectedUser = "All"
            selectedMemberId = null

        } else {

            selectedUser = "All"
            selectedMemberId = null
        }

        squadViewModel.resetActivitiesPagination()

        squadViewModel.fetchSquadActivities(
            squadID = squadViewModel.squad.value?.squadID ?: return@LaunchedEffect,
            memberId = selectedMemberId,
            showLoader = true
        ) { _, _ -> }
    }

    AppBackgroundGradient()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        SSNavigationBar(
            SquadStrings.squadActivities,
            navController
        )

        Spacer(modifier = Modifier.height(16.dp))

        // User Filter
        if (screenType != SquadUserType.SQUAD_MEMBER) {

            DropdownMenuPicker(
                label = "",
                selected = selectedUser,
                items = userList,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) { selected ->

                selectedUser = selected

                selectedMemberId =
                    if (selected == "All") {
                        null
                    } else {
                        squadMembers.firstOrNull {
                            it.name == selected
                        }?.id
                    }

                squadViewModel.resetActivitiesPagination()

                squadViewModel.fetchSquadActivities(
                    squadID = squadViewModel.squad.value?.squadID ?: return@DropdownMenuPicker,
                    memberId = selectedMemberId,
                    showLoader = true
                ) { _, _ -> }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (squadActivities.isEmpty()) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = Color.Gray.copy(alpha = 0.6f)
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "No activities found",
                        style = AppFont.ibmPlexSans(
                            15,
                            FontWeight.Medium
                        ),
                        color = AppColors.secondaryText
                    )
                }
            }

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(
                    12.dp
                )
            ) {

                items(
                    items = squadActivities,
                    key = { it.id ?: "" }
                ) { activity ->

                    ActivityCardComposable(
                        activity = activity
                    )

                    LaunchedEffect(activity.id) {

                        squadViewModel
                            .loadMoreActivitiesIfNeeded(
                                currentActivity = activity,
                                squadID = squadViewModel.squad.value?.squadID
                                    ?: return@LaunchedEffect,
                                memberId = selectedMemberId
                            )
                    }
                }

                if (
                    squadViewModel.isLoadingMoreActivities &&
                    squadActivities.isNotEmpty()
                ) {

                    item {
                        ShimmerLoader()
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityCardComposable(activity: SquadActivity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .appShadow(AppShadows.card)
            .background(
                color = AppColors.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {

        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = activity.userName,
                style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                color = AppColors.headerText
            )

            Text(
                text = CommonFunctions.dateToString(activity.date?.toDate() ?: Date()),
                style = AppFont.ibmPlexSans(12),
                color = AppColors.secondaryText
            )
        }

        Spacer(Modifier.height(6.dp))

        // Description
        Text(
            text = activity.description,
            style = AppFont.ibmPlexSans(14),
            color = AppColors.secondaryText
        )

        // Amount (credit/debit)
        if (activity.activityType == SquadActivityType.AMOUNT_CREDIT ||
            activity.activityType == SquadActivityType.AMOUNT_DEBIT
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = activity.amount.currencyFormattedWithCommas(),
                style = AppFont.ibmPlexSans(13, FontWeight.SemiBold),
                color = if (activity.activityType == SquadActivityType.AMOUNT_CREDIT)
                    AppColors.successAccent
                else
                    AppColors.errorAccent
            )
        }
    }
}