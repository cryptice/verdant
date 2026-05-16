package app.verdant.android.di

import app.verdant.android.data.repository.AuthRepository
import app.verdant.android.data.repository.DefaultTaskRepository
import app.verdant.android.data.repository.InviteOps
import app.verdant.android.data.repository.OrgRepository
import app.verdant.android.data.repository.OrgRepositoryImpl
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
}
