package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Customer
import app.verdant.repository.CustomerRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class CustomerService(
    private val repo: CustomerRepository,
) {
    fun getCustomersForUser(userId: Long, limit: Int = 50, offset: Int = 0): List<CustomerResponse> =
        repo.findByUserId(userId, limit, offset).map { it.toResponse() }

    fun getCustomer(id: Long, userId: Long): CustomerResponse {
        val customer = repo.findById(id) ?: throw NotFoundException("Customer not found")
        if (customer.userId != userId) throw ForbiddenException()
        return customer.toResponse()
    }

    fun createCustomer(request: CreateCustomerRequest, userId: Long): CustomerResponse {
        val customer = repo.persist(
            Customer(
                userId = userId,
                name = request.name,
                channel = request.channel,
                contactInfo = request.contactInfo,
                notes = request.notes,
            )
        )
        return customer.toResponse()
    }

    fun updateCustomer(id: Long, request: UpdateCustomerRequest, userId: Long): CustomerResponse {
        val customer = repo.findById(id) ?: throw NotFoundException("Customer not found")
        if (customer.userId != userId) throw ForbiddenException()
        val updated = customer.copy(
            name = request.name ?: customer.name,
            channel = request.channel ?: customer.channel,
            contactInfo = request.contactInfo ?: customer.contactInfo,
            notes = request.notes ?: customer.notes,
        )
        repo.update(updated)
        return updated.toResponse()
    }

    fun deleteCustomer(id: Long, userId: Long) {
        val customer = repo.findById(id) ?: throw NotFoundException("Customer not found")
        if (customer.userId != userId) throw ForbiddenException()
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
