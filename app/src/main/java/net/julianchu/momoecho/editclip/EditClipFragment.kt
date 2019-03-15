package net.julianchu.momoecho.editclip

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.room.Room
import net.julianchu.momoecho.Const
import net.julianchu.momoecho.ProgressController
import net.julianchu.momoecho.R
import net.julianchu.momoecho.StoreDispatcher
import net.julianchu.momoecho.db.room.RoomStore
import net.julianchu.momoecho.model.Clip
import net.julianchu.momoecho.model.Track
import net.julianchu.momoecho.utils.setClip
import net.julianchu.momoecho.utils.showSingleInputDialog
import net.julianchu.momoecho.utils.toMillis
import net.julianchu.momoecho.utils.toReadable
import net.julianchu.momoecho.viewmodel.MainViewModel


fun createEditClipFragment(): EditClipFragment {
    return EditClipFragment()
}

class EditClipFragment : Fragment() {

    private var zoomRate = 1f
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
    private lateinit var seekBarStart: SeekBar
    private lateinit var seekBarEnd: SeekBar
    private lateinit var seekBarCurrent: SeekBar
    private lateinit var progressCtrl: ProgressController
    private lateinit var scrollView: ViewGroup

    private val ctrlCallback = CtrlCallback()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)
        currentTrack = viewModel.currentTrack.value!!
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
            isEnabled = false,
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
        seekBarStart = view.findViewById(R.id.edit_clip_seek_bar_start)
        seekBarEnd = view.findViewById(R.id.edit_clip_seek_bar_end)
        seekBarCurrent = view.findViewById(R.id.edit_clip_seek_bar_current)
        progressCtrl = ProgressController(seekBarCurrent)
        scrollView = view.findViewById(R.id.edit_clip_seek_bar_container)

        seekBarStart.max = viewModel.duration
        seekBarEnd.max = viewModel.duration
        seekBarCurrent.max = viewModel.duration

        seekBarStart.progress = clip.startTime
        seekBarEnd.progress = clip.endTime

        seekBarCurrent.isEnabled = false

        btnZoomIn.setOnClickListener {
            zoomRate += 0.5f
            zoomRate = Math.min(zoomRate, 5.0f)
            onZoom()
        }
        btnZoomOut.setOnClickListener {
            zoomRate -= 0.5f
            zoomRate = Math.max(zoomRate, 1.0f)
            onZoom()
        }

        startView.setOnClickListener {
            showTimeEditDialog(startView)
        }

        endView.setOnClickListener {
            showTimeEditDialog(endView)
        }

        seekBarStart.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val start = Math.min(progress, seekBarEnd.progress)
                    seekBar?.progress = start
                    startView.text = start.toReadable()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }
            }
        )

        seekBarEnd.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val end = Math.max(progress, seekBarStart.progress)
                    seekBar?.progress = end
                    endView.text = end.toReadable()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }
            }
        )

        seekBarStart.setOnTouchListener(TouchHack)
        seekBarEnd.setOnTouchListener(TouchHack)
        SeekBarsInitializer(scrollView, seekBarStart, seekBarEnd, seekBarCurrent)

        startView.text = clip.startTime.toReadable()
        endView.text = clip.endTime.toReadable()
        editContent.setText(clip.content)

        view.findViewById<Button>(R.id.button_dismiss).setOnClickListener {
            finishSelf(false)
        }

        view.findViewById<Button>(R.id.button_ok).setOnClickListener {
            clip.startTime = startView.text.toString().toMillis()
            clip.endTime = endView.text.toString().toMillis()
            clip.content = editContent.text.toString()
            finishSelf(true)
        }

        btnPlay.tag = false
        btnPlay.setOnClickListener {
            if (seekBarEnd.progress - seekBarStart.progress > 10) {
                val isPlaying = it.tag as? Boolean ?: false
                if (isPlaying) {
                    viewModel.mediaCtrl.transportControls?.pause()
                } else {
                    val now = Clip(
                        startTime = seekBarStart.progress,
                        endTime = seekBarEnd.progress,
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
            seekBarStart.progress = seekBarCurrent.progress
            startView.text = seekBarCurrent.progress.toReadable()
        }

        btnSetEnd.setOnClickListener {
            viewModel.mediaCtrl.transportControls?.pause()
            seekBarEnd.progress = seekBarCurrent.progress
            endView.text = seekBarCurrent.progress.toReadable()
        }
    }

    private fun onZoom() {
        val width = scrollView.measuredWidth - scrollView.paddingStart - scrollView.paddingRight
        seekBarStart.layoutParams.also {
            it.width = (width * zoomRate).toInt()
        }
        seekBarStart.requestLayout()
        seekBarEnd.layoutParams.also {
            it.width = (width * zoomRate).toInt()
        }
        seekBarEnd.requestLayout()
        seekBarCurrent.layoutParams.also {
            it.width = (width * zoomRate).toInt()
        }
        seekBarCurrent.requestLayout()
    }

    private fun showTimeEditDialog(tv: TextView) {
        val editText = showSingleInputDialog(
            requireContext(),
            R.layout.edit_dialog
        ) {
            it?.let {
                val converted = it.toMillis()
                if (converted == 0) {
                    // wrong type
                } else {
                    tv.text = converted.toReadable()
                    if (tv == startView) {
                        seekBarStart.progress = Math.min(converted, seekBarEnd.progress)
                    } else if (tv == endView) {
                        seekBarEnd.progress = Math.max(converted, seekBarStart.progress)
                    }
                }
            }
        }
        editText.setText(tv.text.toString().toMillis().toReadable())
    }

    private fun finishSelf(save: Boolean) {
        if (save) {
            val store = Room.databaseBuilder(
                requireContext().applicationContext,
                RoomStore::class.java,
                RoomStore.DB_NAME
            )
                .build()
                .let { StoreDispatcher(it) }

            store.upsertClip(clip) {
                val extras = Bundle().also { it.putLong(Const.EXTRA_KEY_CLIP, clip.id) }
                viewModel.mediaCtrl.sendCommand(
                    Const.COMMAND_UPDATE_CLIP, extras, null
                )
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

    private class SeekBarsInitializer(
        val container: View,
        val seekBarStart: SeekBar,
        val seekBarEnd: SeekBar,
        val seekBarCurrent: SeekBar
    ) : ViewTreeObserver.OnGlobalLayoutListener {
        init {
            container.viewTreeObserver.addOnGlobalLayoutListener(this)
        }

        override fun onGlobalLayout() {
            val width = container.measuredWidth
            val padding = container.paddingStart + container.paddingEnd
            seekBarStart.layoutParams.also {
                it.width = width - padding
            }
            seekBarStart.requestLayout()
            seekBarEnd.layoutParams.also {
                it.width = width - padding
            }
            seekBarEnd.requestLayout()

            seekBarCurrent.layoutParams.also {
                it.width = width - padding
            }
            seekBarCurrent.requestLayout()

            container.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    }

    inner class CtrlCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(ps: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(ps)
            if (ps != null) {
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
}
