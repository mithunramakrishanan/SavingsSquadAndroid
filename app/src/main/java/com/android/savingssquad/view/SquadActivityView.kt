package com.android.savingssquad.view

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun SquadActivityView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared,
) {
    val dismiss = { navController.popBackStack() }

    var selectedUser by remember { mutableStateOf("All") }

    val currentMember by squadViewModel.currentMember.collectAsStateWithLifecycle()

    val userList = remember {
        listOf("All") + squadViewModel.squadMembers.value.map { it.name }.distinct()
    }

    val screenType =
        if (UserDefaultsManager.getSquadManagerLogged())
            SquadUserType.SQUAD_MANAGER
        else
            SquadUserType.SQUAD_MEMBER

    val squadActivities by squadViewModel.squadActivities.collectAsStateWithLifecycle()

    val filteredActivities = remember(selectedUser, squadActivities) {
        if (selectedUser == "All") squadActivities
        else squadActivities.filter { it.userName == selectedUser }
    }

    LaunchedEffect(Unit) {
        fetchMoreActivities(squadViewModel)

        selectedUser = if (screenType == SquadUserType.SQUAD_MEMBER) {
//            squadViewModel.currentMember.value?.name ?: "All"
            "All"
        } else {
            "All"
        }
    }


    AppBackgroundGradient()

    Column(modifier = Modifier.fillMaxSize()) {

        // NAV BAR
        SSNavigationBar("Squad Activities", navController)

        Spacer(modifier = Modifier.height(16.dp))

        // FILTER SECTION
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (screenType != SquadUserType.SQUAD_MEMBER) {
                DropdownMenuPicker(
                    label = "",
                    selected = selectedUser,
                    items = userList,          // ✅ CHANGED from items = userList
                    modifier = Modifier.weight(1f)
                ) { selectedUser = it }
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            items(
                items = filteredActivities,
                key = { it.id!! }
            ) { activity ->

                ActivityCardComposable(activity = activity)

                // 🔥 Pagination trigger logic
                val lastId = squadViewModel.squadActivities.value.lastOrNull()?.id
                if (activity.id == lastId) {
                    fetchMoreActivities(squadViewModel)
                }
            }
        }
    }
}

private fun fetchMoreActivities(squadViewModel: SquadViewModel) {
    val squadID = squadViewModel.squad.value?.squadID ?: return
    squadViewModel.fetchSquadActivities(true, squadID = squadID)
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