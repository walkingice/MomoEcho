package net.julianchu.momoecho.editclip

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.julianchu.momoecho.Const
import net.julianchu.momoecho.ProgressController
import net.julianchu.momoecho.R
import net.julianchu.momoecho.StoreDispatcher
import net.julianchu.momoecho.db.room.RoomStore
import net.julianchu.momoecho.model.AmplitudeDiagram
import net.julianchu.momoecho.model.Clip
import net.julianchu.momoecho.model.Track
import net.julianchu.momoecho.utils.AudioUtil
import net.julianchu.momoecho.utils.calculateMd5
import net.julianchu.momoecho.utils.setClip
import net.julianchu.momoecho.utils.toMillis
import net.julianchu.momoecho.utils.toReadable
import net.julianchu.momoecho.viewmodel.MainViewModel
import net.julianchu.momoecho.widget.VerticalSeekBar
import net.julianchu.momoecho.widget.WaveformView
import java.io.File


fun createEditClipFragment(): EditClipFragment {
    return EditClipFragment()
}

private const val TAG = "EditClipFragment"

class EditClipFragment : Fragment() {

    private var resolution = 0.2f
    private lateinit var roomStore: StoreDispatcher
    private lateinit var viewModel: MainViewModel
    private lateinit var currentTrack: Track
    private lateinit var clip: Clip
    private lateinit var startView: TextView
    private lateinit var endView: TextView
    private lateinit var editContent: EditText
    private lateinit var btnPlay: ImageButton
    private lateinit var btnSetStart: View
    private lateinit var btnSetEnd: View
    private lateinit var btnZoomIn: View
    private lateinit var btnZoomOut: View
    private lateinit var seekBarLeft: VerticalSeekBar
    private lateinit var seekBarRight: VerticalSeekBar
    private lateinit var seekBarPlayback: VerticalSeekBar
    private lateinit var progressCtrl: ProgressController
    private lateinit var scrollView: ViewGroup
    private lateinit var waveformView: WaveformView
    private lateinit var loadingBg: View
    private lateinit var loadingSpinner: ProgressBar

    private val ctrlCallback = CtrlCallback()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomStore = Room.databaseBuilder(
            requireContext().applicationContext,
            RoomStore::class.java,
            RoomStore.DB_NAME
        ).build().let { StoreDispatcher(it) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.let {
            viewModel = ViewModelProvider(it).get(MainViewModel::class.java)
            currentTrack = viewModel.currentTrack.value!!
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.mediaCtrl.registerCallback(ctrlCallback)
    }

    override fun onPause() {
        super.onPause()
        viewModel.mediaCtrl.unregisterCallback(ctrlCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_clip, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        clip = viewModel.editClip ?: Clip(
            startTime = 0,
            endTime = viewModel.duration,
            isEnabled = true,
            trackId = currentTrack.id
        )

        startView = view.findViewById(R.id.edit_start)
        endView = view.findViewById(R.id.edit_end)
        editContent = view.findViewById(R.id.edit_content)
        btnPlay = view.findViewById(R.id.edit_clip_btn_play)
        btnSetStart = view.findViewById(R.id.edit_clip_btn_set_start)
        btnSetEnd = view.findViewById(R.id.edit_clip_btn_set_end)
        btnZoomIn = view.findViewById(R.id.edit_clip_btn_zoom_in)
        btnZoomOut = view.findViewById(R.id.edit_clip_btn_zoom_out)
        seekBarLeft = view.findViewById(R.id.edit_clip_seek_bar_left)
        seekBarRight = view.findViewById(R.id.edit_clip_seek_bar_right)
        seekBarPlayback = view.findViewById(R.id.edit_clip_seek_bar_playback)
        progressCtrl = ProgressController(seekBar = seekBarPlayback)
        scrollView = view.findViewById(R.id.edit_clip_seek_bar_container)
        loadingSpinner = view.findViewById(R.id.loading_spinner)
        loadingBg = view.findViewById(R.id.loading_container)
        waveformView = view.findViewById(R.id.waveform_view)
        loadAmplitudeForTrack(currentTrack)

        seekBarLeft.max = viewModel.duration
        seekBarRight.max = viewModel.duration
        seekBarPlayback.max = viewModel.duration

        seekBarLeft.progress = clip.startTime
        seekBarRight.progress = clip.endTime

        seekBarPlayback.isEnabled = false

        resolution = waveformView.getResolution()
        btnZoomIn.setOnClickListener {
            resolution += 0.1f
            resolution = Math.min(resolution, 1.0f)
            onZoom()
        }
        btnZoomOut.setOnClickListener {
            resolution -= 0.1f
            resolution = Math.max(resolution, 0.1f)
            onZoom()
        }

        seekBarLeft.setOnSeekBarChangeListener { seekBar: VerticalSeekBar, progress: Int, fromUser: Boolean ->
            updateStartAndEnd()
        }

        seekBarRight.setOnSeekBarChangeListener { seekBar: VerticalSeekBar, progress: Int, fromUser: Boolean ->
            updateStartAndEnd()
        }

        seekBarLeft.setOnTouchListener(TouchHack)
        seekBarRight.setOnTouchListener(TouchHack)

        startView.text = clip.startTime.toReadable()
        endView.text = clip.endTime.toReadable()
        editContent.setText(clip.content)

        view.findViewById<View>(R.id.button_dismiss).setOnClickListener {
            finishSelf(false)
        }

        view.findViewById<View>(R.id.button_ok).setOnClickListener {
            clip.startTime = startView.text.toString().toMillis()
            clip.endTime = endView.text.toString().toMillis()
            clip.content = editContent.text.toString()
            finishSelf(true)
        }

        btnPlay.tag = false
        btnPlay.setOnClickListener {
            if (Math.abs(seekBarRight.progress - seekBarLeft.progress) > 10) {
                val isPlaying = it.tag as? Boolean ?: false
                if (isPlaying) {
                    viewModel.mediaCtrl.transportControls?.pause()
                } else {
                    val now = Clip(
                        startTime = Math.min(seekBarLeft.progress, seekBarRight.progress),
                        endTime = Math.max(seekBarLeft.progress, seekBarRight.progress),
                        trackId = currentTrack.id
                    )
                    viewModel.mediaCtrl.sendCommand(
                        Const.COMMAND_PLAYBACK_ONE_CLIP,
                        Bundle().setClip(now),
                        null
                    )
                }
            }
        }

        btnSetStart.setOnClickListener {
            seekBarLeft.progress = seekBarPlayback.progress
            startView.text = seekBarPlayback.progress.toReadable()
        }

        btnSetEnd.setOnClickListener {
            viewModel.mediaCtrl.transportControls?.pause()
            seekBarRight.progress = seekBarPlayback.progress
            endView.text = seekBarPlayback.progress.toReadable()
        }

        view.findViewById<ImageView>(R.id.top_bar_toggle).setOnClickListener { toggleTopBar(view) }

        updateStartAndEnd()
    }

    private fun loadAmplitudeForTrack(track: Track) {
        viewLifecycleOwner.lifecycleScope.launch {
            showLoading()
            val md5 = getFileMd5(track.uri)
            loadAmplitudeDiagram(md5, track)?.let { waveformView.setDiagram(it) }
            hidLoading()
        }
    }

    private suspend fun loadAmplitudeDiagram(md5: String?, track: Track): AmplitudeDiagram? {
        md5 ?: return null
        val amplitude = loadAmplitudeDiagramFromDb(md5)
        return if (amplitude != null) {
            Log.d(TAG, "load from DB: $md5")
            amplitude
        } else {
            loadAmplitudeDiagramFromStorage(md5, track)
        }
    }

    private suspend fun loadAmplitudeDiagramFromDb(md5: String): AmplitudeDiagram? {
        return roomStore.getAmplitude(md5)
    }

    private suspend fun loadAmplitudeDiagramFromStorage(
        md5: String,
        track: Track
    ): AmplitudeDiagram? {
        val cache = prepareAmplitude(md5, track.uri)
        Log.d(TAG, "${cache?.size}")

        val mediaFormatData = AudioUtil.getMediaFormatData(requireContext(), track.uri)
        if (mediaFormatData != null && cache != null) {
            val amplitudeDiagram = AudioUtil.amplitudeToDiagram(md5, mediaFormatData, cache)
            Log.d(TAG, "save to DB: $md5")
            saveAmplitudeToDb(amplitudeDiagram)
            return amplitudeDiagram
        }
        return null
    }

    private suspend fun saveAmplitudeToDb(amplitude: AmplitudeDiagram) {
        roomStore.addAmplitude(amplitude)
    }

    private suspend fun prepareAmplitude(
        md5: String,
        uri: Uri
    ): ShortArray? = withContext(Dispatchers.IO) {
        val cacheFile = File(requireContext().cacheDir, "$md5.pcm")
        // val cacheFile = File("/sdcard/Music/my_$md5.pcm")
        val useCache = true
        if (useCache && cacheFile.exists() && cacheFile.length() > 0) {
            Log.d(TAG, "cache hit: $md5")
            val byteArray = cacheFile.readBytes()
            AudioUtil.pcmToAmplitude(byteArray)
        } else {
            Log.d(TAG, "no cache, decoding: $md5")
            val data =
                AudioUtil.decodeToAmplitude(requireContext(), uri, ::decodeProgress)?.also {
                    val pcm = AudioUtil.amplitudeToPcm(it)
                    cacheFile.writeBytes(pcm)
                }
            data
        }
    }

    private fun decodeProgress(decodeProgress: Float) {
        val progress = (decodeProgress * 100).toInt()
        if ((progress - loadingSpinner.progress) > 3) {
            viewLifecycleOwner.lifecycleScope.launch {
                updateLoadingProgress(progress)
            }
        }
    }

    // TODO: remove this
    private suspend fun updateLoadingProgress(progress: Int) = withContext(Dispatchers.Main) {
        loadingSpinner.progress = progress
    }

    private fun showLoading() {
        loadingBg.isVisible = true
        loadingBg.setOnClickListener {}
        loadingSpinner.isVisible = true
        loadingSpinner.progress = 0
        loadingSpinner.max = 100
    }

    private fun hidLoading() {
        loadingBg.isVisible = false
        loadingSpinner.isVisible = false
    }

    private suspend fun getFileMd5(uri: Uri): String? = withContext(Dispatchers.IO) {
        calculateMd5(requireContext(), uri)
    }

    private fun updateStartAndEnd() {
        val startSeekBar =
            if (seekBarLeft.progress < seekBarRight.progress) seekBarLeft else seekBarRight
        val endSeekBar = if (startSeekBar == seekBarLeft) seekBarRight else seekBarLeft

        val start = startSeekBar.progress
        val end = endSeekBar.progress
        seekBarPlayback.setHighLightRange(start, end)
        startView.text = start.toReadable()
        endView.text = end.toReadable()
    }

    private fun onZoom() {
        waveformView.setResolution(resolution)
        waveformView.requestLayout()
        seekBarLeft.requestLayout()
        seekBarRight.requestLayout()
        seekBarPlayback.requestLayout()
    }


    private fun toggleTopBar(rootView: View) {
        val toggle = rootView.findViewById<ImageView>(R.id.top_bar_toggle)
        val isOn = (toggle.rotation == 0f)
        toggle.rotation = if (isOn) 180f else 0f
        rootView.findViewById<View>(R.id.edit_title_start).isVisible = isOn
        rootView.findViewById<View>(R.id.edit_title_end).isVisible = isOn
        rootView.findViewById<View>(R.id.edit_title_content).isVisible = isOn
        rootView.findViewById<View>(R.id.edit_content).isVisible = isOn
    }

    private fun finishSelf(save: Boolean) {
        if (save) {
            viewLifecycleOwner.lifecycleScope.launch {
                roomStore.upsertClip(clip)
                val extras = Bundle().also { it.putLong(Const.EXTRA_KEY_CLIP, clip.id) }
                viewModel.mediaCtrl.sendCommand(Const.COMMAND_UPDATE_CLIP, extras, null)
                activity?.supportFragmentManager?.popBackStack()
            }
        } else {
            activity?.supportFragmentManager?.popBackStack()
        }
    }

    private object TouchHack : View.OnTouchListener {
        override fun onTouch(v: View, e: MotionEvent?): Boolean {
            val action = e?.action ?: return false
            when (action) {
                MotionEvent.ACTION_DOWN ->
                    // Disallow ScrollView to intercept touch events.
                    v.parent.requestDisallowInterceptTouchEvent(true)

                MotionEvent.ACTION_UP ->
                    // Allow ScrollView to intercept touch events.
                    v.parent.requestDisallowInterceptTouchEvent(false)
            }

            // Handle SeekBar touch events.
            v.onTouchEvent(e)
            return true
        }
    }

    inner class CtrlCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(ps: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(ps)
            ps ?: return
            when (ps.state) {
                PlaybackStateCompat.STATE_PLAYING -> {
                    btnPlay.setImageResource(R.drawable.ic_pause)
                    btnPlay.tag = true
                    progressCtrl.startAt(ps.position.toInt())
                }
                PlaybackStateCompat.STATE_PAUSED -> {
                    btnPlay.setImageResource(R.drawable.ic_play)
                    btnPlay.tag = false
                    progressCtrl.pauseAt(ps.position.toInt())
                }
            }
        }
    }
}
