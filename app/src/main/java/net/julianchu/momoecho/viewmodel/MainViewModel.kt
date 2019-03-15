package net.julianchu.momoecho.viewmodel

import android.support.v4.media.session.MediaControllerCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.julianchu.momoecho.model.Clip
import net.julianchu.momoecho.model.Track

class MainViewModel : ViewModel() {
    val currentTrack = MutableLiveData<Track>()

    lateinit var mediaCtrl: MediaControllerCompat

    var editClip: Clip? = null
    var duration = 0
}
