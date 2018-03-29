package com.alexsheiko.coffeenearby

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.alexsheiko.coffeenearby.R.string
import com.google.android.gms.location.places.AutocompleteFilter.Builder
import com.google.android.gms.location.places.AutocompleteFilter.TYPE_FILTER_ESTABLISHMENT
import com.google.android.gms.location.places.AutocompletePredictionBufferResponse
import com.google.android.gms.location.places.GeoDataClient
import com.google.android.gms.location.places.PlaceDetectionClient
import com.google.android.gms.location.places.Places
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_main.*

private const val QUERY_STARBUCKS = "Starbucks"

class MainActivity : AppCompatActivity() {

    private lateinit var geoDataClient: GeoDataClient
    private lateinit var placeDetectionClient: PlaceDetectionClient

    private val placesAdapter by lazy { PlacesAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRecyclerView()

        initPlaceLoader()

        val request = createPlacesRequest()
        request.addOnCompleteListener { response ->
            if (response.isSuccessful) {
                // If places are loaded, show them in a list
                response.result.forEach {
                    // Get place info like name, address, images
                    val placeDetailsRequest = geoDataClient.getPlaceById(it.placeId)
                    placeDetailsRequest.addOnCompleteListener {
                        if (it.isSuccessful) {
                            // Display most likely suggestion for place ID
                            placesAdapter.add(it.result.single())
                        } else {
                            it.exception?.printStackTrace()
                        }
                    }
                }
                statusTextView.text = "Loaded ${response.result.count()} places"
            } else {
                // On error, notify user
                statusTextView.text = getString(string.error_loading_failed)
                // Log exception with more details
                response.exception?.printStackTrace()
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView.apply {
            setHasFixedSize(true)
            adapter = placesAdapter
        }
    }

    private fun initPlaceLoader() {
        // Construct a GeoDataClient.
        geoDataClient = Places.getGeoDataClient(this)

        // Construct a PlaceDetectionClient.
        placeDetectionClient = Places.getPlaceDetectionClient(this)
    }

    private fun createPlacesRequest(): Task<AutocompletePredictionBufferResponse> {
        val placeFilter = Builder().setTypeFilter(TYPE_FILTER_ESTABLISHMENT).build()
        return geoDataClient.getAutocompletePredictions(QUERY_STARBUCKS, null,
                placeFilter)
    }
}