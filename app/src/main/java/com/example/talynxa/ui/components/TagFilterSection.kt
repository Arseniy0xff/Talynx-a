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
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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

            // Tag Statistics & Match Mode Selector Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tags (${tagCounts.size})",
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                if (selectedTags.isNotEmpty()) {
                    TextButton(onClick = onClearTags) {
                        Icon(
                            imageVector = Icons.Default.FilterAltOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Filters (${selectedTags.size})")
                    }
                }
            }

            // Match Mode Toggle (Segmented Button Row)
            if (tagCounts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
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
            }

            // Tag Chips list
            if (tagCounts.isEmpty()) {
                Text(
                    text = "No tags found in current note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
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
            AnimatedVisibility(visible = searchQuery.isNotEmpty() || selectedTags.isNotEmpty()) {
                Column {
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

