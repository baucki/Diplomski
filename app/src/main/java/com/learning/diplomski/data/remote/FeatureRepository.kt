package com.learning.diplomski.data.remote

import com.arcgismaps.data.Feature
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.mapping.layers.FeatureLayer

interface FeatureRepository {
    suspend fun addFeature(feature: Feature, onResult: (String) -> Unit)
    suspend fun deleteFeature(feature: Feature, onResult: (Boolean) -> Unit)

    suspend fun selectFeatures(
        queryParameters: QueryParameters,
        featureLayer: FeatureLayer,
        onResult: (List<Feature>) -> Unit,
        onError: (String) -> Unit
    )
    suspend fun updateFeature(feature: Feature, onResult: (Boolean) -> Unit)
}