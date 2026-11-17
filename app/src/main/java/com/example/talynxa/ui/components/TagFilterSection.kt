package com.example.talynxa.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.talynxa.data.model.MatchMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagFilterSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    tagCounts: Map<String, Int>,
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit,
    onClearTags: () -> Unit,
    matchMode: MatchMode,
    onMatchModeChange: (MatchMode) -> Unit,
    totalItemsCount: Int,
    filteredItemsCount: Int,
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable { mutableStateOf(true) }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with Collapse / Expand Toggle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isExpanded) "Filters & Tags" else "Filters & Tags (${tagCounts.size} tags active)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (!isExpanded && selectedTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text("${selectedTags.size}")
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedTags.isNotEmpty() && isExpanded) {
                        TextButton(onClick = onClearTags) {
                            Icon(
                                imageVector = Icons.Default.FilterAltOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear (${selectedTags.size})")
                        }
                    }
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse filter section" else "Expand filter section"
                        )
                    }
                }
            }

            // Collapsible Content
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search text or tags...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Match Mode Toggle (Segmented Button Row)
                    if (tagCounts.isNotEmpty()) {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SegmentedButton(
                                selected = matchMode == MatchMode.ALL,
                                onClick = { onMatchModeChange(MatchMode.ALL) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("Match ALL (AND)")
                            }
                            SegmentedButton(
                                selected = matchMode == MatchMode.ANY,
                                onClick = { onMatchModeChange(MatchMode.ANY) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("Match ANY (OR)")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Tag Chips list
                    if (tagCounts.isEmpty()) {
                        Text(
                            text = "No tags found in current note",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tagCounts.forEach { (tag, count) ->
                                val isSelected = selectedTags.contains(tag)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onTagToggle(tag) },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(tag)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Badge(
                                                containerColor = if (isSelected)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (isSelected)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            ) {
                                                Text("$count")
                                            }
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors()
                                )
                            }
                        }
                    }

                    // Results summary bar
                    if (searchQuery.isNotEmpty() || selectedTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Showing $filteredItemsCount of $totalItemsCount items",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun TagFilterSectionPreview() {
    com.example.talynxa.ui.theme.TalynxaTheme {
        TagFilterSection(
            searchQuery = "",
            onSearchQueryChange = {},
            tagCounts = mapOf("work" to 5, "urgent" to 2, "personal" to 8),
            selectedTags = setOf("work"),
            onTagToggle = {},
            onClearTags = {},
            matchMode = MatchMode.ALL,
            onMatchModeChange = {},
            totalItemsCount = 15,
            filteredItemsCount = 5
        )
    }
}
