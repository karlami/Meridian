package com.arubanetworks.meridiansamples

import androidx.appcompat.app.AlertDialog
import com.arubanetworks.meridian.Meridian
import com.arubanetworks.meridian.editor.EditorKey
import com.arubanetworks.meridian.internal.util.Strings
import com.arubanetworks.meridian.maps.MapFragment
import com.arubanetworks.meridian.maps.Transaction
import com.arubanetworks.meridian.tags.TagBeacon
import com.arubanetworks.meridian.tags.TagMarker
import com.arubanetworks.meridian.tags.TagStream
import java.util.*


/**
 * A Fragment to demonstrate the use of the TagStream for use in displaying Tags
 */
class TagsFragment : MapFragment(), TagStream.Listener {

    // A collection to maintain our TagMarkers
    private val tagMarkers = HashMap<TagBeacon, TagMarker>()

    // The TagStream itself
    private var tagStream: TagStream? = null

    override fun onMapLoadStart() {
        super.onMapLoadStart()
        if (Strings.isNullOrEmpty(Meridian.getShared().editorToken)) {
            if (activity != null) {
                AlertDialog.Builder(activity!!)
                        .setMessage("You need to provide a valid editor token")
                        .setPositiveButton("OK", null)
                        .show()
            }
        } else {
            // Close out any existing tags when loading a new map key
            tagStream?.stopUpdatingTags()

            // Clear any references to old markers so we don't re-add them to the new map on accident
            tagMarkers.clear()

            // Create our TagStream and start it.
            mapView?.mapKey?.let {
                tagStream = TagStream.Builder()
                        .addMapKey(it)
                        .setListener(this)
                        .build()
            }
            tagStream?.startUpdatingTags()
        }
    }

    override fun onTagsUpdated(tags: List<TagBeacon>) {

        val transaction = Transaction.Builder().setType(Transaction.Type.UPDATE)

        // We have new or updated tags lets iterate through and add them to the map.
        for (tag in tags) {
            // ignore tags that are not on the current map
            if (mapView != null && mapView.mapKey != null && tag.mapId != mapView.mapKey.id) continue
            var currentMarker: TagMarker? = tagMarkers[tag]

            // check for a new Tag and add
            if (currentMarker == null) {
                if (context == null) {
                    return
                }
                currentMarker = TagMarker(context!!, tag)
                tagMarkers[tag] = currentMarker
            } else {
                // Otherwise we need to update our current marker to reflect the new tag state
                val point = tag.location?.point
                point?.let { currentMarker.setPosition(it.x, it.y) }

                // The title or image could also have changed but the TagMarker provided with the
                // SDK doesn't show those fields by default so we can ignore those changes here.
            }

            transaction.addMarker(currentMarker)
        }

        // Commit the transaction
        if (transaction.size > 0)
            mapView.commitTransaction(transaction.build())
    }

    override fun onTagsRemoved(tags: List<TagBeacon>) {
        val transaction = Transaction.Builder().setType(Transaction.Type.REMOVE)

        for (tag in tags) {
            val currentMarker = tagMarkers[tag] ?: continue

            // We can ignore removing the tags marker if we never added it.

            transaction.addMarker(currentMarker)
        }

        mapView?.commitTransaction(transaction.build())
    }

    override fun onPause() {
        super.onPause()
        // If we are paused we stop listening to tag updates
        tagStream?.stopUpdatingTags()
    }

    override fun onResume() {
        super.onResume()
        // If we are resuming we start listening for tags again.
        tagStream?.startUpdatingTags()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up memory.
        tagStream?.dispose()
    }

    companion object {

        fun newInstance(mapKey: EditorKey): TagsFragment {
            val f = TagsFragment()
            f.arguments = MapFragment.Builder().setMapKey(mapKey).build().arguments
            return f
        }
    }
}
