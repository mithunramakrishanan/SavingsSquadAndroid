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
import androidx.compose.ui.unit.dp
import java.util.Date
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.android.savingssquad.R
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.model.SquadRule
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.asTimestamp
import com.yourapp.utils.CommonFunctions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SquadRulesView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared,
) {

    var newRuleText by remember { mutableStateOf("") }
    var showRuleSheet by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var ruleBeingEdited by remember { mutableStateOf<SquadRule?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val screenType =
        if (UserDefaultsManager.getSquadManagerLogged())
            SquadUserType.SQUAD_MANAGER
        else
            SquadUserType.SQUAD_MEMBER

    LaunchedEffect(Unit) {
        squadViewModel.fetchRules()
    }

    AppBackgroundGradient()

    Column(modifier = Modifier.fillMaxSize()) {

        // 🔹 Navigation Bar
        SSNavigationBar(
            title = "Squad Rules",
            navController = navController,
            rightButtonDrawable = if (screenType == SquadUserType.SQUAD_MANAGER)
                R.drawable.add_rule_icon
            else null
        ) {
            isEditing = false
            newRuleText = ""
            ruleBeingEdited = null
            showRuleSheet = true
        }

        HorizontalDivider(color = AppColors.border)

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
                    isManager = (screenType == SquadUserType.SQUAD_MANAGER),
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
                        val newRule = SquadRule(
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
    rule: SquadRule,
    isManager: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appShadow(AppShadows.card)
            .background(AppColors.surface, RoundedCornerShape(15.dp))
            .border(0.5.dp, AppColors.border.copy(alpha = 0.5f), RoundedCornerShape(15.dp))
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