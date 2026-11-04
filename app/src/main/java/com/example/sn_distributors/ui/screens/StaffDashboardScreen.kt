
package com.example.sn_distributors.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sn_distributors.data.model.StaffDepartment
import com.example.sn_distributors.data.model.UserAccount

private val StaffGreen = Color(0xFF0A3420)
private val StaffBackground = Color(0xFFF4F6F5)

@Composable
fun StaffDashboardScreen(
    staff: UserAccount,
    onLogout: () -> Unit
) {
    val department = staff.department

    val departmentTitle = when (department) {
        StaffDepartment.SUPERVISOR -> "Supervisor Dashboard"
        StaffDepartment.ACCOUNTS -> "Accounts Dashboard"
        StaffDepartment.FACTORY -> "Factory Dashboard"
        null -> "Staff Dashboard"
    }

    val departmentDescription = when (department) {
        StaffDepartment.SUPERVISOR ->
            "Review and approve distributor requisitions."

        StaffDepartment.ACCOUNTS ->
            "Review financial details and approve requisitions."

        StaffDepartment.FACTORY ->
            "Manage production approval and delivery scheduling."

        null ->
            "No department has been assigned to this account."
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = StaffBackground
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "SN Chemical",
                            color = StaffGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = departmentTitle,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Staff Management Portal",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }

                    TextButton(onClick = onLogout) {
                        Text(
                            text = "Logout",
                            color = StaffGreen
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Welcome, ${staff.name}",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Employee ID: ${staff.id}",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )

                        Text(
                            text = "Department: ${department?.name ?: "Not assigned"}",
                            color = StaffGreen,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = departmentDescription,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Department Overview",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                when (department) {

                    StaffDepartment.SUPERVISOR -> {
                        DepartmentCard(
                            title = "Distributor Requisitions",
                            description = "Review new purchase requests submitted by distributors.",
                            status = "Supervisor Approval"
                        )
                    }

                    StaffDepartment.ACCOUNTS -> {
                        DepartmentCard(
                            title = "Financial Approval",
                            description = "Review requisitions approved by the Supervisor.",
                            status = "Accounts Approval"
                        )
                    }

                    StaffDepartment.FACTORY -> {
                        DepartmentCard(
                            title = "Production & Delivery",
                            description = "Review accounts-approved requisitions, production quantities and delivery schedules.",
                            status = "Factory Approval"
                        )
                    }

                    null -> {
                        DepartmentCard(
                            title = "Access Unavailable",
                            description = "Contact Admin to assign a department.",
                            status = "Unassigned"
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Approval Workflow",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                WorkflowStage(
                    number = "1",
                    title = "Supervisor",
                    description = "Distributor requisition review",
                    isCurrent = department == StaffDepartment.SUPERVISOR
                )
            }

            item {
                WorkflowStage(
                    number = "2",
                    title = "Accounts",
                    description = "Financial approval",
                    isCurrent = department == StaffDepartment.ACCOUNTS
                )
            }

            item {
                WorkflowStage(
                    number = "3",
                    title = "Factory",
                    description = "Production and delivery approval",
                    isCurrent = department == StaffDepartment.FACTORY
                )
            }
        }
    }
}

@Composable
private fun DepartmentCard(
    title: String,
    description: String,
    status: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            Text(
                text = description,
                color = Color.Gray,
                fontSize = 13.sp
            )

            Surface(
                color = Color(0xFFE8F1EB),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 7.dp
                    ),
                    color = StaffGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun WorkflowStage(
    number: String,
    title: String,
    description: String,
    isCurrent: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                Color(0xFFE8F1EB)
            } else {
                Color.White
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = if (isCurrent) {
                    StaffGreen
                } else {
                    Color.LightGray
                },
                shape = RoundedCornerShape(50.dp)
            ) {
                Text(
                    text = number,
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 7.dp
                    ),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                if (isCurrent) {
                    Text(
                        text = "YOUR DEPARTMENT",
                        color = StaffGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
