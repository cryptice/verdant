package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CompleteTaskPartiallyRequest
import app.verdant.android.data.model.CreateScheduledTaskRequest
import app.verdant.android.data.model.ScheduledTaskResponse
import app.verdant.android.data.model.UpdateScheduledTaskRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Scheduled tasks. Interface so ViewModels can be tested with a fake. */
interface TaskRepository {
    suspend fun list(): List<ScheduledTaskResponse>
    suspend fun get(id: Long): ScheduledTaskResponse
    suspend fun create(request: CreateScheduledTaskRequest): ScheduledTaskResponse
    suspend fun update(id: Long, request: UpdateScheduledTaskRequest): ScheduledTaskResponse
    suspend fun completePartially(id: Long, request: CompleteTaskPartiallyRequest)
    suspend fun delete(id: Long)
}

@Singleton
class DefaultTaskRepository @Inject constructor(private val api: VerdantApi) : TaskRepository {
    override suspend fun list() = api.getTasks()
    override suspend fun get(id: Long) = api.getTask(id)
    override suspend fun create(request: CreateScheduledTaskRequest) = api.createTask(request)
    override suspend fun update(id: Long, request: UpdateScheduledTaskRequest) = api.updateTask(id, request)
    override suspend fun completePartially(id: Long, request: CompleteTaskPartiallyRequest) {
        api.completeTaskPartially(id, request)
    }
    override suspend fun delete(id: Long) { api.deleteTask(id) }
}
