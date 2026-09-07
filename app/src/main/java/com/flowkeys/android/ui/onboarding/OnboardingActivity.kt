package com.flowkeys.android.ui.onboarding

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.flowkeys.android.ui.theme.FlowKeysTheme

class OnboardingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlowKeysTheme {
                OnboardingScreen(
                    onFinished = {
                        finish()
                    }
                )
            }
        }
    }
}
