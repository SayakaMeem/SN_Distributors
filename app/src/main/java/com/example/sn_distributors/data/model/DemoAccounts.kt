package com.example.sn_distributors.data.model

data class DemoLogin(val account: UserAccount, val password: String)

/** Local demonstration credentials, also used by the login form's demo selector. */
object DemoAccounts {
    val logins = listOf(
        DemoLogin(UserAccount("ADM-0001", "System Admin", "admin@sn.com", UserRole.ADMIN,
            status = AccountStatus.ACTIVE), "Admin@123"),
        DemoLogin(UserAccount("STF-DEMO-SUPERVISOR", "Demo Supervisor", "demo.supervisor@sn.com", UserRole.STAFF,
            StaffDepartment.SUPERVISOR, AccountStatus.ACTIVE), "Supervisor@123"),
        DemoLogin(UserAccount("STF-DEMO-ACCOUNTS", "Demo Accounts", "demo.accounts@sn.com", UserRole.STAFF,
            StaffDepartment.ACCOUNTS, AccountStatus.ACTIVE), "Accounts@123"),
        DemoLogin(UserAccount("STF-DEMO-FACTORY", "Demo Factory", "demo.factory@sn.com", UserRole.STAFF,
            StaffDepartment.FACTORY, AccountStatus.ACTIVE), "Factory@123"),
        DemoLogin(UserAccount("DST-DEMO-0001", "Demo Distributor", "demo.distributor@sn.com", UserRole.DISTRIBUTOR,
            status = AccountStatus.ACTIVE), "Distributor@123")
    )
}
