package com.learning.diplomski.data.remote

import com.arcgismaps.data.Feature
import com.arcgismaps.data.FeatureQueryResult
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.mapping.layers.FeatureLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FeatureRepositoryImpl @Inject constructor(
    private val serviceFeatureTable: ServiceFeatureTable
) : FeatureRepository {

    override suspend fun addFeature(feature: Feature, onResult: (String) -> Unit) {
        serviceFeatureTable.addFeature(feature).apply {
            onSuccess {
                serviceFeatureTable.applyEdits().onSuccess {
                    onResult("Feature added successfully.")
                }.onFailure {
                    onResult("Failed to apply edits: ${it.message}")
                }
            }
            onFailure {
                onResult("Failed to add feature: ${it.message}")
            }
        }
    }

    override suspend fun deleteFeature(feature: Feature, onResult: (Boolean) -> Unit) {
        serviceFeatureTable.deleteFeature(feature).apply {
            onSuccess {
                serviceFeatureTable.applyEdits().onSuccess {
                    onResult(true) // Indicate success
                }.onFailure {
                    onResult(false) // Indicate failure
                }
            }
            onFailure {
                onResult(false) // Indicate failure
            }
        }
    }

    override suspend fun selectFeatures(
        queryParameters: QueryParameters,
        featureLayer: FeatureLayer,
        onResult: (List<Feature>) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val featureQueryResult = serviceFeatureTable.queryFeatures(queryParameters).getOrThrow() as FeatureQueryResult
                val selectedFeatures = mutableListOf<Feature>()
                for (feature in featureQueryResult) {
                    featureLayer.selectFeature(feature)
                    selectedFeatures.add(feature)
                }
                onResult(selectedFeatures)
            } catch (e: Exception) {
                onError("Error selecting features: ${e.message}")
            }
        }
    }

    override suspend fun updateFeature(feature: Feature, onResult: (Boolean) -> Unit) {
        val serviceFeatureTable = feature.featureTable as? ServiceFeatureTable
        if (serviceFeatureTable != null) {
            try {
                serviceFeatureTable.updateFeature(feature).apply {
                    onSuccess {
                        serviceFeatureTable.applyEdits().onSuccess {
                            onResult(true)
                        }.onFailure {
                            onResult(false)
                        }
                    }
                    onFailure {
                        onResult(false)
                    }
                }
            } catch (e: Exception) {
                onResult(false)
            }
        } else {
            onResult(false)
        }
    }
}