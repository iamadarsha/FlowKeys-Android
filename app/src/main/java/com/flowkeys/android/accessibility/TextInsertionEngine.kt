package com.flowkeys.android.accessibility

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Robust dynamic streaming text insertion engine.
 *
 * Implements:
 * 1. Live dynamic cursor streaming during speech recognition without erasing pre-existing text
 * 2. Primary: Direct ACTION_SET_TEXT via AccessibilityNodeInfo
 * 3. Secondary Fallback: Clipboard write + ACTION_PASTE with automated clipboard rollback
 */
object TextInsertionEngine {

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var sessionBaselineText: String? = null
    @Volatile
    private var isSessionActive = false

    /**
     * Call at the start of a dictation utterance to freeze the baseline text of the focused field.
     */
    fun startSession(targetNode: AccessibilityNodeInfo?) {
        isSessionActive = true
        if (targetNode != null && targetNode.isEditable) {
            val currentText = targetNode.text?.toString() ?: ""
            val hintText = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                targetNode.hintText?.toString() ?: ""
            } else ""

            val isPlaceholder = currentText.isEmpty() ||
                    currentText.equals(hintText, ignoreCase = true) ||
                    currentText.equals("Message", ignoreCase = true) ||
                    currentText.equals("Type a message", ignoreCase = true) ||
                    currentText.equals("Search", ignoreCase = true) ||
                    currentText.equals("Search…", ignoreCase = true)

            sessionBaselineText = if (isPlaceholder) "" else currentText
        } else {
            sessionBaselineText = ""
        }
    }

    /**
     * Streams partial recognized speech tokens into the active field in real-time.
     */
    fun insertStreamingPartial(targetNode: AccessibilityNodeInfo?, partialText: String): Boolean {
        if (targetNode == null || !targetNode.isEditable || partialText.isBlank()) {
            return false
        }

        val baseline = sessionBaselineText ?: ""
        val textToSet = if (baseline.isBlank()) {
            partialText
        } else {
            if (baseline.endsWith(" ")) "$baseline$partialText" else "$baseline $partialText"
        }

        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToSet)
        }
        return targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    /**
     * Inserts final polished/translated/phonetic [text] into [targetNode] and finalizes the dictation session.
     */
    fun insertText(targetNode: AccessibilityNodeInfo?, text: String, context: Context): Boolean {
        val result = if (targetNode != null && targetNode.isEditable) {
            val directSuccess = performDirectSetText(targetNode, text)
            if (directSuccess) true else performPasteFallback(targetNode, text, context)
        } else {
            false
        }

        resetSession()
        return result
    }

    fun resetSession() {
        isSessionActive = false
        sessionBaselineText = null
    }

    private fun performDirectSetText(node: AccessibilityNodeInfo, text: String): Boolean {
        val baseline = sessionBaselineText
        val textToSet: CharSequence = if (baseline != null) {
            if (baseline.isBlank()) {
                text
            } else {
                if (baseline.endsWith(" ") || text.startsWith(" ")) "$baseline$text" else "$baseline $text"
            }
        } else {
            val existingText = node.text?.toString() ?: ""
            val hintText = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                node.hintText?.toString() ?: ""
            } else ""

            val isPlaceholder = existingText.isEmpty() ||
                    existingText.equals(hintText, ignoreCase = true) ||
                    existingText.equals("Message", ignoreCase = true) ||
                    existingText.equals("Type a message", ignoreCase = true) ||
                    existingText.equals("Search", ignoreCase = true) ||
                    existingText.equals("Search…", ignoreCase = true)

            if (isPlaceholder) {
                text
            } else {
                if (existingText.endsWith(" ") || text.startsWith(" ")) "$existingText$text" else "$existingText $text"
            }
        }

        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToSet)
        }

        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun performPasteFallback(node: AccessibilityNodeInfo, text: String, context: Context): Boolean {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false

        // 1. Backup existing clipboard content
        val originalClip: ClipData? = clipboard.primaryClip

        // 2. Set new text to clipboard
        val flowKeysClip = ClipData.newPlainText("FlowKeys Dictation", text)
        clipboard.setPrimaryClip(flowKeysClip)

        // 3. Execute ACTION_PASTE
        val pasteSuccess = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)

        // 4. Restore original clipboard content after delay to preserve user clipboard state
        mainHandler.postDelayed({
            try {
                if (originalClip != null) {
                    clipboard.setPrimaryClip(originalClip)
                } else {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        clipboard.clearPrimaryClip()
                    }
                }
            } catch (ignored: Exception) {}
        }, 1500L)

        return pasteSuccess
    }
}
