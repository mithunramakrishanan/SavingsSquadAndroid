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
import com.android.savingssquad.viewmodel.SquadViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val MemberPickerGreen = Color(0xFF16856B)

/**
 * Member picker that follows MembersListView's search and pagination flow.
 *
 * Pagination is delegated to SquadViewModel.loadMoreMembersIfNeeded(member, searchText).
 * That method should only load another page when the displayed member is the last
 * member currently loaded, and should use the active search query for the next page.
 */
@Composable
fun MemberNameSelectionPopup(
    showPopup: Boolean,
    squadId: String,
    squadViewModel: SquadViewModel,
    onDismiss: () -> Unit,
    onSelect: (Member) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Select Member",
    searchPlaceholder: String = "Search member name"
) {
    if (!showPopup) return

    var searchText by rememberSaveable(squadId) { mutableStateOf("") }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var isSearchRequestRunning by remember { mutableStateOf(false) }
    var requestError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val members by squadViewModel.squadMembers.collectAsState()
    val isFetchingMembers by squadViewModel.isFetchingMembers.collectAsState()

    fun fetchMembers(query: String) {
        isSearchRequestRunning = true
        requestError = null

        // Same sequence as MembersListView when the search changes.
        squadViewModel.resetMembersPagination()
        squadViewModel.fetchMembers(
            showLoader = false,
            searchText = query.trim()
        ) { success, _, error ->
            isSearchRequestRunning = false
            requestError = if (success) null else (error ?: "Failed to fetch members")
        }
    }

    // Initial fetch for this squad. The debounce effect below handles subsequent edits.
    LaunchedEffect(squadId) {
        fetchMembers(searchText)
    }

    // Match MembersListView: debounce search for 300 ms, reset cursor, fetch first page.
    LaunchedEffect(searchText, squadId) {
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(300)
            fetchMembers(searchText)
        }
    }

    DisposableEffect(Unit) {
        onDispose { searchJob?.cancel() }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 12.dp, top = 15.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MemberPickerGreen.copy(alpha = 0.11f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = MemberPickerGreen
                        )
                    }

                    Spacer(Modifier.width(11.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Search and select a squad member",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close member selection")
                    }
                }

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    placeholder = { Text(searchPlaceholder) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { searchText = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(11.dp),
                    keyboardOptions = KeyboardOptions.Default,
                    keyboardActions = KeyboardActions(onDone = {})
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MEMBERS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = squadViewModel.squad.collectAsState().value?.totalMembers.toString(),
                        color = MemberPickerGreen,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(8.dp))

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
                        (isSearchRequestRunning || isFetchingMembers) && members.isEmpty() -> {
                            item(key = "initial_loading") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 38.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MemberPickerGreen)
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
                                        onClick = { fetchMembers(searchText) }
                                    ) {
                                        Text("Try Again", color = MemberPickerGreen)
                                    }
                                }
                            }
                        }

                        members.isEmpty() -> {
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
                            items(
                                items = members,
                                key = { member -> member.id ?: member.phoneNumber }
                            ) { member ->
                                MemberSelectionRow(
                                    member = member,
                                    onClick = {
                                        onSelect(member)
                                        onDismiss()
                                    }
                                )

                                // Exact same trigger and search-aware paging call as MembersListView.
                                androidx.compose.runtime.LaunchedEffect(member.id, members.size) {
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                        .padding(horizontal = 18.dp, vertical = 12.dp),
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
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

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
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MemberPickerGreen.copy(alpha = 0.12f)),
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
