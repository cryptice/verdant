package app.verdant.repository

import app.verdant.entity.PlantWorkflowProgress
import app.verdant.entity.SpeciesWorkflowStep
import app.verdant.entity.WorkflowTemplate
import app.verdant.entity.WorkflowTemplateStep
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Types

@ApplicationScoped
class WorkflowRepository(private val ds: AgroalDataSource) {

    // ── WorkflowTemplate ──

    fun findTemplateById(id: Long): WorkflowTemplate? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM workflow_template WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toTemplate() else null }
            }
        }

    fun findTemplatesByOrgId(orgId: Long): List<WorkflowTemplate> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM workflow_template WHERE org_id = ? ORDER BY name").use { ps ->
                ps.setLong(1, orgId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toTemplate()) }
                }
            }
        }

    fun persistTemplate(template: WorkflowTemplate): WorkflowTemplate {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO workflow_template (org_id, name, description, created_at, updated_at) VALUES (?, ?, ?, now(), now())",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, template.orgId)
                ps.setString(2, template.name)
                ps.setString(3, template.description)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return template.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun updateTemplate(template: WorkflowTemplate) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "UPDATE workflow_template SET name = ?, description = ?, updated_at = now() WHERE id = ?"
            ).use { ps ->
                ps.setString(1, template.name)
                ps.setString(2, template.description)
                ps.setLong(3, template.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun deleteTemplate(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM workflow_template WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Workflow template not found")
            }
        }
    }

    // ── WorkflowTemplateStep ──

    fun findStepsByTemplateId(templateId: Long): List<WorkflowTemplateStep> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM workflow_template_step WHERE template_id = ? ORDER BY sort_order").use { ps ->
                ps.setLong(1, templateId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toTemplateStep()) }
                }
            }
        }

    fun persistStep(step: WorkflowTemplateStep): WorkflowTemplateStep {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO workflow_template_step (template_id, name, description, event_type, days_after_previous,
                   is_optional, is_side_branch, side_branch_name, sort_order, suggested_supply_type_id, suggested_quantity)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, step.templateId)
                ps.setString(2, step.name)
                ps.setString(3, step.description)
                ps.setString(4, step.eventType)
                ps.setObject(5, step.daysAfterPrevious)
                ps.setBoolean(6, step.isOptional)
                ps.setBoolean(7, step.isSideBranch)
                ps.setString(8, step.sideBranchName)
                ps.setInt(9, step.sortOrder)
                step.suggestedSupplyTypeId?.let { ps.setLong(10, it) } ?: ps.setNull(10, Types.BIGINT)
                step.suggestedQuantity?.let { ps.setBigDecimal(11, it) } ?: ps.setNull(11, Types.NUMERIC)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return step.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun updateStep(step: WorkflowTemplateStep) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE workflow_template_step SET name = ?, description = ?, event_type = ?, days_after_previous = ?,
                   is_optional = ?, is_side_branch = ?, side_branch_name = ?, sort_order = ?,
                   suggested_supply_type_id = ?, suggested_quantity = ?
                   WHERE id = ?"""
            ).use { ps ->
                ps.setString(1, step.name)
                ps.setString(2, step.description)
                ps.setString(3, step.eventType)
                ps.setObject(4, step.daysAfterPrevious)
                ps.setBoolean(5, step.isOptional)
                ps.setBoolean(6, step.isSideBranch)
                ps.setString(7, step.sideBranchName)
                ps.setInt(8, step.sortOrder)
                step.suggestedSupplyTypeId?.let { ps.setLong(9, it) } ?: ps.setNull(9, Types.BIGINT)
                step.suggestedQuantity?.let { ps.setBigDecimal(10, it) } ?: ps.setNull(10, Types.NUMERIC)
                ps.setLong(11, step.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun deleteStep(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM workflow_template_step WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Workflow template step not found")
            }
        }
    }

    // ── SpeciesWorkflowStep ──

    fun findStepsBySpeciesId(speciesId: Long): List<SpeciesWorkflowStep> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species_workflow_step WHERE species_id = ? ORDER BY sort_order").use { ps ->
                ps.setLong(1, speciesId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSpeciesStep()) }
                }
            }
        }

    fun persistSpeciesStep(step: SpeciesWorkflowStep): SpeciesWorkflowStep {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO species_workflow_step (species_id, template_step_id, name, description, event_type,
                   days_after_previous, is_optional, is_side_branch, side_branch_name, sort_order,
                   suggested_supply_type_id, suggested_quantity)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, step.speciesId)
                ps.setObject(2, step.templateStepId)
                ps.setString(3, step.name)
                ps.setString(4, step.description)
                ps.setString(5, step.eventType)
                ps.setObject(6, step.daysAfterPrevious)
                ps.setBoolean(7, step.isOptional)
                ps.setBoolean(8, step.isSideBranch)
                ps.setString(9, step.sideBranchName)
                ps.setInt(10, step.sortOrder)
                step.suggestedSupplyTypeId?.let { ps.setLong(11, it) } ?: ps.setNull(11, Types.BIGINT)
                step.suggestedQuantity?.let { ps.setBigDecimal(12, it) } ?: ps.setNull(12, Types.NUMERIC)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return step.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun updateSpeciesStep(step: SpeciesWorkflowStep) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE species_workflow_step SET name = ?, description = ?, event_type = ?, days_after_previous = ?,
                   is_optional = ?, is_side_branch = ?, side_branch_name = ?, sort_order = ?,
                   suggested_supply_type_id = ?, suggested_quantity = ?
                   WHERE id = ?"""
            ).use { ps ->
                ps.setString(1, step.name)
                ps.setString(2, step.description)
                ps.setString(3, step.eventType)
                ps.setObject(4, step.daysAfterPrevious)
                ps.setBoolean(5, step.isOptional)
                ps.setBoolean(6, step.isSideBranch)
                ps.setString(7, step.sideBranchName)
                ps.setInt(8, step.sortOrder)
                step.suggestedSupplyTypeId?.let { ps.setLong(9, it) } ?: ps.setNull(9, Types.BIGINT)
                step.suggestedQuantity?.let { ps.setBigDecimal(10, it) } ?: ps.setNull(10, Types.NUMERIC)
                ps.setLong(11, step.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun deleteSpeciesStep(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM species_workflow_step WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Species workflow step not found")
            }
        }
    }

    fun deleteStepsBySpeciesId(speciesId: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM species_workflow_step WHERE species_id = ?").use { ps ->
                ps.setLong(1, speciesId)
                ps.executeUpdate()
            }
        }
    }

    // ── PlantWorkflowProgress ──

    fun findProgressByPlantId(plantId: Long): List<PlantWorkflowProgress> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM plant_workflow_progress WHERE plant_id = ?").use { ps ->
                ps.setLong(1, plantId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(
                            PlantWorkflowProgress(
                                plantId = rs.getLong("plant_id"),
                                stepId = rs.getLong("step_id"),
                                completedAt = rs.getTimestamp("completed_at").toInstant(),
                            )
                        )
                    }
                }
            }
        }

    fun findProgressByPlantIds(plantIds: Set<Long>): Map<Long, List<Long>> {
        if (plantIds.isEmpty()) return emptyMap()
        val placeholders = plantIds.joinToString(",") { "?" }
        return ds.connection.use { conn ->
            conn.prepareStatement("SELECT plant_id, step_id FROM plant_workflow_progress WHERE plant_id IN ($placeholders)").use { ps ->
                plantIds.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                ps.executeQuery().use { rs ->
                    val result = mutableMapOf<Long, MutableList<Long>>()
                    while (rs.next()) {
                        result.getOrPut(rs.getLong("plant_id")) { mutableListOf() }
                            .add(rs.getLong("step_id"))
                    }
                    result
                }
            }
        }
    }

    fun findPlantIdsByIncompleteStep(speciesId: Long, stepId: Long, orgId: Long): List<Long> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT p.id FROM plant p
                   WHERE p.species_id = ? AND p.org_id = ?
                   AND p.id NOT IN (SELECT plant_id FROM plant_workflow_progress WHERE step_id = ?)"""
            ).use { ps ->
                ps.setLong(1, speciesId)
                ps.setLong(2, orgId)
                ps.setLong(3, stepId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.getLong("id")) }
                }
            }
        }

    fun recordProgress(plantId: Long, stepId: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO plant_workflow_progress (plant_id, step_id, completed_at) VALUES (?, ?, now()) ON CONFLICT DO NOTHING"
            ).use { ps ->
                ps.setLong(1, plantId)
                ps.setLong(2, stepId)
                ps.executeUpdate()
            }
        }
    }

    fun recordProgressBatch(plantIds: List<Long>, stepId: Long) {
        if (plantIds.isEmpty()) return
        ds.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO plant_workflow_progress (plant_id, step_id, completed_at) VALUES (?, ?, now()) ON CONFLICT DO NOTHING"
            ).use { ps ->
                for (plantId in plantIds) {
                    ps.setLong(1, plantId)
                    ps.setLong(2, stepId)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }

    // ── Direct lookups ──

    fun findTemplateStepById(stepId: Long): WorkflowTemplateStep? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM workflow_template_step WHERE id = ?").use { ps ->
                ps.setLong(1, stepId)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toTemplateStep() else null }
            }
        }

    fun findSpeciesStepById(stepId: Long): SpeciesWorkflowStep? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species_workflow_step WHERE id = ?").use { ps ->
                ps.setLong(1, stepId)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toSpeciesStep() else null }
            }
        }

    // ── ResultSet mappers ──

    private fun ResultSet.toTemplate() = WorkflowTemplate(
        id = getLong("id"),
        orgId = getLong("org_id"),
        name = getString("name"),
        description = getString("description"),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )

    private fun ResultSet.toTemplateStep() = WorkflowTemplateStep(
        id = getLong("id"),
        templateId = getLong("template_id"),
        name = getString("name"),
        description = getString("description"),
        eventType = getString("event_type"),
        daysAfterPrevious = getObject("days_after_previous") as? Int,
        isOptional = getBoolean("is_optional"),
        isSideBranch = getBoolean("is_side_branch"),
        sideBranchName = getString("side_branch_name"),
        sortOrder = getInt("sort_order"),
        suggestedSupplyTypeId = getLong("suggested_supply_type_id").takeIf { !wasNull() },
        suggestedQuantity = getBigDecimal("suggested_quantity"),
    )

    private fun ResultSet.toSpeciesStep() = SpeciesWorkflowStep(
        id = getLong("id"),
        speciesId = getLong("species_id"),
        templateStepId = getObject("template_step_id") as? Long,
        name = getString("name"),
        description = getString("description"),
        eventType = getString("event_type"),
        daysAfterPrevious = getObject("days_after_previous") as? Int,
        isOptional = getBoolean("is_optional"),
        isSideBranch = getBoolean("is_side_branch"),
        sideBranchName = getString("side_branch_name"),
        sortOrder = getInt("sort_order"),
        suggestedSupplyTypeId = getLong("suggested_supply_type_id").takeIf { !wasNull() },
        suggestedQuantity = getBigDecimal("suggested_quantity"),
    )
}
