package app.verdant.android.di

import app.verdant.android.data.repository.AuthRepository
import app.verdant.android.data.repository.DefaultTaskRepository
import app.verdant.android.data.repository.InviteOps
import app.verdant.android.data.repository.OrgRepository
import app.verdant.android.data.repository.OrgRepositoryImpl
import app.verdant.android.data.repository.SaleLotRepository
import app.verdant.android.data.repository.SaleLotRepositoryImpl
import app.verdant.android.data.repository.SaleRepository
import app.verdant.android.data.repository.SaleRepositoryImpl
import app.verdant.android.data.repository.SeasonRepository
import app.verdant.android.data.repository.SeasonRepositoryImpl
import app.verdant.android.data.repository.TaskRepository
import app.verdant.android.data.repository.UserRefresher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds repository interfaces to their default implementations so ViewModels
 * can depend on the interface (and tests can swap in a fake).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: DefaultTaskRepository): TaskRepository

    @Binds
    @Singleton
    abstract fun bindOrgRepository(impl: OrgRepositoryImpl): OrgRepository

    @Binds abstract fun bindInviteOps(impl: AuthRepository): InviteOps
    @Binds abstract fun bindUserRefresher(impl: AuthRepository): UserRefresher

    @Binds abstract fun bindSaleRepository(impl: SaleRepositoryImpl): SaleRepository
    @Binds abstract fun bindSaleLotRepository(impl: SaleLotRepositoryImpl): SaleLotRepository
    @Binds abstract fun bindSeasonRepository(impl: SeasonRepositoryImpl): SeasonRepository
}
