package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Customer
import app.verdant.repository.CustomerRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class CustomerService(
    private val repo: CustomerRepository,
) {
    fun getCustomersForUser(orgId: Long, limit: Int = 50, offset: Int = 0): List<CustomerResponse> =
        repo.findByOrgId(orgId, limit, offset).map { it.toResponse() }

    fun getCustomer(id: Long, orgId: Long): CustomerResponse {
        val customer = repo.findById(id) ?: throw NotFoundException("Customer not found")
        if (customer.orgId != orgId) throw NotFoundException("Customer not found")
        return customer.toResponse()
    }

    fun createCustomer(request: CreateCustomerRequest, orgId: Long): CustomerResponse {
        val customer = repo.persist(
            Customer(
                orgId = orgId,
                name = request.name,
                channel = request.channel,
                contactInfo = request.contactInfo,
                notes = request.notes,
            )
        )
        return customer.toResponse()
    }

    fun updateCustomer(id: Long, request: UpdateCustomerRequest, orgId: Long): CustomerResponse {
        val customer = repo.findById(id) ?: throw NotFoundException("Customer not found")
        if (customer.orgId != orgId) throw NotFoundException("Customer not found")
        val updated = customer.copy(
            name = request.name ?: customer.name,
            channel = request.channel ?: customer.channel,
            contactInfo = request.contactInfo ?: customer.contactInfo,
            notes = request.notes ?: customer.notes,
        )
        repo.update(updated)
        return updated.toResponse()
    }

    fun deleteCustomer(id: Long, orgId: Long) {
        val customer = repo.findById(id) ?: throw NotFoundException("Customer not found")
        if (customer.orgId != orgId) throw NotFoundException("Customer not found")
        repo.delete(id)
    }

    private fun Customer.toResponse() = CustomerResponse(
        id = id!!,
        name = name,
        channel = channel,
        contactInfo = contactInfo,
        notes = notes,
        createdAt = createdAt,
    )
}
