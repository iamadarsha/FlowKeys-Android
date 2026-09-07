package com.flowkeys.android.core.model

/**
 * Contextual metadata about the currently focused editable field.
 */
data class DictationContext(
    val packageName: String,
    val fieldType: FieldType = FieldType.GENERIC_TEXT,
    val language: Language = Language.DEFAULT,
    val selectedText: String? = null,
    val existingContentLength: Int = 0
)

enum class FieldType {
    GENERIC_TEXT,
    MESSAGING,
    EMAIL,
    SEARCH_BAR,
    DEVELOPER_TERMINAL,
    SENSITIVE_LOCKED
}
