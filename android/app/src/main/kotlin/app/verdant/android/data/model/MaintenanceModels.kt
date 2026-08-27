package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

data class MaintenanceRuleResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("bedId") val bedId: Long?,
    @SerializedName("bedName") val bedName: String?,
    @SerializedName("gardenAreaId") val gardenAreaId: Long?,
    @SerializedName("gardenAreaName") val gardenAreaName: String?,
    @SerializedName("activityType") val activityType: String,
    @SerializedName("intervalDays") val intervalDays: Int,
    @SerializedName("anchorDate") val anchorDate: String?,
    @SerializedName("seasonStartMonth") val seasonStartMonth: Int?,
    @SerializedName("seasonStartDay") val seasonStartDay: Int?,
    @SerializedName("seasonEndMonth") val seasonEndMonth: Int?,
    @SerializedName("seasonEndDay") val seasonEndDay: Int?,
    @SerializedName("active") val active: Boolean,
    @SerializedName("notes") val notes: String?,
    /** Derived server-side from the event log. Null when never done. */
    @SerializedName("lastDoneDate") val lastDoneDate: String?,
    /** Derived server-side: when the next task will be created. */
    @SerializedName("nextDueDate") val nextDueDate: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
)

/** Exactly one of [bedId] / [gardenAreaId] — supplying both or neither is a 400. */
data class CreateMaintenanceRuleRequest(
    @SerializedName("bedId") val bedId: Long? = null,
    @SerializedName("gardenAreaId") val gardenAreaId: Long? = null,
    @SerializedName("activityType") val activityType: String,
    @SerializedName("intervalDays") val intervalDays: Int,
    @SerializedName("anchorDate") val anchorDate: String? = null,
    @SerializedName("seasonStartMonth") val seasonStartMonth: Int? = null,
    @SerializedName("seasonStartDay") val seasonStartDay: Int? = null,
    @SerializedName("seasonEndMonth") val seasonEndMonth: Int? = null,
    @SerializedName("seasonEndDay") val seasonEndDay: Int? = null,
    @SerializedName("notes") val notes: String? = null,
)

data class UpdateMaintenanceRuleRequest(
    @SerializedName("activityType") val activityType: String? = null,
    @SerializedName("intervalDays") val intervalDays: Int? = null,
    @SerializedName("anchorDate") val anchorDate: String? = null,
    @SerializedName("seasonStartMonth") val seasonStartMonth: Int? = null,
    @SerializedName("seasonStartDay") val seasonStartDay: Int? = null,
    @SerializedName("seasonEndMonth") val seasonEndMonth: Int? = null,
    @SerializedName("seasonEndDay") val seasonEndDay: Int? = null,
    /**
     * The ONLY way to remove a season window — nulls are indistinguishable
     * from omitted fields server-side. Cannot be combined with any season*
     * value; doing so is a 400.
     */
    @SerializedName("clearSeasonWindow") val clearSeasonWindow: Boolean = false,
    @SerializedName("active") val active: Boolean? = null,
    @SerializedName("notes") val notes: String? = null,
)
