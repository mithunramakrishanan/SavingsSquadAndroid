package com.android.savingssquad.view

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import kotlinx.coroutines.launch
import java.util.Date
import com.android.savingssquad.model.GroupFund
import com.android.savingssquad.model.GroupFundActivity
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.MemberLoan
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.GroupFundUserType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.asTimestamp
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.singleton.displayText
import com.android.savingssquad.viewmodel.AlertManager
import com.yourapp.utils.CommonFunctions
import java.util.Calendar
import androidx.compose.foundation.lazy.items
import com.android.savingssquad.singleton.GroupFundActivityType

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun GroupFundActivityView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared,
) {
    val dismiss = { navController.popBackStack() }

    var selectedUser by remember { mutableStateOf("All") }

    val userList = remember {
        listOf("All") + squadViewModel.groupFundMembers.value.map { it.name }.distinct()
    }

    val screenType =
        if (UserDefaultsManager.getGroupFundManagerLogged())
            GroupFundUserType.GROUP_FUND_MANAGER
        else
            GroupFundUserType.GROUP_FUND_MEMBER

    // 🔹 Filter logic
    val filteredActivities = remember(
        selectedUser,
        squadViewModel.groupFundActivities.value
    ) {
        val list = squadViewModel.groupFundActivities.value
        if (selectedUser == "All") list
        else list.filter { it.userName == selectedUser }
    }

    // 🔹 Appear logic
    LaunchedEffect(Unit) {
        fetchMoreActivities(squadViewModel)

        selectedUser = if (screenType == GroupFundUserType.GROUP_FUND_MEMBER) {
            squadViewModel.currentMember.value?.name ?: "All"
        } else {
            "All"
        }
    }


    AppBackgroundGradient()

    Column(modifier = Modifier.fillMaxSize()) {

        // NAV BAR
        SSNavigationBar("Group Fund Activities", navController)

        Spacer(modifier = Modifier.height(16.dp))

        // FILTER SECTION
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (screenType != GroupFundUserType.GROUP_FUND_MEMBER) {
                DropdownMenuPicker(
                    label = "Member",
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
                val lastId = squadViewModel.groupFundActivities.value.lastOrNull()?.id
                if (activity.id == lastId) {
                    fetchMoreActivities(squadViewModel)
                }
            }
        }
    }
}

private fun fetchMoreActivities(squadViewModel: SquadViewModel) {
    val groupFundID = squadViewModel.groupFund.value?.groupFundID ?: return
    squadViewModel.fetchGroupFundActivities(true, groupFundID = groupFundID)
}

@Composable
fun ActivityCardComposable(activity: GroupFundActivity) {
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
        if (activity.activityType == GroupFundActivityType.AMOUNT_CREDIT ||
            activity.activityType == GroupFundActivityType.AMOUNT_DEBIT
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = activity.amount.currencyFormattedWithCommas(),
                style = AppFont.ibmPlexSans(13, FontWeight.SemiBold),
                color = if (activity.activityType == GroupFundActivityType.AMOUNT_CREDIT)
                    AppColors.successAccent
                else
                    AppColors.errorAccent
            )
        }
    }
}