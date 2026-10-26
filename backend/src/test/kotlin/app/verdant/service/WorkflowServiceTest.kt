package app.verdant.service

import app.verdant.dto.CreateWorkflowStepRequest
import app.verdant.dto.CreateWorkflowTemplateRequest
import app.verdant.dto.UpdateWorkflowStepRequest
import app.verdant.entity.Organization
import app.verdant.repository.OrganizationRepository
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@QuarkusTest
class WorkflowServiceTest {
    @Inject lateinit var service: WorkflowService
    @Inject lateinit var orgs: OrganizationRepository

    @Test
    fun `updating a template step keeps fields sent as null and clears fields sent empty`() {
        val orgId = orgs.persist(Organization(name = "Workflow org")).id!!
        val template = service.createTemplate(CreateWorkflowTemplateRequest(name = "Dahlia"), orgId)
        val step = service.addTemplateStep(template.id, CreateWorkflowStepRequest(
            name = "Avhärdning", description = "Svalare och ljusare", eventType = "MOVED", daysAfterPrevious = 12,
        ), orgId)

        val kept = service.updateTemplateStep(step.id, UpdateWorkflowStepRequest(daysAfterPrevious = 0), orgId)
        assertEquals("MOVED", kept.eventType)
        assertEquals("Svalare och ljusare", kept.description)
        assertEquals(0, kept.daysAfterPrevious)

        val cleared = service.updateTemplateStep(step.id, UpdateWorkflowStepRequest(eventType = "", description = ""), orgId)
        assertNull(cleared.eventType)
        assertNull(cleared.description)
        assertNull(service.getTemplate(template.id, orgId).steps.single().eventType)
    }
}
