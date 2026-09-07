package com.flowkeys.android.accessibility

import android.graphics.Rect
import android.text.InputType
import android.view.accessibility.AccessibilityNodeInfo
import com.flowkeys.android.core.model.FieldType

/**
 * Inspects AccessibilityNodeInfo objects to classify input fields,
 * enforce security boundaries on sensitive data (passwords, PINs, OTPs),
 * and determine spatial layout bounds.
 */
object FieldClassifier {

    private val SENSITIVE_PACKAGES = setOf(
        // UPI & Indian Payment Apps
        "com.phonepe.app",
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "net.one97.paytm",
        "in.org.npci.upiapp", // BHIM
        "com.dreamplug.androidapp", // CRED
        "com.freecharge.android",
        "com.mobikwik_new",
        // Indian Major Banks
        "com.sbi.lotusintouch", // YONO SBI
        "com.csam.icici.bank.imobile", // ICICI iMobile
        "com.hdfcbank.android", // HDFC Bank
        "com.axis.mobile", // Axis Mobile
        "com.kotak.mobilebank", // Kotak 811
        "com.bankofbaroda.mconnect", // bob World
        "com.pnb.pnbone", // PNB ONE
        "com.canarabank.ai1mobile", // Canara ai1
        "com.unionbank.challenger", // Union Bank
        "com.idbi.mobilebanking",
        // Global FinTech & Wallets
        "com.paypal.android.p2pmobile",
        "com.squareup.cash",
        "com.binance.dev",
        "com.coinbase.android"
    )

    /**
     * Checks if a package belongs to known banking, payment, or financial applications.
     */
    fun isFinancialOrBankingPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val pkg = packageName.lowercase()
        if (SENSITIVE_PACKAGES.any { pkg.startsWith(it) || pkg == it }) return true
        return pkg.contains(".bank.") || pkg.contains("banking") ||
                pkg.contains("netbanking") || pkg.contains("upiapp")
    }

    /**
     * Determines if a node is an eligible, non-sensitive editable input field.
     */
    fun isEligibleEditableField(node: AccessibilityNodeInfo?, packageName: String? = null): Boolean {
        if (node == null) return false
        if (!node.isEditable) return false
        if (node.isPassword) return false

        // Suppress FlowKeys overlay on banking and financial apps (Wispr Flow safety standard)
        if (isFinancialOrBankingPackage(packageName)) return false

        // Check inputType flags if available
        val inputType = node.inputType
        if (isPasswordInputType(inputType)) return false

        // Filter known sensitive view IDs or descriptions
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        if (viewId.contains("password") || viewId.contains("pin") || viewId.contains("otp") ||
            viewId.contains("cvv") || viewId.contains("security") || viewId.contains("card_num")) {
            return false
        }

        return true
    }

    /**
     * Classifies the field type based on input type and hosting package.
     */
    fun classifyField(node: AccessibilityNodeInfo?, packageName: String): FieldType {
        if (node == null) return FieldType.GENERIC_TEXT
        if (node.isPassword || isPasswordInputType(node.inputType)) {
            return FieldType.SENSITIVE_LOCKED
        }

        val pkg = packageName.lowercase()
        return when {
            pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("signal") || pkg.contains("messaging") -> FieldType.MESSAGING
            pkg.contains("gm") || pkg.contains("mail") || pkg.contains("outlook") -> FieldType.EMAIL
            pkg.contains("termux") -> FieldType.DEVELOPER_TERMINAL
            else -> {
                val inputType = node.inputType
                when (inputType and InputType.TYPE_MASK_CLASS) {
                    InputType.TYPE_CLASS_TEXT -> {
                        when (inputType and InputType.TYPE_MASK_VARIATION) {
                            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> FieldType.EMAIL
                            InputType.TYPE_TEXT_VARIATION_URI -> FieldType.SEARCH_BAR
                            else -> FieldType.GENERIC_TEXT
                        }
                    }
                    else -> FieldType.GENERIC_TEXT
                }
            }
        }
    }

    /**
     * Extracts screen bounds for a node safely.
     */
    fun getBoundsInScreen(node: AccessibilityNodeInfo?): Rect {
        val rect = Rect()
        node?.getBoundsInScreen(rect)
        return rect
    }

    private fun isPasswordInputType(inputType: Int): Boolean {
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
    }
}
