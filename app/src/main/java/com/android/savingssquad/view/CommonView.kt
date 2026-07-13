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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
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
import com.android.savingssquad.model.Member
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.LoaderManager
import com.android.savingssquad.singleton.RecordStatus
import com.android.savingssquad.singleton.color
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.singleton.displayText
import com.android.savingssquad.viewmodel.AppDestination
import com.google.firebase.auth.PhoneAuthOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


// ---------- SSNavigationBar ----------
@Composable
fun SSNavigationBar(
    title: String,
    navController: NavController?, // pass rememberNavController() from caller
    showBackButton: Boolean = true,
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
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
    dropdownIcon: ImageVector = Icons.Default.ArrowDropDown,
    dropdownColor: Color = AppColors.secondaryText,
    onDropdownTap: (() -> Unit)? = null,
    error: String = ""
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
        when {
            disabled -> AppColors.secondaryText.copy(alpha = 0.45f)
            isFocused -> AppColors.primaryButton
            else -> AppColors.secondaryText
        },
        label = ""
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .appShadow(AppShadows.card, RoundedCornerShape(16.dp))
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

                    // ✅ PREMIUM PLACEHOLDER (always inside)
                    if (textState.value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = AppFont.ibmPlexSans(15, FontWeight.Normal),
                            color = if (disabled)
                                AppColors.placeholderText.copy(alpha = 0.6f)
                            else
                                AppColors.placeholderText
                        )
                    }

                    BasicTextField(
                        value = textState.value,
                        onValueChange = {
                            if (!disabled) textState.value = it
                        },
                        singleLine = true,
                        enabled = !disabled,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        cursorBrush = SolidColor(AppColors.primaryButton),
                        textStyle = AppFont.ibmPlexSans(
                            15,
                            FontWeight.Medium
                        ).copy(
                            color = if (disabled)
                                AppColors.secondaryText.copy(alpha = 0.7f)
                            else
                                AppColors.headerText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                isFocused = it.isFocused && !disabled
                            }
                    )
                }

                // 🔽 Dropdown / Loader
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

                        Icon(
                            imageVector = dropdownIcon,
                            contentDescription = null,
                            tint = dropdownColor,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { onDropdownTap?.invoke() }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))
            }
        }

        // ❌ ERROR TEXT
        AnimatedVisibility(error.isNotEmpty()) {
            Text(
                text = error,
                style = AppFont.ibmPlexSans(12, FontWeight.Normal),
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
                color = AppColors.headerText
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
                                text = "No results found",
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
                                        text = value,
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
                text = "Months",
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
fun SSLoaderView(
    loaderManager: LoaderManager = LoaderManager.shared
) {
    val isLoading = loaderManager.isLoading
    val loadingMessage = loaderManager.loadingMessage

    var rotationAngle by remember { mutableStateOf(0f) }

    // 🔹 Animate rotation continuously
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
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .pointerInput(Unit) {} // disable user interaction
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .appShadow(AppShadows.elevated, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.background)
                    .padding(vertical = 20.dp, horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // 🔹 Rotating circular stroke
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

                    // 🔹 Currency symbol rotating in opposite direction
                    Text(
                        text = "₹",
                        style = AppFont.ibmPlexSans(20, FontWeight.Bold),
                        color = AppColors.loaderColor,
                        modifier = Modifier.graphicsLayer(rotationZ = -rotationAngle)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = loadingMessage,
                    style = AppFont.ibmPlexSans(14, FontWeight.SemiBold),
                    color = AppColors.headerText
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
    users: List<Login>
) {
    if (!isVisible) return

    val inPreview = LocalInspectionMode.current

    var selectedRole by remember {
        mutableStateOf(SquadUserType.SQUAD_MANAGER.roleDescription)
    }

    val managers = remember(users) {
        users.filter { it.role == SquadUserType.SQUAD_MANAGER }
    }

    val members = remember(users) {
        users.filter { it.role == SquadUserType.SQUAD_MEMBER }
    }

    val filteredUsers =
        if (selectedRole == SquadUserType.SQUAD_MANAGER.roleDescription)
            managers else members

    // 🔹 BACKDROP (iOS style)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable { onDismiss() }
    ) {

        // 🔹 MODAL CARD
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .width(340.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.background)
                .appShadow(AppShadows.elevated, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {

            // 🔹 TITLE
            Text(
                text = SquadStrings.selectSquad,
                style = AppFont.ibmPlexSans(20, FontWeight.Bold),
                color = AppColors.headerText
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 🔹 SEGMENTED CONTROL
            ModernSegmentedPickerView(
                segments = listOf(
                    SquadUserType.SQUAD_MANAGER.roleDescription,
                    SquadUserType.SQUAD_MEMBER.roleDescription
                ),
                selectedSegment = selectedRole,
                onSegmentSelected = { selectedRole = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 🔹 USERS
            if (filteredUsers.isEmpty()) {

                Text(
                    text = "No ${selectedRole}s available",
                    style = AppFont.ibmPlexSans(14, FontWeight.Medium),
                    color = AppColors.secondaryText,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

            } else {

                LazyColumn(
                    modifier = Modifier.heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(filteredUsers) { user ->

                        UserSelectionCard(
                            user = user,
                            onSelect = {

                                onUserSelected(user)

                                if (!inPreview) {

                                    UserDefaultsManager.saveLogin(user)
                                    UserDefaultsManager.saveIsLoggedIn(true)

                                    val isManager =
                                        user.role == SquadUserType.SQUAD_MANAGER

                                    UserDefaultsManager.saveSquadManagerLogged(isManager)
                                    val route = if (isManager)
                                        AppDestination.MANAGER_HOME.route
                                    else
                                        AppDestination.MEMBER_HOME.route

                                    navController.navigate(route) {
                                        popUpTo(AppDestination.SIGN_IN.route) {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                }

                                onDismiss()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 🔹 CANCEL
            SSCancelButton(
                title = SquadStrings.cancel,
                isButtonLoading = false,
                isDisabled = false
            ) {
                onDismiss()
            }
        }
    }
}


@Composable
fun UserSelectionCard(
    user: Login,
    onSelect: () -> Unit
) {

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                Text(
                    text = user.squadName ?: "",
                    style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                    color = AppColors.headerText
                )

                Text(
                    text =
                        if (user.role == SquadUserType.SQUAD_MANAGER)
                            "Manager since ${
                                CommonFunctions.dateToString(
                                    user.userCreatedDate?.toDate() ?: Date()
                                )
                            }"
                        else
                            "Member since ${
                                CommonFunctions.dateToString(
                                    user.userCreatedDate?.toDate() ?: Date()
                                )
                            }",
                    style = AppFont.ibmPlexSans(13),
                    color = AppColors.secondaryText
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AppColors.secondaryText.copy(alpha = 0.6f)
            )
        }
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
    val transition = updateTransition(selectedSegment, label = "segment")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .appShadow(AppShadows.card)
            .background(
                AppColors.secondaryText.copy(alpha = 0.08f),
                RoundedCornerShape(14.dp)
            )
            .border(
                0.8.dp,
                AppColors.border.copy(alpha = 0.4f),
                RoundedCornerShape(14.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        segments.forEach { segment ->

            val isSelected = segment == selectedSegment

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSegmentSelected(segment) },
                contentAlignment = Alignment.Center
            ) {

                // ================= SLIDING PILL =================
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
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
                }

                // ================= LABEL =================
                Text(
                    text = segment,
                    style = AppFont.ibmPlexSans(13, FontWeight.Medium),
                    color = if (isSelected)
                        AppColors.primaryButtonText
                    else
                        AppColors.secondaryText
                )
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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // ✅ State variables as MutableState
    val memberNameState = remember { mutableStateOf("") }
    val phoneNumberState = remember { mutableStateOf("") }
    val otpCodeState = remember { mutableStateOf("") }
    val verificationIDState = remember { mutableStateOf("") }

    var sendOTPLoading by remember { mutableStateOf(false) }
    var verifyOTPLoading by remember { mutableStateOf(false) }
    var isOTPSent by remember { mutableStateOf(false) }
    var otpVerified by remember { mutableStateOf(false) }
    var otpProcessStarted by remember { mutableStateOf(false) }

    var memberNameError by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf("") }
    var sendOTPError by remember { mutableStateOf("") }
    var verifyOTPError by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    // ----------------- Validation -----------------
    fun validateFields(): Boolean {
        memberNameError = if (memberNameState.value.trim().isEmpty()) "Name is required" else ""
        phoneError = if (Regex("^[0-9]{10}$").matches(phoneNumberState.value))
            "" else "Enter a valid 10-digit phone number"

        return memberNameError.isEmpty() && phoneError.isEmpty()
    }

    fun handleMemberNameChange(
        newValue: String,
        setError: (String) -> Unit,
        squadMembers: List<Member>,   // list from ViewModel
        coroutineScope: CoroutineScope,
        debounceJob: Job?,
        onUpdateDebounceJob: (Job?) -> Unit
    ) {
        // Clear error immediately (like Swift)
        setError("")

        // Cancel previous debounce job
        debounceJob?.cancel()

        // Create new debounce job
        val newJob = coroutineScope.launch {
            delay(500)

            val cleanedName = CommonFunctions.cleanUpName(newValue)

            val exists = squadMembers
                .map { it.name.trim().lowercase() }
                .contains(cleanedName.trim().lowercase())

            if (exists) {
                setError("Name already exists")
            }
        }

        // Update external reference
        onUpdateDebounceJob(newJob)
    }

    fun handlePhoneNameChange(
        newValue: String,
        setError: (String) -> Unit,
        squadMembers: List<Member>,   // list from ViewModel
        coroutineScope: CoroutineScope,
        debounceJob: Job?,
        onUpdateDebounceJob: (Job?) -> Unit
    )
    {
        // Clear error immediately (like Swift)
        setError("")

        // Cancel previous debounce job
        debounceJob?.cancel()

        // Create new debounce job
        val newJob = coroutineScope.launch {
            delay(500)

            val cleanedName = CommonFunctions.cleanUpPhoneNumber(newValue)

            val exists = squadMembers
                .map { it.phoneNumber.trim().lowercase() }
                .contains(cleanedName.trim().lowercase())

            if (exists) {
                setError("Mobile name already exists")
            }
        }

        // Update external reference
        onUpdateDebounceJob(newJob)
    }

    // ----------------- Handle Add Member -----------------
    fun handleAddMember() {
        if (!validateFields()) return

        if (otpVerified) {

            val squad = squadViewModel.squad.value ?: return

            val currentCount = squad.totalMembers

            if (!SubscriptionManager.shared.canAddMember(currentCount)) {
                LoaderManager.shared.hideLoader()
                squadViewModel.setShowUpgradePlan(true)
                return
            }

            val name = CommonFunctions.cleanUpName(memberNameState.value)
            val phone = CommonFunctions.cleanUpPhoneNumber(phoneNumberState.value)
            LoaderManager.shared.showLoader()
            squadViewModel.addMember(true, name, phone) { success, error ->
                LoaderManager.shared.hideLoader()
                onDismiss()
            }
        } else {
            // ✅ OTP not yet verified -> send OTP
            val phoneWithCode = "+91${phoneNumberState.value}"
            sendOTPLoading = true
            otpProcessStarted = true

            val auth = FirebaseAuth.getInstance()
            val activity = context as? Activity
            if (activity != null) {
                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneWithCode)       // your phone number with country code
                    .setTimeout(60L, TimeUnit.SECONDS)       // timeout
                    .setActivity(activity)                   // required for verification
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            otpVerified = true
                            verifyOTPError = ""
                            verifyOTPLoading = false
                            Log.d("AddMember", "OTP auto-verified")
                        }

                        override fun onVerificationFailed(e: FirebaseException) {
                            sendOTPError = e.localizedMessage ?: "OTP failed"
                            sendOTPLoading = false
                            otpProcessStarted = false
                            Log.d("AddMember", "OTP verification failed: ${e.localizedMessage}")
                        }

                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken
                        ) {
                            verificationIDState.value = verificationId
                            sendOTPLoading = false
                            otpProcessStarted = false
                            isOTPSent = true
                            Log.d("AddMember", "OTP code sent: $verificationId")
                        }
                    })
                    .build()

                PhoneAuthProvider.verifyPhoneNumber(options)
            }
        }
    }

    // ----------------- Handle OTP Change -----------------
    fun handleOTPChange(newValue: String) {
        otpCodeState.value = newValue
        verifyOTPError = ""
        if (newValue.length == 6) {
            verifyOTPLoading = true
            val credential = PhoneAuthProvider.getCredential(verificationIDState.value, newValue)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    verifyOTPLoading = false
                    otpVerified = task.isSuccessful
                    if (!task.isSuccessful) {
                        verifyOTPError = task.exception?.localizedMessage ?: "Verification failed"
                    }
                }
        }
    }

    // ----------------- UI -----------------
    AnimatedVisibility(
        visible = true,
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
            Text(
                text = "Add Member",
                style = AppFont.ibmPlexSans(20, FontWeight.Bold),
                color = AppColors.headerText,
                modifier = Modifier.padding(top = 10.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Member Name
                SSTextField(
                    icon = Icons.Default.Person,
                    placeholder = "Member Name",
                    textState = memberNameState,
                    error = memberNameError
                )

                LaunchedEffect(memberNameState.value) {
                    handleMemberNameChange(
                        newValue = memberNameState.value,
                        setError = { memberNameError = it },
                        squadMembers = squadViewModel.squadMembers.value,
                        coroutineScope = coroutineScope,
                        debounceJob = debounceJob,
                        onUpdateDebounceJob = { debounceJob = it }
                    )
                }

                // Phone Number
                SSTextField(
                    icon = Icons.Default.Phone,
                    placeholder = "Member Mobile.No",
                    textState = phoneNumberState,
                    keyboardType = KeyboardType.Number,
                    showDropdown = sendOTPLoading,
                    isLoading = sendOTPLoading,
                    error = phoneError
                )

                LaunchedEffect(phoneNumberState.value) {
                    handlePhoneNameChange(
                        newValue = phoneNumberState.value,
                        setError = { phoneError = it },
                        squadMembers = squadViewModel.squadMembers.value,
                        coroutineScope = coroutineScope,
                        debounceJob = debounceJob,
                        onUpdateDebounceJob = { debounceJob = it }
                    )
                }

                // OTP field
                if (isOTPSent) {
                    // Animated appearance like SwiftUI transition
                    AnimatedVisibility(
                        visible = isOTPSent,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { -40 })
                    ) {
                        SSTextField(
                            icon = Icons.Default.Numbers,
                            placeholder = "Enter OTP",
                            textState = otpCodeState,
                            keyboardType = KeyboardType.Number,
                            showDropdown = verifyOTPLoading || otpVerified,
                            dropdownIcon = Icons.Default.CheckCircle,
                            dropdownColor = AppColors.primaryButton,
                            isLoading = verifyOTPLoading,
                            error = verifyOTPError,
                            onDropdownTap = null
                        )

                        // Observe OTP changes like SwiftUI's .onChange
                        LaunchedEffect(otpCodeState.value) {
                            handleOTPChange(otpCodeState.value)
                        }
                    }
                }
            }

            // Button
            SSButton(
                title = if (otpVerified) "Add Member" else "Send OTP",
                isDisabled = (otpProcessStarted || sendOTPLoading) || phoneError.isNotEmpty() || memberNameError.isNotEmpty() || verifyOTPLoading,
                action = { handleAddMember() }
            )

            // Cancel
            SSCancelButton(
                title = SquadStrings.cancel,
                isButtonLoading = false,
                isDisabled = false,
                action = { onDismiss() }
            )
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
    onCancel: () -> Unit
) {
    val today = remember { Date() }

    Box(
        modifier = Modifier
            .padding(16.dp)
            .widthIn(max = 360.dp)
            .heightIn(max = 470.dp)
            .clip(RoundedCornerShape(24.dp))
            .appShadow(AppShadows.elevated)
            .background(AppColors.surface)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 🔹 Header
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 22.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = AppFont.ibmPlexSans(20, FontWeight.Bold),
                        color = AppColors.headerText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${installments.size} payment${if (installments.size == 1) "" else "s"} total",
                        style = AppFont.ibmPlexSans(12, FontWeight.Medium),
                        color = AppColors.secondaryText
                    )
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AppColors.surface.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AppColors.secondaryText,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            HorizontalDivider(color = AppColors.secondaryText.copy(alpha = 0.15f))

            // 🔹 Installments List
            LazyColumn(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(installments, key = { _, item -> item.installmentNumber }) { index, installment ->
                    val status = computedStatus(installment, today)

                    val isEnabled = if (status == EMIStatus.PENDING) {
                        if (index == 0) true
                        else installments[index - 1].status == EMIStatus.PAID
                    } else false

                    InstallmentRow(
                        installment = installment,
                        status = status,
                        isEnabled = isEnabled,
                        index = index,
                        appearDelayMillis = index * 40L,
                        onClick = {
                            // 🔹 FIXED — mirrors iOS's withAnimation { onSelect(...); isShowing = false }
                            if (isEnabled) {
                                onSelect(installment)
                                onCancel()
                            }
                        }
                    )
                }
            }

            HorizontalDivider(color = AppColors.secondaryText.copy(alpha = 0.15f))

            // 🔹 Footer — FIXED: plain text button, matches iOS (was SSCancelButton)
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = "Close",
                    style = AppFont.ibmPlexSans(15, FontWeight.SemiBold),
                    color = AppColors.secondaryText
                )
            }
        }
    }
}


@Composable
private fun InstallmentRow(
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
                        contentDescription = "Paid",
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
                caption = "Interest",
                value = installment.interestAmount.currencyFormattedWithCommas(),
                modifier = Modifier.weight(1f)
            )

            installment.dueDate?.let {
                DetailChip(
                    icon = Icons.Default.DateRange,
                    caption = "Due date",
                    value = CommonFunctions.dateToString(it.toDate(), format = "MMM dd yyyy"),
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
        text = status.displayText,
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

    var sendOTPLoading by remember { mutableStateOf(false) }
    var verifyOTPLoading by remember { mutableStateOf(false) }

    var isOTPSent by remember { mutableStateOf(false) }
    var otpVerified by remember { mutableStateOf(false) }
    var otpProcessStarted by remember { mutableStateOf(false) }

    var otpError by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf("") }

    fun sendOTP() {

        sendOTPLoading = true
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

                            sendOTPLoading = false
                            otpProcessStarted = false
                            otpError = e.localizedMessage ?: "Failed to send OTP"
                        }

                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken
                        ) {

                            sendOTPLoading = false
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

            otpError = "Enter valid OTP"
            return
        }

        verifyOTPLoading = true

        val credential =
            PhoneAuthProvider.getCredential(
                verificationID,
                otpCode.value
            )

        FirebaseAuth.getInstance()
            .signInWithCredential(credential)
            .addOnCompleteListener { task ->

                verifyOTPLoading = false

                if (task.isSuccessful) {

                    amount.value = currentAmount.toString()
                    otpVerified = true

                } else {

                    otpError =
                        task.exception?.localizedMessage
                            ?: "Invalid OTP"
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
                text = "Edit Amount",
                style = AppFont.ibmPlexSans(20, FontWeight.Bold),
                color = AppColors.headerText,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (!otpVerified) {

                Text(
                    text = "OTP will send to $phoneNumber",
                    style = AppFont.ibmPlexSans(14, FontWeight.Normal),
                    color = AppColors.secondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isOTPSent) {

                    SSTextField(
                        icon = Icons.Default.Lock,
                        placeholder = "Enter OTP",
                        textState = otpCode,
                        keyboardType = KeyboardType.Number,
                        showDropdown = verifyOTPLoading || otpVerified,
                        dropdownIcon = Icons.Default.CheckCircle,
                        dropdownColor = AppColors.primaryButton,
                        isLoading = verifyOTPLoading,
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
                    text = "OTP Verified",
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
                    placeholder = "Enter Amount",
                    textState = amount,
                    keyboardType = KeyboardType.Number,
                    error = amountError
                )
            }

            SSButton(
                title = when {
                    otpVerified -> "Update Amount"
                    isOTPSent -> "Verify OTP"
                    else -> "Send OTP"
                },
                isButtonLoading = sendOTPLoading || verifyOTPLoading,
                isDisabled = otpProcessStarted
            ) {

                when {

                    otpVerified -> {

                        amountError = ""

                        if (amount.value.isBlank()) {

                            amountError = "Amount is required"
                            return@SSButton
                        }

                        val amountInt =
                            amount.value.toIntOrNull()

                        if (amountInt == null) {

                            amountError = "Enter valid amount"
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
                title = "Cancel"
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
            text = "Remind All",
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
                    text = "Check Dues",
                    style = AppFont.ibmPlexSans(
                        16,
                        FontWeight.Bold
                    ),
                    color = AppColors.headerText
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "View pending contributions & EMI details",
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