package com.learning.diplomski.rmv

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.MatrixCursor
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.BaseColumns
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.data.Feature
import com.arcgismaps.data.FeatureQueryResult
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.data.SpatialRelationship
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.tasks.geocode.GeocodeParameters
import com.arcgismaps.tasks.geocode.GeocodeResult
import com.arcgismaps.tasks.geocode.LocatorTask
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.learning.diplomski.data.Repository.feature
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import com.arcgismaps.geometry.AngularUnit
import com.arcgismaps.geometry.AreaUnit
import com.arcgismaps.geometry.GeodeticCurveType
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.PolygonBuilder
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.tasks.networkanalysis.RouteResult
import com.arcgismaps.tasks.networkanalysis.RouteTask
import com.arcgismaps.tasks.networkanalysis.Stop
import com.learning.diplomski.ui.components.DeleteConfirmationDialogFragment
import com.learning.diplomski.ui.adapters.FeatureAttributesAdapter
import com.learning.diplomski.ui.adapters.ItemData
import com.learning.diplomski.R
import com.learning.diplomski.data.Repository
import com.learning.diplomski.ui.adapters.SpinnerAdapter

class MainActivity : AppCompatActivity(), DeleteConfirmationDialogFragment.ConfirmationListener {

    private lateinit var mapView: MapView
    private lateinit var featureAttributesAdapter: FeatureAttributesAdapter
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var bottomSheetView: View

    private lateinit var bottomSheet: View
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private val drawnPoints = mutableListOf<Point>()
    private var tempLineGraphic: Graphic? = null
    private var circleGraphic: Graphic? = null
    private var tempPolygonGraphic: Graphic? = null
    private var polygonGraphic: Graphic? = null

    private var totalDistance = 0.0
    private var totalArea = 0.0
    private lateinit var tvMeasurementLength: TextView
    private lateinit var tvMeasurementArea: TextView


    private lateinit var selectionBottomSheet: View
    private lateinit var selectionBottomSheetBehavior: BottomSheetBehavior<View>

    private var selectionPoints = mutableListOf<Point>()
    private var selectionCircleGraphic: Graphic? = null
    private var tempSelectionPolygonGraphics: Graphic? = null

    private lateinit var findRouteButton: ImageButton
    private lateinit var centerMarker: ImageView
    private lateinit var editButton: Button
    private lateinit var deleteButton: Button

    private var isAddingFeature = false

    private var isSelection = false
    private var isCircleSelection = false
    private var isPolygonSelection = false

    private var isMeasurement = false
    private var isDrawingLine = false
    private var isDrawingCircle = false
    private var isDrawingPolygon = false

    private val serviceFeatureTable = ServiceFeatureTable("http://192.168.1.18:6080/arcgis/rest/services/Servis_SP4_FieldTools/FeatureServer/0")
    private val featureLayer = FeatureLayer.createWithFeatureTable(serviceFeatureTable)

    private lateinit var searchView: SearchView
    private lateinit var graphicsOverlay: GraphicsOverlay
    private lateinit var locatorTask: LocatorTask
    private lateinit var geocodeParameters: GeocodeParameters
    private lateinit var pinSymbol: PictureMarkerSymbol
    private lateinit var pointSymbol: PictureMarkerSymbol

    private val addressGeocodeParameters: GeocodeParameters = GeocodeParameters().apply {
        resultAttributeNames.addAll(listOf("PlaceName", "Place_addr"))
    }

    private val basemapOptions = arrayOf(
        "Streets" to BasemapStyle.ArcGISStreets,
        "Imagery" to BasemapStyle.ArcGISImagery,
        "Topographic" to BasemapStyle.ArcGISTopographic,
        "Oceans" to BasemapStyle.ArcGISOceans,
        "Terrain" to BasemapStyle.ArcGISTerrain
    )

    private lateinit var mainProgressBar: ProgressBar

    var currentPoint: Point? = null
    var featurePoint: Point? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        val toggleButton = findViewById<ToggleButton>(R.id.toggleButton)
        val searchButton = findViewById<ImageButton>(R.id.searchButton)
        centerMarker = findViewById(R.id.center_marker)

        featureAttributesAdapter = FeatureAttributesAdapter(emptyList())

        ArcGISEnvironment.apiKey = ApiKey.create("AAPK1e43bdcf9fa04fa0a729106fdd7a97fbNbpa3VVhaR5eKzfmkAFb0Uy_soNrGAjpslTJLcWQiNV6T3YGoRy8Sfa7a5ZXkBcj")
        lifecycle.addObserver(mapView)

        val map = ArcGISMap(BasemapStyle.ArcGISStreets).apply {
            operationalLayers.add(featureLayer)
            Repository.featureLayer = featureLayer
        }

        mapView.apply {
            mapView.map = map
            // Belgrade Viewpoint
            setViewpoint(
                Viewpoint(
                    Point(
                        x = 20.4489,
                        y = 44.8066,
                        spatialReference = SpatialReference.wgs84()
                    ),
                7e4)
            )
            selectionProperties.color = Color.red

            lifecycleScope.launch {
                onSingleTapConfirmed.collect { tapEvent ->
                    val screenCoordinate = tapEvent.screenCoordinate
                    identifyFeature(screenCoordinate)
                }
            }
        }

        graphicsOverlay = GraphicsOverlay()
        mapView.graphicsOverlays.add(graphicsOverlay)
        lifecycleScope.launch {
            mapView.viewpointChanged.collect {
                updateTemporaryLine()
                updateTemporaryPolygon()
                updateTemporarySelectionPolygon()
                updateDistance()
            }
        }

        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            isAddingFeature = isChecked
        }
        searchButton.setOnClickListener {
            startActivityForResult(Intent(this, SearchActivity::class.java),
                SearchActivity.SEARCH_FEATURES_REQUEST_CODE
            )
        }
        locatorTask = LocatorTask("https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer")
        geocodeParameters = GeocodeParameters().apply {
            resultAttributeNames.addAll(listOf("PlaceName", "Place_addr"))
            maxResults = 1
        }

        lifecycleScope.launch {
            pinSymbol = createPinSymbol()
            pointSymbol = createPointSymbol()
        }

        searchView = findViewById(R.id.searchView)
        setupSearchView()

        mainProgressBar = findViewById<ProgressBar>(R.id.mainProgressBar)

        bottomSheet = findViewById(R.id.sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet).apply {
            peekHeight = 0
        }
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        selectionBottomSheet = findViewById(R.id.select_bottom_sheet)
        selectionBottomSheetBehavior = BottomSheetBehavior.from(selectionBottomSheet).apply {
            peekHeight = 0
        }
        selectionBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN


        setupBottomSheetMeasurement()
        setupBottomSheetSelection()

        val basemapButton = findViewById<ImageButton>(R.id.basemapButton)
        val navigationButton = findViewById<ImageButton>(R.id.navigationButton)
        val measurementButton = findViewById<ImageButton>(R.id.measurementButton)
        val selectionButton = findViewById<ImageButton>(R.id.spatialSelectionButton)
        val spinner = findViewById<Spinner>(R.id.spinner)
        val locationDisplay = mapView.locationDisplay
        lifecycleScope.launch {
            locationDisplay.dataSource.start()
                .onSuccess {
                    // permission already granted, so start the location display
                    spinner.setSelection(0, true)
                }.onFailure {
                    // check permissions to see if failure may be due to lack of permissions
                    requestPermissions(spinner)
                }
        }

        val panModeSpinnerElements = arrayListOf(
            ItemData("Stop", R.drawable.ic_stop),
            ItemData("On", R.drawable.ic_start),
            ItemData("Re-center", R.drawable.ic_re_center),
            ItemData("Navigation", R.drawable.ic_navigation),
            ItemData("Compass", R.drawable.ic_compass)
        )

        spinner.apply {
            adapter = SpinnerAdapter(this@MainActivity, R.id.locationTextView, panModeSpinnerElements)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    when (panModeSpinnerElements[position].text) {
                        "Stop" ->  // stop location display
                            lifecycleScope.launch {
                                locationDisplay.dataSource.stop()
//                                navigationButton.setImageResource(R.drawable.ic_stop)
                            }
                        "On" ->  // start location display
                            lifecycleScope.launch {
                                locationDisplay.dataSource.start()
//                                navigationButton.setImageResource(R.drawable.ic_start)
                                locationDisplay.location.collect {
                                    currentPoint = it?.position
                                }
                            }
                        "Re-center" -> {
                            // re-center MapView on location
                            locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
//                            navigationButton.setImageResource(R.drawable.ic_re_center)
                        }
                        "Navigation" -> {
                            // start navigation mode
                            locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)
//                            navigationButton.setImageResource(R.drawable.ic_navigation)
                        }
                        "Compass" -> {
                            // start compass navigation mode
                            locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.CompassNavigation)
//                            navigationButton.setImageResource(R.drawable.ic_compass)
                        }
                    }
                }


                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        }
        navigationButton.setOnClickListener {
            spinner.performClick()
        }

        basemapButton.setOnClickListener {
            showBasemapSelectionDialog()
        }
        measurementButton.setOnClickListener {
            isMeasurement = !isMeasurement
            if (isMeasurement) {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    isMeasurement = true
                }
            }
        }

        selectionButton.setOnClickListener {
            isSelection = !isSelection
            if (isSelection) {
                if (selectionBottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                    selectionBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                if (selectionBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    selectionBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
                if (selectionBottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    selectionBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    isSelection = true
                }
            }
        }
    }

    private fun setupBottomSheetSelection() {
        val ivSelectCircle: ImageView = findViewById(R.id.iv_circle_selection)
        val ivSelectPolygon: ImageView = findViewById(R.id.iv_polygon_selection)
        val tvSelectionType: TextView = findViewById(R.id.tv_selection_type)
        val circleSelectionRadius: SeekBar = findViewById(R.id.circle_select_slider_radius)
        val btnFinishSelectCircle: Button = findViewById(R.id.btn_select_circle)
        val polygonSelectControls: LinearLayout = findViewById(R.id.polygon_select_controls)
        val btnDrawPolygonPoint: Button = findViewById(R.id.btn_draw_polygon_select_point)
        val btnFinishSelectPolygon: Button = findViewById(R.id.btn_select_polygon)

        circleSelectionRadius.visibility = View.GONE
        btnFinishSelectCircle.visibility = View.GONE
        polygonSelectControls.visibility = View.GONE

        ivSelectCircle.setOnClickListener {
            isCircleSelection = true
            isPolygonSelection = false
            circleSelectionRadius.visibility = View.VISIBLE
            btnFinishSelectCircle.visibility = View.VISIBLE
            polygonSelectControls.visibility = View.GONE
            centerMarker.visibility = View.GONE
            tvSelectionType.text = "Circle Selection"
        }

        ivSelectPolygon.setOnClickListener {
            isCircleSelection = false
            isPolygonSelection = true
            circleSelectionRadius.visibility = View.GONE
            btnFinishSelectCircle.visibility = View.GONE
            polygonSelectControls.visibility = View.VISIBLE
            centerMarker.visibility = View.VISIBLE
            tvSelectionType.text = "Polygon Selection"
        }

        circleSelectionRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                drawSelectionCircle(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnFinishSelectCircle.setOnClickListener {
            selectFeaturesInCircle()
        }

        btnDrawPolygonPoint.setOnClickListener {
            drawPolygonPoint()
        }

        btnFinishSelectPolygon.setOnClickListener {
            finishPolygonSelection()
            centerMarker.visibility = View.GONE
            isPolygonSelection = false
            selectionPoints.clear()
        }
    }

    private fun drawPolygonPoint() {
        val centerPoint = mapView.screenToLocation(ScreenCoordinate(mapView.width / 2.0, mapView.height / 2.0))
        val projectedCenterPoint = GeometryEngine.projectOrNull(centerPoint!!, SpatialReference.wgs84()) as Point
        val pointGraphic = Graphic(projectedCenterPoint, pointSymbol)
        graphicsOverlay.graphics.add(pointGraphic)

        selectionPoints.add(projectedCenterPoint)
        updateTemporarySelectionPolygon()
    }

    private fun updateTemporarySelectionPolygon() {
        if (selectionPoints.size > 1 && isPolygonSelection) {
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

    private fun finishPolygonSelection() {
        if (selectionPoints.size > 2) {
            val polygonGeometry = PolygonBuilder(SpatialReference.wgs84()).apply {
                selectionPoints.forEach { addPoint(it) }
                addPoint(selectionPoints.first()) // Close the polygon
            }.toGeometry()

            val queryParameters = QueryParameters().apply {
                geometry = polygonGeometry
                spatialRelationship = SpatialRelationship.Intersects
            }

            lifecycleScope.launch {
                try {
                    val featureQueryResult = featureLayer.featureTable?.queryFeatures(queryParameters)?.getOrElse {
                        showSnackbar("Error querying features: ${it.message}")
                        return@launch
                    }

                    featureQueryResult?.let { result ->
                        for (feature in result) {
                            featureLayer.selectFeature(feature)
                        }
                    }
                    showSnackbar("${featureQueryResult?.count()} features selected")
                } catch (e: Exception) {
                    showSnackbar("Selection failed: ${e.message}")
                }
            }
        }
    }

    private fun drawSelectionCircle(radius: Int) {
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
        val circleSymbol = SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.fromRgba(0, 0, 0, 100), SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 2.0f))

        // Create a graphic for the circle and add it to the overlay
        selectionCircleGraphic = Graphic(circleGeometry, circleSymbol)
        graphicsOverlay.graphics.add(selectionCircleGraphic!!)
    }

    private fun selectFeaturesInCircle() {
        selectionCircleGraphic?.let { graphic ->
            val circleGeometry = graphic.geometry as Polygon

            val queryParameters = QueryParameters().apply {
                geometry = circleGeometry
                spatialRelationship = SpatialRelationship.Contains
            }

            lifecycleScope.launch {
                try {
                    val featureQueryResult = serviceFeatureTable.queryFeatures(queryParameters).getOrThrow() as FeatureQueryResult
                    for (feature in featureQueryResult) {
                        featureLayer.selectFeature(feature)
                    }
                    showSnackbar("${featureQueryResult.count()} features selected")
                } catch (e: Exception) {
                    showSnackbar("Selection failed: ${e.message}")
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupBottomSheetMeasurement() {
        val ivDrawLine: ImageView = findViewById(R.id.iv_draw_line)
        val ivDrawCircle: ImageView = findViewById(R.id.iv_draw_circle)
        val ivDrawPolygon: ImageView = findViewById(R.id.iv_draw_polygon)
        val tvMeasurementType: TextView = findViewById(R.id.tv_measurement_type)
        tvMeasurementLength = findViewById(R.id.tv_measurement_length)
        tvMeasurementArea = findViewById(R.id.tv_measurement_area)
        val lineControls: LinearLayout = findViewById(R.id.line_controls)
        val btnDrawLinePoint: Button = findViewById(R.id.btn_draw_line_point)
        val btnFinishDrawingLine: Button = findViewById(R.id.btn_finish_drawing_line)

        val circleSliderRadius: SeekBar = findViewById(R.id.circle_slider_radius)
        val btnFinishDrawingCircle: Button = findViewById(R.id.btn_finish_drawing_circle)

        val btnDrawPolygonPoint: Button = findViewById(R.id.btn_draw_polygon_point)
        val btnFinishDrawingPolygon: Button = findViewById(R.id.btn_finish_drawing_polygon)

        lineControls.visibility = View.GONE
        btnFinishDrawingCircle.visibility = View.GONE
        btnFinishDrawingPolygon.visibility = View.GONE
        btnDrawPolygonPoint.visibility = View.GONE
        tvMeasurementArea.visibility = View.GONE

        ivDrawLine.setOnClickListener {
            isDrawingLine = true
            isDrawingCircle = false
            isDrawingPolygon = false
            tvMeasurementType.text = "Drawing Line"
            lineControls.visibility = View.VISIBLE
            tvMeasurementArea.visibility = View.GONE
            circleSliderRadius.visibility = View.GONE
            btnFinishDrawingCircle.visibility = View.GONE
            btnFinishDrawingPolygon.visibility = View.GONE
            btnDrawPolygonPoint.visibility = View.GONE
            centerMarker.visibility = View.VISIBLE
        }

        ivDrawCircle.setOnClickListener {
            isDrawingLine = false
            isDrawingCircle = true
            isDrawingPolygon = false
            tvMeasurementType.text = "Drawing Circle"
            lineControls.visibility = View.GONE
            circleSliderRadius.visibility = View.VISIBLE
            btnFinishDrawingCircle.visibility = View.VISIBLE
            btnFinishDrawingPolygon.visibility = View.GONE
            btnDrawPolygonPoint.visibility = View.GONE
            centerMarker.visibility = View.GONE
            tvMeasurementArea.visibility = View.VISIBLE
        }

        ivDrawPolygon.setOnClickListener {
            isDrawingLine = false
            isDrawingCircle = false
            isDrawingPolygon = true
            tvMeasurementType.text = "Drawing Polygon"
            lineControls.visibility = View.GONE
            circleSliderRadius.visibility = View.GONE
            btnFinishDrawingCircle.visibility = View.GONE
            btnFinishDrawingPolygon.visibility = View.VISIBLE
            btnDrawPolygonPoint.visibility = View.VISIBLE
            centerMarker.visibility = View.VISIBLE
            tvMeasurementArea.visibility = View.VISIBLE
        }

        btnDrawLinePoint.setOnClickListener {
            val centerPoint = mapView.screenToLocation(ScreenCoordinate(mapView.width / 2.0, mapView.height / 2.0))
            val projectedCenterPoint = GeometryEngine.projectOrNull(centerPoint!!, SpatialReference.wgs84()) as Point
            val pointGraphic = Graphic(projectedCenterPoint, pointSymbol)
            graphicsOverlay.graphics.add(pointGraphic)

            if (drawnPoints.isNotEmpty()) {
                finalizeTemporaryLine(projectedCenterPoint)
                totalDistance += GeometryEngine.distanceGeodeticOrNull(
                    drawnPoints.last(),
                    projectedCenterPoint,
                    LinearUnit.meters,
                    AngularUnit.degrees,
                    GeodeticCurveType.Geodesic
                )?.distance!!
            }
            drawnPoints.add(projectedCenterPoint)
            updateDistance()
        }

        btnFinishDrawingLine.setOnClickListener {
            isDrawingLine = false
            tvMeasurementLength.text = "Length: ${String.format("%.2f", totalDistance)} meters"
            centerMarker.visibility = View.GONE
            tempLineGraphic?.let {
                graphicsOverlay.graphics.remove(it)
            }
            tempLineGraphic = null
        }

        circleSliderRadius.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                drawCircle(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })

        btnFinishDrawingCircle.setOnClickListener {
            isDrawingCircle = false
        }

        btnDrawPolygonPoint.setOnClickListener {
            val centerPoint = mapView.screenToLocation(ScreenCoordinate(mapView.width / 2.0, mapView.height / 2.0))
            val projectedCenterPoint = GeometryEngine.projectOrNull(centerPoint!!, SpatialReference.wgs84()) as Point
            val pointGraphic = Graphic(projectedCenterPoint, pointSymbol)
            graphicsOverlay.graphics.add(pointGraphic)

            drawnPoints.add(projectedCenterPoint)
            updateTemporaryPolygon()
        }

        btnFinishDrawingPolygon.setOnClickListener {
            isDrawingPolygon = false
            finalizeTemporaryPolygon()
            tvMeasurementLength.text = "Length: ${String.format("%.2f", totalDistance)}m"
            tvMeasurementArea.text = "Area: ${String.format("%.2f", totalArea)}m²"
            centerMarker.visibility = View.GONE
            tempPolygonGraphic?.let {
                graphicsOverlay.graphics.remove(it)
            }
            tempPolygonGraphic = null
        }

    }

    private fun updateTemporaryPolygon() {
        if (drawnPoints.isNotEmpty() && isDrawingPolygon) {
            val lastPoint = drawnPoints.last()
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

            // Update area and perimeter
            val tempArea = GeometryEngine.areaGeodetic(polygonGeometry, AreaUnit.squareMeters, GeodeticCurveType.Geodesic) ?: 0.0
            val tempPerimeter = GeometryEngine.lengthGeodetic(polygonGeometry, LinearUnit.meters, GeodeticCurveType.Geodesic) ?: 0.0
            tvMeasurementLength.text = "Length: ${String.format("%.2f", tempPerimeter)}m"
            tvMeasurementArea.text = "Area: ${String.format("%.2f", tempArea)}m²"
        }

    }

    private fun finalizeTemporaryPolygon() {
        val polygonBuilder = PolygonBuilder(SpatialReference.wgs84()).apply {
            drawnPoints.forEach { addPoint(it) }
        }

        val polygonGeometry = polygonBuilder.toGeometry()
        val polygonSymbol = SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.fromRgba(0, 0, 0, 100), SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 2.0f))

        // Remove temporary polygon graphic if it exists
        tempPolygonGraphic?.let {
            graphicsOverlay.graphics.remove(it)
            tempPolygonGraphic = null
        }

        // Create a new graphic for the finalized polygon
        polygonGraphic = Graphic(polygonGeometry, polygonSymbol)
        graphicsOverlay.graphics.add(polygonGraphic!!)

        // Calculate the area and perimeter
        totalArea = GeometryEngine.areaGeodetic(polygonGeometry, AreaUnit.squareMeters, GeodeticCurveType.Geodesic) ?: 0.0
        totalDistance = GeometryEngine.lengthGeodetic(polygonGeometry, LinearUnit.meters, GeodeticCurveType.Geodesic) ?: 0.0
    }


    private fun drawCircle(radius: Int) {
        val centerPoint = mapView.screenToLocation(ScreenCoordinate(mapView.width / 2.0, mapView.height / 2.0))
        // Remove the previous circle graphic if it exists
        circleGraphic?.let { graphicsOverlay.graphics.remove(it) }

        // Create a circle geometry

        val scaledRadius = radius * 50.0
        val maxDeviation = 1.0
        totalArea = Math.PI*scaledRadius*scaledRadius

        runOnUiThread {
            tvMeasurementLength.text = "Length: ${String.format("%.2f", 2*scaledRadius*Math.PI)}m"
            tvMeasurementArea.text = "Area: ${String.format("%.2f", totalArea)}m²"
        }
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

    private fun updateTemporaryLine() {
        if (drawnPoints.isNotEmpty() && isDrawingLine) {
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

    private fun finalizeTemporaryLine(centerPoint: Point) {
        val lastPoint = drawnPoints.last()
        val lineGeometry = PolylineBuilder(SpatialReference.wgs84()).apply {
            addPoint(lastPoint)
            addPoint(centerPoint)
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
    private fun updateDistance() {
        if (drawnPoints.isNotEmpty() && isDrawingLine) {
            val lastPoint = drawnPoints.last()
            val centerPoint = mapView.screenToLocation(ScreenCoordinate(mapView.width / 2.0, mapView.height / 2.0))
            val projectedCenterPoint = GeometryEngine.projectOrNull(centerPoint!!, SpatialReference.wgs84()) as Point
            val currentDistance = GeometryEngine.distanceGeodeticOrNull(
                lastPoint,
                projectedCenterPoint,
                LinearUnit.meters,
                AngularUnit.degrees,
                GeodeticCurveType.Geodesic
            )
            val totalDistanceWithCurrent = totalDistance + currentDistance?.distance!!
            tvMeasurementLength.text = "Length: ${String.format("%.2f", totalDistanceWithCurrent)} meters"
        }
    }

    private suspend fun createPointSymbol(): PictureMarkerSymbol {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_point) as BitmapDrawable
        val symbol = PictureMarkerSymbol.createWithImage(drawable)
        symbol.load().getOrThrow()
        symbol.width = 10f
        symbol.height = 10f
        return symbol
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    geocodeAddress(it)
                    searchView.clearFocus()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    if (it.isNotEmpty()) {
                        suggestAddresses(it)
                    }
                }
                return true
            }
        })
    }

    private fun createSimpleCursorAdapter(): androidx.cursoradapter.widget.SimpleCursorAdapter {
        // set up parameters for searching with MatrixCursor
        val columnNames = arrayOf(BaseColumns._ID, "address")
        val suggestionsCursor = MatrixCursor(columnNames)
        // column names for the adapter to look at when mapping data
        val cols = arrayOf("address")
        // ids that show where data should be assigned in the layout
        val to = intArrayOf(R.id.suggestion_address)
        // define SimpleCursorAdapter
        return androidx.cursoradapter.widget.SimpleCursorAdapter(
            this@MainActivity,
            R.layout.suggestion, suggestionsCursor, cols, to, 0
        )
    }

    private fun suggestAddresses(query: String) {
        lifecycleScope.launch {
            locatorTask.suggest(query).onSuccess { suggestResults ->

                val simpleCursorAdapter = createSimpleCursorAdapter()
                searchView.suggestionsAdapter = simpleCursorAdapter

                for ((key, result) in suggestResults.withIndex()) {
                    val suggestionCursor = simpleCursorAdapter.cursor as MatrixCursor
                    suggestionCursor.addRow(arrayOf<Any>(key, result.label))
                }
                // notify the adapter when the data updates, so the view can refresh itself
                simpleCursorAdapter.notifyDataSetChanged()

                searchView.setOnSuggestionListener(object :
                    SearchView.OnSuggestionListener {
                    override fun onSuggestionSelect(position: Int): Boolean {
                        return false
                    }

                    override fun onSuggestionClick(position: Int): Boolean {
                        // get the selected row
                        (simpleCursorAdapter.getItem(position) as? MatrixCursor)?.let { selectedRow ->
                            // get the row's index
                            val selectedCursorIndex =
                                selectedRow.getColumnIndex("address")
                            // get the string from the row at index
                            val selectedAddress =
                                selectedRow.getString(selectedCursorIndex)
                            // geocode the typed address
                            geocodeAddress(selectedAddress)
                            searchView.isIconified = true
                            searchView.clearAndHideKeyboard()
                        }
                        return true
                    }
                })

            }
        }
    }

    fun SearchView.clearAndHideKeyboard() {
        // clear the searched text from the view
        this.clearFocus()
        // close the keyboard once search is complete
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun geocodeAddress(address: String) {
        lifecycleScope.launch {
            graphicsOverlay.graphics.clear()

            addressGeocodeParameters.searchArea = null
            addressGeocodeParameters.maxResults = 1

            locatorTask.load().getOrThrow()
            val results = locatorTask.geocode(address, addressGeocodeParameters).getOrElse {
                return@launch
            }

            if (results.isNotEmpty()) {
                displayResult(results.first())
            }
        }
    }


    private fun showBasemapSelectionDialog() {
        val basemapNames = basemapOptions.map { it.first }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Basemap")
            .setItems(basemapNames) { _, which ->
                val selectedBasemap = basemapOptions[which].second
                val viewPoint = mapView.getCurrentViewpoint(ViewpointType.CenterAndScale)
                mapView.map?.operationalLayers?.clear()
                mapView.map = ArcGISMap(selectedBasemap).apply {
                    operationalLayers.add(featureLayer)
                }
                viewPoint.let {
                    mapView.setViewpoint(it!!)
                }
            }
            .show()
    }

    private fun displayResult(result: GeocodeResult) {
        graphicsOverlay.graphics.clear()
        val graphic = Graphic(result.displayLocation, result.attributes, pinSymbol)
        graphicsOverlay.graphics.add(graphic)
        mapView.setViewpoint(Viewpoint(result.displayLocation!!, 30000.0))
    }

    private suspend fun createPinSymbol(): PictureMarkerSymbol {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_pin) as BitmapDrawable
        val symbol = PictureMarkerSymbol.createWithImage(drawable)
        symbol.load().getOrThrow()
        symbol.width = 24f
        symbol.height = 36f
        symbol.offsetY = 20f
        return symbol
    }

    private fun requestPermissions(spinner: Spinner) {
        // coarse location permission
        val permissionCheckCoarseLocation =
            ContextCompat.checkSelfPermission(this@MainActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) ==
                    PackageManager.PERMISSION_GRANTED
        // fine location permission
        val permissionCheckFineLocation =
            ContextCompat.checkSelfPermission(this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) ==
                    PackageManager.PERMISSION_GRANTED

        // if permissions are not already granted, request permission from the user
        if (!(permissionCheckCoarseLocation && permissionCheckFineLocation)) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                2
            )
        } else {
            // permission already granted, so start the location display
            lifecycleScope.launch {
                mapView.locationDisplay.dataSource.start().onSuccess {
                    spinner.setSelection(1, true)
                }
            }
        }
    }

    private suspend fun solveRoute() {
        // set the applicationContext as it is required with RouteTask
        ArcGISEnvironment.applicationContext = applicationContext
        // create a route task instance
        val routeTask = RouteTask(
            "https://route-api.arcgis.com/arcgis/rest/services/World/Route/NAServer/Route_World"
        )

        // show the progress bar
        mainProgressBar.visibility = View.VISIBLE
        routeTask.createDefaultParameters().onSuccess { routeParams ->

            // create stops
            if (currentPoint != null && featurePoint != null) {
                showSnackbar("Coordinates Current Point: ${currentPoint!!.x} ${currentPoint!!.y}")

                val stops = listOf(
                    Stop(currentPoint!!),
                    Stop(featurePoint!!)
                )

                routeParams.apply {
                    setStops(stops)
                    // set return directions as true to return turn-by-turn directions in the route's directionManeuvers
                    returnDirections = false
                }

                // solve the route
                val routeResult = routeTask.solveRoute(routeParams).getOrElse {
                    showSnackbar(it.message.toString())
                    return@onSuccess
                } as RouteResult

                val route = routeResult.routes[0]
                // create a simple line symbol for the route
                val routeSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.red, 5f)

                // create a graphic for the route and add it to the graphics overlay
                graphicsOverlay.graphics.add(Graphic(route.routeGeometry, routeSymbol))
                mainProgressBar.visibility = View.GONE
            } else {
                showSnackbar("Points are null")
                mainProgressBar.visibility = View.GONE
            }
        }.onFailure {
            showSnackbar(it.message.toString())
            mainProgressBar.visibility = View.GONE
        }
    }


    private fun initListeners() {
        findRouteButton.findViewById<ImageButton>(R.id.findRoute).setOnClickListener {
            lifecycleScope.launch {
                solveRoute()
            }
        }
        editButton.findViewById<Button>(R.id.editButton).setOnClickListener {
            startActivityForResult(Intent(this, EditFeatureActivity::class.java),
                EditFeatureActivity.EDIT_FEATURE_REQUEST_CODE
            )
        }
        deleteButton.findViewById<Button>(R.id.deleteButton).setOnClickListener {
            val dialog = DeleteConfirmationDialogFragment()
            dialog.show(supportFragmentManager, "deleteConfirmationDialog")
        }
    }
    override fun onConfirmDelete() {
        try {
            lifecycleScope.launch {
                serviceFeatureTable.deleteFeature(feature!!).apply {
                    onSuccess {
                        serviceFeatureTable.applyEdits()
                    }
                    onFailure {
                        val rootView = findViewById<View>(android.R.id.content)
                        Snackbar.make(rootView, "Failed to update feature", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            val rootView = findViewById<View>(android.R.id.content)
            Snackbar.make(rootView, "An error occurred", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun displayFeatureAttributes(featureAttributes: Map<String, Any?>) {
        runOnUiThread {
            if (!::bottomSheetDialog.isInitialized) {
                bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_attributes, null)
                bottomSheetDialog = BottomSheetDialog(this)
                bottomSheetDialog.setContentView(bottomSheetView)

                findRouteButton = bottomSheetView.findViewById(R.id.findRoute)
                editButton = bottomSheetView.findViewById(R.id.editButton)
                deleteButton = bottomSheetView.findViewById(R.id.deleteButton)

                initListeners()

                val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView.parent as View)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

                val recyclerView = bottomSheetView.findViewById<RecyclerView>(R.id.recyclerView)
                recyclerView.layoutManager = LinearLayoutManager(this)
                recyclerView.adapter = featureAttributesAdapter
            }
            featureAttributesAdapter.updateData(listOf(featureAttributes))
            bottomSheetDialog.show()
        }
    }
    private suspend fun identifyFeature(screenCoordinate: ScreenCoordinate) {
        featureLayer.clearSelection()
        clearGraphics()
        val identifyLayerResult =
            mapView.identifyLayer(featureLayer, screenCoordinate, 5.0, false, 1)

        identifyLayerResult.apply {
            onSuccess { identifyLayerResult ->
                val geoElements = identifyLayerResult.geoElements

                if (geoElements.isNotEmpty() && geoElements[0] is Feature) {
                    val identifiedFeature = geoElements[0] as Feature
                    feature = identifiedFeature
                    val geometry = identifiedFeature.geometry
                    if (geometry is Point) featurePoint = geometry as Point
                    val featureAttributes = identifiedFeature.attributes
                    val aliasAttributes = mutableMapOf<String, Any?>()

                    val featureTable = identifiedFeature.featureTable as ServiceFeatureTable
                    val fields = featureTable.fields
                    val types = featureTable.featureTypes

                    Repository.fields = fields
                    Repository.types = types
                    Repository.typeObject = "tip"
                    Repository.dataTypeObject = "Short"

                    for (type in types) {
                        Repository.typeObjectNamesMap[type.id] = type.name
                        Repository.typeObjectIdMap[type.name] = type.id
                    }

                    for (field in fields) {
                        if (field.alias == "objectid" || field.alias == "globalid") continue
                        val alias = field.alias
                        val attributeName = field.name
                        val attributeValue = featureAttributes[attributeName]
                        aliasAttributes[alias] = attributeValue
                        if (Repository.fieldTypeMap[field.fieldType] == "Date" && attributeValue != null) {
                            val dateString = attributeValue.toString()
                            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

                            val date: Date = sdf.parse(dateString)
                            val outputFormat = SimpleDateFormat("d/M/yyyy")
                            val formattedDate = outputFormat.format(date)
                            aliasAttributes[alias] = formattedDate
                        }
                        else if (field.name == Repository.typeObject) {
                            aliasAttributes[alias] = Repository.typeObjectNamesMap[attributeValue]
                        }
                        else if (field.name == "ocena_dekorativnosti" || field.name == "ocena_kondicije") {
                            for (option in Repository.numbersCustomInputFieldList) {
                                if (option.key == attributeValue)
                                    aliasAttributes[alias] = option.value
                            }
                        }
                    }
                    featureLayer.selectFeature(identifiedFeature)
                    displayFeatureAttributes(aliasAttributes)
                } else {
                    if (isAddingFeature) {
                        val mapPoint = mapView.screenToLocation(screenCoordinate)
                        val attributes = mutableMapOf<String, Any?>(
                            "tip" to 1.toShort(),
                            "vrsta" to null,
                            "fitopatoloske_promene" to null,
                            "entomoloske_promene" to null,
                            "slomljene_grane" to null,
                            "suve_grane" to null,
                            "suhovrhost" to null,
                            "isecene_debele_grane" to null,
                            "premaz" to null,
                            "ocena_kondicije" to null,
                            "ocena_dekorativnosti" to null,
                            "procena_starosti" to null,
                            "vreme_sadnje" to null,
                            "rasadnik" to null,
                            "cena_sadnice" to null,
                            "visina_stabla" to null,
                            "visina_debla" to null,
                            "prsni_precnik" to null,
                            "sirina_krosnje" to null,
                            "fitopatoloska_oboljenja" to null,
                            "entomoloska_oboljenja" to null,
                            "ottrulez_debla_izrazenost" to null,
                            "ottrulez_debla_velicina" to null,
                            "ottrulez_grana_izrazenost" to null,
                            "ottrulez_grana_velicina" to null,
                            "napomena" to null,
                            "pripada_drvoredu" to null,
                            "tip_kragne" to null,
                            "globalid" to null,
                            "ostalo" to null,
                            "list" to null,
                            "stablo" to null,
                            "koren" to null,
                            "grana" to null,
                            "krosnja" to null
                        )

                        val feature = serviceFeatureTable.createFeature(attributes, mapPoint)
                        serviceFeatureTable.addFeature(feature).apply {
                            onSuccess {
                                serviceFeatureTable.applyEdits()
                                showSnackbar("Feature added successfully.")
                            }
                            onFailure {
                                showSnackbar("Failed to add feature: ${it.message}")
                            }
                        }
                    } else {
                        showSnackbar("No feature identified.")
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EditFeatureActivity.EDIT_FEATURE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val updateSuccess = data?.getBooleanExtra("updateSuccess", false) ?: false
            if (updateSuccess) {
                refreshBottomSheet()
            }
        }
        if (requestCode == SearchActivity.SEARCH_FEATURES_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val queryString = data?.getStringExtra("queryString") ?: ""

            featureLayer.clearSelection()
            val queryParameters = QueryParameters().apply {
                whereClause = queryString
            }

            lifecycleScope.launch {
                try {
                    val featureQueryResult = serviceFeatureTable.queryFeatures(queryParameters).getOrThrow() as FeatureQueryResult

                    for (feature in featureQueryResult) {
                        featureLayer.selectFeature(feature)
                    }
                } catch (e: Exception) {
                    showSnackbar("error: ${e.message}")
                    println("error: ${e.message}")
                }
            }
            showSnackbar(queryString)
            println(queryString)
        }
    }

    private fun refreshBottomSheet() {
        val featureAttributes = feature!!.attributes
        val aliasAttributes = mutableMapOf<String, Any?>()
        for (field in Repository.fields) {
            if (field!!.alias == "objectid" || field.alias == "globalid") continue
            val alias = field.alias
            val attributeName = field.name
            val attributeValue = featureAttributes[attributeName]
            aliasAttributes[alias] = attributeValue
            if (Repository.fieldTypeMap[field.fieldType] == "Date" && attributeValue != null) {
                val dateString = attributeValue.toString()
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                val date: Date = sdf.parse(dateString)
                val outputFormat = SimpleDateFormat("d/M/yyyy")
                val formattedDate = outputFormat.format(date)
                aliasAttributes[alias] = formattedDate
            }
            else if (field.name == Repository.typeObject) {
                aliasAttributes[alias] = Repository.typeObjectNamesMap[attributeValue]
            }
            else if (field.name == "ocena_dekorativnosti" || field.name == "ocena_kondicije") {
                for (option in Repository.numbersCustomInputFieldList) {
                    if (option.key == attributeValue)
                        aliasAttributes[alias] = option.value
                }
            }
        }
        updateBottomSheetAttributes(aliasAttributes)
    }

    private fun updateBottomSheetAttributes(updatedAttributes: Map<String, Any?>) {
        runOnUiThread {
            featureAttributesAdapter.updateData(listOf(updatedAttributes))
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }

    @SuppressLint("SetTextI18n")
    private fun clearGraphics() {
        if (!isDrawingLine && !isDrawingCircle && !isDrawingPolygon) {
            graphicsOverlay.graphics.clear()
            drawnPoints.clear()
            totalDistance = 0.0
            tvMeasurementLength.text = "Length: ${String.format("%.2f", totalDistance)}m"
            totalArea = 0.0
            tvMeasurementArea.text = "Area: ${String.format("%.2f", totalArea)}m²"
        }
    }

}