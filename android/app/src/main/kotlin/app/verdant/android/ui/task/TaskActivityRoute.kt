package app.verdant.android.ui.task

import app.verdant.android.data.model.ScheduledTaskResponse
import app.verdant.android.ui.navigation.Screen

fun taskActivityRoute(task: ScheduledTaskResponse): String? {
    val species = task.speciesId?.let { "&speciesId=$it" } ?: ""
    return when (task.activityType) {
        "SOW" -> Screen.Sow.create(taskId = task.id, speciesId = task.speciesId, bedId = task.bedId)
        "POT_UP" -> Screen.BatchPotUp.create(taskId = task.id, speciesId = task.speciesId)
        "PLANT" -> "activity/plant-picker/SEEDED,POTTED_UP/plant-out?taskId=${task.id}$species"
        "HARVEST" -> "activity/plant-picker/GROWING/harvest?taskId=${task.id}$species"
        "RECOVER" -> "activity/plant-picker/GROWING/recover?taskId=${task.id}$species"
        "DISCARD" -> "activity/plant-picker/SEEDED,POTTED_UP,PLANTED_OUT,GROWING,HARVESTED,RECOVERED/discard?taskId=${task.id}$species"
        else -> null
    }
}
