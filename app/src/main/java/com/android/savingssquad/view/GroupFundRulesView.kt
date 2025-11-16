package com.android.savingssquad.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.Date
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.R
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import kotlinx.coroutines.launch
import com.android.savingssquad.model.GroupFund
import com.android.savingssquad.model.GroupFundRule
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupFundRulesView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared,
) {

    var newRuleText by remember { mutableStateOf("") }
    var showRuleSheet by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var ruleBeingEdited by remember { mutableStateOf<GroupFundRule?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val screenType =
        if (UserDefaultsManager.getGroupFundManagerLogged())
            GroupFundUserType.GROUP_FUND_MANAGER
        else
            GroupFundUserType.GROUP_FUND_MEMBER

    LaunchedEffect(Unit) {
        squadViewModel.fetchRules()
    }

    AppBackgroundGradient()

    Column(modifier = Modifier.fillMaxSize()) {

        // 🔹 Navigation Bar
        SSNavigationBar(
            title = "Group Fund Rules",
            navController = navController,
            rightButtonDrawable = if (screenType == GroupFundUserType.GROUP_FUND_MANAGER)
                R.drawable.add_rule_icon
            else null
        ) {
            isEditing = false
            newRuleText = ""
            ruleBeingEdited = null
            showRuleSheet = true
        }

        Divider(color = AppColors.border)

        // 🔹 Rules List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            items(squadViewModel.rules.value, key = { it.id!! }) { rule ->

                RuleCard(
                    rule = rule,
                    isManager = (screenType == GroupFundUserType.GROUP_FUND_MANAGER),
                    onDelete = {
                        squadViewModel.deleteRule(rule, true) { _, _ -> }
                    },
                    onEdit = {
                        ruleBeingEdited = rule
                        newRuleText = rule.ruleText
                        isEditing = true
                        showRuleSheet = true
                    }
                )
            }

            if (loaderManager.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.loaderColor)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(60.dp)) }
        }
    }

    // 🔹 BOTTOM SHEET
    if (showRuleSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                newRuleText = ""
                isEditing = false
                ruleBeingEdited = null
                showRuleSheet = false
            },
            sheetState = sheetState,
            containerColor = AppColors.surface,
            dragHandle = {}
        ) {

            RuleEditorSheet(
                ruleText = newRuleText,
                onTextChange = { newRuleText = it },
                isEditing = isEditing,
                onCancel = {
                    newRuleText = ""
                    isEditing = false
                    ruleBeingEdited = null
                    showRuleSheet = false
                },
                onSave = {

                    val text = newRuleText.trim()
                    if (text.isEmpty()) return@RuleEditorSheet

                    if (isEditing && ruleBeingEdited != null) {
                        val updated = ruleBeingEdited!!.copy(ruleText = text)
                        squadViewModel.updateRule(updated) { success, _ ->
                            if (success) {
                                resetRuleEditor(
                                    onReset = {
                                        newRuleText = ""
                                        ruleBeingEdited = null
                                        isEditing = false
                                        showRuleSheet = false
                                    }
                                )
                            }
                        }
                    } else {
                        val newRule = GroupFundRule(
                            id = CommonFunctions.generateRuleID(),
                            ruleText = text,
                            createdDate = Date().asTimestamp
                        )
                        squadViewModel.addRule(newRule) { success, _ ->
                            if (success) {
                                resetRuleEditor(
                                    onReset = {
                                        newRuleText = ""
                                        ruleBeingEdited = null
                                        isEditing = false
                                        showRuleSheet = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

private fun resetRuleEditor(onReset: () -> Unit) = onReset()

@Composable
fun RuleCard(
    rule: GroupFundRule,
    isManager: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surface, RoundedCornerShape(15.dp))
            .border(0.5.dp, AppColors.border.copy(alpha = 0.5f), RoundedCornerShape(15.dp))
            .appShadow(AppShadows.card)
            .padding(16.dp)
            .clickable(enabled = isManager) { onEdit() },
        verticalAlignment = Alignment.Top
    ) {

        Text(
            text = rule.ruleText,
            color = AppColors.headerText,
            style = AppFont.ibmPlexSans(16, FontWeight.Medium),
            modifier = Modifier.weight(1f)
        )

        if (isManager) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,   // ← System delete icon
                    contentDescription = "Delete",
                    tint = AppColors.errorAccent,
                    modifier = Modifier
                        .size(22.dp)
                        .background(AppColors.errorAccent.copy(alpha = 0.15f), CircleShape)
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun RuleEditorSheet(
    ruleText: String,
    onTextChange: (String) -> Unit,
    isEditing: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Text(
            text = if (isEditing) "Edit Rule" else "Add Rule",
            style = AppFont.ibmPlexSans(20, FontWeight.Bold),
            color = AppColors.headerText,
            modifier = Modifier.padding(top = 10.dp)
        )

        SSTextView(
            placeholder = "Write your rule...",
            text = ruleText,
            onTextChange = { onTextChange(it) },
            error = if (ruleText.isEmpty()) "Rule text cannot be empty" else "",
            maxCharacters = 500
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            SSButton(
                title = if (isEditing) "Update" else "Add",
                isDisabled = ruleText.isEmpty(),
                action = onSave
            )

            SSCancelButton(
                title = "Cancel",
                action = onCancel
            )
        }
    }
}