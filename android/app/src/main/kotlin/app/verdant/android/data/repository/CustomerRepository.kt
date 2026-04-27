package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreateCustomerRequest
import app.verdant.android.data.model.UpdateCustomerRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Customers. */
@Singleton
class CustomerRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun list() = api.getCustomers()
    suspend fun create(request: CreateCustomerRequest) = api.createCustomer(request)
    suspend fun update(id: Long, request: UpdateCustomerRequest) = api.updateCustomer(id, request)
    suspend fun delete(id: Long) = api.deleteCustomer(id)
}
