package com.example.sn_distributors.ui.screens

import androidx.compose.runtime.Composable
import com.example.sn_distributors.data.model.UserAccount
import com.example.sn_distributors.data.repository.CrmRepository

/** Staff land directly in their stage's approval queue. No customer entry forms. */
@Composable
fun StaffDashboardScreen(staff: UserAccount, accounts: List<UserAccount>, repository: CrmRepository, onLogout: () -> Unit) {
    OrdersScreen(staff, accounts, repository, onLogout)
}
