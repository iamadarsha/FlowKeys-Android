package com.flowkeys.android.dictionary

import android.content.Context

/**
 * Contact Vocabulary Learner — contact sync intentionally disabled.
 *
 * READ_CONTACTS permission was removed from AndroidManifest.xml to eliminate
 * a spyware-category heuristic trigger in OEM security scanners.
 * This class is retained as a stub so callers compile without changes.
 *
 * If contact-name hints are reintroduced in a future version they must:
 *   1. Use a scoped contacts query (projection = DISPLAY_NAME_PRIMARY only)
 *   2. Require explicit opt-in from the user in Settings
 *   3. Re-add READ_CONTACTS to the manifest with prominent disclosure
 *   4. Never send contact names to any cloud endpoint
 */
object ContactVocabularyLearner {

    /**
     * No-op — contact sync has been removed. Always returns 0.
     * Callers can safely call this without any side effects.
     */
    suspend fun learnFromContacts(@Suppress("UNUSED_PARAMETER") context: Context): Int = 0
}

