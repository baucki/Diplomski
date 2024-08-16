package com.learning.diplomski.ui.presentation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.ToggleButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.FeatureQueryResult
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.mapping.view.MapView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.learning.diplomski.ui.components.DeleteConfirmationDialogFragment
import com.learning.diplomski.R
import com.learning.diplomski.data.Repository
import com.learning.diplomski.ui.adapters.FeatureAttributesAdapter
import com.learning.diplomski.viewmodels.mainviewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

@AndroidEntryPoint
class MainActivity: AppCompatActivity(), DeleteConfirmationDialogFragment.ConfirmationListener {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var mapView: MapView
    private lateinit var graphicsOverlay: GraphicsOverlay
    private lateinit var featureAttributesAdapter: FeatureAttributesAdapter
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var bottomSheetView: View

    private lateinit var identifyBottomSheet: View
    private lateinit var identifyBottomSheetBehavior: BottomSheetBehavior<View>

    private lateinit var measurementBottomSheet: View
    private lateinit var measurementBottomSheetBehavior: BottomSheetBehavior<View>

    private lateinit var selectionBottomSheet: View
    private lateinit var selectionBottomSheetBehavior: BottomSheetBehavior<View>

    private lateinit var centerMarker: ImageView

    private lateinit var mainProgressBar: ProgressBar

    private lateinit var searchView: SearchView
    private lateinit var basemapButton: ImageButton

    private lateinit var measurementButton: ImageButton
    // Measurement bottom sheet
    private lateinit var measurementIvDrawLine: ImageView
    private lateinit var measurementIvDrawCircle: ImageView
    private lateinit var measurementIvDrawPolygon: ImageView
    private lateinit var measurementTvMeasurementType: TextView
    private lateinit var measurementTvMeasurementLength: TextView
    private lateinit var measurementTvMeasurementArea: TextView
    private lateinit var measurementLineControls: LinearLayout
    private lateinit var measurementBtnDrawLinePoint: Button
    private lateinit var measurementBtnFinishDrawingLine: Button
    private lateinit var measurementCircleSliderRadius: SeekBar
    private lateinit var measurementBtnFinishDrawingCircle: Button
    private lateinit var measurementBtnDrawPolygonPoint: Button
    private lateinit var measurementBtnFinishDrawingPolygon: Button

    private lateinit var selectionButton: ImageButton
    // Selection bottom sheet
    private lateinit var selectionIvSelectCircle: ImageView
    private lateinit var selectionIvSelectPolygon: ImageView
    private lateinit var selectionTvSelectionType: TextView
    private lateinit var selectionCircleSelectionRadius: SeekBar
    private lateinit var selectionBtnFinishSelectCircle: Button
    private lateinit var selectionPolygonSelectControls: LinearLayout
    private lateinit var selectionBtnDrawPolygonPoint: Button
    private lateinit var selectionBtnFinishSelectPolygon: Button

    private lateinit var navigationButton: ImageButton
    private lateinit var spinner: Spinner
    private lateinit var locationDisplay: LocationDisplay
    private lateinit var searchButton: ImageButton
    private lateinit var toggleButton: ToggleButton

    private lateinit var findRouteButton: ImageButton
    private lateinit var editButton: Button
    private lateinit var deleteButton: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        graphicsOverlay = GraphicsOverlay()

        featureAttributesAdapter = FeatureAttributesAdapter(emptyList())
        ArcGISEnvironment.apiKey = viewModel.getApiKey()
        lifecycle.addObserver(mapView)

        setupView()
        setupListeners()
        setupMapView()
        setupObservers()

        lifecycleScope.launch {
            mapView.viewpointChanged.collect {
                viewModel.measurementUseCase.updateDistance(
                    mapView,
                    measurementTvMeasurementLength
                )
                viewModel.measurementUseCase.updateTemporaryLine(
                    mapView,
                    graphicsOverlay
                )
                viewModel.measurementUseCase.updateTemporaryPolygon(
                    mapView,
                    graphicsOverlay
                )
                viewModel.selectionUseCase.updateTemporarySelectionPolygon(
                    mapView,
                    graphicsOverlay,
                )
            }
        }

    }

    private fun setupView() {
        selectionBottomSheet = findViewById(R.id.select_bottom_sheet)
        selectionBottomSheetBehavior = BottomSheetBehavior.from(selectionBottomSheet).apply {
            peekHeight = 0
        }
        selectionBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        measurementBottomSheet = findViewById(R.id.sheet)
        measurementBottomSheetBehavior = BottomSheetBehavior.from(measurementBottomSheet).apply {
            peekHeight = 0
        }
        measurementBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        centerMarker = findViewById(R.id.center_marker)
        mainProgressBar = findViewById(R.id.mainProgressBar)

        measurementButton = findViewById(R.id.measurementButton)
        selectionButton = findViewById(R.id.spatialSelectionButton)
        initMeasurementBottomSheetView()
        initMeasurementBottomSheetListeners()

        initSelectionBottomSheetView()
        initSelectionBottomSheetListeners()

        basemapButton = findViewById(R.id.basemapButton)

        navigationButton = findViewById(R.id.navigationButton)
        initNavigationView()

        searchView = findViewById(R.id.searchView)

        searchButton = findViewById(R.id.searchButton)
        toggleButton = findViewById(R.id.toggleButton)
    }

    private fun setupListeners() {
        selectionButton.setOnClickListener {
            viewModel.selectionUseCase.selectionButtonClickListener(selectionBottomSheetBehavior)
        }
        measurementButton.setOnClickListener {
            viewModel.measurementUseCase.measurementButtonClickListener(measurementBottomSheetBehavior)
        }
        basemapButton.setOnClickListener {
            viewModel.basemapUseCase.showBasemapSelectionDialog(
                this,
                mapView,
                viewModel.basemapOptions,
                viewModel.featureLayer
            )
        }
        navigationButton.setOnClickListener {
            spinner.performClick()
        }

        searchButton.setOnClickListener {
            viewModel.startSearchActivity(this)
        }

        toggleButton.setOnClickListener {
            viewModel.toggleAdding()
        }


    }

    private fun setupBottomSheetListeners() {
        findRouteButton.setOnClickListener {
            lifecycleScope.launch {
                viewModel.navigationUseCase.solveRoute(
                    mainProgressBar,
                    viewModel.featurePoint!!,
                    graphicsOverlay,
                    this@MainActivity
                ) {
                    showSnackbar("")
                }
            }
        }

        editButton.setOnClickListener {
            viewModel.editFeature(this)
        }

        deleteButton.setOnClickListener {
            viewModel.onDeleteRequested()
        }
    }

    override fun onConfirmDelete() {
        try {
            lifecycleScope.launch {
                viewModel.serviceFeatureTable.deleteFeature(Repository.feature!!).apply {
                    onSuccess {
                        viewModel.serviceFeatureTable.applyEdits()
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

    private fun initNavigationView() {
        spinner = findViewById(R.id.spinner)
        locationDisplay = mapView.locationDisplay
        viewModel.navigationUseCase.setupSpinner(
            this@MainActivity,
            spinner,
            viewModel.panModeSpinnerElements,
            locationDisplay,
            lifecycleScope
        )
        startLocationDisplay(spinner)
    }

    private fun startLocationDisplay(spinner: Spinner) {
        lifecycleScope.launch {
            locationDisplay.dataSource.start()
                .onSuccess {
                    // permission already granted, so start the location display
                    spinner.setSelection(0, true)
                }.onFailure {
                    // check permissions to see if failure may be due to lack of permissions
                    viewModel.navigationUseCase.requestPermissions(
                        this@MainActivity,
                        this@MainActivity,
                        spinner,
                        mapView,
                        lifecycleScope
                    )
                }
        }
    }

    private fun initSelectionBottomSheetView() {
        selectionIvSelectCircle = findViewById(R.id.iv_circle_selection)
        selectionIvSelectPolygon = findViewById(R.id.iv_polygon_selection)
        selectionTvSelectionType = findViewById(R.id.tv_selection_type)
        selectionCircleSelectionRadius = findViewById(R.id.circle_select_slider_radius)
        selectionBtnFinishSelectCircle = findViewById(R.id.btn_select_circle)
        selectionPolygonSelectControls = findViewById(R.id.polygon_select_controls)
        selectionBtnDrawPolygonPoint = findViewById(R.id.btn_draw_polygon_select_point)
        selectionBtnFinishSelectPolygon = findViewById(R.id.btn_select_polygon)

        selectionCircleSelectionRadius.visibility = View.GONE
        selectionBtnFinishSelectCircle.visibility = View.GONE
        selectionPolygonSelectControls.visibility = View.GONE
    }

    private fun  initSelectionBottomSheetListeners() {
        selectionIvSelectCircle.setOnClickListener {
            viewModel.selectionUseCase.startCircleSelection(
                selectionCircleSelectionRadius,
                selectionBtnFinishSelectCircle,
                selectionPolygonSelectControls,
                centerMarker,
                selectionTvSelectionType
            )
        }

        selectionIvSelectPolygon.setOnClickListener {
            viewModel.selectionUseCase.startPolygonSelection(
                selectionCircleSelectionRadius,
                selectionBtnFinishSelectCircle,
                selectionPolygonSelectControls,
                centerMarker,
                selectionTvSelectionType
            )
        }

        selectionCircleSelectionRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.selectionUseCase.drawSelectionCircle(
                    progress,
                    mapView,
                    graphicsOverlay
                )
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        selectionBtnFinishSelectCircle.setOnClickListener {
            viewModel.selectionUseCase.selectFeaturesInCircle(
                viewModel.featureLayer,
                viewModel.serviceFeatureTable,
                lifecycleScope
            ) { message ->
                showSnackbar(message)
            }
        }

        selectionBtnDrawPolygonPoint.setOnClickListener {
            viewModel.selectionUseCase.drawPolygonPoint(
                mapView,
                graphicsOverlay,
                viewModel.pointSymbol.value!!
            )
        }

        selectionBtnFinishSelectPolygon.setOnClickListener {
            viewModel.selectionUseCase.finishPolygonSelection(
                viewModel.featureLayer,
                lifecycleScope,
                centerMarker
            ) { message ->
                showSnackbar(message)
            }
        }
    }

    private fun initMeasurementBottomSheetView() {
        measurementIvDrawLine = findViewById(R.id.iv_draw_line)
        measurementIvDrawCircle = findViewById(R.id.iv_draw_circle)
        measurementIvDrawPolygon = findViewById(R.id.iv_draw_polygon)
        measurementTvMeasurementType = findViewById(R.id.tv_measurement_type)
        measurementTvMeasurementLength = findViewById(R.id.tv_measurement_length)
        measurementTvMeasurementArea = findViewById(R.id.tv_measurement_area)
        measurementLineControls = findViewById(R.id.line_controls)
        measurementBtnDrawLinePoint = findViewById(R.id.btn_draw_line_point)
        measurementBtnFinishDrawingLine = findViewById(R.id.btn_finish_drawing_line)
        measurementCircleSliderRadius = findViewById(R.id.circle_slider_radius)
        measurementBtnFinishDrawingCircle = findViewById(R.id.btn_finish_drawing_circle)
        measurementBtnDrawPolygonPoint = findViewById(R.id.btn_draw_polygon_point)
        measurementBtnFinishDrawingPolygon = findViewById(R.id.btn_finish_drawing_polygon)

        measurementLineControls.visibility = View.GONE
        measurementBtnFinishDrawingCircle.visibility = View.GONE
        measurementBtnFinishDrawingPolygon.visibility = View.GONE
        measurementBtnDrawPolygonPoint.visibility = View.GONE
        measurementTvMeasurementArea.visibility = View.GONE
    }

    private fun initMeasurementBottomSheetListeners() {
        measurementIvDrawLine.setOnClickListener {
            viewModel.measurementUseCase.startDrawingLine(
                measurementTvMeasurementType,
                measurementLineControls,
                measurementTvMeasurementArea,
                measurementCircleSliderRadius,
                measurementBtnFinishDrawingCircle,
                measurementBtnFinishDrawingPolygon,
                measurementBtnDrawPolygonPoint,
                centerMarker
            )
        }
        measurementIvDrawCircle.setOnClickListener {
            viewModel.measurementUseCase.startDrawingCircle(
                measurementTvMeasurementType,
                measurementLineControls,
                measurementTvMeasurementArea,
                measurementCircleSliderRadius,
                measurementBtnFinishDrawingCircle,
                measurementBtnFinishDrawingPolygon,
                measurementBtnDrawPolygonPoint,
                centerMarker
            )
        }
        measurementIvDrawPolygon.setOnClickListener {
            viewModel.measurementUseCase.startDrawingPolygon(
                measurementTvMeasurementType,
                measurementLineControls,
                measurementTvMeasurementArea,
                measurementCircleSliderRadius,
                measurementBtnFinishDrawingCircle,
                measurementBtnFinishDrawingPolygon,
                measurementBtnDrawPolygonPoint,
                centerMarker
            )
        }

        measurementBtnDrawLinePoint.setOnClickListener {
            viewModel.measurementUseCase.drawLinePoint(
                mapView,
                viewModel.pointSymbol.value!!,
                graphicsOverlay,
                measurementTvMeasurementLength
            )
        }

        measurementBtnFinishDrawingLine.setOnClickListener {
            viewModel.measurementUseCase.finishDrawingLine(
                measurementTvMeasurementLength,
                centerMarker,
                graphicsOverlay
            )
        }

        measurementCircleSliderRadius.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.measurementUseCase.drawCircle(
                    progress,
                    mapView,
                    graphicsOverlay,
                    measurementTvMeasurementLength,
                    measurementTvMeasurementArea
                )

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        measurementBtnFinishDrawingCircle.setOnClickListener {
            viewModel.measurementUseCase.finishDrawingCircle()
        }

        measurementBtnDrawPolygonPoint.setOnClickListener {
            viewModel.measurementUseCase.drawPolygonPoint(
                mapView,
                viewModel.pointSymbol.value!!,
                graphicsOverlay
            )
        }

        measurementBtnFinishDrawingPolygon.setOnClickListener {
            viewModel.measurementUseCase.finalizeTemporaryPolygon(graphicsOverlay, centerMarker)
        }
    }

    private fun showDeleteConfirmationDialog() {
        val dialog = DeleteConfirmationDialogFragment()
        dialog.show(supportFragmentManager, "deleteConfirmationDialog")
    }

    private fun setupMapView() {
        mapView.apply {
            map = viewModel.createMap()
            this.graphicsOverlays.add(graphicsOverlay)
            setViewpoint(viewModel.getDefaultViewpoint())

            lifecycleScope.launch {
                onSingleTapConfirmed.collect { tapEvent ->
                    val screenCoordinate = tapEvent.screenCoordinate
                    viewModel.identifyFeature(
                        screenCoordinate,
                        mapView,
                    onResult = {
                        message -> showSnackbar(message)
                    },
                    onFeatureIdentified = { featureAttributes ->
                        displayFeatureAttributes(featureAttributes)
                    })
                }
            }
        }
    }

    private fun setupObservers() {

        viewModel.measurementUseCase.totalArea.observe(this) { area ->
            measurementTvMeasurementArea.text = "Area: ${String.format("%.2f", area)}m²"
        }

        viewModel.measurementUseCase.totalDistance.observe(this) { length ->
            measurementTvMeasurementLength.text = "Length: ${String.format("%.2f", length)}m"
        }

        viewModel.pinSymbol.observe(this, Observer { symbol ->
            if (symbol != null) {
                viewModel.quickSearchUseCase.setupSearchView(
                    searchView,
                    graphicsOverlay,
                    viewModel.locatorTask,
                    viewModel.geocodeParameters,
                    lifecycleScope,
                    mapView,
                    symbol,
                    this
                )
            }
        })

        viewModel.showDeleteDialog.observe(this, Observer { showDialog ->
            if (showDialog) {
                showDeleteConfirmationDialog()
                viewModel.dialogShown() // Reset the LiveData value
            }
        })

    }

    private fun displayFeatureAttributes(featureAttributes: Map<String, Any?>) {
        if (!::bottomSheetDialog.isInitialized) {
            bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_attributes, null)
            bottomSheetDialog = BottomSheetDialog(this)
            bottomSheetDialog.setContentView(bottomSheetView)

            findRouteButton = bottomSheetView.findViewById(R.id.findRoute)
            editButton = bottomSheetView.findViewById(R.id.editButton)
            deleteButton = bottomSheetView.findViewById(R.id.deleteButton)

            setupBottomSheetListeners()

            val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView.parent as View)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

            val recyclerView = bottomSheetView.findViewById<RecyclerView>(R.id.recyclerView)
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = featureAttributesAdapter
        }
        featureAttributesAdapter.updateData(listOf(featureAttributes))
        bottomSheetDialog.show()
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

            viewModel.featureLayer.clearSelection()
            val queryParameters = QueryParameters().apply {
                whereClause = queryString
            }

            lifecycleScope.launch {
                try {
                    val featureQueryResult = viewModel.serviceFeatureTable.queryFeatures(queryParameters).getOrThrow() as FeatureQueryResult

                    for (feature in featureQueryResult) {
                        viewModel.featureLayer.selectFeature(feature)
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
        val featureAttributes = Repository.feature!!.attributes
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

}