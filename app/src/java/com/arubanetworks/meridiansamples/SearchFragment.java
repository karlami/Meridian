package com.arubanetworks.meridiansamples;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.arubanetworks.meridian.Meridian;
import com.arubanetworks.meridian.editor.EditorKey;
import com.arubanetworks.meridian.editor.Placemark;
import com.arubanetworks.meridian.internal.util.Strings;
import com.arubanetworks.meridian.maps.MapInfo;
import com.arubanetworks.meridian.maps.directions.DirectionsDestination;
import com.arubanetworks.meridian.search.Search;
import com.arubanetworks.meridian.search.SearchResponse;
import com.arubanetworks.meridian.search.SearchResult;
import com.arubanetworks.meridian.tags.TagBeacon;

import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.ListFragment;

/**
 * Demonstrates the use of the search API.
 */

public class SearchFragment extends ListFragment
        implements SearchView.OnQueryTextListener, Search.SearchListener {

    private static final String APP_KEY = "meridian.AppKey";

    private static final String TAG = "SearchFragment";
    private EditorKey appKey;
    private List<SearchResult> results = new ArrayList<>();
    private Search searchQuery;
    private SearchView searchView;
    private ProgressBar spinner;
    private ArrayAdapter<SearchResult> adapter;

    /**
     * Constructs a SearchFragment for the given Meridian AppKey.
     */
    public static SearchFragment newInstance(@NonNull EditorKey appKey) {
        SearchFragment nf = new SearchFragment();
        Bundle b = nf.getArguments();
        if (b == null) b = new Bundle();
        b.putSerializable(APP_KEY, appKey);
        nf.setArguments(b);
        return nf;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() == null || getActivity() == null) {
            return;
        }
        appKey = (EditorKey) getArguments().getSerializable(APP_KEY);

        adapter = new ArrayAdapter<SearchResult>(getActivity(), android.R.layout.simple_list_item_2, android.R.id.text1, results) {
            @Override
            @NonNull
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                SearchResult result = results.get(position);

                if (result.getType() == SearchResult.ResultType.PLACEMARK) {
                    Placemark placemark = result.getPlacemark();
                    if (!Strings.isNullOrEmpty(placemark.getName()))
                        text1.setText(placemark.getName());
                    else
                        text1.setText(placemark.getTypeName());
                } else if (result.getType() == SearchResult.ResultType.TAG){
                    TagBeacon tag = result.getTag();
                    text1.setText(tag.getName());
                } else if (result.getType() == SearchResult.ResultType.BEACONTAG){
                    text1.setText(result.getTagLabel());
                } else if (result.getType() == SearchResult.ResultType.MAP){
                    MapInfo info = result.getMap();
                    text1.setText(info.getName());
                }
                text2.setText(result.getType().toString());
                return view;
            }
        };
        setListAdapter(adapter);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_nearby, container, false);
        spinner = layout.findViewById(R.id.progress_bar);
        return layout;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.nearby, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchItem.getIcon().setTintList(ColorStateList.valueOf(Color.BLACK));
        searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.search_query_hint));
        if (!Strings.isNullOrEmpty(Meridian.getShared().getEditorToken())) {
            searchView.setOnQueryTextListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Strings.isNullOrEmpty(Meridian.getShared().getEditorToken())) {
            spinner.setVisibility(View.INVISIBLE);
            if (getActivity() != null) {
                new AlertDialog.Builder(getActivity())
                        .setMessage("You need to provide a valid editor token")
                        .setPositiveButton("OK", null)
                        .show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (searchView != null) {
            searchView.setOnQueryTextListener(null);
        }
    }

    @Override
    public boolean onQueryTextChange(String currentSearchTerm) {
        // kick off a new search query
        updateSearchResults(currentSearchTerm);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        SearchResult result = results.get(position);
        DirectionsDestination destination = null;
        if (result.getType() == SearchResult.ResultType.PLACEMARK) {
            destination = DirectionsDestination.forPlacemarkKey(result.getPlacemark().getKey());
        } else if (result.getType() == SearchResult.ResultType.TAG) {
            destination = DirectionsDestination.forTag(result.getTag());
        }
        startActivity(DirectionsActivity.createIntent(getActivity(), appKey, Application.MAP_KEY, destination));
    }

    private void updateSearchResults(String term) {

        if (searchQuery != null)
            searchQuery.cancel();

        spinner.setVisibility(View.VISIBLE);
        // only search for placemarks
        if (Strings.isNullOrEmpty(term))
            term = "kind=placemark";
        else
            term = term + " AND kind=placemark";
        searchQuery = new Search.Builder()
                .setQuery(term)
                .setLimit(10)
                .setApp(appKey)
                .setListener(this)
                .build();
        adapter.clear();
        adapter.notifyDataSetChanged();
        searchQuery.start();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onSearchResult(SearchResponse response) {
        adapter.addAll(response.getResults());
        adapter.notifyDataSetChanged();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onSearchComplete() {
        spinner.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onSearchError(Throwable tr) {
        spinner.setVisibility(View.INVISIBLE);
        Toast.makeText(this.getContext(),"Search Request Failed",Toast.LENGTH_SHORT).show();
        Log.e(TAG, "search error: " + tr);
    }
}
