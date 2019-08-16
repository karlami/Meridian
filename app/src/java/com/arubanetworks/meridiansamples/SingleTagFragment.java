package com.arubanetworks.meridiansamples;

import android.graphics.PointF;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;

import com.arubanetworks.meridian.Meridian;
import com.arubanetworks.meridian.editor.EditorKey;
import com.arubanetworks.meridian.internal.util.Strings;
import com.arubanetworks.meridian.maps.MapFragment;
import com.arubanetworks.meridian.maps.Transaction;
import com.arubanetworks.meridian.tags.TagBeacon;
import com.arubanetworks.meridian.tags.TagMarker;
import com.arubanetworks.meridian.tags.TagStream;

import java.util.List;

public class SingleTagFragment extends MapFragment implements TagStream.Listener {

    private static final String TAG_MAC = "meridian.TAG_MAC";

    private String tagMac;
    private TagMarker tagMarker;
    private TagStream tagStream;

    public static SingleTagFragment newInstance(EditorKey mapKey, String tagMac) {
        SingleTagFragment f = new SingleTagFragment();
        Bundle args = new MapFragment.Builder().setMapKey(mapKey).build().getArguments();
        if (args != null) {
            args.putString(TAG_MAC, tagMac);
            f.setArguments(args);
        }
        return f;
    }

    @Override
    public void onMapLoadStart() {
        super.onMapLoadStart();
        if (Strings.isNullOrEmpty(Meridian.getShared().getEditorToken())) {
            if (getActivity() != null) {
                new AlertDialog.Builder(getActivity())
                        .setMessage("You need to provide a valid editor token")
                        .setPositiveButton("OK", null)
                        .show();
            }
        } else {
            // Close out any existing tags when loading a new mapkey
            if (tagStream != null) tagStream.stopUpdatingTags();

            // Create our TagStream and start it.
            tagStream = new TagStream.Builder()
                    .addTagMac(Application.APP_KEY, tagMac)
                    .setListener(this)
                    .build();
            tagStream.startUpdatingTags();
        }
    }
    @Override
    public void onTagsUpdated(List<TagBeacon> tags) {
        Transaction.Builder transaction = new Transaction.Builder().setType(Transaction.Type.UPDATE);

        // We have new or updated tags lets iterate through and add them to the map.
        for (TagBeacon tag : tags) {
            if (!tagMac.equals(tag.getMac())) continue;
            if (getMapView() != null && getMapView().getMapKey() != null && !tag.getMapId().equals(getMapView().getMapKey().getId())) {
                getMapView().setMapKey(EditorKey.forMap(tag.getMapId(), getMapView().getAppKey()));
                continue;
            }

            if (tagMarker == null && getContext() != null) {
                tagMarker = new TagMarker(getContext(), tag);

            } else {
                PointF point = tag.getLocation().getPoint();
                tagMarker.setPosition(point.x, point.y);
            }

            transaction.addMarker(tagMarker);
            // Commit the transaction
            getMapView().commitTransaction(transaction.build());
            break; // found it, no need to do more
        }
    }

    @Override
    public void onTagsRemoved(List<TagBeacon> tags) {
        Transaction.Builder transaction = new Transaction.Builder().setType(Transaction.Type.REMOVE);

        if (tagMarker == null) return;

        for (TagBeacon tag : tags) {
            if (!tagMac.equals(tag.getMac())) continue;

            transaction.addMarker(tagMarker);
            getMapView().commitTransaction(transaction.build());
            break; // found it, no need to do more
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tagMac = getArguments().getString(TAG_MAC);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // If we are paused we stop listening to tag updates
        if (tagStream != null) tagStream.stopUpdatingTags();
    }

    @Override
    public void onResume() {
        super.onResume();
        // If we are resuming we start listening for tags again.
        if (tagStream != null) tagStream.startUpdatingTags();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up memory.
        if (tagStream != null) tagStream.dispose();
    }
}
