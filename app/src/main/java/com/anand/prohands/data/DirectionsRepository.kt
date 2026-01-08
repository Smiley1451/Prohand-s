package com.anand.prohands.data

import android.util.Log
import com.anand.prohands.network.DirectionsApi
import com.anand.prohands.network.DirectionsResponse
import com.anand.prohands.network.PolylineDecoder
import com.google.android.gms.maps.model.LatLng
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Repository for fetching directions from Google Directions API.
 */
class DirectionsRepository {

    companion object {
        private const val TAG = "DirectionsRepository"
        private const val BASE_URL = "https://maps.googleapis.com/"
    }

    private val directionsApi: DirectionsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DirectionsApi::class.java)
    }

    /**
     * Fetch directions between two points.
     * @param originLat Starting latitude
     * @param originLng Starting longitude
     * @param destLat Destination latitude
     * @param destLng Destination longitude
     * @param apiKey Google Maps API key
     * @return DirectionsResult with route info and steps, or null if failed
     */
    suspend fun getDirections(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        apiKey: String
    ): DirectionsResult? {
        return try {
            val origin = "$originLat,$originLng"
            val destination = "$destLat,$destLng"

            Log.d(TAG, "Fetching directions from $origin to $destination")

            val response = directionsApi.getDirections(
                origin = origin,
                destination = destination,
                mode = "driving",
                apiKey = apiKey
            )

            if (response.isSuccessful) {
                val directionsResponse = response.body()
                if (directionsResponse != null && directionsResponse.status == "OK") {
                    parseDirectionsResponse(directionsResponse)
                } else {
                    Log.e(TAG, "Directions API returned status: ${directionsResponse?.status}")
                    null
                }
            } else {
                Log.e(TAG, "Directions API failed: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching directions", e)
            null
        }
    }

    private fun parseDirectionsResponse(response: DirectionsResponse): DirectionsResult? {
        val route = response.routes.firstOrNull() ?: return null
        val leg = route.legs.firstOrNull() ?: return null

        // Decode the overview polyline
        val routePoints = route.overview_polyline?.points?.let {
            PolylineDecoder.decode(it)
        } ?: emptyList()

        // Parse navigation steps
        val navigationSteps = leg.steps.map { step ->
            NavigationStep(
                instruction = cleanHtmlInstructions(step.html_instructions),
                distance = step.distance?.text ?: "",
                distanceMeters = step.distance?.value ?: 0,
                duration = step.duration?.text ?: "",
                maneuver = step.maneuver ?: "",
                polylinePoints = step.polyline?.points?.let { PolylineDecoder.decode(it) } ?: emptyList()
            )
        }

        return DirectionsResult(
            totalDistance = leg.distance?.text ?: "",
            totalDistanceMeters = leg.distance?.value ?: 0,
            totalDuration = leg.duration?.text ?: "",
            totalDurationSeconds = leg.duration?.value ?: 0,
            startAddress = leg.start_address,
            endAddress = leg.end_address,
            routePoints = routePoints,
            steps = navigationSteps
        )
    }

    private fun cleanHtmlInstructions(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), "") // Remove HTML tags
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }
}

/**
 * Parsed directions result
 */
data class DirectionsResult(
    val totalDistance: String,
    val totalDistanceMeters: Int,
    val totalDuration: String,
    val totalDurationSeconds: Int,
    val startAddress: String,
    val endAddress: String,
    val routePoints: List<LatLng>,
    val steps: List<NavigationStep>
)

/**
 * Individual navigation step
 */
data class NavigationStep(
    val instruction: String,
    val distance: String,
    val distanceMeters: Int,
    val duration: String,
    val maneuver: String,
    val polylinePoints: List<LatLng>
)
