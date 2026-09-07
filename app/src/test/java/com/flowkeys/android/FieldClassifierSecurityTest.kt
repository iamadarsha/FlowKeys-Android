package com.flowkeys.android

import com.flowkeys.android.accessibility.FieldClassifier
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldClassifierSecurityTest {

    @Test
    fun testIndianBankingAndUpiPackagesAreSuppressed() {
        val bankingApps = listOf(
            "com.phonepe.app",
            "com.google.android.apps.nbu.paisa.user",
            "net.one97.paytm",
            "in.org.npci.upiapp",
            "com.dreamplug.androidapp",
            "com.sbi.lotusintouch",
            "com.hdfcbank.android",
            "com.csam.icici.bank.imobile",
            "com.axis.mobile",
            "com.kotak.mobilebank"
        )

        for (pkg in bankingApps) {
            assertTrue("Expected $pkg to be recognized as financial", FieldClassifier.isFinancialOrBankingPackage(pkg))
            assertFalse("Expected eligible field check to reject banking app $pkg", FieldClassifier.isEligibleEditableField(null, pkg))
        }
    }

    @Test
    fun testMessagingAndGeneralPackagesAreNotSuppressed() {
        val standardApps = listOf(
            "com.whatsapp",
            "org.telegram.messenger",
            "org.thoughtcrime.securesms",
            "com.google.android.gm",
            "com.android.chrome"
        )

        for (pkg in standardApps) {
            assertFalse("Expected $pkg NOT to be recognized as financial", FieldClassifier.isFinancialOrBankingPackage(pkg))
        }
    }
}
