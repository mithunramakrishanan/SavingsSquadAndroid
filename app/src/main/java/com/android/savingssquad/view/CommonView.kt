package com.android.savingssquad.view

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.ShadowStyle
import com.android.savingssquad.singleton.appShadow

import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.Login
import com.android.savingssquad.singleton.GroupFundUserType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.viewmodel.SquadViewModel
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.delay
import com.android.savingssquad.singleton.RootType
import com.android.savingssquad.viewmodel.LoaderManager
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import java.util.Date
import java.util.concurrent.TimeUnit
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.android.savingssquad.R
import com.android.savingssquad.singleton.AppNavigator
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.currencyFormattedWithCommas


// ---------- SSNavigationBar ----------
@Composable
fun SSNavigationBar(
    title: String,
    navController: NavController?, // pass rememberNavController() from caller
    showBackButton: Boolean = true,
    rightButtonIcon: ImageVector? = null,
    rightButtonAction: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showBackButton) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clickable {
                        navController?.popBackStack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = AppColors.headerText,
                    modifier = Modifier.size(25.dp)
                )
            }
        }

        Text(
            text = title,
            style = AppFont.ibmPlexSans(24, FontWeight.Bold),
            color = AppColors.headerText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        if (rightButtonIcon != null && rightButtonAction != null) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clickable { rightButtonAction() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = rightButtonIcon,
                    contentDescription = "Action",
                    tint = AppColors.headerText,
                    modifier = Modifier.size(25.dp)
                )
            }
        }
    }
}

// ---------- SSButton ----------
@Composable
fun SSButton(
    title: String,
    isButtonLoading: Boolean = false,
    isDisabled: Boolean = false,
    action: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = if (isDisabled) AppColors.disabledButton else AppColors.primaryButton,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .appShadow(if (isDisabled) ShadowStyle(Color.Transparent, 0.dp, 0.dp, 0.dp) else AppShadows.elevated, RoundedCornerShape(12.dp))
                .clickable(enabled = !isDisabled) { if (!isDisabled) action() },
            contentAlignment = Alignment.Center
        ) {
            if (isButtonLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = AppColors.primaryButtonText,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = title,
                    style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                    color = AppColors.primaryButtonText
                )
            }
        }
    }
}

// ---------- SSTextField ----------
@Composable
fun SSTextField(
    icon: ImageVector,
    placeholder: String,
    textState: MutableState<String>,
    keyboardType: KeyboardType = KeyboardType.Text,
    isSecure: Boolean = false,
    disabled: Boolean = false,
    showDropdown: Boolean = false,
    dropdownIcon: ImageVector = Icons.Default.ArrowDropDown,
    dropdownColor: Color = AppColors.secondaryText,
    isLoading: Boolean = false,
    error: String = "",
    onDropdownTap: (() -> Unit)? = null
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White) // ✅ white iOS-style background
                .border(
                    width = 1.5.dp,
                    color = when {
                        error.isNotEmpty() -> AppColors.errorAccent
                        isFocused -> AppColors.primaryButton
                        else -> AppColors.border
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                .shadow(
                    elevation = if (isFocused) 6.dp else 2.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = Color.Black.copy(alpha = if (isFocused) 0.1f else 0.05f),
                    spotColor = Color.Black.copy(alpha = if (isFocused) 0.1f else 0.05f)
                )
                .clickable(
                    enabled = !disabled && !showDropdown,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    // ✅ Tap anywhere to request focus
                    focusRequester.requestFocus()
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(modifier = Modifier.width(12.dp))

                // 🔹 Leading icon
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (disabled)
                        AppColors.secondaryText.copy(alpha = 0.6f)
                    else
                        AppColors.headerText,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 🔹 TextField and placeholder area
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = textState.value,
                        onValueChange = {
                            if (!disabled && !showDropdown) textState.value = it
                        },
                        singleLine = true,
                        textStyle = AppFont.ibmPlexSans(15, FontWeight.Normal).copy(
                            color = if (disabled)
                                AppColors.secondaryText
                            else
                                AppColors.headerText
                        ),
                        cursorBrush = SolidColor(AppColors.primaryButton),
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { isFocused = it.isFocused }
                    )

                    // 🔹 Centered placeholder (clickable to focus)
                    if (textState.value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = AppFont.ibmPlexSans(15, FontWeight.Normal),
                            color = AppColors.placeholderText,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 4.dp)
                        )
                    }
                }

                // 🔹 Password toggle
                if (isSecure && !disabled && !showDropdown) {
                    Icon(
                        imageVector = if (isPasswordVisible)
                            Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "toggle_password",
                        tint = AppColors.secondaryText,
                        modifier = Modifier
                            .size(22.dp)
                            .padding(end = 12.dp)
                            .clickable { isPasswordVisible = !isPasswordVisible }
                    )
                }

                // 🔹 Dropdown icon / loader
                if (showDropdown) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = dropdownColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 12.dp)
                        )
                    } else {
                        Icon(
                            imageVector = dropdownIcon,
                            contentDescription = "dropdown",
                            tint = dropdownColor,
                            modifier = Modifier
                                .size(22.dp)
                                .padding(end = 12.dp)
                                .clickable { onDropdownTap?.invoke() }
                        )
                    }
                }

                // 🔹 Error icon
                if (error.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "error",
                        tint = AppColors.errorAccent,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 12.dp)
                    )
                }
            }
        }

        // 🔹 Error message below
        if (error.isNotEmpty()) {
            Text(
                text = error,
                style = AppFont.ibmPlexSans(12, FontWeight.Normal),
                color = AppColors.errorAccent,
                modifier = Modifier.padding(start = 25.dp, top = 2.dp)
            )
        }
    }
}


// ---------- SSTextView ----------
@Composable
fun SSTextView(
    placeholder: String,
    textState: MutableState<String>,
    error: String = "",
    maxCharacters: Int? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.surface)
            .then(Modifier)
        ) {
            TextField(
                value = textState.value,
                onValueChange = {
                    if (maxCharacters != null && it.length > maxCharacters) {
                        textState.value = it.take(maxCharacters)
                    } else {
                        textState.value = it
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .padding(0.dp),
                textStyle = AppFont.ibmPlexSans(15, FontWeight.Normal),
                placeholder = { Text(text = placeholder, color = AppColors.placeholderText) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
        }

        if (error.isNotEmpty()) {
            Text(
                text = error,
                style = AppFont.ibmPlexSans(12, FontWeight.Normal),
                color = AppColors.errorAccent,
                modifier = Modifier.padding(start = 25.dp, top = 8.dp)
            )
        }

        if (maxCharacters != null) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${textState.value.length}/$maxCharacters",
                    style = AppFont.ibmPlexSans(12, FontWeight.Normal),
                    color = AppColors.secondaryText,
                    modifier = Modifier.padding(end = 25.dp, top = 6.dp)
                )
            }
        }
    }
}

// ---------- SSCancelButton ----------
@Composable
fun SSCancelButton(
    title: String,
    isButtonLoading: Boolean = false,
    isDisabled: Boolean = false,
    action: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(Modifier)
                .clickable(enabled = !isDisabled) { if (!isDisabled) action() }
                .appShadow(if (isDisabled) ShadowStyle(Color.Transparent, 0.dp, 0.dp, 0.dp) else AppShadows.card, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isButtonLoading) {
                CircularProgressIndicator(color = AppColors.headerText, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Text(
                    text = title,
                    style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                    color = AppColors.headerText
                )
            }
        }
    }
}

// ---------- AppBackgroundGradient ----------
@Composable
fun AppBackgroundGradient() {
    Box(modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(
            colors = listOf(AppColors.background, AppColors.background)
        )
    ))
}

// ---------- OverlayBackgroundView ----------
@Composable
fun OverlayBackgroundView() {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.5f))
    )
}

// ---------- ProgressCircleView ----------
@Composable
fun ProgressCircleView(
    completedMonths: Int,
    totalMonths: Int,
    monthlyContribution: String
) {
    val progress = remember(completedMonths, totalMonths) {
        if (totalMonths <= 0) 0f else (completedMonths.toFloat() / totalMonths.toFloat()).coerceIn(0f, 1f)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
    )

    Box(
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        androidx.compose.foundation.Canvas(modifier = Modifier.size(160.dp)) {
            drawCircle(
                color = AppColors.primaryButton.copy(alpha = 0.2f),
                style = Stroke(width = 14f, cap = StrokeCap.Round)
            )
        }

        // Progress arc
        androidx.compose.foundation.Canvas(modifier = Modifier.size(160.dp)) {
            val stroke = Stroke(width = 14f, cap = StrokeCap.Round)
            val sweep = animatedProgress * 360f
            drawArc(
                brush = Brush.linearGradient(listOf(AppColors.primaryButton, AppColors.successAccent)),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = stroke
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$completedMonths / $totalMonths",
                style = AppFont.ibmPlexSans(20, FontWeight.SemiBold),
                color = AppColors.headerText
            )
            Text(
                text = "Months Complete",
                style = AppFont.ibmPlexSans(14, FontWeight.Normal),
                color = AppColors.secondaryText
            )
            Text(
                text = monthlyContribution,
                style = AppFont.ibmPlexSans(18, FontWeight.Medium),
                color = AppColors.primaryButton,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ActionButton(
    title: String,
    caption: String? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AppColors.surface,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .appShadow(AppShadows.card, RoundedCornerShape(12.dp))
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                    color = AppColors.headerText,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Arrow",
                    tint = AppColors.primaryButton,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        caption?.let {
            Text(
                text = it,
                style = AppFont.ibmPlexSans(14, FontWeight.Normal),
                color = AppColors.secondaryText,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun SSLoaderView(
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var rotationAngle by remember { mutableStateOf(0f) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            while (true) {
                rotationAngle = (rotationAngle + 5f) % 360f
                delay(16L)
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .pointerInput(Unit) {} // block touch
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.background)
                    .appShadow(AppShadows.elevated, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(50.dp)) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    AppColors.loaderColor,
                                    AppColors.loaderColor.copy(alpha = 0.3f)
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = 4f, cap = StrokeCap.Round)
                        )
                    }

                    Text(
                        text = "₹",
                        style = AppFont.ibmPlexSans(20, FontWeight.Bold),
                        color = AppColors.loaderColor,
                        modifier = Modifier.graphicsLayer(rotationZ = -rotationAngle)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Processing...",
                    style = AppFont.ibmPlexSans(14, FontWeight.SemiBold),
                    color = AppColors.headerText
                )
            }
        }
    }
}

@Composable
fun RootView() {
    val rootState = AppNavigator.currentRoot

    when (rootState.value) {
        RootType.MANAGER -> ManagerTabView()
        RootType.MEMBER -> MemberTabView()
        RootType.LOGIN -> TODO()
    }
}

@Composable
fun LoginListPopup(
    showPopup: MutableState<Boolean>,
    selectedUser: MutableState<Login?>,
    users: List<Login>,
    squadViewModel: SquadViewModel
) {
    var selectedRole by remember { mutableStateOf(GroupFundUserType.GROUP_FUND_MANAGER.value) }

    val managers = remember(users) { users.filter { it.role == GroupFundUserType.GROUP_FUND_MANAGER } }
    val members = remember(users) { users.filter { it.role == GroupFundUserType.GROUP_FUND_MEMBER } }
    val filteredUsers = if (selectedRole == GroupFundUserType.GROUP_FUND_MANAGER.value) managers else members

    if (showPopup.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { showPopup.value = false },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(330.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.background)
                    .appShadow(AppShadows.elevated, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                // 🔹 Title
                Text(
                    text = SquadStrings.selectGroupFund,
                    style = AppFont.ibmPlexSans(20, FontWeight.Bold),
                    color = AppColors.headerText
                )

                // 🔹 Segmented Picker
                ModernSegmentedPickerView(
                    segments = listOf(
                        GroupFundUserType.GROUP_FUND_MANAGER.value,
                        GroupFundUserType.GROUP_FUND_MEMBER.value
                    ),
                    selectedSegment = selectedRole,
                    onSegmentSelected = { selectedRole = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 🔹 User List
                if (filteredUsers.isEmpty()) {
                    Text(
                        text = "No $selectedRole available",
                        style = AppFont.ibmPlexSans(14, FontWeight.Medium),
                        color = AppColors.secondaryText,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 220.dp)
                            .padding(vertical = 5.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredUsers) { user ->
                            UserSelectionCard(
                                user = user,
                                onSelect = {
                                    selectedUser.value = user
                                    showPopup.value = false
                                    UserDefaultsManager.saveLogin(user)
                                    UserDefaultsManager.saveIsLoggedIn(true)

                                    if (user.role == GroupFundUserType.GROUP_FUND_MANAGER) {
                                        AppNavigator.setRoot(RootType.MANAGER)
                                        UserDefaultsManager.saveGroupFundManagerLogged(true)
                                    } else {
                                        AppNavigator.setRoot(RootType.MEMBER)
                                        UserDefaultsManager.saveGroupFundManagerLogged(false)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 🔹 Close Button
                SSCancelButton(
                    title = "Cancel",
                    isButtonLoading = false,
                    isDisabled = false
                ) {
                    showPopup.value = false
                }
            }
        }
    }
}

@Composable
fun UserSelectionCard(
    user: Login,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppColors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .appShadow(AppShadows.card, RoundedCornerShape(14.dp))
            .padding(horizontal = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = user.groupFundName,
                style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                color = AppColors.headerText
            )

            val dateText = if (user.role == GroupFundUserType.GROUP_FUND_MANAGER)
                "Manager since ${CommonFunctions.dateToString(user.userCreatedDate?.toDate() ?: Date())}"
            else
                "Member since ${CommonFunctions.dateToString(user.userCreatedDate?.toDate() ?: Date())}"

            Text(
                text = dateText,
                style = AppFont.ibmPlexSans(13, FontWeight.Normal),
                color = AppColors.secondaryText
            )
        }
    }
}

@Composable
fun SectionView(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // 🔹 Header Section
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
        ) {
            Text(
                text = title,
                style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                color = AppColors.headerText
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AppFont.ibmPlexSans(13, FontWeight.Normal),
                    color = AppColors.secondaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 🔹 Dynamic Content
        content()
    }
}

@Composable
fun ModernSegmentedPickerView(
    segments: List<String>,
    selectedSegment: String,
    onSegmentSelected: (String) -> Unit
) {
    val transition = remember { androidx.compose.animation.core.MutableTransitionState(selectedSegment) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                color = AppColors.surface,
                shape = RoundedCornerShape(15.dp)
            )
            .appShadow(AppShadows.card)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        segments.forEach { segment ->
            val isSelected = segment == selectedSegment

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) AppColors.primaryButton.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .clickable {
                        onSegmentSelected(segment)
                    }
                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = segment,
                    style = AppFont.ibmPlexSans(14, FontWeight.Medium),
                    color = if (isSelected) AppColors.headerText else AppColors.secondaryText
                )
            }
        }
    }
}

@Composable
fun DuesCardView(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Default.CreditCard, // Material Icon
    iconColor: Color = AppColors.primaryButton,
    gradientColors: List<Color> = listOf(AppColors.surface, AppColors.background),
    showChevron: Boolean = true,
    onTap: (() -> Unit)? = null
) {
    val modifier = Modifier
        .fillMaxWidth()
        .appShadow(AppShadows.card)
        .clip(RoundedCornerShape(16.dp))
        .background(
            brush = Brush.linearGradient(
                colors = gradientColors,
                start = Offset(0f, 0f),
                end = Offset(1000f, 1000f)
            )
        )
        .clickable(enabled = onTap != null) { onTap?.invoke() }
        .padding(16.dp)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icon Circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        // Texts
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                color = AppColors.headerText
            )
            Text(
                text = subtitle,
                style = AppFont.ibmPlexSans(13, FontWeight.Normal),
                color = AppColors.secondaryText
            )
        }

        // Chevron
        if (showChevron) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AppColors.secondaryText.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 4.dp)
            )
        }
    }
}

@Composable
fun UserPicker(
    selectedUser: MutableState<String>,
    userList: List<String>
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Picker Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(AppColors.surface)
                .border(1.dp, AppColors.border, RoundedCornerShape(10.dp))
                .appShadow(AppShadows.card)
                .clickable {
                    isExpanded = !isExpanded
                }
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedUser.value,
                    style = AppFont.ibmPlexSans(14, FontWeight.Medium),
                    color = AppColors.headerText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppColors.secondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Expanded Dropdown
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
            exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.surface)
                    .appShadow(ShadowStyle(color = Color.Black.copy(alpha = 0.06f), radius = 4.dp, x = 0.dp, y = 2.dp))
            ) {
                userList.forEach { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedUser.value = user
                                isExpanded = false
                            }
                            .background(
                                if (selectedUser.value == user)
                                    AppColors.primaryButton.copy(alpha = 0.1f)
                                else
                                    AppColors.surface
                            )
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = user,
                            style = AppFont.ibmPlexSans(14, FontWeight.Normal),
                            color = AppColors.headerText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        if (selectedUser.value == user) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = AppColors.primaryButton,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddMemberPopup(
    squadViewModel: SquadViewModel,
    showPopup: MutableState<Boolean>
) {
    val context = LocalContext.current

    var memberName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var verificationID by remember { mutableStateOf("") }

    var sendOTPLoading by remember { mutableStateOf(false) }
    var verifyOTPLoading by remember { mutableStateOf(false) }
    var isOTPSent by remember { mutableStateOf(false) }
    var otpVerified by remember { mutableStateOf(false) }
    var otpProcessStarted by remember { mutableStateOf(false) }

    var memberNameError by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf("") }
    var sendOTPError by remember { mutableStateOf("") }
    var verifyOTPError by remember { mutableStateOf("") }

    if (!showPopup.value) return

    // ✅ Validation
    fun validateFields(): Boolean {
        memberNameError = if (memberName.trim().isEmpty()) "Name is required" else ""
        phoneError = if (Regex("^[0-9]{10}$").matches(phoneNumber))
            ""
        else
            "Enter a valid 10-digit phone number"
        return memberNameError.isEmpty() && phoneError.isEmpty()
    }

    // ✅ Handle Add Member or Send OTP
    fun handleAddMember() {
        if (validateFields()) {
            if (otpVerified) {
                val name = CommonFunctions.cleanUpName(memberName)
                val phone = CommonFunctions.cleanUpPhoneNumber(phoneNumber)
                LoaderManager.shared.showLoader()
                squadViewModel.addMember(true,name, phone) { _, _ ->
                    LoaderManager.shared.hideLoader()
                    showPopup.value = false
                }
            } else {
                val phoneWithCode = "+91$phoneNumber"
                sendOTPLoading = true
                otpProcessStarted = true

                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    phoneWithCode,
                    60L,
                    TimeUnit.SECONDS,
                    context as Activity,
                    object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            verifyOTPError = ""
                            otpVerified = true
                            verifyOTPLoading = false
                        }

                        override fun onVerificationFailed(e: FirebaseException) {
                            sendOTPError = e.localizedMessage ?: "OTP failed"
                            sendOTPLoading = false
                            otpProcessStarted = false
                        }

                        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                            verificationID = verificationId
                            sendOTPLoading = false
                            otpProcessStarted = false
                            isOTPSent = true
                        }
                    }
                )
            }
        }
    }

    // ✅ Debounced Name Validation
    @Composable
    fun handleNameChange(newValue: String) {
        memberNameError = ""
        LaunchedEffect(newValue) {
            delay(500)
            val name = CommonFunctions.cleanUpName(newValue)
            if (squadViewModel.groupFundMembers.value.any { it.name == name }) {
                memberNameError = "Name already exists"
            }
        }
    }

    // ✅ OTP Input Handler
    fun handleOTPChange(newValue: String) {
        verifyOTPError = ""
        verifyOTPLoading = newValue.length == 6
        if (newValue.length == 6) {
            val credential = PhoneAuthProvider.getCredential(verificationID, otpCode)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        otpVerified = true
                        verifyOTPLoading = false
                    } else {
                        verifyOTPError = task.exception?.localizedMessage ?: "Verification failed"
                        verifyOTPLoading = false
                    }
                }
        }
    }

    // ✅ Popup UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = showPopup.value,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.background)
                    .appShadow(AppShadows.elevated)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 🔹 Title
                Text(
                    text = "Add Member",
                    style = AppFont.ibmPlexSans(20, FontWeight.Bold),
                    color = AppColors.headerText,
                    modifier = Modifier.padding(top = 10.dp)
                )

                // 🔹 Fields
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SSTextField(
                        icon = Icons.Default.Person,
                        placeholder = "Member Name",
                        textState = remember { mutableStateOf(memberName) },
                        error = memberNameError
                    )

                    SSTextField(
                        icon = Icons.Default.Phone,
                        placeholder = "Member Mobile.No",
                        textState = remember { mutableStateOf(phoneNumber) },
                        keyboardType = KeyboardType.Number,
                        showDropdown = sendOTPLoading,
                        isLoading = sendOTPLoading,
                        error = phoneError
                    )

                    AnimatedVisibility(visible = isOTPSent) {
                        SSTextField(
                            icon = Icons.Default.Numbers,
                            placeholder = "Enter OTP",
                            textState = remember { mutableStateOf(otpCode) },
                            keyboardType = KeyboardType.Number,
                            showDropdown = verifyOTPLoading || otpVerified,
                            dropdownIcon = Icons.Default.CheckCircle,
                            dropdownColor = AppColors.primaryButton,
                            isLoading = verifyOTPLoading,
                            error = verifyOTPError
                        )
                    }
                }

                // 🔹 Action Buttons
                SSButton(
                    title = if (otpVerified) "Add Member" else "Send OTP",
                    isDisabled = memberNameError.isNotEmpty() || otpProcessStarted,
                    action = { handleAddMember() }
                )

                SSCancelButton(
                    title = "Cancel",
                    isButtonLoading = false,
                    isDisabled = false,
                    action = { showPopup.value = false }
                )
            }
        }
    }
}

@Composable
fun HomeStatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    background: Color = AppColors.surface,
    isLoading: Boolean = false
) {
    val iconForegroundColor = when (icon) {
        Icons.Default.Group -> AppColors.infoAccent
        Icons.Default.DateRange -> AppColors.warningAccent
        Icons.Default.AccessTime -> AppColors.infoAccent
        Icons.Default.AttachMoney -> AppColors.successAccent
        Icons.Default.ArrowDownward -> AppColors.errorAccent
        Icons.Default.ArrowUpward -> AppColors.successAccent
        Icons.Default.Error -> AppColors.errorAccent
        Icons.Default.Percent -> AppColors.infoAccent
        Icons.Default.CreditCard -> AppColors.primaryButton
        else -> AppColors.infoAccent
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth()
            .background(
                color = background,
                shape = RoundedCornerShape(12.dp)
            )
            .appShadow(AppShadows.card)
            .border(0.5.dp, AppColors.border, RoundedCornerShape(12.dp))
    ) {
        // Icon
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(iconForegroundColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconForegroundColor,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // Title
        Text(
            text = title,
            style = AppFont.ibmPlexSans(12, FontWeight.Normal),
            color = AppColors.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Value / Loader
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = AppColors.primaryButton,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Text(
                text = value,
                style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                color = AppColors.headerText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Subtitle
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = AppFont.ibmPlexSans(10, FontWeight.Normal),
                color = AppColors.secondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    isLoading: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(AppColors.surface, RoundedCornerShape(20.dp))
            .appShadow(AppShadows.card)
            .border(0.7.dp, AppColors.border, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = title,
            style = AppFont.ibmPlexSans(12, FontWeight.Medium),
            color = AppColors.secondaryText
        )

        // Value or loader
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = AppColors.primaryButton,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Text(
                text = value,
                style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                color = AppColors.headerText
            )
        }
    }
}

@Composable
fun StatCardHome(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    background: Color = AppColors.surface
) {
    val iconColor = when (icon) {
        Icons.Default.Group -> AppColors.infoAccent
        Icons.Default.DateRange -> AppColors.warningAccent
        Icons.Default.AccessTime -> AppColors.infoAccent
        Icons.Default.AttachMoney -> AppColors.successAccent
        Icons.Default.ArrowDownward -> AppColors.errorAccent
        Icons.Default.ArrowUpward -> AppColors.successAccent
        Icons.Default.Error -> AppColors.errorAccent
        Icons.Default.Percent -> AppColors.infoAccent
        Icons.Default.CreditCard -> AppColors.primaryButton
        else -> AppColors.infoAccent
    }

    Column(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth()
            .background(background, RoundedCornerShape(16.dp))
            .appShadow(AppShadows.card),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // 🔹 Icon Circle
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 🔹 Title
        Text(
            text = title,
            style = AppFont.ibmPlexSans(12, FontWeight.Medium),
            color = AppColors.secondaryText
        )

        // 🔹 Value
        Text(
            text = value,
            style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
            color = AppColors.headerText
        )

        // 🔹 Subtitle
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = AppFont.ibmPlexSans(10, FontWeight.Normal),
                color = AppColors.secondaryText
            )
        }
    }
}

@Composable
fun InstallmentPopupView(
    title: String = "Select Installment",
    installments: List<Installment>,
    onSelect: (Installment) -> Unit,
    isShowing: MutableState<Boolean>
) {
    val today = remember { Date() }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.surface)
            .appShadow(AppShadows.elevated)
            .widthIn(max = 350.dp)
            .heightIn(max = 450.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 🔹 Title Header
        Text(
            text = title,
            style = AppFont.ibmPlexSans(18, FontWeight.SemiBold),
            color = AppColors.headerText,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppColors.surface,
                    shape = RoundedCornerShape(12.dp)
                )
                .appShadow(AppShadows.card)
                .padding(vertical = 12.dp),
            textAlign = TextAlign.Center
        )

        // 🔹 Installments List
        LazyColumn(
            modifier = Modifier
                .padding(vertical = 10.dp)
                .heightIn(max = 300.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(installments) { installment ->
                val status = computedStatus(installment, today)

                Button(
                    onClick = {
                        if (status == EMIStatus.PENDING) {
                            onSelect(installment)
                            isShowing.value = false
                        }
                    },
                    enabled = status == EMIStatus.PENDING,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.surface,
                        disabledContainerColor = AppColors.surface
                    ),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .appShadow(AppShadows.card)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Section
                        Column(
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = installment.installmentNumber,
                                style = AppFont.ibmPlexSans(13, FontWeight.Normal),
                                color = AppColors.secondaryText
                            )
                            Text(
                                text = installment.installmentAmount.currencyFormattedWithCommas(),
                                style = AppFont.ibmPlexSans(16, FontWeight.Bold),
                                color = AppColors.headerText
                            )
                            Text(
                                text = "Interest: ${installment.interestAmount.currencyFormattedWithCommas()}",
                                style = AppFont.ibmPlexSans(13, FontWeight.Normal),
                                color = AppColors.secondaryText
                            )
                            installment.dueDate?.let {
                                Text(
                                    text = "Due: ${CommonFunctions.dateToString(it.toDate())}",
                                    style = AppFont.ibmPlexSans(13, FontWeight.Normal),
                                    color = AppColors.secondaryText
                                )
                            }
                        }

                        // Right Section – Status Badge
                        Text(
                            text = status.displayText,
                            color = AppColors.primaryButtonText,
                            style = AppFont.ibmPlexSans(12, FontWeight.Bold),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(status.color)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // 🔹 Close Button
        SSCancelButton(title = "Close") {
            isShowing.value = false
        }
    }
}

/**
 * Detect overdue installments
 */
fun computedStatus(installment: Installment, today: Date): EMIStatus {
    return if (installment.status == EMIStatus.PENDING &&
        installment.dueDate?.toDate()?.before(today) == true
    ) {
        EMIStatus.OVERDUE
    } else {
        installment.status
    }
}


@Composable
fun FloatingGroupFundButton(
    onGroupFundActivity: () -> Unit,
    onPaymentHistory: () -> Unit,
    onGroupFundRules: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val menuItems = listOf(
        MenuItem("list_bullet", "Group Fund Activity", onGroupFundActivity),
        MenuItem("creditcard_fill", "Payment History", onPaymentHistory),
        MenuItem("book_fill", "Group Fund Rules", onGroupFundRules)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Dimmed background when menu open
        if (showMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f))
                    .clickable { showMenu = false }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 12.dp, end = 20.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            // 🟢 Menu items
            AnimatedVisibility(
                visible = showMenu,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.offset(y = (-80).dp)
                ) {
                    menuItems.forEachIndexed { index, item ->
                        FloatingMenuButton(
                            icon = item.icon,
                            title = item.title,
                            onClick = {
                                item.action()
                                showMenu = false
                            },
                            animationDelay = index * 50L
                        )
                    }
                }
            }

            // 🔵 Main Floating Action Button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AppColors.primaryBrand)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.25f)
                    )
                    .clickable {
                        showMenu = !showMenu
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        if (showMenu) R.drawable.groupfund_activity_unselected
                        else R.drawable.groupfund_activity_selected
                    ),
                    contentDescription = null,
                    tint = AppColors.primaryButtonText,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
private fun FloatingMenuButton(
    icon: String,
    title: String,
    onClick: () -> Unit,
    animationDelay: Long = 0L
) {
    LaunchedEffect(Unit) { delay(animationDelay) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        // Text chip
        Box(
            modifier = Modifier
                .background(AppColors.primaryBrand, RoundedCornerShape(12.dp))
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = Color.Black.copy(alpha = 0.15f)
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = title,
                color = AppColors.primaryButtonText,
                style = AppFont.ibmPlexSans(14, FontWeight.SemiBold)
            )
        }

        // Icon Circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(AppColors.primaryBrand)
                .shadow(
                    elevation = 4.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = AppIcons.resolve(icon)), // your helper for mapping systemName → resource
                contentDescription = null,
                tint = AppColors.primaryButtonText,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private data class MenuItem(
    val icon: String,
    val title: String,
    val action: () -> Unit
)

object AppIcons {
    fun resolve(name: String): Int = when (name) {
        "book_fill" -> R.drawable.groupfund_activity_unselected
        "list_bullet" -> R.drawable.groupfund_activity_unselected
        "creditcard_fill" -> R.drawable.groupfund_activity_unselected
        else -> R.drawable.groupfund_activity_selected
    }
}