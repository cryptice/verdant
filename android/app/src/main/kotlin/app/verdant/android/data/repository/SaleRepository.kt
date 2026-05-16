package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.EditSaleRequest
import app.verdant.android.data.model.RecordSaleRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Recording and editing sales against a specific sale lot. */
@Singleton
class SaleRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun record(lotId: Long, request: RecordSaleRequest) = api.recordSale(lotId, request)
    suspend fun edit(saleId: Long, request: EditSaleRequest) = api.editSale(saleId, request)

    suspend fun listLedger(
        seasonId: Long?,
        limit: Int = 500,
        offset: Int = 0,
    ): List<app.verdant.android.data.model.SaleLedgerEntry> =
        api.listSales(seasonId, limit, offset)
}
