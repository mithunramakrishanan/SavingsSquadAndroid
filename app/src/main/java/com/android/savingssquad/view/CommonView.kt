package com.android.savingssquad.view

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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.input.pointer.pointerInput
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.ShadowStyle
import com.android.savingssquad.singleton.appShadow

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.Login
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.viewmodel.SquadViewModel
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.android.savingssquad.R
import com.android.savingssquad.SquadSubscription.SubscriptionManager
import com.android.savingssquad.model.ForceCloseSummary
import com.android.savingssquad.model.InterestType
import com.android.savingssquad.model.Member
import com.android.savingssquad.model.MemberLoan
import com.android.savingssquad.model.forceCloseSummary
import com.android.savingssquad.singleton.AlertType
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.SquadLanguages
import com.android.savingssquad.singleton.LoaderManager
import com.android.savingssquad.singleton.RecordStatus
import com.android.savingssquad.singleton.color
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.singleton.displayText
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.viewmodel.AppDestination
import com.google.firebase.auth.PhoneAuthOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.runtime.collectAsState
import com.android.savingssquad.singleton.SecondaryFirebaseAuthManager


// ---------- SSNavigationBar ----------
@Composable
fun SSNavigationBar(
    title: String,
    navController: NavController?, // pass rememberNavController() from caller
    showBackButton: Boolean = true,
    titleTap: (() -> Unit)? = null,
    rightButtonIcon: ImageVector? = null, // Material icon
    @DrawableRes rightButtonDrawable: Int? = null, // ✅ now supports drawable resource
    rightButtonAction: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 🔹 Back Button
        if (showBackButton) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current
                    ) {
                        navController?.popBackStack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.back_icon),
                    contentDescription = "Back",
                    tint = AppColors.headerText,
                    modifier = Modifier.size(25.dp)
                )
            }
        }

        // 🔹 Title
        Text(
            text = title,
            style = AppFont.ibmPlexSans(21, FontWeight.Bold),
            color = AppColors.headerText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .then(
                    if (titleTap != null) {
                        Modifier.clickable {
                            titleTap.invoke()
                        }
                    } else {
                        Modifier
                    }
                )
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 🔹 Right Button
        if (rightButtonAction != null && (rightButtonIcon != null || rightButtonDrawable != null)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clickable { rightButtonAction() },
                contentAlignment = Alignment.Center
            ) {
                when {
                    // ✅ Use drawable resource
                    rightButtonDrawable != null -> {
                        Image(
                            painter = painterResource(id = rightButtonDrawable),
                            contentDescription = "Action",
                            modifier = Modifier.size(25.dp),
                            colorFilter = ColorFilter.tint(AppColors.headerText)
                        )
                    }

                    // ✅ Use ImageVector
                    rightButtonIcon != null -> {
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
    }
}

// ---------- SSButton ----------
@Composable
fun SSButton(
    title: String,
    modifier: Modifier = Modifier,
    isButtonLoading: Boolean = false,
    isDisabled: Boolean = false,
    action: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = if (isDisabled) AppColors.disabledButton else AppColors.primaryButton,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 0.dp,
        onClick = {
            if (!isDisabled && !isButtonLoading) {
                action()
            }
        },
        enabled = !isDisabled && !isButtonLoading
    ) {
        Box(
            modifier = Modifier
                .height(44.dp)
                .appShadow(
                    if (isDisabled) ShadowStyle(Color.Transparent, 0.dp, 0.dp, 0.dp)
                    else AppShadows.elevated,
                    RoundedCornerShape(18.dp)
                ),
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

@Composable
fun SSCancelButton(
    title: String,
    modifier: Modifier = Modifier,
    isButtonLoading: Boolean = false,
    isDisabled: Boolean = false,
    action: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (isDisabled) AppColors.surface.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.95f),
        shadowElevation = 0.dp,
        onClick = {
            if (!isDisabled && !isButtonLoading) {
                action()
            }
        },
        enabled = !isDisabled && !isButtonLoading
    ) {
        Box(
            modifier = Modifier
                .height(44.dp)
                .border(
                    1.dp,
                    AppColors.headerText.copy(alpha = if (isDisabled) 0.3f else 0.8f),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isButtonLoading) {
                CircularProgressIndicator(
                    color = AppColors.headerText,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = title,
                    style = AppFont.ibmPlexSans(15, FontWeight.SemiBold),
                    color = if (isDisabled)
                        AppColors.secondaryText.copy(alpha = 0.5f)
                    else AppColors.headerText
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
    isLoading: Boolean = false,
    dropdownIcon: ImageVector? = Icons.Default.ArrowDropDown,
    dropdownColor: Color = AppColors.secondaryText,
    onDropdownTap: (() -> Unit)? = null,
    error: String = "",
    maxLength: Int? = null
) {

    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = when {
            error.isNotEmpty() -> AppColors.errorAccent
            isFocused -> AppColors.primaryButton
            else -> AppColors.border
        },
        label = ""
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 1.8.dp else 1.dp,
        label = ""
    )

    val bgColor = when {
        disabled -> Color(0xFFF6F8F9)
        else -> AppColors.textFieldBackground
    }

    val iconTint by animateColorAsState(
        targetValue = when {
            disabled -> AppColors.secondaryText.copy(alpha = 0.45f)
            isFocused -> AppColors.primaryButton
            else -> AppColors.secondaryText
        },
        label = ""
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (showDropdown) {
                    onDropdownTap?.invoke()
                } else if (!disabled) {
                    focusRequester.requestFocus()
                }
            },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .appShadow(
                    AppShadows.card,
                    RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {

                Spacer(modifier = Modifier.width(14.dp))

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {

                    if (textState.value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = AppFont.ibmPlexSans(
                                15,
                                FontWeight.Normal
                            ),
                            color = if (disabled) {
                                AppColors.placeholderText.copy(alpha = 0.6f)
                            } else {
                                AppColors.placeholderText
                            }
                        )
                    }

                    BasicTextField(
                        value = textState.value,
                        onValueChange = { newValue ->

                            if (!disabled) {

                                val filteredValue = when {
                                    maxLength == null -> newValue

                                    keyboardType == KeyboardType.Number ||
                                            keyboardType == KeyboardType.Phone -> {
                                        newValue
                                            .filter { it.isDigit() }
                                            .take(maxLength)
                                    }

                                    else -> newValue.take(maxLength)
                                }

                                textState.value = filteredValue
                            }
                        },
                        singleLine = true,
                        enabled = !disabled && !showDropdown,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = keyboardType
                        ),
                        cursorBrush = SolidColor(
                            AppColors.primaryButton
                        ),
                        textStyle = AppFont.ibmPlexSans(
                            15,
                            FontWeight.Medium
                        ).copy(
                            color = if (disabled) {
                                AppColors.secondaryText.copy(alpha = 0.7f)
                            } else {
                                AppColors.headerText
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                isFocused = it.isFocused && !disabled
                            }
                    )
                }

                if (showDropdown) {

                    if (isLoading) {

                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(20.dp),
                            strokeWidth = 2.dp,
                            color = AppColors.loaderColor
                        )

                    } else {

                        dropdownIcon?.let { icon ->

                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = dropdownColor,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable {
                                        onDropdownTap?.invoke()
                                    }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))
            }
        }

        AnimatedVisibility(
            visible = error.isNotEmpty()
        ) {
            Text(
                text = error,
                style = AppFont.ibmPlexSans(
                    12,
                    FontWeight.Normal
                ),
                color = AppColors.errorAccent,
                modifier = Modifier.padding(start = 24.dp)
            )
        }
    }
}

@Composable
fun SingleSelectionPopupView(
    listValues: List<String>,
    title: String,
    onItemSelected: (String) -> Unit,
    onCancelClick: () -> Unit,
    enableOnlyFirstIndex: Boolean = false

) {
    var searchText by remember { mutableStateOf("") }

    val filteredValues = remember(searchText, listValues) {
        listValues.filter { searchText.isEmpty() || it.contains(searchText, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { }, // tap outside to dismiss
        contentAlignment = Alignment.Center
    ) {
        // ❌ Removed the previous .clickable(enabled = false) here

        Column(
            modifier = Modifier
                .width(340.dp)
                .clip(RoundedCornerShape(20.dp))
                .shadow(
                    elevation = 8.dp,
                    spotColor = Color.Black.copy(alpha = 0.15f),
                    ambientColor = Color.Black.copy(alpha = 0.15f)
                )
                .background(AppColors.surface)
                .padding(vertical = 16.dp), // optional padding inside
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ▣ TITLE
            Text(
                text = title,
                style = AppFont.ibmPlexSans(20, FontWeight.Bold),
                color = AppColors.headerText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ▣ SEARCH BAR
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .background(AppColors.textFieldBackground, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = AppColors.secondaryText
                )
                Spacer(modifier = Modifier.width(8.dp))

                BasicTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = AppFont.ibmPlexSans(14, FontWeight.Normal)
                        .copy(color = AppColors.headerText),
                    decorationBox = { inner ->
                        if (searchText.isEmpty()) {
                            Text(
                                text = "Search",
                                style = AppFont.ibmPlexSans(14, FontWeight.Normal),
                                color = AppColors.secondaryText
                            )
                        }
                        inner()
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ▣ LIST VALUES
            Box(
                modifier = Modifier
                    .heightIn(max = 260.dp)
                    .padding(horizontal = 8.dp)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (filteredValues.isEmpty()) {
                        item {
                            Text(
                                text = SquadStrings.noResults,
                                color = AppColors.secondaryText,
                                style = AppFont.ibmPlexSans(14, FontWeight.Normal),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        itemsIndexed(filteredValues) { index, value ->

                            val isEnabled = !enableOnlyFirstIndex || index == 0

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(AppColors.primaryBackground)
                                        .alpha(if (isEnabled) 1f else 0.5f)
                                        .shadow(
                                            elevation = 3.dp,
                                            spotColor = Color.Black.copy(alpha = 0.03f),
                                            ambientColor = Color.Black.copy(alpha = 0.03f)
                                        )
                                        .clickable(enabled = isEnabled) {
                                            onItemSelected(value)
                                        }
                                        .padding(
                                            vertical = 12.dp,
                                            horizontal = 16.dp
                                        )
                                ) {
                                    Text(
                                        text = SquadStrings.localizedMonthInText(value),
                                        style = AppFont.ibmPlexSans(
                                            14,
                                            FontWeight.Medium
                                        ),
                                        color = if (isEnabled)
                                            AppColors.headerText
                                        else
                                            AppColors.secondaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ▣ CANCEL BUTTON
            SSCancelButton(
                title = SquadStrings.cancel,
                action = onCancelClick
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ---------- SSTextView ----------
@Composable
fun SSTextView(
    placeholder: String,
    text: String,
    onTextChange: (String) -> Unit,
    error: String = "",
    maxCharacters: Int? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = when {
            error.isNotEmpty() -> AppColors.errorAccent
            isFocused -> AppColors.primaryButton
            else -> Color.Transparent
        },
        label = "borderColor"
    )

    val elevationAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.14f else 0.06f,
        label = "elevation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AppColors.surface)
                .border(
                    width = if (isFocused || error.isNotEmpty()) 1.2.dp else 0.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(4.dp)
        ) {

            BasicTextField(
                value = text,
                onValueChange = {
                    if (maxCharacters != null && it.length > maxCharacters)
                        onTextChange(it.take(maxCharacters))
                    else
                        onTextChange(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
                    .padding(16.dp)
                    .onFocusChanged { isFocused = it.isFocused },
                textStyle = AppFont.ibmPlexSans(15, FontWeight.Normal)
                    .copy(
                        color = AppColors.headerText,
                        lineHeight = 22.sp
                    ),
                cursorBrush = SolidColor(AppColors.primaryButton)
            )

            // Floating placeholder effect
            if (text.isEmpty()) {
                Text(
                    text = placeholder,
                    color = AppColors.placeholderText,
                    style = AppFont.ibmPlexSans(15, FontWeight.Normal),
                    modifier = Modifier
                        .padding(start = 20.dp, top = 20.dp)
                        .alpha(if (isFocused) 0.6f else 1f)
                )
            }
        }

        // Error text (premium style)
        AnimatedVisibility(visible = error.isNotEmpty()) {
            Text(
                text = error,
                color = AppColors.errorAccent,
                style = AppFont.ibmPlexSans(12, FontWeight.Medium),
                modifier = Modifier.padding(start = 26.dp, top = 2.dp)
            )
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
fun OverlayBackgroundView(
    showPopup: State<Boolean>,
    onDismiss: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (showPopup.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .pointerInput(Unit) {
                    // Consume all touches to block interactions behind popup
                }
                .then(
                    if (onDismiss != null) Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {  } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun ProgressCircleView(
    completedMonths: Int,
    totalMonths: Int,
    monthlyContribution: String,
    onClick: (() -> Unit)? = null
) {

    val progress = remember(completedMonths, totalMonths) {
        if (totalMonths <= 0) 0f
        else (completedMonths.toFloat() / totalMonths.toFloat()).coerceIn(0f, 1f)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        )
    )

    Box(
        modifier = Modifier
            .size(180.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {

        // ================= OUTER GLOW LAYER =================
        Canvas(modifier = Modifier.size(160.dp)) {
            drawCircle(
                color = Color.White.copy(alpha = 0.65f),
                style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // ================= BASE TRACK =================
        Canvas(modifier = Modifier.size(150.dp)) {
            drawCircle(
                color = AppColors.secondaryText.copy(alpha = 0.10f),
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // ================= PROGRESS RING =================
        Canvas(modifier = Modifier.size(150.dp)) {

            val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            val sweep = animatedProgress * 360f

            rotate(-90f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            AppColors.primaryButton,
                            AppColors.successAccent,
                            AppColors.primaryButton.copy(alpha = 0.7f)
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = stroke
                )
            }
        }

        // ================= CENTER CONTENT =================
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Text(
                text = "$completedMonths/$totalMonths",
                style = AppFont.ibmPlexSans(18, FontWeight.SemiBold),
                color = AppColors.headerText
            )

            Text(
                text = SquadStrings.months,
                style = AppFont.ibmPlexSans(12, FontWeight.Medium),
                color = AppColors.secondaryText
            )

            HorizontalDivider(
                modifier = Modifier.width(40.dp),
                thickness = 1.dp,
                color = AppColors.border.copy(alpha = 0.4f)
            )

            Text(
                text = monthlyContribution,
                style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                color = AppColors.primaryButton
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
fun SSLoaderView() {
    val isLoading = LoaderManager.shared.isLoading
    val loadingMessage = LoaderManager.shared.loadingMessage

    if (!isLoading) return

    // 🔹 Smooth continuous rotation via infinite transition instead of a manual 16ms delay loop
    val infiniteTransition = rememberInfiniteTransition(label = "ssLoaderAnim")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val symbolScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "symbolScale"
    )

    var dotPhase by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400.milliseconds)
            dotPhase = (dotPhase + 1) % 4
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .pointerInput(Unit) {
                detectTapGestures { /* swallow taps — disables interaction underneath */ }
            }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .background(AppColors.background, RoundedCornerShape(22.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(vertical = 26.dp, horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Box(
                modifier = Modifier.size(78.dp),
                contentAlignment = Alignment.Center
            ) {

                // Outer breathing glow
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                            alpha = pulseAlpha
                        }
                        .background(AppColors.loaderColor.copy(alpha = 0.12f), CircleShape)
                )

                // Static track ring — depth layer beneath the moving arc
                Canvas(modifier = Modifier.size(54.dp)) {
                    drawArc(
                        color = AppColors.loaderColor.copy(alpha = 0.08f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Rotating gradient arc
                Canvas(modifier = Modifier.size(54.dp)) {
                    rotate(rotation) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    AppColors.loaderColor,
                                    AppColors.loaderColor.copy(alpha = 0.15f),
                                    AppColors.loaderColor
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // ₹ symbol — held upright, gently breathing (no counter-rotation)
                Text(
                    text = "₹",
                    style = AppFont.ibmPlexSans(20, FontWeight.Bold),
                    color = AppColors.loaderColor,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = symbolScale
                            scaleY = symbolScale
                        }
                        .drawBehind {
                            // manual glow instead of a tinted shadow — avoids the emulator white-box bug
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        AppColors.loaderColor.copy(alpha = 0.35f),
                                        Color.Transparent
                                    )
                                ),
                                radius = size.maxDimension
                            )
                        }
                )
            }

            // Message + animated dots
            Row(verticalAlignment = Alignment.CenterVertically) {

                Text(
                    text = loadingMessage,
                    style = AppFont.ibmPlexSans(14, FontWeight.SemiBold),
                    color = AppColors.headerText
                )

                Text(
                    text = ".".repeat(dotPhase),
                    style = AppFont.ibmPlexSans(14, FontWeight.SemiBold),
                    color = AppColors.headerText,
                    modifier = Modifier.width(16.dp)
                )
            }
        }
    }
}


@Composable
fun LoginListPopup(
    navController: NavController,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    selectedUser: Login?,
    onUserSelected: (Login) -> Unit,
    users: List<Login>,
    squadViewModel: SquadViewModel
) {

    if (!isVisible) return

    val inPreview = LocalInspectionMode.current

    var selectedRole by remember {
        mutableStateOf(
            SquadUserType.SQUAD_MANAGER.localizedName
        )
    }

    var selectedTab by remember {
        mutableStateOf("Current")
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var isUpdating by remember {
        mutableStateOf(false)
    }

    // --------------------------------
    // ROLE FILTER
    // --------------------------------

    val roleFilteredUsers = remember(users, selectedRole) {

        if (selectedRole == SquadUserType.SQUAD_MANAGER.localizedName) {

            users.filter {
                it.role == SquadUserType.SQUAD_MANAGER
            }

        } else {

            users.filter {
                it.role == SquadUserType.SQUAD_MEMBER
            }
        }
    }

    // --------------------------------
    // CURRENT / ARCHIVE FILTER
    // --------------------------------

    val statusFilteredUsers = remember(
        roleFilteredUsers,
        selectedTab
    ) {

        if (selectedTab == "Current") {

            roleFilteredUsers.filter {
                !it.isArchived
            }

        } else {

            roleFilteredUsers.filter {
                it.isArchived
            }
        }
    }

    // --------------------------------
    // SEARCH FILTER
    // --------------------------------

    val filteredUsers = remember(
        statusFilteredUsers,
        searchText
    ) {

        val query = searchText.trim()

        if (query.isBlank()) {

            statusFilteredUsers

        } else {

            statusFilteredUsers.filter { user ->

                user.localizedSquadName.contains(
                    query,
                    ignoreCase = true
                ) ||
                        user.localizedMemberName.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        user.phoneNumber.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        user.squadName.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        user.memberName.contains(
                            query,
                            ignoreCase = true
                        )
            }
        }
    }

    // --------------------------------
    // ARCHIVE / RESTORE ACTION
    // --------------------------------

    fun updateArchiveStatus(
        user: Login,
        isArchived: Boolean
    ) {

        if (isUpdating) return

        isUpdating = true

        squadViewModel.updateLoginArchiveStatus(
            showLoader = true,
            login = user,
            isArchived = isArchived
        ) { success, error ->

            isUpdating = false

            if (!success) {

                AlertManager.shared.showAlert(
                    title = SquadStrings.savingsSquad,
                    message = error ?: "Unable to update profile.",
                    primaryButtonTitle = SquadStrings.ok,
                    primaryAction = {}
                )
            }

            // On success:
            // ViewModel updates users locally.
            // Compose receives the updated list.
            // Current / Archive automatically refreshes.
        }
    }

    // --------------------------------
    // BACKDROP
    // --------------------------------

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(enabled = !isUpdating) {
                onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {

        // --------------------------------
        // MODAL CARD
        // --------------------------------

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(360.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.background)
                .appShadow(
                    AppShadows.elevated,
                    RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {

            // TITLE

            Text(
                text = SquadStrings.selectSquad,
                style = AppFont.ibmPlexSans(
                    20,
                    FontWeight.Bold
                ),
                color = AppColors.headerText
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --------------------------------
            // CURRENT / ARCHIVE TABS
            // --------------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                LoginStatusTab(
                    title = "Current",
                    count = roleFilteredUsers.count {
                        !it.isArchived
                    },
                    selected = selectedTab == "Current",
                    modifier = Modifier.weight(1f)
                ) {
                    selectedTab = "Current"
                    searchText = ""
                }

                LoginStatusTab(
                    title = "Archive",
                    count = roleFilteredUsers.count {
                        it.isArchived
                    },
                    selected = selectedTab == "Archive",
                    modifier = Modifier.weight(1f)
                ) {
                    selectedTab = "Archive"
                    searchText = ""
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --------------------------------
            // MANAGER / MEMBER PICKER
            // --------------------------------

            ModernSegmentedPickerView(
                segments = listOf(
                    SquadUserType.SQUAD_MANAGER.localizedName,
                    SquadUserType.SQUAD_MEMBER.localizedName
                ),
                selectedSegment = selectedRole,
                onSegmentSelected = {
                    selectedRole = it
                    searchText = ""
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // --------------------------------
            // SEARCH BAR
            // --------------------------------

            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Search squad, member or phone",
                        style = AppFont.ibmPlexSans(13)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {

                    if (searchText.isNotEmpty()) {

                        IconButton(
                            onClick = {
                                searchText = ""
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // --------------------------------
            // USER LIST
            // --------------------------------

            if (filteredUsers.isEmpty()) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        tint = AppColors.secondaryText,
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (searchText.isNotEmpty()) {
                            "No matching profiles found"
                        } else if (selectedTab == "Archive") {
                            "No archived profiles"
                        } else {
                            "No current profiles found"
                        },
                        style = AppFont.ibmPlexSans(
                            14,
                            FontWeight.Medium
                        ),
                        color = AppColors.secondaryText
                    )
                }

            } else {

                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(
                        items = filteredUsers,
                        key = { it.id ?: it.squadID + it.squadUserId }
                    ) { user ->

                        UserSelectionCard(
                            user = user,
                            isUpdating = isUpdating,
                            onSelect = {

                                if (user.isArchived || isUpdating) {
                                    return@UserSelectionCard
                                }

                                onUserSelected(user)

                                if (!inPreview) {

                                    UserDefaultsManager.saveLogin(user)

                                    UserDefaultsManager.saveIsLoggedIn(true)

                                    val isManager =
                                        user.role == SquadUserType.SQUAD_MANAGER

                                    UserDefaultsManager.saveSquadManagerLogged(
                                        isManager
                                    )

                                    val route = if (isManager) {
                                        AppDestination.MANAGER_HOME.route
                                    } else {
                                        AppDestination.MEMBER_HOME.route
                                    }

                                    navController.navigate(route) {

                                        popUpTo(
                                            AppDestination.SIGN_IN.route
                                        ) {
                                            inclusive = true
                                        }

                                        launchSingleTop = true
                                    }
                                }

                                onDismiss()
                            },
                            onArchive = {

                                updateArchiveStatus(
                                    user = user,
                                    isArchived = true
                                )
                            },
                            onRestore = {

                                updateArchiveStatus(
                                    user = user,
                                    isArchived = false
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // CANCEL

            SSCancelButton(
                title = SquadStrings.cancel,
                isButtonLoading = false,
                isDisabled = isUpdating
            ) {
                onDismiss()
            }
        }
    }
}


@Composable
fun UserSelectionCard(
    user: Login,
    isUpdating: Boolean,
    onSelect: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit
) {

    var menuExpanded by remember {
        mutableStateOf(false)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // --------------------------------
            // USER DETAILS
            // --------------------------------

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        enabled = !user.isArchived && !isUpdating
                    ) {
                        onSelect()
                    },
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {

                Text(
                    text = user.localizedSquadName,
                    style = AppFont.ibmPlexSans(
                        16,
                        FontWeight.SemiBold
                    ),
                    color = AppColors.headerText
                )

                Text(
                    text = user.localizedMemberName,
                    style = AppFont.ibmPlexSans(12),
                    color = AppColors.secondaryText
                )

                val createdDate =
                    user.userCreatedDate?.toDate() ?: Date()

                val dateText = CommonFunctions.dateToString(
                    createdDate
                )

                val subtitle = if (
                    user.role == SquadUserType.SQUAD_MANAGER
                ) {

                    SquadStrings.managerSince(
                        SquadStrings.localizedMonthInText(dateText)
                    )

                } else {

                    SquadStrings.memberSince(
                        SquadStrings.localizedMonthInText(dateText)
                    )
                }

                Text(
                    text = subtitle,
                    style = AppFont.ibmPlexSans(11),
                    color = AppColors.secondaryText
                )
            }

            // --------------------------------
            // ARCHIVE / RESTORE MENU
            // --------------------------------

            Box {

                IconButton(
                    onClick = {
                        menuExpanded = true
                    },
                    enabled = !isUpdating
                ) {

                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = AppColors.secondaryText
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = {
                        menuExpanded = false
                    }
                ) {

                    if (user.isArchived) {

                        DropdownMenuItem(

                            text = {
                                Text("Move to Current")
                            },

                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Unarchive,
                                    contentDescription = null
                                )
                            },

                            onClick = {

                                menuExpanded = false
                                onRestore()
                            }
                        )

                    } else {

                        DropdownMenuItem(

                            text = {
                                Text("Archive")
                            },

                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = null
                                )
                            },

                            onClick = {

                                menuExpanded = false
                                onArchive()
                            }
                        )
                    }
                }
            }

            // --------------------------------
            // NAVIGATION INDICATOR
            // --------------------------------

            if (!user.isArchived) {

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AppColors.secondaryText.copy(
                        alpha = 0.6f
                    )
                )
            }
        }
    }
}


@Composable
fun LoginStatusTab(
    title: String,
    count: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    AppColors.headerText
                } else {
                    AppColors.surface
                }
            )
            .clickable {
                onClick()
            }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = if (title == "Current") {
                Icons.Default.People
            } else {
                Icons.Default.Archive
            },
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = if (selected) {
                Color.White
            } else {
                AppColors.secondaryText
            }
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = title,
            style = AppFont.ibmPlexSans(
                13,
                FontWeight.SemiBold
            ),
            color = if (selected) {
                Color.White
            } else {
                AppColors.secondaryText
            }
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = count.toString(),
            style = AppFont.ibmPlexSans(
                11,
                FontWeight.Medium
            ),
            color = if (selected) {
                Color.White
            } else {
                AppColors.secondaryText
            },
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (selected) {
                        Color.White.copy(alpha = 0.2f)
                    } else {
                        AppColors.secondaryText.copy(alpha = 0.1f)
                    }
                )
                .padding(
                    horizontal = 7.dp,
                    vertical = 3.dp
                )
        )
    }
}

@Composable
fun SectionView(
    title: String,
    subtitle: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {

        // 🔹 Header Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
        ) {

            // gradient accent bar — gives the header a premium, branded anchor point
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(AppColors.primaryBrand, AppColors.primaryBrand.copy(alpha = 0.5f))
                        )
                    )
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = AppFont.ibmPlexSans(17, FontWeight.SemiBold),
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

            // optional trailing action, e.g. "See All" or an icon button
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingContent()
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

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {

        val segmentWidth = maxWidth / segments.size

        val selectedIndex = segments.indexOf(selectedSegment).coerceAtLeast(0)

        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessMedium
            ),
            label = "indicator"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .background(
                    AppColors.secondaryText.copy(alpha = 0.08f),
                    RoundedCornerShape(14.dp)
                )
                .border(
                    0.8.dp,
                    AppColors.border.copy(alpha = 0.4f),
                    RoundedCornerShape(14.dp)
                )
                .padding(4.dp)
        ) {

            // Sliding Indicator
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(segmentWidth - 8.dp)
                    .fillMaxHeight()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                AppColors.primaryButton,
                                AppColors.successAccent.copy(alpha = 0.85f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
            )

            Row(
                modifier = Modifier.fillMaxSize()
            ) {

                segments.forEach { segment ->

                    val selected = segment == selectedSegment

                    var pressed by remember {
                        mutableStateOf(false)
                    }

                    val scale by animateFloatAsState(
                        if (pressed) 0.96f else 1f,
                        animationSpec = spring(),
                        label = "scale"
                    )

                    val textColor by animateColorAsState(
                        if (selected)
                            AppColors.primaryButtonText
                        else
                            AppColors.secondaryText,
                        label = "text"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .pointerInput(Unit) {

                                detectTapGestures(

                                    onPress = {

                                        pressed = true

                                        tryAwaitRelease()

                                        pressed = false
                                    },

                                    onTap = {

                                        haptic.performHapticFeedback(
                                            HapticFeedbackType.TextHandleMove
                                        )

                                        onSegmentSelected(segment)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = segment,
                            style = AppFont.ibmPlexSans(
                                13,
                                if (selected)
                                    FontWeight.SemiBold
                                else
                                    FontWeight.Medium
                            ),
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AllCaughUPView(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Default.CreditCard,
    iconColor: Color = AppColors.primaryButton,
    showChevron: Boolean = true,
    onTap: (() -> Unit)? = null
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {

        val cardModifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .appShadow(AppShadows.card)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        AppColors.surface,
                        AppColors.surface.copy(alpha = 0.92f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
            .then(
                if (onTap != null) {
                    Modifier.clickable { onTap.invoke() }
                } else Modifier
            )
            .padding(18.dp)

        Row(
            modifier = cardModifier,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 🔹 ICON (iOS style circle)
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 🔹 TEXT BLOCK
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {

                Text(
                    text = title,
                    style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                    color = AppColors.headerText,
                    maxLines = 1
                )

                Text(
                    text = subtitle,
                    style = AppFont.ibmPlexSans(13, FontWeight.Medium),
                    color = AppColors.secondaryText,
                    maxLines = 2
                )
            }

            // 🔹 CHEVRON
            if (showChevron) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AppColors.secondaryText.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private val NON_PHONE_NUMBERS = listOf(
    "1111111111",
    "2222222222",
    "3333333333"
)

@Composable
fun AddMemberPopup(
    squadViewModel: SquadViewModel,
    onDismiss: () -> Unit
) {

    val context = LocalContext.current
    val activity = context as? Activity

    val coroutineScope = rememberCoroutineScope()

    // ---------------------------------------------------------
    // MEMBER DETAILS
    // ---------------------------------------------------------

    var memberName by remember {
        mutableStateOf("")
    }

    var phoneNumber by remember {
        mutableStateOf("")
    }


    // ---------------------------------------------------------
    // OTP DETAILS
    // ---------------------------------------------------------

    var otpCode by remember {
        mutableStateOf("")
    }

    var verificationID by remember {
        mutableStateOf("")
    }

    var memberFirebaseUID by remember {
        mutableStateOf("")
    }

    var isOTPSent by remember {
        mutableStateOf(false)
    }

    var OTPVerified by remember {
        mutableStateOf(false)
    }

    var OTPProcessStarted by remember {
        mutableStateOf(false)
    }

    var isOTPVerificationInProgress by remember {
        mutableStateOf(false)
    }

    var lastAttemptedOTP by remember {
        mutableStateOf("")
    }


    // ---------------------------------------------------------
    // ERRORS
    // ---------------------------------------------------------

    var memberNameError by remember {
        mutableStateOf("")
    }

    var phoneError by remember {
        mutableStateOf("")
    }

    var sendOTPError by remember {
        mutableStateOf("")
    }

    var verifyOTPError by remember {
        mutableStateOf("")
    }


    // ---------------------------------------------------------
    // FINANCIAL DETAILS
    // ---------------------------------------------------------

    var contributionReceived by remember {
        mutableStateOf("")
    }

    var interestReceived by remember {
        mutableStateOf("")
    }

    var markCurrentMonthPaid by remember {
        mutableStateOf(false)
    }


    // ---------------------------------------------------------
    // UI STATES
    // ---------------------------------------------------------

    var isSavingMember by remember {
        mutableStateOf(false)
    }

    var isValidatingMember by remember {
        mutableStateOf(false)
    }


    // ---------------------------------------------------------
    // VIEWMODEL DATA
    // ---------------------------------------------------------

    val squadMembers by squadViewModel.squadMembers.collectAsState()

    val squad = squadViewModel.squad.collectAsState().value


    // ---------------------------------------------------------
    // SQUAD STATUS
    // ---------------------------------------------------------

    val squadHasStarted = remember(squad?.squadStartDate) {

        val startDate = squad?.squadStartDate?.toDate()

        startDate != null && !startDate.after(Date())
    }


    // ---------------------------------------------------------
    // FINANCIAL AMOUNTS
    // ---------------------------------------------------------

    val contributionAmount =
        contributionReceived.toIntOrNull() ?: 0

    val interestAmount =
        interestReceived.toIntOrNull() ?: 0

    val totalReceived =
        contributionAmount + interestAmount


    val contributionValid =
        contributionReceived.isEmpty() ||
                contributionReceived.toIntOrNull() != null

    val interestValid =
        interestReceived.isEmpty() ||
                interestReceived.toIntOrNull() != null

    val areFinancialAmountsValid =
        contributionValid && interestValid


    // ---------------------------------------------------------
    // NAME VALIDATION
    // ---------------------------------------------------------

    LaunchedEffect(memberName, squadMembers) {

        memberNameError = ""

        val cleanedName = memberName.trim()

        if (cleanedName.isEmpty()) {
            return@LaunchedEffect
        }

        delay(350)

        val nameExists = squadMembers.any {

            it.localizedMemberName
                .trim()
                .equals(cleanedName, ignoreCase = true)
        }

        if (nameExists) {
            memberNameError = SquadStrings.nameAlreadyExists
        }
    }


    // ---------------------------------------------------------
    // PHONE VALIDATION
    // ---------------------------------------------------------

    LaunchedEffect(phoneNumber, squadMembers) {

        phoneError = ""

        val cleanedPhone =
            CommonFunctions.cleanUpPhoneNumber(phoneNumber)

        if (cleanedPhone.length != 10) {
            return@LaunchedEffect
        }

        delay(350)

        val phoneExists = squadMembers.any {

            CommonFunctions.cleanUpPhoneNumber(
                it.phoneNumber
            ) == cleanedPhone
        }

        if (phoneExists) {
            phoneError =
                SquadStrings.mobileNumberAlreadyExists
        }
    }


    // ---------------------------------------------------------
    // RESET OTP WHEN PHONE NUMBER CHANGES
    // ---------------------------------------------------------

    LaunchedEffect(phoneNumber) {

        if (isOTPSent || OTPVerified) {

            isOTPSent = false
            OTPVerified = false

            verificationID = ""
            otpCode = ""

            memberFirebaseUID = ""

            lastAttemptedOTP = ""

            verifyOTPError = ""
            sendOTPError = ""

            OTPProcessStarted = false
            isOTPVerificationInProgress = false

            SecondaryFirebaseAuthManager.getInstance(context)
                .signOut()
        }
    }


    // ---------------------------------------------------------
    // OTP VERIFICATION
    // ---------------------------------------------------------

    fun verifyMemberOTP() {

        if (OTPProcessStarted ||
            isOTPVerificationInProgress ||
            OTPVerified
        ) {
            return
        }

        if (verificationID.isBlank()) {

            verifyOTPError =
                "Invalid verification ID. Please request OTP again."

            return
        }

        if (otpCode.length != 6) {

            verifyOTPError = "Enter 6-digit OTP"

            return
        }

        OTPProcessStarted = true
        isOTPVerificationInProgress = true

        verifyOTPError = ""

        squadViewModel.setIsVerifyingOTP(true)

        SecondaryFirebaseAuthManager.getInstance(context)
            .verifyOTP(
                verificationId = verificationID,
                otp = otpCode,
                expectedPhoneNumber = "+91$phoneNumber"
            ) { result ->

                when {

                    result.isSuccess -> {

                        val verifiedUID = result.getOrNull()

                        if (verifiedUID.isNullOrBlank()) {

                            memberFirebaseUID = ""
                            OTPVerified = false

                            verifyOTPError =
                                "Unable to retrieve verified Firebase UID."

                            SecondaryFirebaseAuthManager
                                .getInstance(context)
                                .signOut()

                        } else {

                            memberFirebaseUID = verifiedUID

                            OTPVerified = true

                            verifyOTPError = ""

                            Log.d(
                                "AddMember",
                                "Member OTP verified successfully"
                            )

                            Log.d(
                                "AddMember",
                                "Member Firebase UID: $verifiedUID"
                            )

                            // Release secondary authentication session.
                            SecondaryFirebaseAuthManager
                                .getInstance(context)
                                .signOut()
                        }
                    }

                    else -> {

                        memberFirebaseUID = ""
                        OTPVerified = false

                        verifyOTPError =
                            result.exceptionOrNull()?.localizedMessage
                                ?: "Invalid OTP"

                        SecondaryFirebaseAuthManager
                            .getInstance(context)
                            .signOut()
                    }
                }

                OTPProcessStarted = false
                isOTPVerificationInProgress = false

                squadViewModel.setIsVerifyingOTP(false)
            }
    }


    // ---------------------------------------------------------
    // AUTOMATIC OTP VERIFICATION
    // ---------------------------------------------------------

    LaunchedEffect(otpCode, verificationID) {

        if (
            otpCode.length == 6 &&
            verificationID.isNotBlank() &&
            !OTPVerified &&
            !OTPProcessStarted &&
            lastAttemptedOTP != otpCode
        ) {

            lastAttemptedOTP = otpCode

            delay(250)

            verifyMemberOTP()
        }
    }


    // ---------------------------------------------------------
    // VALIDATE MEMBER DETAILS
    // ---------------------------------------------------------

    fun validateMemberDetails(): Boolean {

        memberNameError = ""
        phoneError = ""
        sendOTPError = ""

        val cleanedName = memberName.trim()

        val cleanedPhone =
            CommonFunctions.cleanUpPhoneNumber(phoneNumber)

        if (cleanedName.isBlank()) {

            memberNameError = SquadStrings.nameRequired

            return false
        }

        if (!Regex("^[0-9]{10}$").matches(cleanedPhone)) {

            phoneError =
                SquadStrings.enterValidPhoneNumber

            return false
        }

        if (!areFinancialAmountsValid) {

            sendOTPError =
                "Enter valid whole-number amounts."

            return false
        }

        val nameExists = squadMembers.any {

            it.localizedMemberName
                .trim()
                .equals(cleanedName, ignoreCase = true)
        }

        if (nameExists) {

            memberNameError =
                SquadStrings.nameAlreadyExists

            return false
        }

        val phoneExists = squadMembers.any {

            CommonFunctions.cleanUpPhoneNumber(
                it.phoneNumber
            ) == cleanedPhone
        }

        if (phoneExists) {

            phoneError =
                SquadStrings.mobileNumberAlreadyExists

            return false
        }

        return true
    }


    // ---------------------------------------------------------
    // SEND OTP
    // ---------------------------------------------------------

    fun sendMemberOTP() {

        if (!validateMemberDetails()) {
            return
        }

        if (activity == null) {

            sendOTPError =
                "Unable to access Activity for phone verification."

            return
        }

        val formattedPhone =
            "+91" + CommonFunctions.cleanUpPhoneNumber(phoneNumber)

        OTPProcessStarted = true

        isOTPSent = false
        OTPVerified = false

        memberFirebaseUID = ""

        verificationID = ""
        otpCode = ""

        lastAttemptedOTP = ""

        sendOTPError = ""
        verifyOTPError = ""

        squadViewModel.setIsSendingOTP(true)

        SecondaryFirebaseAuthManager.getInstance(context)
            .sendOTP(
                activity = activity,
                phoneNumber = formattedPhone,
                callback = object :
                    SecondaryFirebaseAuthManager.SendOTPCallback {

                    override fun onCodeSent(
                        verificationId: String
                    ) {

                        verificationID = verificationId

                        isOTPSent = true

                        OTPProcessStarted = false

                        squadViewModel.setIsSendingOTP(false)

                        sendOTPError = ""

                        Log.d(
                            "AddMember",
                            "OTP sent successfully"
                        )
                    }

                    override fun onAutoVerified(
                        firebaseUID: String
                    ) {

                        memberFirebaseUID = firebaseUID

                        OTPVerified = true

                        isOTPSent = true

                        OTPProcessStarted = false

                        squadViewModel.setIsSendingOTP(false)

                        verifyOTPError = ""

                        SecondaryFirebaseAuthManager
                            .getInstance(context)
                            .signOut()
                    }

                    override fun onFailure(
                        error: String
                    ) {

                        sendOTPError = error

                        isOTPSent = false
                        OTPVerified = false

                        memberFirebaseUID = ""

                        OTPProcessStarted = false

                        squadViewModel.setIsSendingOTP(false)

                        SecondaryFirebaseAuthManager
                            .getInstance(context)
                            .signOut()

                        Log.e(
                            "AddMember",
                            "OTP failed: $error"
                        )
                    }
                }
            )
    }


    // ---------------------------------------------------------
    // ADD MEMBER
    // ---------------------------------------------------------

    fun handleAddMember() {

        if (isSavingMember || OTPProcessStarted) {
            return
        }

        if (!validateMemberDetails()) {
            return
        }

        // First action: Send OTP.
        if (!isOTPSent) {

            sendMemberOTP()

            return
        }

        // OTP must be verified before adding member.
        if (!OTPVerified) {

            if (otpCode.length != 6) {

                verifyOTPError = "Enter 6-digit OTP"

                return
            }

            verifyMemberOTP()

            return
        }

        // UID must be available.
        if (memberFirebaseUID.isBlank()) {

            verifyOTPError =
                "Member Firebase UID is missing. Verify OTP again."

            return
        }

        val currentSquad = squad

        if (currentSquad == null) {

            sendOTPError = "Squad not found."

            return
        }

        // Subscription validation.
        if (
            !SubscriptionManager.shared.canAddMember(
                currentCount = currentSquad.totalMembers
            )
        ) {

            squadViewModel.setShowUpgradePlan(true)

            return
        }

        val cleanedName =
            CommonFunctions.cleanUpName(memberName)

        val cleanedPhone =
            CommonFunctions.cleanUpPhoneNumber(phoneNumber)

        val initialContribution =
            if (squadHasStarted) contributionAmount else 0

        val initialInterest =
            if (squadHasStarted) interestAmount else 0

        val currentMonthPaid =
            squadHasStarted && markCurrentMonthPaid

        isSavingMember = true

        squadViewModel.addMember(

            showLoader = true,

            name = cleanedName,

            phone = cleanedPhone,

            firebaseUID = memberFirebaseUID,

            contributionReceived = initialContribution,

            interestReceived = initialInterest,

            markCurrentMonthPaid = currentMonthPaid

        ) { success, message ->

            isSavingMember = false

            if (success) {
                LoaderManager.shared.hideLoader()
                SecondaryFirebaseAuthManager
                    .getInstance(context)
                    .signOut()
                onDismiss()

            } else {

                sendOTPError =
                    message ?: "Failed to add member."
            }
        }
    }


    // ---------------------------------------------------------
    // POPUP UI
    // ---------------------------------------------------------

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                enabled = !OTPProcessStarted && !isSavingMember,
                indication = null,
                interactionSource = remember {
                    androidx.compose.foundation.interaction
                        .MutableInteractionSource()
                }
            ) {
                SecondaryFirebaseAuthManager
                    .getInstance(context)
                    .signOut()

                onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.background)
                .border(
                    width = 1.dp,
                    color = AppColors.border.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(24.dp)
                )
                .heightIn(max = 720.dp)
        ) {

            // -------------------------------------------------
            // HEADER
            // -------------------------------------------------

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 18.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {

                    Text(
                        text = SquadStrings.addMember,
                        style = AppFont.ibmPlexSans(
                            20,
                            FontWeight.Bold
                        ),
                        color = AppColors.headerText
                    )

                    Text(
                        text = if (isOTPSent) {
                            SquadStrings.verifyMemberPhoneNumber
                        } else {
                            SquadStrings.addNewMemberToYourSquad
                        },
                        style = AppFont.ibmPlexSans(
                            12,
                            FontWeight.Normal
                        ),
                        color = AppColors.secondaryText
                    )
                }

                IconButton(
                    onClick = {

                        if (!OTPProcessStarted && !isSavingMember) {

                            SecondaryFirebaseAuthManager
                                .getInstance(context)
                                .signOut()

                            onDismiss()
                        }
                    }
                ) {

                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AppColors.headerText
                    )
                }
            }


            HorizontalDivider(
                color = AppColors.secondaryText.copy(alpha = 0.15f)
            )


            // -------------------------------------------------
            // SCROLLABLE CONTENT
            // -------------------------------------------------

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // MEMBER DETAILS

                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        Text(
                            text = SquadStrings.memberDetails,
                            style = AppFont.ibmPlexSans(
                                15,
                                FontWeight.Bold
                            ),
                            color = AppColors.headerText
                        )

                        Text(
                            text = SquadStrings.enterMemberInformation,
                            style = AppFont.ibmPlexSans(
                                11,
                                FontWeight.Normal
                            ),
                            color = AppColors.secondaryText
                        )
                    }

                    SSTextField(
                        icon = Icons.Default.Person,
                        placeholder = SquadStrings.memberName,
                        textState = remember {
                            mutableStateOf(memberName)
                        }.also { state ->

                            LaunchedEffect(state.value) {
                                memberName = state.value
                            }
                        },
                        keyboardType = KeyboardType.Text,
                        error = memberNameError
                    )

                    SSTextField(
                        icon = Icons.Default.Phone,
                        placeholder = SquadStrings.memberPhone,
                        textState = remember {
                            mutableStateOf(phoneNumber)
                        }.also { state ->

                            LaunchedEffect(state.value) {
                                phoneNumber = state.value
                            }
                        },
                        keyboardType = KeyboardType.Number,
                        error = phoneError,
                        maxLength = 10
                    )
                }


                // NON-PHONE MEMBER HINT

                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.Top
                ) {

                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = AppColors.secondaryText
                    )

                    Text(
                        text = SquadStrings.testPhoneHint,
                        style = AppFont.ibmPlexSans(
                            11,
                            FontWeight.Normal
                        ),
                        color = AppColors.secondaryText
                    )
                }


                // FINANCIAL DETAILS

                if (squadHasStarted) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                AppColors.secondaryText.copy(alpha = 0.035f)
                            )
                            .border(
                                1.dp,
                                AppColors.border.copy(alpha = 0.4f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {

                            Text(
                                text = SquadStrings.initialFinancialDetails,
                                style = AppFont.ibmPlexSans(
                                    15,
                                    FontWeight.Bold
                                ),
                                color = AppColors.headerText
                            )

                            Text(
                                text = SquadStrings.recordInitialAmountsReceived,
                                style = AppFont.ibmPlexSans(
                                    11,
                                    FontWeight.Normal
                                ),
                                color = AppColors.secondaryText
                            )
                        }


                        // CONTRIBUTION

                        SSTextField(
                            icon = Icons.Default.AccountBalanceWallet,
                            placeholder = SquadStrings.contributionReceived,
                            textState = remember {
                                mutableStateOf(contributionReceived)
                            }.also { state ->

                                LaunchedEffect(state.value) {
                                    contributionReceived = state.value
                                }
                            },
                            keyboardType = KeyboardType.Number,
                            error = ""
                        )


                        // INTEREST

                        SSTextField(
                            icon = Icons.Default.Payments,
                            placeholder = SquadStrings.interestReceived,
                            textState = remember {
                                mutableStateOf(interestReceived)
                            }.also { state ->

                                LaunchedEffect(state.value) {
                                    interestReceived = state.value
                                }
                            },
                            keyboardType = KeyboardType.Number,
                            error = ""
                        )


                        // CURRENT MONTH PAID

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    AppColors.primaryButton.copy(alpha = 0.06f)
                                )
                                .clickable {
                                    markCurrentMonthPaid =
                                        !markCurrentMonthPaid
                                }
                                .padding(13.dp),
                            verticalAlignment = Alignment.Top
                        ) {

                            Checkbox(
                                checked = markCurrentMonthPaid,
                                onCheckedChange = {
                                    markCurrentMonthPaid = it
                                }
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = SquadStrings.markCurrentMonthAsPaid,
                                    style = AppFont.ibmPlexSans(
                                        13,
                                        FontWeight.SemiBold
                                    ),
                                    color = AppColors.headerText
                                )

                                Text(
                                    text = SquadStrings.previousMonthsAutomaticallyPaid,
                                    style = AppFont.ibmPlexSans(
                                        11,
                                        FontWeight.Normal
                                    ),
                                    color = AppColors.secondaryText
                                )
                            }
                        }


                        // FINANCIAL SUMMARY

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    AppColors.secondaryText.copy(alpha = 0.06f)
                                )
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            FinancialPreviewRow(
                                title = SquadStrings.contributionReceived,
                                amount = contributionAmount
                            )

                            FinancialPreviewRow(
                                title = SquadStrings.interestReceived,
                                amount = interestAmount
                            )

                            HorizontalDivider()

                            FinancialPreviewRow(
                                title = SquadStrings.totalAmount,
                                amount = totalReceived,
                                isTotal = true
                            )
                        }
                    }
                }


                // OTP SECTION

                AnimatedVisibility(
                    visible = isOTPSent,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                AppColors.secondaryText.copy(alpha = 0.035f)
                            )
                            .padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {

                            Text(
                                text = SquadStrings.verifyOTP,
                                style = AppFont.ibmPlexSans(
                                    15,
                                    FontWeight.Bold
                                ),
                                color = AppColors.headerText
                            )

                            Text(
                                text = SquadStrings.enterOTP,
                                style = AppFont.ibmPlexSans(
                                    11,
                                    FontWeight.Normal
                                ),
                                color = AppColors.secondaryText
                            )
                        }

                        SSTextField(
                            icon = Icons.Default.Numbers,
                            placeholder = SquadStrings.enterOTP,
                            textState = remember {
                                mutableStateOf(otpCode)
                            }.also { state ->

                                LaunchedEffect(state.value) {

                                    val filtered = state.value
                                        .filter { it in '0'..'9' }
                                        .take(6)

                                    otpCode = filtered

                                    if (state.value != filtered) {
                                        state.value = filtered
                                    }
                                }
                            },
                            keyboardType = KeyboardType.Number,
                            dropdownIcon = if (OTPVerified) {
                                Icons.Default.CheckCircle
                            } else {
                                null
                            },
                            dropdownColor = AppColors.primaryButton,
                            error = verifyOTPError
                        )


                        if (OTPVerified) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AppColors.primaryButton,
                                    modifier = Modifier.size(17.dp)
                                )

                                Spacer(
                                    modifier = Modifier.width(7.dp)
                                )

                                Text(
                                    text = SquadStrings.otpVerified,
                                    style = AppFont.ibmPlexSans(
                                        12,
                                        FontWeight.Medium
                                    ),
                                    color = AppColors.primaryButton
                                )
                            }
                        }
                    }
                }


                // SEND OTP ERROR

                if (sendOTPError.isNotBlank()) {

                    Text(
                        text = sendOTPError,
                        color = AppColors.errorAccent,
                        style = AppFont.ibmPlexSans(
                            12,
                            FontWeight.Normal
                        )
                    )
                }


                // FINANCIAL VALIDATION ERROR

                if (!areFinancialAmountsValid) {

                    Text(
                        text = "Enter valid whole-number amounts.",
                        color = AppColors.errorAccent,
                        fontSize = 12.sp
                    )
                }
            }


            HorizontalDivider(
                color = AppColors.secondaryText.copy(alpha = 0.15f)
            )


            // -------------------------------------------------
            // FOOTER
            // -------------------------------------------------

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 18.dp,
                        vertical = 13.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                SSButton(
                    title = when {

                        OTPVerified ->
                            SquadStrings.addMember

                        isOTPSent ->
                            SquadStrings.addMember

                        else ->
                            SquadStrings.sendOTP
                    },
                    isButtonLoading = isSavingMember,
                    isDisabled =
                        OTPProcessStarted ||
                                isOTPVerificationInProgress ||
                                isSavingMember ||
                                !areFinancialAmountsValid ||
                                memberNameError.isNotEmpty() ||
                                phoneError.isNotEmpty()
                ) {

                    handleAddMember()
                }


                SSCancelButton(
                    title = SquadStrings.cancel,
                    action = {

                        if (!OTPProcessStarted && !isSavingMember) {

                            SecondaryFirebaseAuthManager
                                .getInstance(context)
                                .signOut()

                            onDismiss()
                        }
                    }
                )
            }
        }
    }


    // ---------------------------------------------------------
    // OTP OVERLAY
    // ---------------------------------------------------------

    val isSendingOTP by
    squadViewModel.isSendingOTP.collectAsState()

    val isVerifyingOTP by
    squadViewModel.isVerifyingOTP.collectAsState()

    when {

        isSendingOTP -> {

            OTPOverlayView(
                state = OTPOverlayState.SENDING
            )
        }

        isVerifyingOTP -> {

            OTPOverlayView(
                state = OTPOverlayState.VERIFYING
            )
        }
    }
}


// =====================================================================
// FINANCIAL PREVIEW ROW
// =====================================================================

@Composable
private fun FinancialPreviewRow(
    title: String,
    amount: Int,
    isTotal: Boolean = false
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = title,
            style = AppFont.ibmPlexSans(
                12,
                if (isTotal) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                }
            ),
            color = if (isTotal) {
                AppColors.headerText
            } else {
                AppColors.secondaryText
            }
        )

        Text(
            text = "₹${String.format("%,d", amount)}",
            style = AppFont.ibmPlexSans(
                13,
                FontWeight.Bold
            ),
            color = AppColors.headerText
        )
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
    title: String = SquadStrings.selectInstallment,
    loan: MemberLoan,
    installments: List<Installment>,
    interestType: InterestType,
    interestRate: Double,
    onSelect: (Installment) -> Unit,
    onForceClose: (ForceCloseSummary) -> Unit,
    onCancel: () -> Unit
) {
    var showForceCloseConfirm by remember { mutableStateOf(false) }
    val today = remember { Date() }

    // Locked whenever a force close verification is pending on this loan
    val isLocked = loan.isForceCloseVerification

    val forceCloseSummary = remember(loan, interestType, interestRate) {
        loan.forceCloseSummary(interestType, interestRate, today)
    }

    Box(contentAlignment = Alignment.Center) {

        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .heightIn(max = 500.dp)
                .graphicsLayer {
                    alpha = if (showForceCloseConfirm) 0.15f else 1f
                }
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.surface)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            // MARK: - Header
            Box(modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 14.dp)) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.headerText
                    )
                    Text(
                        text = SquadStrings.paymentsTotal(installments.size),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.secondaryText
                    )
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .size(28.dp)
                        .background(AppColors.surface.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = SquadStrings.close,
                        tint = AppColors.secondaryText,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // MARK: - Verification Pending Banner
            if (isLocked) {
                VerificationPendingBanner(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp)
                )
            }

            // MARK: - Force Close Banner
            if (installments.isNotEmpty()) {
                ForceCloseBanner(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp)
                        .graphicsLayer { alpha = if (isLocked) 0.6f else 1f },
                    onClick = {
                        if (!isLocked) showForceCloseConfirm = true
                    }
                )
            }

            HorizontalDivider(color = AppColors.secondaryText.copy(alpha = 0.15f))

            // MARK: - Installment List
            LazyColumn(
                modifier = Modifier
                    .heightIn(max = 280.dp)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(installments, key = { _, item -> item.id ?: item.installmentNumber }) { index, installment ->
                    val status = computedStatus(installment, today)
                    // 🔒 locked while force close verification pending
                    val isEnabled = !isLocked && isEnabled(installments, index, status)

                    InstallmentDetailsRow(
                        installment = installment,
                        status = status,
                        isEnabled = isEnabled,
                        index = index,
                        appearDelayMillis = index * 40L
                    ) {
                        if (isEnabled) {
                            onSelect(installment)
                        }
                    }
                }
            }

            HorizontalDivider(color = AppColors.secondaryText.copy(alpha = 0.15f))

            // MARK: - Footer
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(
                    text = SquadStrings.close,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.secondaryText,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }

        // MARK: - Force Close Confirmation Card
        AnimatedVisibility(
            visible = showForceCloseConfirm,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            ForceCloseConfirmationCard(
                summary = forceCloseSummary,
                interestType = interestType,
                onCancel = { showForceCloseConfirm = false },
                onConfirm = {

                    val hasVerificationPending = installments.any {
                        it.status == EMIStatus.INVERIFICATION
                    }

                    if (hasVerificationPending) {

                        AlertManager.shared.showAlert(
                            title = SquadStrings.verificationPending,
                            message = SquadStrings.forceCloseVerificationMessage,
                            type = AlertType.INFO
                        )

                    } else {

                        onForceClose(forceCloseSummary)
                        onCancel()
                    }

                }
            )
        }
    }
}

// MARK: - Verification Pending Banner
@Composable
private fun VerificationPendingBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF2196F3).copy(alpha = 0.08f))
            .border(1.dp, Color(0xFF2196F3).copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFF2196F3).copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.HourglassEmpty,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(16.dp)
            )
        }

        Column {
            Text(
                text = SquadStrings.forceCloseVerificationPending,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.headerText
            )
            Text(
                text = SquadStrings.emiSelectionAndForceCloseLocked,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.secondaryText
            )
        }
    }
}

@Composable
private fun ForceCloseBanner(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFFFF9800).copy(alpha = 0.12f), Color(0xFFFF9800).copy(alpha = 0.05f))
                )
            )
            .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFFFF9800).copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(16.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = SquadStrings.forceCloseLoan,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.headerText
            )
            Text(
                text = SquadStrings.payOffEarlyWithUpdatedInterest,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.secondaryText
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = AppColors.secondaryText.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun isEnabled(installments: List<Installment>, index: Int, status: EMIStatus): Boolean {
    if (status != EMIStatus.PENDING) return false
    if (index == 0) return true
    return installments[index - 1].status == EMIStatus.PAID
}

@Composable
private fun ForceCloseConfirmationCard(
    summary: ForceCloseSummary,
    interestType: InterestType,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = 320.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.surface)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = SquadStrings.forceCloseLoan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.headerText
        )

        Text(
            text = SquadStrings.interestRecalculatedAsOfToday(interestType.localizedName,summary.daysElapsed),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.secondaryText,
            textAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AppColors.surface.copy(alpha = 0.6f))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryRow(SquadStrings.outstandingPrincipal, summary.outstandingPrincipal)
            SummaryRow(SquadStrings.recalculatedInterest, summary.recalculatedInterest)
            HorizontalDivider()
            SummaryRow(SquadStrings.totalPayable, summary.totalPayable, bold = true)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.surface.copy(alpha = 0.4f))
            ) {
                Text(SquadStrings.cancel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.secondaryText)
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(SquadStrings.confirm, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: Int, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) AppColors.headerText else AppColors.secondaryText
        )
        Text(
            text = "₹$value",
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = AppColors.headerText
        )
    }
}

@Composable
private fun InstallmentDetailsRow(
    installment: Installment,
    status: EMIStatus,
    isEnabled: Boolean,
    index: Int,
    appearDelayMillis: Long,
    onClick: () -> Unit
) {
    val isPaid = installment.status == EMIStatus.PAID

    // Staggered fade/slide-in, mirrors the SwiftUI appear animation
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(appearDelayMillis)
        appeared = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "rowAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (appeared) 0f else 8f,
        animationSpec = tween(durationMillis = 350),
        label = "rowOffset"
    )

    // Gentle press-scale, mirrors PressableStyle
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "pressScale"
    )

    // 🔹 FIXED — mirrors iOS's unconditional `.scaleEffect(isEnabled ? 1.0 : 0.99)`,
    // independent of the press-scale above. Combined multiplicatively below.
    val disabledScale = if (isEnabled) 1f else 0.99f

    val rowAlpha = if (isEnabled || isPaid) 1f else 0.55f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha * rowAlpha
                translationY = offsetY
                scaleX = pressScale * disabledScale
                scaleY = pressScale * disabledScale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.surface)
            .border(
                width = 1.dp,
                color = if (isEnabled) AppColors.headerText.copy(alpha = 0.08f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (isEnabled) Modifier.appShadow(AppShadows.card) else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isEnabled,
                onClick = onClick
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top row: badge, number/amount, status
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number badge / paid checkmark
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPaid) Brush.linearGradient(
                            listOf(Color(0xFF34C759).copy(alpha = 0.15f), Color(0xFF34C759).copy(alpha = 0.15f))
                        )
                        else Brush.linearGradient(
                            listOf(
                                AppColors.headerText.copy(alpha = 0.12f),
                                AppColors.headerText.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isPaid) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = SquadStrings.paid,
                        tint = Color(0xFF34C759),
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Text(
                        text = "${index + 1}",
                        style = AppFont.ibmPlexSans(14, FontWeight.Bold),
                        color = AppColors.headerText.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = installment.installmentNumber,
                    style = AppFont.ibmPlexSans(12, FontWeight.Medium),
                    color = AppColors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = installment.installmentAmount.currencyFormattedWithCommas(),
                    style = AppFont.ibmPlexSans(18, FontWeight.Bold),
                    color = AppColors.headerText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusBadge(status = status)

                if (isEnabled) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = AppColors.headerText.copy(alpha = 0.35f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Detail chips: two fixed-weight columns so widths never
        // renegotiate mid-animation (avoids any "shaking" text)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DetailChip(
                icon = Icons.Default.Percent, // 🔹 FIXED — was Icons.Default.Info, iOS uses "percent"
                caption = SquadStrings.interest,
                value = installment.interestAmount.currencyFormattedWithCommas(),
                modifier = Modifier.weight(1f)
            )

            installment.dueDate?.let {
                DetailChip(
                    icon = Icons.Default.DateRange,
                    caption = SquadStrings.dueDate,
                    value = SquadStrings.localizedMonthInText(CommonFunctions.dateToString(it.toDate(), format = "MMM dd yyyy")),
                    modifier = Modifier.weight(1f)
                )
            } ?: Spacer(modifier = Modifier.weight(1f))
        }
    }
}



@Composable
private fun DetailChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    caption: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.secondaryText.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.secondaryText.copy(alpha = 0.7f),
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = caption,
                style = AppFont.ibmPlexSans(9, FontWeight.SemiBold),
                color = AppColors.secondaryText.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = AppFont.ibmPlexSans(12, FontWeight.SemiBold),
                color = AppColors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatusBadge(status: EMIStatus) {
    Text(
        text = status.localizedName,
        style = AppFont.ibmPlexSans(10, FontWeight.Bold),
        color = AppColors.primaryButtonText,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(status.color)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    )
}

/// Auto-detect overdue if pending and past due date
private fun computedStatus(installment: Installment, today: Date): EMIStatus {
    val dueDate = installment.dueDate?.toDate()
    return if (installment.status == EMIStatus.PENDING && dueDate != null && dueDate.before(today)) {
        EMIStatus.OVERDUE
    } else {
        installment.status
    }
}


private data class MenuItem(
    val icon: ImageVector,
    val title: String,
    val action: () -> Unit
)

object AppIcons {
    fun resolve(name: String): Int = when (name) {
        "book_fill" -> R.drawable.squad_activity_unselected
        "list_bullet" -> R.drawable.squad_activity_unselected
        "creditcard_fill" -> R.drawable.squad_activity_unselected
        else -> R.drawable.squad_activity_selected
    }
}

@Composable
fun EditAmountPopup(
    squadViewModel: SquadViewModel,
    phoneNumber: String,
    currentAmount: Int,
    onDismiss: () -> Unit,
    updateAmountAction: (Int) -> Unit
) {

    val context = LocalContext.current
    val activity = context as Activity

    val otpCode = remember { mutableStateOf("") }
    val amount = remember { mutableStateOf("") }

    var verificationID by remember { mutableStateOf("") }

    var isOTPSent by remember { mutableStateOf(false) }
    var otpVerified by remember { mutableStateOf(false) }
    var otpProcessStarted by remember { mutableStateOf(false) }

    var otpError by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf("") }

    fun sendOTP() {

        squadViewModel.setIsSendingOTP(true)

        otpProcessStarted = true

        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber("+91$phoneNumber")
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(
                    object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                        override fun onVerificationCompleted(
                            credential: PhoneAuthCredential
                        ) {
                        }

                        override fun onVerificationFailed(
                            e: FirebaseException
                        ) {

                            squadViewModel.setIsSendingOTP(false)
                            otpProcessStarted = false
                            otpError = e.localizedMessage ?: "Failed to send OTP"
                        }

                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken
                        ) {

                            squadViewModel.setIsSendingOTP(false)
                            otpProcessStarted = false

                            verificationID = verificationId
                            isOTPSent = true
                        }
                    }
                )
                .build()
        )
    }

    fun verifyOTP() {

        if (otpCode.value.length != 6) {

            otpError = SquadStrings.enterValidOtp
            return
        }


        squadViewModel.setIsVerifyingOTP(true)

        val credential =
            PhoneAuthProvider.getCredential(
                verificationID,
                otpCode.value
            )

        FirebaseAuth.getInstance()
            .signInWithCredential(credential)
            .addOnCompleteListener { task ->

                squadViewModel.setIsVerifyingOTP(false)

                if (task.isSuccessful) {

                    amount.value = currentAmount.toString()
                    otpVerified = true

                } else {

                    otpError =
                        task.exception?.localizedMessage
                            ?: SquadStrings.invalidOTP
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {

        Column(
            modifier = Modifier
                .background(
                    AppColors.background,
                    RoundedCornerShape(20.dp)
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = SquadStrings.editAmount,
                style = AppFont.ibmPlexSans(20, FontWeight.Bold),
                color = AppColors.headerText,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (!otpVerified) {

                Text(
                    text = SquadStrings.otpWillSendTo(phoneNumber),
                    style = AppFont.ibmPlexSans(14, FontWeight.Normal),
                    color = AppColors.secondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isOTPSent) {

                    SSTextField(
                        icon = Icons.Default.Lock,
                        placeholder = SquadStrings.enterOTP,
                        textState = otpCode,
                        keyboardType = KeyboardType.Number,
                        showDropdown = otpVerified,
                        dropdownIcon = Icons.Default.CheckCircle,
                        dropdownColor = AppColors.primaryButton,
                        error = otpError
                    )

                    LaunchedEffect(otpCode.value) {

                        otpError = ""

                        if (otpCode.value.length == 6) {

                            delay(500)
                            verifyOTP()
                        }
                    }
                }

            } else {

                Text(
                    text = SquadStrings.otpVerified,
                    style = AppFont.ibmPlexSans(
                        14,
                        FontWeight.Medium
                    ),
                    color = Color.Green,
                    modifier = Modifier.align(
                        Alignment.CenterHorizontally
                    )
                )

                SSTextField(
                    icon = Icons.Default.Edit,
                    placeholder = SquadStrings.enterAmount,
                    textState = amount,
                    keyboardType = KeyboardType.Number,
                    error = amountError
                )
            }

            SSButton(
                title = when {
                    otpVerified -> SquadStrings.updateAmount
                    isOTPSent -> SquadStrings.verifyOTP
                    else -> SquadStrings.sendOTP
                },
                isDisabled = otpProcessStarted
            ) {

                when {

                    otpVerified -> {

                        amountError = ""

                        if (amount.value.isBlank()) {

                            amountError = SquadStrings.amountIsRequired
                            return@SSButton
                        }

                        val amountInt =
                            amount.value.toIntOrNull()

                        if (amountInt == null) {

                            amountError = SquadStrings.amountIsRequired
                            return@SSButton
                        }

                        updateAmountAction(amountInt)
                        onDismiss()
                    }

                    isOTPSent -> {

                        verifyOTP()
                    }

                    else -> {

                        sendOTP()
                    }
                }
            }

            SSCancelButton(
                title = SquadStrings.cancel
            ) {
                onDismiss()
            }
        }
    }
}

@Composable
fun RequestNotificationPermission() {

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("PERMISSION", "Notification granted")
        } else {
            Log.d("PERMISSION", "Notification denied")
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            val permission = Manifest.permission.POST_NOTIFICATIONS

            val isGranted = ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED

            if (!isGranted) {
                launcher.launch(permission)
            }
        }
    }
}

@Composable
fun ShimmerLoader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 16.dp)
            .background(Color.Gray.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
    )
}

@Composable
fun SSBadge(
    title: String,
    value: String,
    icon: String? = null,
    style: BadgeStyle = BadgeStyle.PRIMARY
) {
    val colors = getBadgeColors(style)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = colors.background,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = colors.border,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {

        icon?.let {
            Text(
                text = it,
                style = AppFont.ibmPlexSans(
                    size = 12,
                    weight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text = title,
            style = AppFont.ibmPlexSans(
                size = 10,
                weight = FontWeight.Medium
            ),
            color = AppColors.secondaryText
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = value,
            style = AppFont.ibmPlexSans(
                size = 12,
                weight = FontWeight.Bold
            ),
            color = colors.text
        )
    }
}

enum class BadgeStyle {
    PRIMARY,
    SECONDARY,
    SUCCESS,
    WARNING,
    ERROR,
    INFO
}

data class BadgeColors(
    val text: Color,
    val background: Color,
    val border: Color
)

fun getBadgeColors(style: BadgeStyle): BadgeColors {
    return when (style) {

        BadgeStyle.PRIMARY -> BadgeColors(
            text = AppColors.primaryBrand,
            background = AppColors.primaryBackground,
            border = AppColors.primaryBrand.copy(alpha = 0.3f)
        )

        BadgeStyle.SECONDARY -> BadgeColors(
            text = AppColors.secondaryAccent,
            background = AppColors.secondaryBackground,
            border = AppColors.secondaryAccent.copy(alpha = 0.3f)
        )

        BadgeStyle.SUCCESS -> BadgeColors(
            text = AppColors.successAccent,
            background = AppColors.successAccent.copy(alpha = 0.1f),
            border = AppColors.successAccent.copy(alpha = 0.3f)
        )

        BadgeStyle.WARNING -> BadgeColors(
            text = AppColors.warningAccent,
            background = AppColors.warningAccent.copy(alpha = 0.1f),
            border = AppColors.warningAccent.copy(alpha = 0.3f)
        )

        BadgeStyle.ERROR -> BadgeColors(
            text = AppColors.errorAccent,
            background = AppColors.errorAccent.copy(alpha = 0.1f),
            border = AppColors.errorAccent.copy(alpha = 0.3f)
        )

        BadgeStyle.INFO -> BadgeColors(
            text = AppColors.infoAccent,
            background = AppColors.infoAccent.copy(alpha = 0.1f),
            border = AppColors.infoAccent.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun RemindAllButton(
    count: Int,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.primaryButton
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
    ) {

        Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = SquadStrings.remindAll,
            style = AppFont.ibmPlexSans(14, FontWeight.SemiBold),
            color = Color.White
        )

        if (count > 0) {

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .background(Color.White, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$count",
                    style = AppFont.ibmPlexSans(12, FontWeight.Bold),
                    color = AppColors.primaryButton
                )
            }
        }
    }
}

@Composable
fun SetSystemBars() {
    val view = LocalView.current

    SideEffect {
        val window = (view.context as Activity).window

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, view)

        // IMPORTANT: THIS controls white vs dark icon behavior
        controller.isAppearanceLightStatusBars = false

        // This is required for full transparency behavior on some OEMs
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun CheckDuesButtonPreview() {

    CheckDuesButton(

        modifier = Modifier.padding(bottom = 20.dp),

        onClick = {


        }

    )
}

@Composable
fun CheckDuesButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = ""
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .appShadow(
                style = AppShadows.card,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.surface
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        AppColors.warningAccent.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    tint = AppColors.warningAccent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = SquadStrings.checkDues,
                    style = AppFont.ibmPlexSans(
                        16,
                        FontWeight.Bold
                    ),
                    color = AppColors.headerText
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = SquadStrings.viewPendingContributionsAndEMIDetails,
                    style = AppFont.ibmPlexSans(12),
                    color = AppColors.secondaryText,
                    maxLines = 2
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = AppColors.secondaryText.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SSSearchField(
    placeHolder: String,
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
                    placeHolder,
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
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
fun SSStatusMenuButton(
    current: RecordStatus,
    onSelect: (RecordStatus) -> Unit
)
{

    var expanded by remember { mutableStateOf(false) }

    Box {

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(current.tintColor().copy(alpha = 0.1f))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(current.tintColor(), CircleShape)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(text= current.localizedName,style = AppFont.ibmPlexSans(10, FontWeight.Bold))

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
                            Text(status.localizedName)
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

@Composable
fun LanguageDropDownView(
    squadViewModel: SquadViewModel
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    val selectedLanguage by squadViewModel.language.collectAsState()

    Box {

        OutlinedButton(
            onClick = {
                expanded = true
            },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(
                horizontal = 12.dp,
                vertical = 8.dp
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {

            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = selectedLanguage.value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(
                modifier = Modifier.width(4.dp)
            )

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {

            SquadLanguages.entries.forEach { language ->

                DropdownMenuItem(
                    text = {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(language.value)

                            Spacer(
                                modifier = Modifier.weight(1f)
                            )

                            if (language == selectedLanguage) {

                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AppColors.primaryButton
                                )
                            }
                        }
                    },
                    onClick = {

                        expanded = false

                        squadViewModel.setLanguage(language)
                    }
                )
            }
        }
    }
}

private val SquadToggleGreen = Color(0xFF16856B)

@Composable
fun SquadToggleView(

    title: String,

    subtitle: String,

    isOn: Boolean,

    onToggleChange: (Boolean) -> Unit,

    modifier: Modifier = Modifier

) {

    val shape = RoundedCornerShape(14.dp)

    Row(

        modifier = modifier

            .fillMaxWidth()

            .background(

                color = MaterialTheme.colorScheme.surface,

                shape = shape

            )

            .border(

                width = 1.dp,

                color = MaterialTheme.colorScheme.outlineVariant.copy(

                    alpha = 0.7f

                ),

                shape = shape

            )

            .padding(16.dp),

        horizontalArrangement = Arrangement.SpaceBetween,

        verticalAlignment = Alignment.CenterVertically

    ) {

        // Title and subtitle

        Column(

            modifier = Modifier.weight(1f),

            verticalArrangement = Arrangement.spacedBy(5.dp)

        ) {

            Text(

                text = title,

                style = MaterialTheme.typography.bodyMedium.copy(

                    fontSize = 14.sp,

                    fontWeight = FontWeight.SemiBold

                ),

                color = MaterialTheme.colorScheme.onSurface

            )

            Text(

                text = subtitle,

                style = MaterialTheme.typography.bodySmall.copy(

                    fontSize = 12.sp

                ),

                color = MaterialTheme.colorScheme.onSurfaceVariant

            )

        }

        Spacer(modifier = Modifier.width(8.dp))

        // Toggle switch

        Switch(

            checked = isOn,

            onCheckedChange = onToggleChange,

            colors = SwitchDefaults.colors(

                checkedThumbColor = Color.White,

                checkedTrackColor = SquadToggleGreen,

                checkedBorderColor = SquadToggleGreen

            )

        )

    }

}