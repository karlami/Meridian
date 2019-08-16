package com.arubanetworks.meridiansamples;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.arubanetworks.meridian.editor.EditorKey;
import com.arubanetworks.meridian.editor.Placemark;
import com.arubanetworks.meridian.log.MeridianLogger;
import com.arubanetworks.meridian.maps.MapFragment;
import com.arubanetworks.meridian.maps.MapInfo;
import com.arubanetworks.meridian.maps.Marker;
import com.arubanetworks.meridian.maps.PlacemarkMarker;
import com.arubanetworks.meridian.maps.Transaction;

import java.util.ArrayList;
import java.util.Random;


public class CustomMarkerFragment extends MapFragment {

    private static final MeridianLogger LOG = MeridianLogger.forTag("TEST").andFeature(MeridianLogger.Feature.MAPS);

    public static CustomMarkerFragment newInstance(EditorKey mapKey){
        CustomMarkerFragment f = new CustomMarkerFragment();
        MapFragment mapFragment = new MapFragment.Builder()
                .setMapKey(mapKey)
                .build();
        f.setArguments(mapFragment.getArguments());
        return f;
    }

    /**
     * This method is an opportunity to supply your own {@link Marker} instance for a given Placemark.
     * You can use this to completely customize the look of placemarks on the map. The default implementation
     * returns null.
     *
     * @param placemark The placemark that is requesting a {@link Marker} for displaying on the map
     * @return A new instance of a {@link Marker} subclass, or null to request that MapView create a default {@link Marker}.
     */
    @Override
    public Marker markerForPlacemark(Placemark placemark) {
        Context c = getActivity();
        if(c == null)return null;
        if(placemark.getType().contains("shop")){
            PlacemarkMarker pm = new PlacemarkMarker.Builder(c, placemark)
                    //.setIcon(R.drawable.my_icon) // use your custom icon
                    //.setIconColor(ContextCompat.getColor(c, R.color.my_color)) // sets the background color
                    //.setIconPadding(R.dimen.my_padding) // sets the padding for the icon
                    .build();
            //pm.setWeight(2f); // Make shops show up on top of other placemarks.
            //pm.setCollision(false); // Exclude shops from collision checks.
            //pm.setScaleFixedToMap(true);// Make shops with fixed scale on the map surface.
            return pm;
        }
        return super.markerForPlacemark(placemark);
    }

    /**
     * This method is an opportunity to supply your own selected {@link Marker} instance for a given {@link Marker}.
     * You can use this to completely customize the look of selected placemarks on the map. The default implementation
     * returns null.
     *
     * @param markerToSelect The Marker that is being selected.
     * @return A new instance of a {@link Marker} subclass, or null to request that MapView create a default {@link Marker}.
     */
    @Override
    public Marker markerForSelectedMarker(Marker markerToSelect) {
        if (markerToSelect instanceof RandMarker && getContext() != null)
            return new RandSelectedMarker(getContext(), markerToSelect);
        return super.markerForSelectedMarker(markerToSelect);
    }

    /**
     * Called after the map and placemarks have been loaded and rendered. Changing any markers
     * should be avoided.
     */
    @Override
    public void onMapRenderFinish() {
        super.onMapRenderFinish();
        // Remove an existing marker named lobby.
        for (Marker m : getMapView().getAllMarkers()) {
            if (m instanceof PlacemarkMarker && ((PlacemarkMarker) m).getPlacemark().getName().equalsIgnoreCase("lobby")) {
                getMapView().commitTransaction(new Transaction.Builder().setType(Transaction.Type.REMOVE).addMarker(m).setListener(new Transaction.Listener() {
                    @Override
                    public void onTransactionAnimationComplete() {
                        LOG.d("Animation complete removal.");
                    }

                    @Override
                    public void onTransactionComplete() {
                        LOG.d("Completed removal.");
                    }

                    @Override
                    public void onTransactionCanceled() {
                        LOG.d("Canceled removal.");
                    }

                    @Override
                    public void onTransactionFailed() {
                        LOG.d("Failed removal.");
                    }
                }).build());
                break;
            }
        }

        // Create 20 random Markers
        Random random = new Random(18923501986340L);
        ArrayList<Marker> markerList = new ArrayList<>();
        for(int i=0;i<20;i++) {
            Context c = getActivity();
            if (c == null) break;
            if (getMapView() != null) {
                RandMarker marker = new RandMarker(getActivity(), getMapView().getMapInfo(), random);
                marker.setName("Custom marker " + i);
                markerList.add(marker);
            }
        }

        // Add them to the map the markers.
        getMapView().commitTransaction(new Transaction.Builder().setAnimationDuration(500).addMarkers(markerList).build());
    }

    /**
     * A class the extends Marker and displays a copy of the apps launcher icon to use as an
     * example.
     */
    private static class RandMarker extends Marker{

        private final Context appContext;
        public RandMarker(@NonNull Context context, @NonNull MapInfo mapInfo, @NonNull Random random){
            super(random.nextInt((int)mapInfo.getWidth()),random.nextInt((int)mapInfo.getHeight()));
            this.appContext = context.getApplicationContext();
            setWeight(2.1f);
        }
        @Override
        public Bitmap getBitmap() {
            return BitmapFactory.decodeResource(appContext.getResources(), R.drawable.ic_launcher);
        }
    }

    /**
     * A class the extends Marker and displays an enlarged copy of the apps launcher icon to use as an
     * example.
     */
    private static class RandSelectedMarker extends Marker {

        private final Context appContext;

        public RandSelectedMarker(@NonNull Context context, @NonNull Marker baseMarker) {
            super(baseMarker.getPosition()[0], baseMarker.getPosition()[1]);
            this.appContext = context.getApplicationContext();
            setWeight(3.1f);
            setXScale(1.5f);
            setYScale(1.5f);
        }

        @Override
        public Bitmap getBitmap() {
            return BitmapFactory.decodeResource(appContext.getResources(), R.drawable.ic_launcher);
        }

        @Override
        public boolean canBeSelected() {
            return false;
        }
    }
}
