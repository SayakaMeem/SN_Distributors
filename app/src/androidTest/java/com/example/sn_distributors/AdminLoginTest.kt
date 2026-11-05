package com.example.sn_distributors

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class AdminLoginTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun defaultAdminCredentialsOpenMainDashboard() {
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Email address").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Email address").performScrollTo().performTextInput("admin@sn.com")
        compose.onNodeWithText("Password").performScrollTo().performTextInput("Admin@123")
        compose.onNodeWithText("LOG IN").performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Orders").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Orders").assertIsDisplayed()
        compose.onNodeWithText("Products").assertIsDisplayed()
        compose.onNodeWithText("Accounts").assertIsDisplayed()
        compose.onNodeWithText("Dashboard").assertIsSelected()
        compose.onNodeWithText("Admin dashboard").assertIsDisplayed()
        compose.onNodeWithText("+ Customer & purchase order").assertDoesNotExist()
    }
}
