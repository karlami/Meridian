package com.arubanetworks.meridiansamples;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.arubanetworks.meridian.editor.EditorKey;
import com.arubanetworks.meridian.editor.Placemark;
import com.arubanetworks.meridian.internal.util.Strings;
import com.arubanetworks.meridian.location.MeridianLocation;
import com.arubanetworks.meridian.location.MeridianOrientation;
import com.arubanetworks.meridian.maps.MapFragment;
import com.arubanetworks.meridian.maps.MapView;
import com.arubanetworks.meridian.maps.Marker;
import com.arubanetworks.meridian.requests.MeridianRequest;
import com.arubanetworks.meridian.requests.PlacemarkRequest;


public class SingleMarkerIDFragment extends Fragment implements MeridianRequest.Listener<Placemark>, MeridianRequest.ErrorListener {

    private static final String KEY_UID = "meridian.SingleMarkerIDFragment.UID";
    private static final String KEY_PID = "meridian.SingleMarkerIDFragment.PID";
    private static final String KEY_APPKEY = "meridian.SingleMarkerIDFragment.APPKEY";

    public static SingleMarkerIDFragment newInstance(EditorKey appKey, String uid, String pid) {
        Bundle args = new Bundle();

        if (!Strings.isNullOrEmpty(uid))
            args.putString(KEY_UID, uid);
        else if (!Strings.isNullOrEmpty(pid))
            args.putString(KEY_PID, pid);
        else
            throw new RuntimeException("UID or PID must not be null when initialising SingleMarkerIDFragment!");

        if (appKey == null || appKey.getParent() != null)
            throw new RuntimeException("AppKey must be a valid top level EditorKey when initialising SingleMarkerIDFragment!");

        args.putSerializable(KEY_APPKEY, appKey);
        SingleMarkerIDFragment f = new SingleMarkerIDFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            PlacemarkRequest.Builder builder = new PlacemarkRequest.Builder()
                    .setAppKey((EditorKey) getArguments().getSerializable(KEY_APPKEY))
                    .setListener(this)
                    .setErrorListener(this);

            if (!Strings.isNullOrEmpty(getArguments().getString(KEY_PID))) {
                builder.setPlacemarkID(getArguments().getString(KEY_PID));
            } else if (!Strings.isNullOrEmpty(getArguments().getString(KEY_UID))) {
                builder.setUID(getArguments().getString(KEY_UID));
            }

            builder.build().sendRequest();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ProgressBar v = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleLarge);
        v.setIndeterminate(true);
        return v;
    }

    @Override
    public void onResponse(Placemark placemark) {
        if (placemark == null || placemark.getKey() == null || placemark.getKey().getParent() == null) {
            if (getActivity() != null) {
                new AlertDialog.Builder(getActivity())
                        .setMessage("You need to provide a valid placemark")
                        .setPositiveButton("OK", null)
                        .show();
            }
            if (getView() != null)
                getView().setVisibility(View.INVISIBLE);
            return;
        }
        if (getView() != null) {
            if (getFragmentManager() != null) {
                getFragmentManager().beginTransaction().replace(((ViewGroup) getView().getParent()).getId(), SingleMarkerInteriorFragment.newInstance(placemark)).commitAllowingStateLoss();
            }
        }
    }

    @Override
    public void onError(Throwable tr) {
        Toast.makeText(getActivity(), "Failed with error " + tr.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        if (getView() != null)
            getView().setVisibility(View.INVISIBLE);
    }

    public static class SingleMarkerInteriorFragment extends MapFragment {

        private static final String KEY_PKEY = "meridian.SingleMarkerInteriorFragment.PKEY";
        EditorKey placemarkKey;


        @Override
        public void onCreate(Bundle savedInstanceState) {
            if (getArguments() != null) {
                placemarkKey = (EditorKey) getArguments().getSerializable(KEY_PKEY);
            }
            super.onCreate(savedInstanceState);
        }

        public static SingleMarkerInteriorFragment newInstance(Placemark placemark) {
            MapFragment f = new MapFragment.Builder()
                    .setMapKey(placemark.getKey().getParent())
                    .setPlacemarkId(placemark.getKey().getId())
                    .build();
            Bundle args = f.getArguments();
            if (args != null) {
                args.putSerializable(KEY_PKEY, placemark.getKey());
            }
            SingleMarkerInteriorFragment tf = new SingleMarkerInteriorFragment();
            tf.setArguments(args);
            return tf;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = super.onCreateView(inflater, container, savedInstanceState);
            getMapView().setMapEventListener(new MapView.MapEventListener() {
                @Override public void onMapLoadStart() { }
                @Override public void onMapLoadFinish() {}
                @Override public void onPlacemarksLoadFinish() { }
                @Override public void onMapLoadFail(Throwable tr) { }
                @Override public void onMapTransformChange(Matrix transform) { }
                @Override public void onLocationUpdated(MeridianLocation location) { }
                @Override public void onOrientationUpdated(MeridianOrientation orientation) { }
                @Override public boolean onLocationButtonClick() { return false; }

                @Override
                public void onMapRenderFinish() {
                    // Set our callout to the correct Marker
                    Marker target = getMapView().getAllMarkers().get(0);
                    getMapView().onMarkerClick(target);

                    // Get the View rect and center it on 0,0
                    RectF r = new RectF(0,0,getMapView().getWidth(), getMapView().getHeight());
                    r.offset(-r.width()/2, -r.height()/2);

                    // map that to the map coordinates and center on the placemark
                    Matrix t = new Matrix();
                    getMapView().getCurrentScreenToMapTransform().invert(t);
                    t.mapRect(r);
                    r.offset(target.getPosition()[0] - 100,target.getPosition()[1]);

                    // Scroll on over
                    getMapView().scrollToRect(r, true);
                }

            });
            return v;
        }
    }
}
