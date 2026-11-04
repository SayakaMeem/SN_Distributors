package com.example.sn_distributors

import com.example.sn_distributors.data.model.ApprovalStatus
import com.example.sn_distributors.data.model.CartItem
import com.example.sn_distributors.data.model.ProductItem
import com.example.sn_distributors.data.repository.ErpRepository
import com.example.sn_distributors.viewmodel.DistributorViewModel
import org.junit.Assert.*
import org.junit.Test

class WorkflowTest {
    @Test
    fun accountsApprovalMakesRequisitionAvailableToAssignedFactory() {
        val repository = ErpRepository()
        val product = ProductItem("product", "Paint", "P-1", 100.0, 90.0, "Bag", "")
        val requisition = requireNotNull(repository.submitRequisition(
            "distributor", listOf(CartItem(product, 2)), "supervisor", "accounts", "factory"
        ))

        assertTrue(repository.getRequisitionsForFactory("factory").isEmpty())
        assertTrue(repository.approveBySupervisor(requisition.id, "supervisor"))
        assertTrue(repository.approveByAccounts(requisition.id, "accounts"))
        assertEquals(listOf(requisition.id), repository.getRequisitionsForFactory("factory").map { it.id })
        assertTrue(repository.getRequisitionsForFactory("other-factory").isEmpty())
        assertTrue(repository.acceptByFactory(requisition.id, "factory", "2026-11-10"))
        assertEquals(ApprovalStatus.FACTORY_ACCEPTED,
            repository.getRequisitionsForFactory("factory").single().status)
    }

    @Test
    fun distributorLoadingFinishesAfterAuthentication() {
        val viewModel = DistributorViewModel()

        assertNotNull(viewModel.distributor.value)
        assertFalse(viewModel.isLoading.value)
        viewModel.authenticateDistributor("token")
        assertEquals("token", viewModel.distributor.value?.token)
        assertFalse(viewModel.isLoading.value)
    }
}
