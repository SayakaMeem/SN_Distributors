package com.example.sn_distributors.viewmodel

import androidx.lifecycle.ViewModel
import com.example.sn_distributors.data.model.ApprovalStatus
import com.example.sn_distributors.data.model.CartItem
import com.example.sn_distributors.data.model.Distributor
import com.example.sn_distributors.data.model.PortalTab
import com.example.sn_distributors.data.model.ProductItem
import com.example.sn_distributors.data.model.Requisition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DistributorViewModel : ViewModel() {

    private val _currentTab = MutableStateFlow(PortalTab.PRODUCTS)
    val currentTab: StateFlow<PortalTab> = _currentTab.asStateFlow()

    private val _distributorToken = MutableStateFlow<String?>(null)
    val distributorToken: StateFlow<String?> = _distributorToken.asStateFlow()

    private val _distributor = MutableStateFlow<Distributor?>(null)
    val distributor: StateFlow<Distributor?> = _distributor.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _products = MutableStateFlow<List<ProductItem>>(emptyList())
    val products: StateFlow<List<ProductItem>> = _products.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _requisitions = MutableStateFlow<List<Requisition>>(emptyList())
    val requisitions: StateFlow<List<Requisition>> = _requisitions.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMockCatalog()
        authenticateDistributor(null)
    }

    fun authenticateDistributor(token: String?) {
        _isLoading.value = true
        _distributorToken.value = token
        val tokenVal = token ?: "DST-0001"
        _distributor.value = Distributor(
            id = "DST-0001",
            name = "NAHA CONSTRUCTION & DISTRIBUTORS",
            code = "DST-0001",
            token = tokenVal,
            creditLimit = 500000.0,
            currentBalance = 142500.0,
        )

        if (_requisitions.value.isEmpty()) {
            _requisitions.value = listOf(
                Requisition(
                    id = "DR-0007",
                    distributorId = "DST-0001",
                    items = listOf(
                        CartItem(
                            product = ProductItem(
                                id = "P-101",
                                name = "APPLE WHITE PREMIUM",
                                code = "SN-AW-01",
                                basePrice = 150.0,
                                discountedPrice = 120.0,
                                unit = "Bag (50kg)",
                                recipeBreakdown = "Titanium Dioxide 45% + Resin Polymer 30% + Calcium Powder 25%",
                            ),
                            quantity = 10,
                        ),
                    ),
                    totalAmount = 1200.0,
                    status = ApprovalStatus.APPROVED_SUPERVISOR,
                    createdAt = "2026-11-03 10:15",
                    updatedAt = "2026-11-03 11:30",
                ),
            )
        }
    }

    fun selectTab(tab: PortalTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addToCart(product: ProductItem, quantity: Int) {
        if (quantity <= 0) return
        val currentCart = _cart.value.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = currentCart[index]
            currentCart[index] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            currentCart.add(CartItem(product = product, quantity = quantity))
        }
        _cart.value = currentCart
    }

    fun requisitionSingleProduct(product: ProductItem, quantity: Int) {
        if (quantity <= 0) return
        val reqId = "DR-000" + (_requisitions.value.size + 8)
        val newReq = Requisition(
            id = reqId,
            distributorId = _distributor.value?.id ?: "DST-0001",
            items = listOf(CartItem(product = product, quantity = quantity)),
            totalAmount = product.discountedPrice * quantity,
            status = ApprovalStatus.PENDING_SUPERVISOR,
            createdAt = "Just now",
            updatedAt = "Just now",
        )
        _requisitions.value = listOf(newReq) + _requisitions.value
        _currentTab.value = PortalTab.DASHBOARD
    }

    fun advanceRequisitionStatus(requisitionId: String) {
        val currentList = _requisitions.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == requisitionId }
        if (index >= 0) {
            val req = currentList[index]
            val nextStatus = when (req.status) {
                ApprovalStatus.PENDING_SUPERVISOR -> ApprovalStatus.APPROVED_SUPERVISOR
                ApprovalStatus.APPROVED_SUPERVISOR -> ApprovalStatus.PENDING_ACCOUNTS
                ApprovalStatus.PENDING_ACCOUNTS -> ApprovalStatus.APPROVED_ACCOUNTS
                ApprovalStatus.APPROVED_ACCOUNTS -> ApprovalStatus.FACTORY_ACCEPTED
                ApprovalStatus.FACTORY_ACCEPTED -> ApprovalStatus.READY_FOR_DELIVERY
                ApprovalStatus.READY_FOR_DELIVERY -> ApprovalStatus.READY_FOR_DELIVERY
            }
            currentList[index] = req.copy(status = nextStatus, updatedAt = "Updated just now")
            _requisitions.value = currentList
        }
    }

    private fun loadMockCatalog() {
        _products.value = listOf(
            ProductItem(
                id = "P-101",
                name = "APPLE WHITE PREMIUM",
                code = "SN-AW-01",
                basePrice = 150.0,
                discountedPrice = 120.0,
                unit = "Bag (50kg)",
                recipeBreakdown = "Titanium Dioxide 45% + Resin Polymer 30% + Calcium Powder 25%",
            ),
            ProductItem(
                id = "P-102",
                name = "SN-ULTRA-TOP COATING",
                code = "SN-UT-02",
                basePrice = 320.0,
                discountedPrice = 275.0,
                unit = "Bucket (20L)",
                recipeBreakdown = "Polyurethane Hybrid 60% + UV Stabilizer 20% + Acrylic Binder 20%",
            ),
            ProductItem(
                id = "P-103",
                name = "SN-PRIMER SEALER PRO",
                code = "SN-PS-03",
                basePrice = 210.0,
                discountedPrice = 180.0,
                unit = "Bucket (20L)",
                recipeBreakdown = "Silicone Emulsion 40% + Water Repellent Resin 35% + Fillers 25%",
            ),
            ProductItem(
                id = "P-104",
                name = "SN-HEAVY-DUTY EPOXY",
                code = "SN-EP-04",
                basePrice = 450.0,
                discountedPrice = 390.0,
                unit = "Set (Part A+B)",
                recipeBreakdown = "Bisphenol-A Epoxy Resin 70% + Hardener Polyamine 30%",
            ),
        )
    }
}
