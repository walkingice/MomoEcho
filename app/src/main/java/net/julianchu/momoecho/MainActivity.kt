package net.julianchu.momoecho

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.julianchu.momoecho.model.Track
import net.julianchu.momoecho.player.PlaybackService
import net.julianchu.momoecho.viewmodel.MainViewModel

private const val REQ_EXTERNAL_STORAGE_PERMISSION = 0xAA02

class MainActivity : AppCompatActivity(), LifecycleOwner {

    private lateinit var mediaBrowser: MediaBrowserCompat
    private val lifecycleRegistry = LifecycleRegistry(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ensurePermission()) {
            if (savedInstanceState == null) {
                onPermissionGranted()
            }
        } else {
            // show something while granting permission
            setContentView(R.layout.activity_main)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaBrowser.isConnected) {
            mediaBrowser.disconnect()
        }
    }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQ_EXTERNAL_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onPermissionGranted()
                } else {
                    // FIXME: avoid using globe scope
                    GlobalScope.launch {
                        delay(1000)
                        ensurePermission()
                    }
                }
            }
        }
    }

    private fun ensurePermission(): Boolean {
        val permissionCheck = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        return if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            requestPermissions(
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQ_EXTERNAL_STORAGE_PERMISSION
            )
            false
        }
    }

    private fun onPermissionGranted() {
        val viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        val callback = ConnectionCallback(viewModel)
        val comp = ComponentName(this, PlaybackService::class.java)
        mediaBrowser = MediaBrowserCompat(this, comp, callback, null)
        mediaBrowser.connect()
    }

    inner class ConnectionCallback(
        private val viewModel: MainViewModel
    ) : MediaBrowserCompat.ConnectionCallback() {

        private val observer = Observer<Track> { track ->
            val bundle = Bundle()
            bundle.putLong(Const.EXTRA_KEY_TRACK, track.id)
            viewModel.mediaCtrl.transportControls?.prepareFromUri(track.uri, bundle)
        }

        override fun onConnected() {
            super.onConnected()
            viewModel.mediaCtrl = MediaControllerCompat(
                this@MainActivity,
                mediaBrowser.sessionToken
            )
            findViewById<ViewGroup>(android.R.id.content).removeAllViews()
            initPlayerFragment(supportFragmentManager)
            viewModel.currentTrack.observe(this@MainActivity, observer)
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            viewModel.currentTrack.removeObserver(observer)
        }
    }
}
