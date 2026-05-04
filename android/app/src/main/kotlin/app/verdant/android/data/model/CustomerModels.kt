package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

// ── Customers ──

data class CustomerResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("outletId") val outletId: Long?,
    @SerializedName("contactInfo") val contactInfo: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
)

data class CreateCustomerRequest(
    @SerializedName("name") val name: String,
    @SerializedName("outletId") val outletId: Long? = null,
    @SerializedName("contactInfo") val contactInfo: String? = null,
    @SerializedName("notes") val notes: String? = null,
)

data class UpdateCustomerRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("outletId") val outletId: Long? = null,
    @SerializedName("contactInfo") val contactInfo: String? = null,
    @SerializedName("notes") val notes: String? = null,
)

// Outlet channel values (matches backend Channel enum). Lives here for now since
// the per-customer channel was the original home; a customer's outlet now owns it.
object OutletChannel {
    const val FLORIST = "FLORIST"
    const val FARMERS_MARKET = "FARMERS_MARKET"
    const val CSA = "CSA"
    const val WEDDING = "WEDDING"
    const val WHOLESALE = "WHOLESALE"
    const val DIRECT = "DIRECT"
    const val OTHER = "OTHER"
    val values = listOf(FLORIST, FARMERS_MARKET, CSA, WEDDING, WHOLESALE, DIRECT, OTHER)
}
