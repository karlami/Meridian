package com.arubanetworks.meridiansamples

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import com.arubanetworks.meridian.editor.EditorKey
import com.arubanetworks.meridian.maps.MapFragment
import com.arubanetworks.meridian.maps.directions.DirectionsDestination
import com.arubanetworks.meridian.maps.directions.DirectionsSource

/**
 * Demonstrates the use of the directions API to request a route and display the steps as text.
 */

class DirectionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_directions)

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)


        var appKey: EditorKey? = null
        var mapKey: EditorKey? = null
        var source: DirectionsSource? = null
        var destination: DirectionsDestination? = null

        intent?.let { intent ->
            intent.getSerializableExtra(APP_KEY)?.let { appKey = it as EditorKey }
            intent.getSerializableExtra(MAP_KEY)?.let { mapKey = it as EditorKey }
            intent.getSerializableExtra(DIRECTIONS_SOURCE)?.let { source = it as DirectionsSource }
            intent.getSerializableExtra(DIRECTIONS_DESTINATION)?.let { destination = it as DirectionsDestination }
        }

        if (savedInstanceState == null) {
            val mapFragment = MapFragment.Builder()
                    .setAppKey(appKey)
                    .setMapKey(mapKey)
                    .setSource(source)
                    .setDestination(destination)
                    .build()

            supportFragmentManager
                    .beginTransaction()
                    .add(android.R.id.content, mapFragment)
                    .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // this will pass through to our map fragment
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {

        private const val APP_KEY = "meridian.AppKey"
        private const val MAP_KEY = "meridian.MapKey"
        private const val DIRECTIONS_SOURCE = "meridian.DirectionsSource"
        private const val DIRECTIONS_DESTINATION = "meridian.DirectionsDestination"

        fun createIntent(context: Context?, appKey: EditorKey?, source: DirectionsSource?, destination: DirectionsDestination?): Intent {
            return Intent(context, DirectionsActivity::class.java)
                    .putExtra(APP_KEY, appKey)
                    .putExtra(DIRECTIONS_SOURCE, source)
                    .putExtra(DIRECTIONS_DESTINATION, destination)
        }

        fun createIntent(context: Context?, appKey: EditorKey?, mapKey: EditorKey?, destination: DirectionsDestination?): Intent {
            return Intent(context, DirectionsActivity::class.java)
                    .putExtra(APP_KEY, appKey)
                    .putExtra(MAP_KEY, mapKey)
                    .putExtra(DIRECTIONS_DESTINATION, destination)
        }
    }
}
