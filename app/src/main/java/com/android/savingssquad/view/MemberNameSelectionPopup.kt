package com.android.savingssquad.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.savingssquad.model.Member
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.viewmodel.SquadViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val MemberPickerGreen = Color(0xFF16856B)

/**
 * Member picker with optional ALL selection.
 *
 * ALL is a separate action and is not represented as a Member object.
 *
 * Search and pagination remain handled by SquadViewModel.
 */
@Composable
fun MemberNameSelectionPopup(
    showPopup: Boolean,
    squadId: String,
    squadViewModel: SquadViewModel,
    onDismiss: () -> Unit,
    onSelect: (Member) -> Unit,
    modifier: Modifier = Modifier,
    title: String = SquadStrings.selectMember,
    searchPlaceholder: String = SquadStrings.memberName,
    showSelectAll: Boolean = false,
    onSelectAll: (() -> Unit)? = null
) {
    if (!showPopup) return

    var searchText by rememberSaveable(squadId) {
        mutableStateOf("")
    }

    var searchJob by remember {
        mutableStateOf<Job?>(null)
    }

    var isSearchRequestRunning by remember {
        mutableStateOf(false)
    }

    var requestError by remember {
        mutableStateOf<String?>(null)
    }

    val scope = rememberCoroutineScope()

    val members by squadViewModel.squadMembers.collectAsState()
    val isFetchingMembers by squadViewModel.isFetchingMembers.collectAsState()
    val squad by squadViewModel.squad.collectAsState()

    fun fetchMembers(query: String) {
        isSearchRequestRunning = true
        requestError = null

        squadViewModel.resetMembersPagination()

        squadViewModel.fetchMembers(
            showLoader = false,
            searchText = query.trim()
        ) { success, _, error ->
            isSearchRequestRunning = false
            requestError = if (success) {
                null
            } else {
                error ?: "Failed to fetch members"
            }
        }
    }

    // Initial fetch for this squad.
    LaunchedEffect(squadId) {
        fetchMembers(searchText)
    }

    // Debounced search.
    LaunchedEffect(searchText, squadId) {
        searchJob?.cancel()

        searchJob = scope.launch {
            delay(300)
            fetchMembers(searchText)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            searchJob?.cancel()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp
        ) {
            Column {
                // MARK: Header

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 18.dp,
                            end = 12.dp,
                            top = 15.dp,
                            bottom = 14.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                MemberPickerGreen.copy(alpha = 0.11f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = MemberPickerGreen
                        )
                    }

                    Spacer(Modifier.width(11.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(Modifier.height(3.dp))

                        Text(
                            text = SquadStrings.searchMembers,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close member selection"
                        )
                    }
                }

                // MARK: Search

                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    placeholder = {
                        Text(searchPlaceholder)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
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
                    shape = RoundedCornerShape(11.dp),
                    keyboardOptions = KeyboardOptions.Default,
                    keyboardActions = KeyboardActions(
                        onDone = {}
                    )
                )

                Spacer(Modifier.height(12.dp))

                // MARK: List heading

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = SquadStrings.members,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.weight(1f))

                    Text(
                        text = (squad?.totalMembers ?: 0).toString(),
                        color = MemberPickerGreen,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(8.dp))

                // MARK: Members and ALL

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 13.dp,
                        end = 13.dp,
                        bottom = 10.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    when {
                        (isSearchRequestRunning || isFetchingMembers) &&
                                members.isEmpty() -> {

                            item(key = "initial_loading") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 38.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = MemberPickerGreen
                                    )
                                }
                            }
                        }

                        requestError != null && members.isEmpty() -> {
                            item(key = "error_state") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 30.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = requestError.orEmpty(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    TextButton(
                                        onClick = {
                                            fetchMembers(searchText)
                                        }
                                    ) {
                                        Text(
                                            text = "Try Again",
                                            color = MemberPickerGreen
                                        )
                                    }
                                }
                            }
                        }

                        members.isEmpty() -> {
                            // Keep ALL available even if the current result
                            // list is empty, provided there is no search.
                            if (
                                showSelectAll &&
                                searchText.isBlank()
                            ) {
                                item(key = "select_all") {
                                    SelectAllMembersRow(
                                        onClick = {
                                            onSelectAll?.invoke()
                                            onDismiss()
                                        }
                                    )
                                }
                            }

                            item(key = "empty_state") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 30.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonSearch,
                                        contentDescription = null,
                                        modifier = Modifier.size(30.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(Modifier.height(9.dp))

                                    Text(
                                        text = if (searchText.isBlank()) {
                                            "No members found"
                                        } else {
                                            "No matching members"
                                        },
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = "Try searching with another name.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        else -> {
                            // Show ALL before individual members.
                            if (
                                showSelectAll &&
                                searchText.isBlank()
                            ) {
                                item(key = "select_all") {
                                    SelectAllMembersRow(
                                        onClick = {
                                            onSelectAll?.invoke()
                                            onDismiss()
                                        }
                                    )
                                }
                            }

                            items(
                                items = members,
                                key = { member ->
                                    member.id ?: member.phoneNumber
                                }
                            ) { member ->
                                MemberSelectionRow(
                                    member = member,
                                    onClick = {
                                        onSelect(member)
                                        onDismiss()
                                    }
                                )

                                // Search-aware pagination trigger.
                                LaunchedEffect(member.id, members.size) {
                                    squadViewModel.loadMoreMembersIfNeeded(
                                        currentMember = member,
                                        searchText = searchText
                                    )
                                }
                            }
                        }
                    }

                    if (squadViewModel.isLoadingMoreMembers) {
                        item(key = "pagination_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MemberPickerGreen,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }

                // MARK: Footer

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.35f
                            )
                        )
                        .padding(
                            horizontal = 18.dp,
                            vertical = 12.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MemberPickerGreen)
                    )

                    Spacer(Modifier.width(7.dp))

                    Text(
                        text = "Select a member to continue",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.weight(1f))

                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// MARK: ALL row

@Composable
private fun SelectAllMembersRow(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                MemberPickerGreen.copy(alpha = 0.06f)
            )
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = MemberPickerGreen.copy(alpha = 0.30f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    MemberPickerGreen.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = MemberPickerGreen
            )
        }

        Spacer(Modifier.width(11.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = SquadStrings.all,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(3.dp))
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MemberPickerGreen
        )
    }
}

// MARK: Individual member row

@Composable
private fun MemberSelectionRow(
    member: Member,
    onClick: () -> Unit
) {
    val displayName = member.localizedMemberName
    val initial = displayName.trim().firstOrNull()?.uppercase() ?: "?"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    MemberPickerGreen.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = MemberPickerGreen,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(11.dp))

        Text(
            text = displayName,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MemberPickerGreen
        )
    }
}