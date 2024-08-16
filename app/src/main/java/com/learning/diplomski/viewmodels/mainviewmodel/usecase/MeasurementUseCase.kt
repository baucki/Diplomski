package com.learning.diplomski.viewmodels.mainviewmodel.usecase

import android.annotation.SuppressLint
import android.health.connect.datatypes.units.Length
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arcgismaps.Color
import com.arcgismaps.geometry.AngularUnit
import com.arcgismaps.geometry.AreaUnit
import com.arcgismaps.geometry.GeodeticCurveType
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.PolygonBuilder
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class MeasurementUseCase @Inject constructor() {

    private val _isMeasurement = MutableLiveData<Boolean>(false)
    val isMeasurement: LiveData<Boolean> get() = _isMeasurement

    private val _isDrawingLine = MutableLiveData<Boolean>(false)
    val isDrawingLine: LiveData<Boolean> get() = _isDrawingLine

    private val _isDrawingCircle = MutableLiveData<Boolean>(false)
    val isDrawingCircle: LiveData<Boolean> get() = _isDrawingCircle

    private val _isDrawingPolygon = MutableLiveData<Boolean>(false)
    val isDrawingPolygon: LiveData<Boolean> get() = _isDrawingPolygon

    private val _totalDistance = MutableLiveData<Double>(0.0)
    val totalDistance: LiveData<Double> get() = _totalDistance

    private val _totalArea = MutableLiveData<Double>(0.0)
    val totalArea: LiveData<Double> get() = _totalArea

    private var tempLineGraphic: Graphic? = null
    private var circleGraphic: Graphic? = null
    private var tempPolygonGraphic: Graphic? = null
    private var polygonGraphic: Graphic? = null

    private val drawnPoints = mutableListOf<Point>()

    fun measurementButtonClickListener(bottomSheetBehavior: BottomSheetBehavior<View>) {
        _isMeasurement.value = !_isMeasurement.value!!
        if (_isMeasurement.value == true) {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        } else {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                _isMeasurement.value = true
            }
        }
    }

    fun startDrawingLine(
        tvMeasurementType: TextView,
        lineControls: View,
        tvMeasurementArea: View,
        circleSliderRadius: View,
        btnFinishDrawingCircle: View,
        btnFinishDrawingPolygon: View,
        btnDrawPolygonPoint: View,
        centerMarker: View
    ) {
        _isDrawingLine.value = true
        _isDrawingCircle.value = false
        _isDrawingPolygon.value = false
        tvMeasurementType.text = "Drawing Line"
        lineControls.visibility = View.VISIBLE
        tvMeasurementArea.visibility = View.GONE
        circleSliderRadius.visibility = View.GONE
        btnFinishDrawingCircle.visibility = View.GONE
        btnFinishDrawingPolygon.visibility = View.GONE
        btnDrawPolygonPoint.visibility = View.GONE
        centerMarker.visibility = View.VISIBLE
    }

    fun startDrawingCircle(
        tvMeasurementType: TextView,
        lineControls: View,
        tvMeasurementArea: View,
        circleSliderRadius: View,
        btnFinishDrawingCircle: View,
        btnFinishDrawingPolygon: View,
        btnDrawPolygonPoint: View,
        centerMarker: View
    ) {
        _isDrawingLine.value = false
        _isDrawingCircle.value = true
        _isDrawingPolygon.value = false
        tvMeasurementType.text = "Drawing Circle"
        lineControls.visibility = View.GONE
        circleSliderRadius.visibility = View.VISIBLE
        btnFinishDrawingCircle.visibility = View.VISIBLE
        btnFinishDrawingPolygon.visibility = View.GONE
        btnDrawPolygonPoint.visibility = View.GONE
        centerMarker.visibility = View.GONE
        tvMeasurementArea.visibility = View.VISIBLE

    }

    fun startDrawingPolygon(
        tvMeasurementType: TextView,
        lineControls: View,
        tvMeasurementArea: View,
        circleSliderRadius: View,
        btnFinishDrawingCircle: View,
        btnFinishDrawingPolygon: View,
        btnDrawPolygonPoint: View,
        centerMarker: View
    ) {
        _isDrawingLine.value = false
        _isDrawingCircle.value = false
        _isDrawingPolygon.value = true
        tvMeasurementType.text = "Drawing Polygon"
        lineControls.visibility = View.GONE
        circleSliderRadius.visibility = View.GONE
        btnFinishDrawingCircle.visibility = View.GONE
        btnFinishDrawingPolygon.visibility = View.VISIBLE
        btnDrawPolygonPoint.visibility = View.VISIBLE
        centerMarker.visibility = View.VISIBLE
        tvMeasurementArea.visibility = View.VISIBLE
    }

    fun drawLinePoint(
        mapView: MapView,
        pointSymbol: PictureMarkerSymbol,
        graphicsOverlay: GraphicsOverlay,
        tvMeasurementLength: TextView,
    ) {
        val centerPoint = mapView.screenToLocation(ScreenCoordinate(mapView.width / 2.0, mapView.height / 2.0))
        val projectedCenterPoint = GeometryEngine.projectOrNull(centerPoint!!, SpatialReference.wgs84()) as Point
        val pointGraphic = Graphic(projectedCenterPoint, pointSymbol)
        graphicsOverlay.graphics.add(pointGraphic)

        if (drawnPoints.isNotEmpty()) {
            finalizeTemporaryLine(projectedCenterPoint, graphicsOverlay)
            _totalDistance.value = _totalDistance.value?.plus(
                GeometryEngine.distanceGeodeticOrNull(
                    drawnPoints.last(),
                    projectedCenterPoint,
                    LinearUnit.meters,
                    AngularUnit.degrees,
                    GeodeticCurveType.Geodesic
                )?.distance!!
            )
        }
        drawnPoints.add(projectedCenterPoint)
        updateDistance(mapView, tvMeasurementLength)
    }

    fun updateTemporaryLine(mapView: MapView, graphicsOverlay: GraphicsOverlay) {
        if (drawnPoints.isNotEmpty() && _isDrawingLine.value == true) {
            val lastPoint = drawnPoints.last()
            val centerPoint = mapView.screenToLocation(ScreenCoordinate(mapView.width / 2.0, mapView.height / 2.0))
            val projectedCenterPoint = GeometryEngine.projectOrNull(centerPoint!!, SpatialReference.wgs84()) as Point
            val lineGeometry = PolylineBuilder(SpatialReference.wgs84()).apply {
                addPoint(lastPoint)
                addPoint(projectedCenterPoint)
            }.toGeometry()

            if (tempLineGraphic != null) {
                graphicsOverlay.graphics.remove(tempLineGraphic)
            }

            val lineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 1.0f)
            tempLineGraphic = Graphic(lineGeometry, lineSymbol)
            graphicsOverlay.graphics.add(tempLineGraphic!!)
        }
    }

    private fun finalizeTemporaryLine(
        projectedCenterPoint: Point,
        graphicsOverlay: GraphicsOverlay,
    ) {
        val lastPoint = drawnPoints.last()
        val lineGeometry = PolylineBuilder(SpatialReference.wgs84()).apply {
            addPoint(lastPoint)
            addPoint(projectedCenterPoint)
        }.toGeometry()

        val lineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 3.0f)
        val lineGraphic = Graphic(lineGeometry, lineSymbol)
        graphicsOverlay.graphics.add(lineGraphic)

        if (tempLineGraphic != null) {
            graphicsOverlay.graphics.remove(tempLineGraphic)
            tempLineGraphic = null
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateDistance(mapView: MapView, tvMeasurementLength: TextView) {
        if (drawnPoints.isNotEmpty() && _isDrawingLine.value == true) {
            val lastPoint = drawnPoints.last()
            val centerPoint = mapView.screenToLocation(
                ScreenCoordinate(
                    mapView.width / 2.0,
                    mapView.height / 2.0
                )
            )
            val projectedCenterPoint =
                GeometryEngine.projectOrNull(centerPoint!!, SpatialReference.wgs84()) as Point
            val currentDistance = GeometryEngine.distanceGeodeticOrNull(
                lastPoint,
                projectedCenterPoint,
                LinearUnit.meters,
                AngularUnit.degrees,
                GeodeticCurveType.Geodesic
            )
            val totalDistanceWithCurrent =  totalDistance.value?.plus(currentDistance?.distance!!)
            tvMeasurementLength.text = "Length: ${String.format("%.2f", totalDistanceWithCurrent)} meters"
        }
    }

    @SuppressLint("SetTextI18n")
    fun finishDrawingLine(tvMeasurementLength: TextView, centerMarker: View, graphicsOverlay: GraphicsOverlay) {
        _isDrawingLine.value = false
        tvMeasurementLength.text = "Length: ${String.format("%.2f", totalDistance.value)} meters"
        centerMarker.visibility = View.GONE
        tempLineGraphic?.let {
            graphicsOverlay.graphics.remove(it)
        }
        tempLineGraphic = null
    }


    fun drawCircle(
        radius: Int,
        mapView: MapView,
        graphicsOverlay: GraphicsOverlay,
        tvMeasurementLength: TextView,
        tvMeasurementArea: TextView
    ) {
        val centerPoint = mapView.screenToLocation(ScreenCoordinate(mapView.width / 2.0, mapView.height / 2.0))
        // Remove the previous circle graphic if it exists
        circleGraphic?.let { graphicsOverlay.graphics.remove(it) }

        // Create a circle geometry

        val scaledRadius = radius * 50.0
        val maxDeviation = 1.0
        _totalDistance.value = 2 *scaledRadius * Math.PI
        _totalArea.value = Math.PI*scaledRadius*scaledRadius

        val circleGeometry = GeometryEngine.bufferGeodeticOrNull(
            centerPoint!!,
            scaledRadius,
            LinearUnit.meters,
            maxDeviation,
            GeodeticCurveType.Geodesic
        )

        // Create a symbol for the circle
        val circleSymbol = SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.fromRgba(0, 0, 0, 100), SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 2.0f))

        // Create a graphic for the circle and add it to the overlay
        circleGraphic = Graphic(circleGeometry, circleSymbol)
        graphicsOverlay.graphics.add(circleGraphic!!)
    }

    fun finishDrawingCircle() {
        _isDrawingCircle.value = false
    }

    fun drawPolygonPoint(mapView: MapView, pointSymbol: PictureMarkerSymbol, graphicsOverlay: GraphicsOverlay) {
        val centerPoint = mapView.screenToLocation(ScreenCoordinate(mapView.width / 2.0, mapView.height / 2.0))
        val projectedCenterPoint = GeometryEngine.projectOrNull(centerPoint!!, SpatialReference.wgs84()) as Point
        val pointGraphic = Graphic(projectedCenterPoint, pointSymbol)
        graphicsOverlay.graphics.add(pointGraphic)

        drawnPoints.add(projectedCenterPoint)
        updateTemporaryPolygon(
            mapView,
            graphicsOverlay
        )
    }

    fun updateTemporaryPolygon(
        mapView: MapView,
        graphicsOverlay: GraphicsOverlay
    ) {
        if (drawnPoints.isNotEmpty() && _isDrawingPolygon.value == true) {
            val centerPoint = mapView.screenToLocation(ScreenCoordinate(mapView.width / 2.0, mapView.height / 2.0))
            val projectedCenterPoint = GeometryEngine.projectOrNull(centerPoint!!, SpatialReference.wgs84()) as Point

            val polygonBuilder = PolygonBuilder(SpatialReference.wgs84()).apply {
                drawnPoints.forEach { addPoint(it) }
                addPoint(projectedCenterPoint)
            }
            val polygonGeometry = polygonBuilder.toGeometry()

            // Remove previous temporary polygon graphic
            tempPolygonGraphic?.let { graphicsOverlay.graphics.remove(it) }

            val polygonSymbol = SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.fromRgba(0, 0, 0, 50), SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 1.0f))
            tempPolygonGraphic = Graphic(polygonGeometry, polygonSymbol)
            graphicsOverlay.graphics.add(tempPolygonGraphic!!)

            _totalArea.value = GeometryEngine.areaGeodetic(polygonGeometry, AreaUnit.squareMeters, GeodeticCurveType.Geodesic)
            _totalDistance.value = GeometryEngine.lengthGeodetic(polygonGeometry, LinearUnit.meters, GeodeticCurveType.Geodesic)

        }
    }

    fun finalizeTemporaryPolygon(
        graphicsOverlay: GraphicsOverlay,
        centerMarker: View
    ) {
        val polygonBuilder = PolygonBuilder(SpatialReference.wgs84()).apply {
            drawnPoints.forEach { addPoint(it) }
        }

        val polygonGeometry = polygonBuilder.toGeometry()
        val polygonSymbol = SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.fromRgba(0, 0, 0, 100), SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 2.0f))

        centerMarker.visibility = View.GONE
        tempPolygonGraphic?.let {
            graphicsOverlay.graphics.remove(it)
            tempPolygonGraphic = null
        }

        polygonGraphic = Graphic(polygonGeometry, polygonSymbol)
        graphicsOverlay.graphics.add(polygonGraphic!!)

        _isDrawingPolygon.value = false
        _totalArea.value = GeometryEngine.areaGeodetic(polygonGeometry, AreaUnit.squareMeters, GeodeticCurveType.Geodesic)
        _totalDistance.value = GeometryEngine.lengthGeodetic(polygonGeometry, LinearUnit.meters, GeodeticCurveType.Geodesic)
    }

}