package com.arubanetworks.meridiansamples

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import com.arubanetworks.meridian.Meridian
import com.arubanetworks.meridian.internal.util.Strings
import com.arubanetworks.meridian.locationsharing.*

class LocationSharingFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // we need to initialize Location Sharing first
        if (!Strings.isNullOrEmpty(Meridian.getShared().editorToken)) {
            LocationSharing.initWithAppKey(Application.APP_KEY)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.location_sharing_example, container, false)

        val loggedOutLayout = rootView.findViewById<LinearLayout>(R.id.location_sharing_logged_out)
        val loggedInLayout = rootView.findViewById<LinearLayout>(R.id.location_sharing_logged_in)

        val createProfileButton = rootView.findViewById<Button>(R.id.location_sharing_create_profile)
        createProfileButton.setOnClickListener {
            if (Strings.isNullOrEmpty(Meridian.getShared().editorToken)) {
                if (activity != null) {
                    AlertDialog.Builder(activity!!)
                            .setMessage("You need to provide a valid editor token")
                            .setPositiveButton("OK", null)
                            .show()
                }
            } else {
                val sampleUser = User()
                sampleUser.fullName = "Sample User"

                LocationSharing.shared().createUser(sampleUser, object : LocationSharing.Callback<User> {
                    override fun onSuccess(user: User) {

                        loggedOutLayout.visibility = View.GONE
                        loggedInLayout.visibility = View.VISIBLE
                        if (activity != null) {
                            AlertDialog.Builder(activity!!)
                                    .setMessage("User created successfully!")
                                    .setPositiveButton("OK", null)
                                    .show()
                        }
                    }

                    override fun onError(t: LocationSharingException) {
                        if (activity != null) {
                            AlertDialog.Builder(activity!!)
                                    .setMessage("Unable to create user")
                                    .setPositiveButton("OK", null)
                                    .show()
                        }
                    }
                })
            }
        }
        val acceptInviteButton = rootView.findViewById<Button>(R.id.location_sharing_accept_invite)
        acceptInviteButton.setOnClickListener {
            if (Strings.isNullOrEmpty(Meridian.getShared().editorToken)) {
                if (activity != null) {
                    AlertDialog.Builder(activity!!)
                        .setMessage("You need to provide a valid editor token")
                        .setPositiveButton("OK", null)
                        .show()
                }
            } else {
                if (activity != null) {
                    val inviteEditText = EditText(activity)
                    AlertDialog.Builder(activity!!)
                        .setMessage("Accept Invite")
                        .setView(inviteEditText)
                        .setPositiveButton("OK", object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                LocationSharing.shared().acceptInvite(inviteEditText.text.toString(), object : LocationSharing.Callback<Friend> {
                                    override fun onSuccess(friend: Friend) {
                                        AlertDialog.Builder(activity!!)
                                                .setMessage(friend.fullName + " added as a friend!")
                                                .setPositiveButton("OK", null)
                                                .show()
                                    }

                                    override fun onError(t: LocationSharingException?) {
                                        AlertDialog.Builder(activity!!)
                                                .setMessage("Unable to accept invite")
                                                .setPositiveButton("OK", null)
                                                .show()
                                    }
                                })
                            }
                        })
                        .show()
                }
            }
        }

        if (Strings.isNullOrEmpty(Meridian.getShared().editorToken)) {
            return rootView
        }
        val startPostingLocationUpdates = rootView.findViewById<Button>(R.id.location_sharing_start_updating_location)

        // optionally, we can set a listener so we know when the service is running
        LocationSharing.shared().addListener(object : LocationSharing.Listener {
            override fun onPostingLocationUpdatesStarted() {
                if (activity != null) {
                    startPostingLocationUpdates.text = getString(R.string.location_sharing_stop_updating_location)
                }
            }

            override fun onFriendsUpdated(friends: List<Friend>) {}

            override fun onPostingLocationUpdatesStopped() {
                if (activity != null) {
                    startPostingLocationUpdates.text = getString(R.string.location_sharing_start_updating_location)
                }
            }
        })

        startPostingLocationUpdates.setOnClickListener {
            if (activity != null) {
                // we are using the same button to start/stop posting, so we need to check if we are already posting or not
                // before starting/stopping it
                if (LocationSharing.shared().isUploadingServiceRunning) {
                    LocationSharing.shared().stopPostingLocationUpdates(activity!!.applicationContext)
                } else {
                    LocationSharing.shared().startPostingLocationUpdates(activity!!.applicationContext)
                }
            }
        }

        val createInviteButton = rootView.findViewById<Button>(R.id.location_sharing_create_invite)

        createInviteButton.setOnClickListener {
            LocationSharing.shared().createInvite(object : LocationSharing.Callback<Invite> {
                override fun onSuccess(result: Invite) {
                    if (activity != null) {
                        AlertDialog.Builder(activity!!)
                                .setMessage("Invited created. URL: " + result.shareUrl)
                                .setPositiveButton("OK", null)
                                .show()

                        // you can share the invite URL here
                    }
                }

                override fun onError(e: LocationSharingException) {
                    // do something
                }
            })
        }

        val retrieveFriendsButton = rootView.findViewById<Button>(R.id.location_sharing_retrieve_friends)

        retrieveFriendsButton.setOnClickListener {
            LocationSharing.shared().getFriends(object : LocationSharing.Callback<List<Friend>> {
                override fun onSuccess(result: List<Friend>) {
                    if (activity != null) {
                        AlertDialog.Builder(activity!!)
                                .setMessage("Success! You have " + result.size + " friends")
                                .setPositiveButton("OK", null)
                                .show()
                    }
                }

                override fun onError(e: LocationSharingException) {
                    // do something
                }
            })
        }

        val uploadUserImageButton = rootView.findViewById<Button>(R.id.location_sharing_upload_user_image)
        uploadUserImageButton.setOnClickListener {
            val i = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(i, IMAGE_REQUEST_FOR_USER_IMAGE)
        }

        if (LocationSharing.shared().currentUser != null) {
            loggedOutLayout.visibility = View.GONE
            loggedInLayout.visibility = View.VISIBLE
            if (LocationSharing.shared().isUploadingServiceRunning) {
                startPostingLocationUpdates.text = getString(R.string.location_sharing_stop_updating_location)
            }
        }
        return rootView
    }

    private fun uploadURIasUserImage(uri: Uri?) {
        if (uri == null || context == null || activity == null) return
        LocationSharing.shared().uploadUserPhoto(context!!, uri, object : LocationSharing.Callback<User> {
            override fun onSuccess(result: User) {
                AlertDialog.Builder(activity!!)
                        .setMessage("Success! image uploaded.")
                        .setPositiveButton("OK", null)
                        .show()
            }

            override fun onError(t: LocationSharingException) {
                if (activity != null) {
                    AlertDialog.Builder(activity!!)
                            .setMessage(String.format("Error uploading image. %s", t.errorMessage))
                            .setPositiveButton("OK", null)
                            .show()
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        when (requestCode) {
            IMAGE_REQUEST_FOR_USER_IMAGE -> uploadURIasUserImage(data?.data)
        }
    }

    companion object {

        private const val IMAGE_REQUEST_FOR_USER_IMAGE = 0x0000000F

        fun newInstance(): LocationSharingFragment {
            return LocationSharingFragment()
        }
    }
}
