package com.learning.diplomski.di

import com.arcgismaps.data.ServiceFeatureTable
import com.learning.diplomski.data.remote.FeatureRepository
import com.learning.diplomski.data.remote.FeatureRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun providesServiceFeatureTable(): ServiceFeatureTable{
        return ServiceFeatureTable("http://192.168.1.18:6080/arcgis/rest/services/Servis_SP4_FieldTools/FeatureServer/0")
    }

    @Provides
    @Singleton
    fun providesFeatureRepository(serviceFeatureTable: ServiceFeatureTable): FeatureRepository {
        return FeatureRepositoryImpl(serviceFeatureTable)
    }

}