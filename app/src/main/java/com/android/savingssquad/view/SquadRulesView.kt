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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.android.savingssquad.R
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.model.SquadRule
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.asTimestamp
import com.android.savingssquad.viewmodel.SSToast
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

    Box(
        modifier = Modifier

            .fillMaxSize()

            .windowInsetsPadding(WindowInsets.safeDrawing)
    )
    {
        AppBackgroundGradient()

        Column(modifier = Modifier.fillMaxSize())
        {

            // ================= NAVBAR =================

            SSNavigationBar(

                title = "Squad Rules",

                navController = navController,

                rightButtonDrawable = if (screenType == SquadUserType.SQUAD_MANAGER)

                    R.drawable.add_rule_icon else null

            ) {

                isEditing = false

                newRuleText = ""

                ruleBeingEdited = null

                showRuleSheet = true

            }

            HorizontalDivider(color = AppColors.border)

            // ================= LIST =================

            LazyColumn(

                modifier = Modifier

                    .fillMaxSize()

                    .padding(horizontal = 16.dp),

                verticalArrangement = Arrangement.spacedBy(14.dp)

            ) {

                items(squadViewModel.rules.value, key = { it.id!! }) { rule ->

                    PremiumRuleCard(

                        rule = rule,

                        isManager = screenType == SquadUserType.SQUAD_MANAGER,

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

        // ================= BOTTOM SHEET =================

        if (showRuleSheet) {

            ModalBottomSheet(

                onDismissRequest = {

                    resetEditor {

                        newRuleText = ""

                        isEditing = false

                        ruleBeingEdited = null

                        showRuleSheet = false

                    }

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

                        resetEditor {

                            newRuleText = ""

                            isEditing = false

                            ruleBeingEdited = null

                            showRuleSheet = false

                        }

                    },

                    onSave = {

                        val text = newRuleText.trim()

                        if (text.isEmpty()) return@RuleEditorSheet

                        if (isEditing && ruleBeingEdited != null) {

                            val updated = ruleBeingEdited!!.copy(ruleText = text)

                            squadViewModel.updateRule(updated) { success, _ ->

                                if (success) {

                                    resetEditor {

                                        newRuleText = ""

                                        ruleBeingEdited = null

                                        isEditing = false

                                        showRuleSheet = false

                                    }

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

                                    resetEditor {

                                        newRuleText = ""

                                        ruleBeingEdited = null

                                        isEditing = false

                                        showRuleSheet = false

                                    }

                                }

                            }

                        }

                    }

                )

            }

        }


        }

}

private fun resetEditor(onReset: () -> Unit) = onReset()

@Composable
fun PremiumRuleCard(
    rule: SquadRule,
    isManager: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AppColors.surface,
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 0.5.dp,
                color = AppColors.border.copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp)
            )
            .appShadow(
                style = AppShadows.card,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(18.dp)
            .clickable(enabled = isManager) { onEdit() },
        verticalAlignment = Alignment.Top
    ) {

        // ================= LEFT ACCENT =================
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(44.dp)
                .background(
                    AppColors.primaryBrand.copy(alpha = 0.35f),
                    RoundedCornerShape(6.dp)
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        // ================= TEXT =================
        Text(
            text = rule.ruleText,
            color = AppColors.headerText,
            style = AppFont.ibmPlexSans(16, FontWeight.Medium),
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f)
        )

        // ================= DELETE =================
        if (isManager) {

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        AppColors.errorAccent.copy(alpha = 0.12f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = AppColors.errorAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        Text(
            text = if (isEditing) "Edit Rule" else "Add Rule",
            style = AppFont.ibmPlexSans(20, FontWeight.Bold),
            color = AppColors.headerText
        )

        SSTextView(
            placeholder = "Write your rule...",
            text = ruleText,
            onTextChange = onTextChange,
            error = if (ruleText.isEmpty()) "Rule text cannot be empty" else "",
            maxCharacters = 500
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {

            SSButton(
                title = if (isEditing) "Update" else "Add",
                isDisabled = ruleText.isEmpty(),
                action = onSave,
                modifier = Modifier.weight(1f)
            )

            SSCancelButton(
                title = "Cancel",
                action = onCancel,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}