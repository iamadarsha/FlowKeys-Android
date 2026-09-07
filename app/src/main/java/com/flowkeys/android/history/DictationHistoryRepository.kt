package com.flowkeys.android.history

import com.flowkeys.android.core.model.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HistoryItem(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val text: String,
    val language: Language,
    val packageName: String
)

/**
 * In-memory transient repository for recent dictations.
 * Never uploads to the cloud. Automatically cleared or pruned.
 */
object DictationHistoryRepository {

    private val items = mutableListOf<HistoryItem>()
    private val _historyFlow = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyFlow: StateFlow<List<HistoryItem>> = _historyFlow.asStateFlow()

    fun recordInsertion(text: String, language: Language, packageName: String) {
        val item = HistoryItem(
            text = text,
            language = language,
            packageName = packageName
        )
        items.add(0, item)
        if (items.size > 20) {
            items.removeAt(items.size - 1)
        }
        _historyFlow.value = items.toList()
    }

    fun getLastInsertion(): HistoryItem? = items.firstOrNull()

    fun clear() {
        items.clear()
        _historyFlow.value = emptyList()
    }
}
