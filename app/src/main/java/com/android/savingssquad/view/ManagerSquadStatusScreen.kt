package com.android.savingssquad.view

import androidx.compose.runtime.Composable
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.runtime.getValue

import com.android.savingssquad.model.Login
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.savingssquad.singleton.AlertType
import com.android.savingssquad.singleton.RecordStatus
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.ToastManager
import com.android.savingssquad.viewmodel.ToastType

@Composable
fun ManagerSquadStatusScreen(
    navController: NavController,
    squadViewModel: SquadViewModel,
) {

    var searchText by remember { mutableStateOf("") }
    var hasLoaded by remember { mutableStateOf(false) }
    val logins by squadViewModel.managerLogins.collectAsStateWithLifecycle()

    val filteredLogins = remember(logins, searchText) {
        if (searchText.isEmpty()) {
            logins
        } else {
            logins.filter {
                it. squadName.contains(searchText, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLoaded) {
            hasLoaded = true
            squadViewModel.fetchManagerLogins(true, squadViewModel.squad.value?.phoneNumber ?: ""){ _, _ ->}
        }
    }

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
                .background(Color(0xFFE6EFEB))
        )
        {
            SSNavigationBar("My Squads", navController)


            SSSearchField(
                placeHolder = "Search your squads...",
                searchText = searchText,
                onTextChange = { searchText = it }
            )

            if (filteredLogins.isEmpty()) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.Gray
                        )

                        Text("You are not managing any squads.")
                    }
                }

            } else {

                LazyColumn(
                    contentPadding = PaddingValues(12.dp)
                ) {

                    items(filteredLogins, key = { it.id!! }) { login ->

                        LoginRow(
                            login = login,
                            onSelectStatus = { newStatus ->

                                handleStatusSelection(
                                    login,
                                    newStatus,
                                    squadViewModel
                                )
                            }
                        )
                    }
                }
            }
        }

    }


}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun LoginSearchFieldViewPreview() {

    val login = Login()
    login.squadName = "Mithun"

    LoginRow(login) { }
}



@Composable
fun LoginRow(
    login: Login,
    onSelectStatus: (RecordStatus) -> Unit
) {

    var displayedStatus by remember { mutableStateOf(login.recordStatus) }

    Card(

        modifier = Modifier

            .fillMaxWidth()

            .padding(vertical = 6.dp),

        shape = RoundedCornerShape(18.dp),

        colors = CardDefaults.cardColors(

            containerColor = Color.White

        ),

        elevation = CardDefaults.cardElevation(

            defaultElevation = 4.dp

        )

    ) {

        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF1A9988).copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = login.squadName.first().uppercase(),
                    color = Color(0xFF1A9988),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = login.squadName,
                    fontWeight = FontWeight.SemiBold
                )
            }

            SSStatusMenuButton(
                current = displayedStatus,
                onSelect = {
                    displayedStatus = it
                    onSelectStatus(it)
                }
            )
        }
    }
}

fun fetchLogins(showLoader: Boolean,squadViewModel: SquadViewModel, phoneNumber : String) {

    squadViewModel.fetchManagerLogins(
        showLoader = showLoader,
        phoneNumber = phoneNumber
    ) { success, error ->

        if (!success && error != null) {
            ToastManager.show(
                message = error,
                type = ToastType.ERROR
            )
        }
    }
}

fun applyStatus(
    squadViewModel: SquadViewModel,
    newStatus: RecordStatus,
    login: Login
) {

    squadViewModel.updateMemberLoginStatusForSquad(
        showLoader = true,
        phoneNumber = squadViewModel.squad.value?.phoneNumber ?: "",
        squadID = login.squadID,
        status = newStatus.value
    ) { success, error ->

        if (success) {

            ToastManager.show(
                message = "${login.squadName} is now ${newStatus.displayName()}",
                type = if (newStatus == RecordStatus.INACTIVE) {
                    ToastType.ERROR
                } else {
                    ToastType.SUCCESS
                }
            )

             fetchLogins(showLoader = false,squadViewModel,squadViewModel.squad.value?.phoneNumber ?: "")

        } else {

            ToastManager.show(
                message = error ?: "Failed to update status",
                type = ToastType.ERROR
            )
        }
    }
}

fun handleStatusSelection(
    login: Login,
    newStatus: RecordStatus,
    squadViewModel: SquadViewModel
)
{
    if (newStatus == login.recordStatus) return

    if (newStatus == RecordStatus.INACTIVE) {
        // show dialog (Compose AlertDialog or custom bottom sheet)
        AlertManager.shared.showAlert(
            title = "Deactivate Squad?",
            message = "If you deactivate ${login.squadName}, all members will lose access and won’t be able to use this squad until it is reactivated.",
            type = AlertType.ERROR,
            primaryButtonTitle = "Deactivate",
            primaryAction = {

                applyStatus(squadViewModel,newStatus,login)

            },
            secondaryButtonTitle = "Cancel",
            secondaryAction = {}
        )


    } else {

        applyStatus(squadViewModel,newStatus,login)
    }
}