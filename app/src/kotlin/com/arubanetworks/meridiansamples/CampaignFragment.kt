package com.arubanetworks.meridiansamples

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast

import com.arubanetworks.meridian.campaigns.CampaignsService
import com.arubanetworks.meridian.editor.EditorKey

class CampaignFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val arg: Bundle? = arguments
        val appKey: EditorKey? = arg?.getSerializable(APP_KEY) as EditorKey
        if (appKey == null) {
            Toast.makeText(activity, "No EditorKey passed to Fragment!", Toast.LENGTH_LONG).show()
            return FrameLayout(activity!!)
        }

        val view = inflater.inflate(R.layout.campaign_example, container, false)
        val startButton = view.findViewById<Button>(R.id.start_button)
        startButton?.setOnClickListener { CampaignsService.startMonitoring(activity!!, appKey) }
        val stopButton = view.findViewById<Button>(R.id.stop_button)
        stopButton?.setOnClickListener { CampaignsService.stopMonitoring(activity!!) }
        val resetButton = view.findViewById<Button>(R.id.reset_button)
        resetButton?.setOnClickListener { CampaignsService.resetAllCampaigns(activity!!, appKey, null, null) }
        val resetIndividualButton = view.findViewById<Button>(R.id.reset_single_button)
        resetIndividualButton?.setOnClickListener { CampaignsService.resetCampaign(activity!!, appKey, Application.CAMPAIGN_ID, null, null) }
        return view
    }

    companion object {

        private const val APP_KEY = "CampaignFragment.AppKey"

        fun newInstance(appKey: EditorKey): CampaignFragment {
            val fragment = CampaignFragment()
            if (appKey.parent != null) throw IllegalArgumentException("appKey must have null parent.")
            var b = fragment.arguments
            if (b == null) b = Bundle()
            b.putSerializable(APP_KEY, appKey)
            fragment.arguments = b
            return fragment
        }
    }
}
