package com.elyedali.debttracker

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.elyedali.debttracker.ui.AppRoot
import com.elyedali.debttracker.ui.AppViewModel
import com.elyedali.debttracker.ui.LockScreen
import com.elyedali.debttracker.ui.theme.DebtTheme

class MainActivity : FragmentActivity() {

    private val vm: AppViewModel by viewModels()
    private var authInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DebtTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (vm.locked) LockScreen(onUnlock = { authenticate() }) else AppRoot(vm)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val left = vm.leftAt
        if (left > 0L && SystemClock.elapsedRealtime() - left > 30_000L && vm.lockActive()) {
            vm.locked = true
        }
    }

    override fun onResume() {
        super.onResume()
        (application as App).billing.refresh()
        if (vm.locked) authenticate()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) vm.leftAt = SystemClock.elapsedRealtime()
    }

    private fun authenticate() {
        if (authInProgress) return
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            // The phone has no screen lock anymore: never lock the user out of their data.
            vm.locked = false
            return
        }
        authInProgress = true
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authInProgress = false
                    vm.locked = false
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    authInProgress = false
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Debt Tracker")
            .setSubtitle("Confirm it's you")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }
}
