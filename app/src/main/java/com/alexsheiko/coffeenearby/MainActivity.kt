package com.alexsheiko.coffeenearby

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.alexsheiko.coffeenearby.R.string
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers.io
import kotlinx.android.synthetic.main.activity_main.*

private const val QUERY_STARBUCKS = "Starbucks"

class MainActivity : AppCompatActivity() {

    private lateinit var geoDataClient: GeoDataClient
    private lateinit var placeDetectionClient: PlaceDetectionClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val placesAdapter by lazy { PlacesAdapter() }
    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRecyclerView()

        initPlaceFinder()
        initMyLocationProvider()

        // Add async operation to disposables to be able to stop and release resources onDestroy
        disposables.add(
                // Load all places that match "Starbucks" query
                loadPlaces(QUERY_STARBUCKS)
                        // Convert response to a list of places
                        .flatMapIterable { response -> response.asIterable() }
                        // For each place, load additional info to show in a preview
                        .flatMap { loadPlaceInfo(it.placeId) }
                        // Combine all places with details back to list
                        .toList()
                        // Execute all these operations on the background IO thread
                        .subscribeOn(io())
                        // Get notified about the results on the main UI thread
                        .observeOn(mainThread())
                        // Afterwards, hide progress indicator
                        .doAfterSuccess { statusTextView.text = null }
                        // Subscribe to observable chain to get results
                        .subscribe(
                                {
                                    // On success, show loaded place list in the recycler view
                                    placeList ->
                                    placesAdapter.addAll(placeList)
                                },
                                {
                                    // On error, notify user and log error
                                    handleError(it)
                                }
                        )
        )
    }

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    private fun handleError(it: Throwable) {
        // On error, notify user
        statusTextView.text = getString(string.error_loading_failed)
        // Log exception with more details
        it.printStackTrace()
    }

    private fun initMyLocationProvider() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupRecyclerView() {
        recyclerView.apply {
            setHasFixedSize(true)
            adapter = placesAdapter
        }
    }

    private fun initPlaceFinder() {
        // Construct a GeoDataClient.
        geoDataClient = Places.getGeoDataClient(this)

        // Construct a PlaceDetectionClient.
        placeDetectionClient = Places.getPlaceDetectionClient(this)
    }

    private fun loadPlaces(query: String): Observable<AutocompletePredictionBufferResponse> {
        return Observable.create { emitter ->
            val request = geoDataClient.getAutocompletePredictions(query, null, null)
            request.addOnSuccessListener {
                emitter.onNext(it)
                emitter.onComplete()
            }
            request.addOnFailureListener { emitter.onError(it) }
        }
    }

    private fun loadPlaceInfo(placeId: String?): Observable<Place> {
        // If no Id provided, return nothing and log error
        if (placeId == null) {
            Log.e(javaClass.name, "Encountered a place with no Id")
            return Observable.empty()
        }
        // Get place info like name, address, images
        return Observable.create { emitter ->
            val request = geoDataClient.getPlaceById(placeId)
            request.addOnSuccessListener {
                // Use most likely match for place Id
                emitter.onNext(it.single())
                emitter.onComplete()
            }
            request.addOnFailureListener { emitter.onError(it) }
        }
    }
}