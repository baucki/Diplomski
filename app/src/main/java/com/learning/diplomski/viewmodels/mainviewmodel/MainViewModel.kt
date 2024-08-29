package com.learning.diplomski.viewmodels.mainviewmodel

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arcgismaps.ApiKey
import com.arcgismaps.data.Feature
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
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
import com.arcgismaps.tasks.geocode.LocatorTask
import com.learning.diplomski.ui.presentation.EditFeatureActivity
import com.learning.diplomski.ui.adapters.ItemData
import com.learning.diplomski.R
import com.learning.diplomski.data.Repository
import com.learning.diplomski.ui.presentation.SearchActivity
import com.learning.diplomski.viewmodels.mainviewmodel.usecase.BasemapUseCase
import com.learning.diplomski.viewmodels.mainviewmodel.usecase.MeasurementUseCase
import com.learning.diplomski.viewmodels.mainviewmodel.usecase.NavigationUseCase
import com.learning.diplomski.viewmodels.mainviewmodel.usecase.QuickSearchUseCase
import com.learning.diplomski.viewmodels.mainviewmodel.usecase.SelectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    val quickSearchUseCase: QuickSearchUseCase,
    val navigationUseCase: NavigationUseCase,
    val basemapUseCase: BasemapUseCase,
    val measurementUseCase: MeasurementUseCase,
    val selectionUseCase: SelectionUseCase,
) : AndroidViewModel(application) {

    private val _featureAttributes = MutableLiveData<Map<String, Any?>>()
    val featureAttributes: LiveData<Map<String, Any?>>
        get() = _featureAttributes

    private val _showSnackbarMessage = MutableLiveData<String>()
    val showSnackbarMessage: LiveData<String>
        get() = _showSnackbarMessage

    private val _routeResult = MutableLiveData<Graphic>()
    val routeResult: LiveData<Graphic>
        get() = _routeResult

    private val _pinSymbol = MutableLiveData<PictureMarkerSymbol>()
    val pinSymbol: LiveData<PictureMarkerSymbol>
        get() = _pinSymbol

    private val _pointSymbol = MutableLiveData<PictureMarkerSymbol>()
    val pointSymbol: LiveData<PictureMarkerSymbol>
        get() = _pointSymbol

    private var _isAddingFeature = MutableLiveData<Boolean>(false)
    val isAddingFeature: LiveData<Boolean> get() = _isAddingFeature

    private var _showDeleteDialog  = MutableLiveData<Boolean>(false)
    val showDeleteDialog : LiveData<Boolean> get() = _showDeleteDialog

    var featurePoint: Point? = null

    val locatorTask = LocatorTask("https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer")
    val geocodeParameters = GeocodeParameters().apply {
        resultAttributeNames.addAll(listOf("PlaceName", "Place_addr"))
        maxResults = 1
    }

    val serviceFeatureTable = ServiceFeatureTable("http://192.168.1.18:6080/arcgis/rest/services/Servis_SP4_FieldTools/FeatureServer/0")
    val featureLayer = FeatureLayer.createWithFeatureTable(serviceFeatureTable)

    lateinit var graphicsOverlay: GraphicsOverlay

    init {
        viewModelScope.launch {
            _pinSymbol.value = createPinSymbol()
            _pointSymbol.value = createPointSymbol()
        }
    }

    val panModeSpinnerElements = arrayListOf(
        ItemData("Stop", R.drawable.ic_stop),
        ItemData("On", R.drawable.ic_start),
        ItemData("Re-center", R.drawable.ic_re_center),
        ItemData("Navigation", R.drawable.ic_navigation),
        ItemData("Compass", R.drawable.ic_compass)
    )

    val basemapOptions = arrayOf(
        "Streets" to BasemapStyle.ArcGISStreets,
        "Imagery" to BasemapStyle.ArcGISImagery,
        "Topographic" to BasemapStyle.ArcGISTopographic,
        "Oceans" to BasemapStyle.ArcGISOceans,
        "Terrain" to BasemapStyle.ArcGISTerrain
    )

    fun toggleAdding() {
        _isAddingFeature.value = !_isAddingFeature.value!!
    }

    fun getApiKey(): ApiKey {
        return ApiKey.create("AAPK1e43bdcf9fa04fa0a729106fdd7a97fbNbpa3VVhaR5eKzfmkAFb0Uy_soNrGAjpslTJLcWQiNV6T3YGoRy8Sfa7a5ZXkBcj")!!
    }

    fun createMap(): ArcGISMap {
        return ArcGISMap(BasemapStyle.ArcGISStreets).apply {
            operationalLayers.add(featureLayer)
            Repository.featureLayer = featureLayer
        }
    }

    fun getDefaultViewpoint(): Viewpoint {
        return Viewpoint(Point(20.4489, 44.8066, SpatialReference.wgs84()), 7e4)
    }

    fun startSearchActivity(activity: Activity) {
        activity.startActivityForResult(
            Intent(activity, SearchActivity::class.java),
//            Intent(activity, SearchActivity::class.java),
            SearchActivity.SEARCH_FEATURES_REQUEST_CODE
        )
    }

    fun editFeature(activity: Activity) {
        activity.startActivityForResult(
//            Intent(activity, EditFeatureActivity::class.java),
            Intent(activity, EditFeatureActivity::class.java),
            EditFeatureActivity.EDIT_FEATURE_REQUEST_CODE
//            EditFeatureActivity.EDIT_FEATURE_REQUEST_CODE
        )
    }

    private suspend fun createPinSymbol(): PictureMarkerSymbol {
        val drawable = ContextCompat.getDrawable(getApplication(), R.drawable.ic_pin) as BitmapDrawable
        val symbol = PictureMarkerSymbol.createWithImage(drawable)
        symbol.load().getOrThrow()
        symbol.width = 24f
        symbol.height = 36f
        symbol.offsetY = 20f
        return symbol
    }

    private suspend fun createPointSymbol(): PictureMarkerSymbol {
        val drawable = ContextCompat.getDrawable(getApplication(), R.drawable.ic_point) as BitmapDrawable
        val symbol = PictureMarkerSymbol.createWithImage(drawable)
        symbol.load().getOrThrow()
        symbol.width = 10f
        symbol.height = 10f
        return symbol
    }

    suspend fun identifyFeature(
        screenCoordinate: ScreenCoordinate,
        mapView: MapView,
        onResult: (String) -> Unit,
        onFeatureIdentified: (Map<String, Any?>) -> Unit
    ) {
        featureLayer.clearSelection()
        graphicsOverlay.graphics.clear()
        val identifyLayerResult =
            mapView.identifyLayer(featureLayer, screenCoordinate, 5.0, false, 1)

        identifyLayerResult.apply {
            onSuccess { identifyLayerResult ->
                val geoElements = identifyLayerResult.geoElements

                if (geoElements.isNotEmpty() && geoElements[0] is Feature) {
                    val identifiedFeature = geoElements[0] as Feature
                    Repository.feature = identifiedFeature
                    val geometry = identifiedFeature.geometry
                    if (geometry is Point) featurePoint = geometry
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
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

                            val date: java.util.Date = sdf.parse(dateString)
                            val outputFormat = java.text.SimpleDateFormat("d/M/yyyy")
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
                    onFeatureIdentified(aliasAttributes)

                } else {
                    if (_isAddingFeature.value == true) {
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
                                onResult("Feature added successfully.")
                            }
                            onFailure {
                                onResult("Failed to add feature: ${it.message}")
                            }
                        }
                    } else {
                        onResult("No feature identified.")
                    }
                }
            }
        }
    }

    fun onDeleteRequested() {
        _showDeleteDialog.value = true
    }

    fun dialogShown() {
        _showDeleteDialog.value = false
    }

}
