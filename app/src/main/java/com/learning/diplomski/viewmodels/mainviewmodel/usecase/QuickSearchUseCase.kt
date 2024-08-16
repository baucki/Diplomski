package com.learning.diplomski.viewmodels.mainviewmodel.usecase

import android.content.Context
import android.database.MatrixCursor
import android.provider.BaseColumns
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.SearchView
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.tasks.geocode.GeocodeParameters
import com.arcgismaps.tasks.geocode.GeocodeResult
import com.arcgismaps.tasks.geocode.LocatorTask
import com.learning.diplomski.ui.presentation.MainActivity
import com.learning.diplomski.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class QuickSearchUseCase @Inject constructor() {


     fun setupSearchView(
        searchView: SearchView,
        graphicsOverlay: GraphicsOverlay,
        locatorTask: LocatorTask,
        geocodeParameters: GeocodeParameters,
        scope: CoroutineScope,
        mapView: MapView,
        pinSymbol: PictureMarkerSymbol,
        activity: MainActivity
    ) {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    geocodeAddress(
                        it,
                        graphicsOverlay,
                        locatorTask,
                        geocodeParameters,
                        scope,
                        mapView,
                        pinSymbol
                    )
                    searchView.clearFocus()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    if (it.isNotEmpty()) {
                        suggestAddresses(
                            it,
                            scope,
                            locatorTask,
                            searchView,
                            graphicsOverlay,
                            geocodeParameters,
                            mapView,
                            pinSymbol,
                            activity
                        )
                    }
                }
                return true
            }
        })
    }

    fun geocodeAddress(
        address: String,
        graphicsOverlay: GraphicsOverlay,
        locatorTask: LocatorTask,
        geocodeParameters: GeocodeParameters,
        scope: CoroutineScope,
        mapView: MapView,
        pinSymbol: PictureMarkerSymbol
    ) {
        scope.launch {
            graphicsOverlay.graphics.clear()

            geocodeParameters.searchArea = null
            geocodeParameters.maxResults = 1

            locatorTask.load().getOrThrow()
            val results = locatorTask.geocode(address, geocodeParameters).getOrElse {
                return@launch
            }

            if (results.isNotEmpty()) {
                displayResult(results.first(), graphicsOverlay, mapView, pinSymbol)
            }
        }
    }

    private fun displayResult(
        result: GeocodeResult,
        graphicsOverlay: GraphicsOverlay,
        mapView: MapView,
        pinSymbol: PictureMarkerSymbol
    ) {
        graphicsOverlay.graphics.clear()
        val graphic = Graphic(result.displayLocation, result.attributes, pinSymbol)
        graphicsOverlay.graphics.add(graphic)
        mapView.setViewpoint(Viewpoint(result.displayLocation!!, 30000.0))
    }

    private fun suggestAddresses(
        query: String,
        scope: CoroutineScope,
        locatorTask: LocatorTask,
        searchView: SearchView,
        graphicsOverlay: GraphicsOverlay,
        geocodeParameters: GeocodeParameters,
        mapView: MapView,
        pinSymbol: PictureMarkerSymbol,
        activity: MainActivity
    ) {
        scope.launch {
            locatorTask.suggest(query).onSuccess { suggestResults ->

                val simpleCursorAdapter = createSimpleCursorAdapter(activity)
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
                            geocodeAddress(
                                selectedAddress,
                                graphicsOverlay,
                                locatorTask,
                                geocodeParameters,
                                scope,
                                mapView,
                                pinSymbol
                            )
                            searchView.isIconified = true
                            searchView.clearAndHideKeyboard()
                        }
                        return true
                    }
                })

            }
        }
    }

    private fun createSimpleCursorAdapter(activity: MainActivity): androidx.cursoradapter.widget.SimpleCursorAdapter {
        // set up parameters for searching with MatrixCursor
        val columnNames = arrayOf(BaseColumns._ID, "address")
        val suggestionsCursor = MatrixCursor(columnNames)
        // column names for the adapter to look at when mapping data
        val cols = arrayOf("address")
        // ids that show where data should be assigned in the layout
        val to = intArrayOf(R.id.suggestion_address)
        // define SimpleCursorAdapter
        return androidx.cursoradapter.widget.SimpleCursorAdapter(
            activity,
            R.layout.suggestion, suggestionsCursor, cols, to, 0
        )
    }

    fun SearchView.clearAndHideKeyboard() {
        // clear the searched text from the view
        this.clearFocus()
        // close the keyboard once search is complete
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }


}