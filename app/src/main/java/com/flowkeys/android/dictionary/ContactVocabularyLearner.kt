package com.flowkeys.android.dictionary

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Auto-Learning Contact & Custom Vocabulary Engine.
 * Extracts contact display names and proper nouns from device contacts (when permission granted)
 * to bias ASR, micro-polisher, and avoid name corruption during speech dictation.
 */
object ContactVocabularyLearner {

    private val nameFilterRegex = Regex("^[\\p{L}\\s.'-]+$")

    suspend fun learnFromContacts(context: Context): Int = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            return@withContext 0
        }

        val learnedNames = mutableSetOf<String>()
        val contentResolver = context.contentResolver

        val projection = arrayOf(
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        )

        try {
            contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                if (nameIndex != -1) {
                    while (cursor.moveToNext()) {
                        val fullName = cursor.getString(nameIndex)?.trim() ?: continue
                        if (fullName.isNotBlank() && fullName.length in 2..40 && fullName.matches(nameFilterRegex)) {
                            // Add full name
                            learnedNames.add(fullName)
                            PersonalDictionary.addContactName(fullName)

                            // Also index individual first & last names if multi-word
                            val tokens = fullName.split(Regex("\\s+"))
                            if (tokens.size > 1) {
                                for (token in tokens) {
                                    if (token.length >= 3 && token.all { it.isLetter() }) {
                                        learnedNames.add(token)
                                        PersonalDictionary.addContactName(token)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactLearner", "Error querying contacts: ${e.localizedMessage}")
        }

        return@withContext learnedNames.size
    }
}
