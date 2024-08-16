package com.learning.diplomski.viewmodels.mainviewmodel.usecase

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.Color
import com.arcgismaps.data.FeatureQueryResult
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.data.SpatialRelationship
import com.arcgismaps.geometry.GeodeticCurveType
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.PolygonBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class SelectionUseCase @Inject constructor() {

    private val _isSelection = MutableLiveData<Boolean>(false)
    val isSelection: LiveData<Boolean> get() = _isSelection

    private val _isCircleSelection = MutableLiveData<Boolean>(false)
    val isCircleSelection: LiveData<Boolean> get() = _isCircleSelection

    private val _isPolygonSelection = MutableLiveData<Boolean>(false)
    val isPolygonSelection: LiveData<Boolean> get() = _isPolygonSelection

    private var selectionCircleGraphic: Graphic? = null
    private var tempSelectionPolygonGraphics: Graphic? = null

    val selectionPoints = mutableListOf<Point>()

    fun selectionButtonClickListener(bottomSheetBehavior: BottomSheetBehavior<View>) {
        _isSelection.value = !_isSelection.value!!
        if (_isSelection.value == true) {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        } else {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                _isSelection.value = true
            }
        }
    }

    fun startCircleSelection(
        circleSelectionRadius: SeekBar,
        btnFinishSelectCircle: Button,
        polygonSelectControls: LinearLayout,
        centerMarker: ImageView,
        tvSelectionType: TextView
    ) {
        _isCircleSelection.value = true
        _isPolygonSelection.value = false
        circleSelectionRadius.visibility = View.VISIBLE
        btnFinishSelectCircle.visibility = View.VISIBLE
        polygonSelectControls.visibility = View.GONE
        centerMarker.visibility = View.GONE
        tvSelectionType.text = "Circle Selection"
        selectionPoints.clear()
    }

    fun startPolygonSelection(
        circleSelectionRadius: SeekBar,
        btnFinishSelectCircle: Button,
        polygonSelectControls: LinearLayout,
        centerMarker: ImageView,
        tvSelectionType: TextView
    ) {
        _isCircleSelection.value = false
        _isPolygonSelection.value = true
        circleSelectionRadius.visibility = View.GONE
        btnFinishSelectCircle.visibility = View.GONE
        polygonSelectControls.visibility = View.VISIBLE
        centerMarker.visibility = View.VISIBLE
        tvSelectionType.text = "Polygon Selection"
        selectionPoints.clear()
    }

    fun drawSelectionCircle(
        radius: Int,
        mapView: MapView,
        graphicsOverlay: GraphicsOverlay
    ) {
        if (selectionPoints.isEmpty()) graphicsOverlay.graphics.clear()
        val centerPoint = mapView.screenToLocation(ScreenCoordinate(mapView.width / 2.0, mapView.height / 2.0))
        val scaledRadius = radius * 50.0;
        val maxDeviation = 1.0;

        // Remove the previous circle graphic if it exists
        selectionCircleGraphic?.let { graphicsOverlay.graphics.remove(it) }

        // Create a circle geometry
        val circleGeometry = GeometryEngine.bufferGeodeticOrNull(
            centerPoint!!,
            scaledRadius,
            LinearUnit.meters,
            maxDeviation,
            GeodeticCurveType.Geodesic
        )

        // Create a symbol for the circle
        val circleSymbol = SimpleFillSymbol(
            SimpleFillSymbolStyle.Solid, Color.fromRgba(0, 0, 0, 100), SimpleLineSymbol(
                SimpleLineSymbolStyle.Solid, Color.black, 2.0f)
        )

        // Create a graphic for the circle and add it to the overlay
        selectionCircleGraphic = Graphic(circleGeometry, circleSymbol)
        graphicsOverlay.graphics.add(selectionCircleGraphic!!)
    }

    fun selectFeaturesInCircle(
        featureLayer: FeatureLayer,
        serviceFeatureTable: ServiceFeatureTable,
        scope: CoroutineScope,
        onResult: (String) -> Unit
    ) {
        selectionCircleGraphic?.let { graphic ->
            val circleGeometry = graphic.geometry as Polygon

            val queryParameters = QueryParameters().apply {
                geometry = circleGeometry
                spatialRelationship = SpatialRelationship.Contains
            }

            scope.launch {
                try {
                    val featureQueryResult = serviceFeatureTable.queryFeatures(queryParameters).getOrThrow() as FeatureQueryResult
                    for (feature in featureQueryResult) {
                        featureLayer.selectFeature(feature)
                    }
                    onResult("${featureQueryResult.count()} features selected")
                } catch (e: Exception) {
                    onResult("Selection failed: ${e.message}")
                }
            }
        }
    }

    fun drawPolygonPoint(
        mapView: MapView,
        graphicsOverlay: GraphicsOverlay,
        pointSymbol: PictureMarkerSymbol,
        ) {

        if (selectionPoints.isEmpty()) graphicsOverlay.graphics.clear()
        val centerPoint = mapView.screenToLocation(ScreenCoordinate(mapView.width / 2.0, mapView.height / 2.0))
        val projectedCenterPoint = GeometryEngine.projectOrNull(centerPoint!!, SpatialReference.wgs84()) as Point
        val pointGraphic = Graphic(projectedCenterPoint, pointSymbol)
        graphicsOverlay.graphics.add(pointGraphic)

        selectionPoints.add(projectedCenterPoint)
        updateTemporarySelectionPolygon(mapView, graphicsOverlay)
    }

    fun updateTemporarySelectionPolygon(
        mapView: MapView,
        graphicsOverlay: GraphicsOverlay
    ) {
        if (selectionPoints.size > 1 && _isPolygonSelection.value == true) {
            val centerPoint = mapView.screenToLocation(ScreenCoordinate(mapView.width / 2.0, mapView.height / 2.0))
            val projectedCenterPoint = GeometryEngine.projectOrNull(centerPoint!!, SpatialReference.wgs84()) as Point

            val polygonBuilder = PolygonBuilder(SpatialReference.wgs84()).apply {
                selectionPoints.forEach { addPoint(it) }
                addPoint(projectedCenterPoint)
            }
            val polygonGeometry = polygonBuilder.toGeometry()

            // Remove previous temporary polygon graphic
            tempSelectionPolygonGraphics?.let { graphicsOverlay.graphics.remove(it) }

            val polygonSymbol = SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.fromRgba(0, 0, 0, 50), SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 1.0f))
            tempSelectionPolygonGraphics = Graphic(polygonGeometry, polygonSymbol)
            graphicsOverlay.graphics.add(tempSelectionPolygonGraphics!!)

        }
    }

    fun finishPolygonSelection(
        featureLayer: FeatureLayer,
        scope: CoroutineScope,
        centerMarker: ImageView,
        onResult: (String) -> Unit,
    ) {
        if (selectionPoints.size > 2) {
            val polygonGeometry = PolygonBuilder(SpatialReference.wgs84()).apply {
                selectionPoints.forEach { addPoint(it) }
                addPoint(selectionPoints.first()) // Close the polygon
            }.toGeometry()

            val queryParameters = QueryParameters().apply {
                geometry = polygonGeometry
                spatialRelationship = SpatialRelationship.Intersects
            }

            scope.launch {
                try {
                    val featureQueryResult = featureLayer.featureTable?.queryFeatures(queryParameters)?.getOrElse {
                        onResult("Error querying features: ${it.message}")
                        return@launch
                    }

                    featureQueryResult?.let { result ->
                        for (feature in result) {
                            featureLayer.selectFeature(feature)
                        }
                    }
                    onResult("${featureQueryResult?.count()} features selected")
                } catch (e: Exception) {
                    onResult("Selection failed: ${e.message}")
                }
            }
            clear(centerMarker)
        }
    }

    private fun clear(
        centerMarker: ImageView,
    ) {
        centerMarker.visibility = View.GONE
        _isPolygonSelection.value = false
        selectionPoints.clear()
    }

}