package com.alexsheiko.coffeenearby

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.alexsheiko.coffeenearby.R.string
import com.google.android.gms.location.places.AutocompleteFilter
import com.google.android.gms.location.places.AutocompleteFilter.TYPE_FILTER_ESTABLISHMENT
import com.google.android.gms.location.places.GeoDataClient
import com.google.android.gms.location.places.PlaceDetectionClient
import com.google.android.gms.location.places.Places
import kotlinx.android.synthetic.main.activity_main.*

private const val QUERY_STARBUCKS = "Starbucks"

class MainActivity : AppCompatActivity() {

    private lateinit var mGeoDataClient: GeoDataClient
    private lateinit var mPlaceDetectionClient: PlaceDetectionClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(this)

        // Construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this)

        val placeFilter = AutocompleteFilter.Builder().setTypeFilter(TYPE_FILTER_ESTABLISHMENT).build();
        val results = mGeoDataClient.getAutocompletePredictions(QUERY_STARBUCKS, null,
                placeFilter)

        results.addOnCompleteListener { response ->
            if (response.isSuccessful) {
                // If places are loaded, show them in a list
                response.result.forEach { item -> item.getPrimaryText(null) }
                statusTextView.text = "Loaded ${response.result.count()} places"
            } else {
                // On error, notify user
                statusTextView.text = getString(string.error_loading_failed)
                // Log exception with more details
                response.exception?.printStackTrace()
            }
        }
    }
}