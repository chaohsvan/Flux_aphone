package com.example.flux

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.flux.app.navigation.AppDestinationLaunchRequest
import com.example.flux.app.navigation.AppLaunchIntent
import com.example.flux.app.navigation.FluxAppNavHost
import com.example.flux.ui.theme.FluxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var destinationLaunchRequest by mutableStateOf<AppDestinationLaunchRequest?>(null)
    private var destinationLaunchRequestId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            updateDestinationLaunchRequest()
        }
        enableEdgeToEdge()
        setContent {
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = {}
            )
            LaunchedEffect(Unit) {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            FluxTheme {
                FluxAppNavHost(destinationLaunchRequest = destinationLaunchRequest)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateDestinationLaunchRequest()
    }

    private fun updateDestinationLaunchRequest() {
        val destination = AppLaunchIntent.destinationFrom(
            intent?.getStringExtra(AppLaunchIntent.EXTRA_DESTINATION)
        ) ?: return
        destinationLaunchRequest = AppDestinationLaunchRequest(
            destination = destination,
            id = ++destinationLaunchRequestId
        )
    }
}
