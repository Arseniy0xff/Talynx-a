package com.example.talynxa.ui.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ReorderableLazyListState(
    val lazyListState: LazyListState,
    private val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    private val scope: CoroutineScope
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set
    var draggingItemKey by mutableStateOf<Any?>(null)
        private set
    var draggedDistance by mutableFloatStateOf(0f)
        private set

    fun onDragStart(key: Any) {
        val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
        if (itemInfo != null) {
            draggingItemIndex = itemInfo.index
            draggingItemKey = key
            draggedDistance = 0f
        }
    }

    fun onDrag(dragAmount: Offset) {
        val currentIndex = draggingItemIndex ?: return
        val currentKey = draggingItemKey ?: return
        draggedDistance += dragAmount.y

        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val currentDraggingItem = visibleItems.firstOrNull { it.key == currentKey } ?: return

        val currentItemTop = currentDraggingItem.offset + draggedDistance
        val currentItemCenter = currentItemTop + currentDraggingItem.size / 2f

        val targetItem = visibleItems.firstOrNull { item ->
            val itemTop = item.offset.toFloat()
            val itemBottom = (item.offset + item.size).toFloat()
            currentItemCenter in itemTop..itemBottom && item.key != currentKey
        }

        if (targetItem != null) {
            val targetIndex = targetItem.index
            onMove(currentIndex, targetIndex)
            draggedDistance -= (targetItem.offset - currentDraggingItem.offset)
            draggingItemIndex = targetIndex
        }

        val viewportStart = lazyListState.layoutInfo.viewportStartOffset
        val viewportEnd = lazyListState.layoutInfo.viewportEndOffset
        val edgeThreshold = 100f

        val distanceFromTop = currentItemTop - viewportStart
        val distanceFromBottom = viewportEnd - (currentItemTop + currentDraggingItem.size)

        if (distanceFromTop < edgeThreshold) {
            val scrollSpeed = (edgeThreshold - distanceFromTop).coerceAtLeast(0f) / 2.5f
            scope.launch { lazyListState.scrollBy(-scrollSpeed) }
        } else if (distanceFromBottom < edgeThreshold) {
            val scrollSpeed = (edgeThreshold - distanceFromBottom).coerceAtLeast(0f) / 2.5f
            scope.launch { lazyListState.scrollBy(scrollSpeed) }
        }
    }

    fun onDragEnd() {
        draggingItemIndex = null
        draggingItemKey = null
        draggedDistance = 0f
    }

    fun onDragCancel() {
        onDragEnd()
    }
}

@Composable
fun rememberReorderableLazyListState(
    lazyListState: LazyListState,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit
): ReorderableLazyListState {
    val scope = rememberCoroutineScope()
    val onMoveUpdated by rememberUpdatedState(onMove)
    return remember(lazyListState) {
        ReorderableLazyListState(
            lazyListState = lazyListState,
            onMove = { from, to -> onMoveUpdated(from, to) },
            scope = scope
        )
    }
}
