package com.example.sn_distributors

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.sn_distributors.ui.screens.DistributorPortalScreen
import com.example.sn_distributors.ui.theme.SN_DistributorsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Extract deep link token or path if launched via snerpsolution.com/distributor
        val data = intent?.data
        val distributorToken = data?.getQueryParameter("token") ?: "DST-0001"

        setContent {
            SN_DistributorsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DistributorPortalScreen(distributorToken = distributorToken)
                }
            }
        }
    }
}