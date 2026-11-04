
package com.example.sn_distributors.data.model

enum class UserRole {
    ADMIN,
    STAFF,
    DISTRIBUTOR
}

enum class StaffDepartment {
    SUPERVISOR,
    ACCOUNTS,
    FACTORY
}

enum class AccountStatus {
    PENDING,
    ACTIVE,
    REJECTED,
    SUSPENDED
}

data class UserAccount(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val department: StaffDepartment? = null,
    val status: AccountStatus = AccountStatus.PENDING
) {
    val isActive: Boolean
        get() = status == AccountStatus.ACTIVE
}
