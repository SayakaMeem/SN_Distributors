package com.example.sn_distributors.ui.screens

import androidx.compose.runtime.Composable
import com.example.sn_distributors.data.model.UserAccount
import com.example.sn_distributors.data.repository.CrmRepository

/** The signed-in distributor's real catalog and persisted purchase orders. */
@Composable
fun DistributorPortalScreen(user: UserAccount, accounts: List<UserAccount>, repository: CrmRepository, onLogout: () -> Unit) {
    OrdersScreen(user, accounts, repository, onLogout)
}

