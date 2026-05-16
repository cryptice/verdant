package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.EditSaleRequest
import app.verdant.android.data.model.RecordSaleRequest
import app.verdant.android.data.model.SaleLedgerEntry
import app.verdant.android.data.model.SaleResponse
import javax.inject.Inject
import javax.inject.Singleton

/** Recording and editing sales against a specific sale lot. Interface so ViewModels can be tested with a fake. */
interface SaleRepository {
    suspend fun record(lotId: Long, request: RecordSaleRequest): SaleResponse
    suspend fun edit(saleId: Long, request: EditSaleRequest): SaleResponse
    suspend fun listLedger(seasonId: Long?, limit: Int = 500, offset: Int = 0): List<SaleLedgerEntry>
}

@Singleton
class SaleRepositoryImpl @Inject constructor(private val api: VerdantApi) : SaleRepository {
    override suspend fun record(lotId: Long, request: RecordSaleRequest) = api.recordSale(lotId, request)
    override suspend fun edit(saleId: Long, request: EditSaleRequest) = api.editSale(saleId, request)
    override suspend fun listLedger(seasonId: Long?, limit: Int, offset: Int) =
        api.listSales(seasonId, limit, offset)
}
