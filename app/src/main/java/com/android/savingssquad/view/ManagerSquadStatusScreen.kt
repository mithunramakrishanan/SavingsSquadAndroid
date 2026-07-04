package com.android.savingssquad.view

import androidx.compose.runtime.Composable
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.pointer.pointerInput
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.ShadowStyle
import com.android.savingssquad.singleton.appShadow

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.Login
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.delay
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import java.util.Date
import java.util.concurrent.TimeUnit
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.android.savingssquad.R
import com.android.savingssquad.SquadSubscription.SubscriptionManager
import com.android.savingssquad.model.Member
import com.android.savingssquad.singleton.AlertType
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.RecordStatus
import com.android.savingssquad.singleton.color
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.singleton.displayText
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.ToastManager
import com.android.savingssquad.viewmodel.ToastType
import com.google.firebase.auth.PhoneAuthOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

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


            LoginSearchField(
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
fun LoginSearchField(
    searchText: String,
    onTextChange: (String) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .background(
                color = Color.White,
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFFE8ECEF),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = Color(0xFF8E99A8),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        TextField(
            value = searchText,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937)
            ),
            placeholder = {
                Text(
                    "Search your squads...",
                    color = Color(0xFF9CA3AF),
                    fontSize = 15.sp
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,

                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,

                cursorColor = Color(0xFF14B8A6)
            )
        )

        AnimatedVisibility(
            visible = searchText.isNotEmpty(),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {

            IconButton(
                onClick = { onTextChange("") }
            ) {

                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear Search",
                    tint = Color(0xFF8E99A8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
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

            StatusMenuButton(
                current = displayedStatus,
                onSelect = {
                    displayedStatus = it
                    onSelectStatus(it)
                }
            )
        }
    }
}

@Composable
fun StatusMenuButton(
    current: RecordStatus,
    onSelect: (RecordStatus) -> Unit
) {

    var expanded by remember { mutableStateOf(false) }

    Box {

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(current.tintColor().copy(alpha = 0.1f))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(current.tintColor(), CircleShape)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(current.displayName())

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {

            RecordStatus.toggleCases.forEach { status ->

                DropdownMenuItem(
                    text = {
                        Row {
                            if (status == current) {
                                Text("✔ ")
                            }
                            Text(status.displayName())
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(status)
                    }
                )
            }
        }
    }
}

fun handleStatusSelection(
    login: Login,
    newStatus: RecordStatus,
    squadViewModel: SquadViewModel
) {
    if (newStatus == login.recordStatus) return

    if (newStatus == RecordStatus.INACTIVE) {
        // show dialog (Compose AlertDialog or custom bottom sheet)
        AlertManager.shared.showAlert(
            title = "Deactivate Squad?",
            message = "If you deactivate ${login.squadName}, all members will lose access and won’t be able to use this squad until it is reactivated.",
            type = AlertType.ERROR,
            primaryButtonTitle = "Deactivate",
            primaryAction = {

                squadViewModel.updateMemberLoginStatusForSquad(
                    showLoader = true,
                    phoneNumber = squadViewModel.squad.value?.phoneNumber ?: "",
                    squadID = login.squadID,
                    status = newStatus.value
                )
                { success,error ->

                    if (success) {

                        ToastManager.show(
                            message = "${login.squadName} is now ${newStatus.displayName()}",
                            type = if (newStatus == RecordStatus.INACTIVE) {

                                ToastType.ERROR

                            } else {

                                ToastType.SUCCESS

                            }
                        )

                        squadViewModel.fetchManagerLogins(false,
                            squadViewModel.squad.value?.phoneNumber ?: ""
                        ){ _, _ ->}

                    } else {

                        ToastManager.show(
                            message = error ?: "Failed to update status",
                            type = ToastType.ERROR
                        )

                        // Refresh so UI snaps back to server state
                        squadViewModel.fetchManagerLogins(false,
                            squadViewModel.squad.value?.phoneNumber ?: ""
                        ){ _, _ ->}
                    }

                }

            },
            secondaryButtonTitle = "Cancel",
            secondaryAction = {}
        )


    } else {
        squadViewModel.updateMemberLoginStatusForSquad(
            showLoader = true,
            phoneNumber = squadViewModel.squad.value?.phoneNumber ?: "",
            squadID = login.squadID,
            status = newStatus.value
        )
        { success,error ->

            if (success) {

                ToastManager.show(
                    message = "${login.squadName} is now ${newStatus.displayName()}",
                    type = if (newStatus == RecordStatus.INACTIVE) {

                        ToastType.ERROR

                    } else {

                        ToastType.SUCCESS

                    }
                )

                squadViewModel.fetchManagerLogins(false,
                    squadViewModel.squad.value?.phoneNumber ?: ""
                ){ _, _ ->}

            } else {

                ToastManager.show(
                    message = error ?: "Failed to update status",
                    type = ToastType.ERROR
                )

                // Refresh so UI snaps back to server state
                squadViewModel.fetchManagerLogins(false,
                    squadViewModel.squad.value?.phoneNumber ?: ""
                ){ _, _ ->}
            }

        }
    }
}