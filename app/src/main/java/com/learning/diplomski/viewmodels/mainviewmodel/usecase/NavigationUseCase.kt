package com.learning.diplomski.viewmodels.mainviewmodel.usecase

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import android.widget.AdapterView
import android.widget.ProgressBar
import android.widget.Spinner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.tasks.networkanalysis.RouteResult
import com.arcgismaps.tasks.networkanalysis.RouteTask
import com.arcgismaps.tasks.networkanalysis.Stop
import com.learning.diplomski.ui.adapters.ItemData
import com.learning.diplomski.R
import com.learning.diplomski.ui.adapters.SpinnerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class NavigationUseCase @Inject constructor() {

    var currentPoint: Point? = null

    fun requestPermissions(
        context: Context,
        activity: Activity, // For requesting permissions
        spinner: Spinner,
        mapView: MapView,
        scope: CoroutineScope // To manage coroutines
    ) {
        val permissionCheckCoarseLocation =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val permissionCheckFineLocation =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!(permissionCheckCoarseLocation && permissionCheckFineLocation)) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                2
            )
        } else {
            scope.launch {
                mapView.locationDisplay.dataSource.start().onSuccess {
                    spinner.setSelection(1, true)
                }
            }
        }
    }

    fun setupSpinner(
        activity: Activity,
        spinner: Spinner,
        panModeSpinnerElements: ArrayList<ItemData>,
        locationDisplay: LocationDisplay,
        lifecycleScope: LifecycleCoroutineScope // To manage coroutines
    ) {
        spinner.apply {
            adapter = SpinnerAdapter(activity, R.id.locationTextView, panModeSpinnerElements)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    when (panModeSpinnerElements[position].text) {
                        "Stop" -> {
                            lifecycleScope.launch {
                                locationDisplay.dataSource.stop()
                            }
                        }
                        "On" -> {
                            lifecycleScope.launch {
                                locationDisplay.dataSource.start()
                                locationDisplay.location.collect { location ->
                                    currentPoint = location?.position
                                }
                            }
                        }
                        "Re-center" -> {
                            locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
                        }
                        "Navigation" -> {
                            locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)
                        }
                        "Compass" -> {
                            locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.CompassNavigation)
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    suspend fun solveRoute(
        mainProgressBar: ProgressBar,
//        currentPoint: Point,
        featurePoint: Point,
        graphicsOverlay: GraphicsOverlay,
        activity: Activity,
        onResult: (message: String) -> Unit
    ) {
        ArcGISEnvironment.applicationContext = activity.applicationContext
        // create a route task instance
        val routeTask = RouteTask(
            "https://route-api.arcgis.com/arcgis/rest/services/World/Route/NAServer/Route_World"
        )

        // show the progress bar
        mainProgressBar.visibility = View.VISIBLE
        routeTask.createDefaultParameters().onSuccess { routeParams ->

            // create stops
            if (currentPoint != null && featurePoint != null) {

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
                    onResult(it.message.toString())
                    return@onSuccess
                } as RouteResult

                val route = routeResult.routes[0]
                // create a simple line symbol for the route
                val routeSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.red, 5f)

                // create a graphic for the route and add it to the graphics overlay
                graphicsOverlay.graphics.add(Graphic(route.routeGeometry, routeSymbol))
                mainProgressBar.visibility = View.GONE
            } else {
                onResult("Points are null")
                mainProgressBar.visibility = View.GONE
            }
        }.onFailure {
            onResult(it.message.toString())
            mainProgressBar.visibility = View.GONE
        }
    }

}