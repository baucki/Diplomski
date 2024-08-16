package com.learning.diplomski.viewmodels.mainviewmodel.usecase

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.MapView
import javax.inject.Inject

class BasemapUseCase @Inject constructor() {

    fun showBasemapSelectionDialog(
        context: Context,
        mapView: MapView,
        basemapOptions: Array<Pair<String, BasemapStyle>>,
        featureLayer: FeatureLayer
    ) {
        val basemapNames = basemapOptions.map { it.first }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Select Basemap")
            .setItems(basemapNames) { _, which ->
                val selectedBasemap = basemapOptions[which].second
                val viewPoint = mapView.getCurrentViewpoint(ViewpointType.CenterAndScale)
                mapView.map?.operationalLayers?.clear()
                mapView.map = ArcGISMap(selectedBasemap).apply {
                    operationalLayers.add(featureLayer)
                }
                viewPoint?.let {
                    mapView.setViewpoint(it)
                }
            }
            .show()
    }


}