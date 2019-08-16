package com.arubanetworks.meridiansamples

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import android.util.Log
import android.view.*
import android.widget.*
import com.arubanetworks.meridian.Meridian
import com.arubanetworks.meridian.editor.EditorKey
import com.arubanetworks.meridian.internal.util.Strings
import com.arubanetworks.meridian.maps.directions.DirectionsDestination
import com.arubanetworks.meridian.search.Search
import com.arubanetworks.meridian.search.SearchResponse
import com.arubanetworks.meridian.search.SearchResult
import java.util.*

/**
 * Demonstrates the use of the search API.
 */

class SearchFragment : androidx.fragment.app.ListFragment(), SearchView.OnQueryTextListener, Search.SearchListener {
    private var appKey: EditorKey? = null
    private val results = ArrayList<SearchResult>()
    private var searchQuery: Search? = null
    private var searchView: SearchView? = null
    private var spinner: ProgressBar? = null
    private var adapter: ArrayAdapter<SearchResult>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments == null || activity == null) {
            return
        }
        appKey = arguments!!.getSerializable(APP_KEY) as EditorKey

        if (activity != null) {
            adapter = object : ArrayAdapter<SearchResult>(activity!!, android.R.layout.simple_list_item_2, android.R.id.text1, results) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val text1 = view.findViewById<TextView>(android.R.id.text1)
                    val text2 = view.findViewById<TextView>(android.R.id.text2)
                    val result = results[position]

                    if (result.type == SearchResult.ResultType.PLACEMARK) {
                        val placemark = result.placemark
                        if (!Strings.isNullOrEmpty(placemark.name))
                            text1.text = placemark.name
                        else
                            text1.text = placemark.typeName
                    } else if (result.type == SearchResult.ResultType.TAG) {
                        val tag = result.tag
                        text1.text = tag.name
                    } else if (result.type == SearchResult.ResultType.MAP) {
                        val info = result.map
                        text1.text = info.name
                    }
                    text2.text = result.type.toString()
                    return view
                }
            }
            listAdapter = adapter
            setHasOptionsMenu(true)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.fragment_nearby, container, false)
        spinner = layout.findViewById(R.id.progress_bar)
        return layout
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.nearby, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        searchItem?.icon?.setTintList(ColorStateList.valueOf(Color.BLACK))
        searchView = searchItem?.actionView as SearchView
        searchView?.queryHint = getString(R.string.search_query_hint)
        if (!Strings.isNullOrEmpty(Meridian.getShared().editorToken)) {
            searchView?.setOnQueryTextListener(this)
        }
    }

    override fun onResume() {
        super.onResume()
        if (Strings.isNullOrEmpty(Meridian.getShared().editorToken)) {
            spinner?.visibility = View.INVISIBLE
            if (activity != null) {
                AlertDialog.Builder(activity!!)
                        .setMessage("You need to provide a valid editor token")
                        .setPositiveButton("OK", null)
                        .show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        searchView?.setOnQueryTextListener(null)
    }

    override fun onQueryTextChange(currentSearchTerm: String): Boolean {
        // kick off a new search query
        updateSearchResults(currentSearchTerm)
        return true
    }

    override fun onQueryTextSubmit(s: String): Boolean {
        return false
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long) {
        val result = results[position]
        var destination: DirectionsDestination? = null
        if (result.type == SearchResult.ResultType.PLACEMARK) {
            destination = DirectionsDestination.forPlacemarkKey(result.placemark.key)
        } else if (result.type == SearchResult.ResultType.TAG) {
            destination = DirectionsDestination.forTag(result.tag)
        }
        startActivity(DirectionsActivity.createIntent(activity!!, appKey, Application.MAP_KEY, destination))
    }

    private fun updateSearchResults(searchTerm: String) {
        var term = searchTerm

        searchQuery?.cancel()

        spinner?.visibility = View.VISIBLE
        // only search for placemarks
        term = if (Strings.isNullOrEmpty(term))
            "kind=placemark"
        else
            "$term AND kind=placemark"
        searchQuery = Search.Builder()
                .setQuery(term)
                .setLimit(10)
                .setApp(appKey)
                .setListener(this)
                .build()
        adapter?.let {
            it.clear()
            it.notifyDataSetChanged()
        }
        searchQuery?.start()
    }

    override fun onSearchResult(response: SearchResponse) {
        adapter?.let {
            it.addAll(response.results)
            it.notifyDataSetChanged()
        }
    }

    override fun onSearchComplete() {
        spinner?.visibility = View.INVISIBLE
    }

    override fun onSearchError(tr: Throwable) {
        spinner?.visibility = View.INVISIBLE
        Toast.makeText(this.context, "Search Request Failed", Toast.LENGTH_SHORT).show()
        Log.e(TAG, "search error: $tr")
    }

    companion object {

        private const val APP_KEY = "meridian.AppKey"

        private const val TAG = "SearchFragment"

        /**
         * Constructs a SearchFragment for the given Meridian AppKey.
         */
        fun newInstance(appKey: EditorKey): SearchFragment {
            val nf = SearchFragment()
            var b = nf.arguments
            if (b == null) b = Bundle()
            b.putSerializable(APP_KEY, appKey)
            nf.arguments = b
            return nf
        }
    }
}
