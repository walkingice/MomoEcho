package net.julianchu.momoecho.player

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import kotlinx.coroutines.launch
import net.julianchu.momoecho.Const
import net.julianchu.momoecho.Const.Companion.PREF_KEY_PERIOD
import net.julianchu.momoecho.ProgressController
import net.julianchu.momoecho.R
import net.julianchu.momoecho.StoreDispatcher
import net.julianchu.momoecho.db.room.RoomStore
import net.julianchu.momoecho.model.Clip
import net.julianchu.momoecho.openEditClipFragment
import net.julianchu.momoecho.player.ui.ClipAdapter
import net.julianchu.momoecho.utils.showConfirmDialog
import net.julianchu.momoecho.utils.showSingleInputDialog
import net.julianchu.momoecho.utils.sortClips
import net.julianchu.momoecho.utils.toReadable
import net.julianchu.momoecho.viewmodel.MainViewModel

fun createPlayerFragment(): PlayerFragment {
    return PlayerFragment()
}

private const val TAG = "PlayerFragment"

class PlayerFragment : Fragment() {
    private val data = mutableListOf<Clip>()

    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: ClipAdapter
    private lateinit var mainList: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressCtrl: ProgressController
    private lateinit var btnPlay: ImageButton
    private lateinit var btnStop: ImageButton
    private lateinit var title: TextView
    private lateinit var filePath: TextView
    private lateinit var lengthView: TextView
    private var period = 1000L
    private val ctrlCallback = CtrlCallback()

    private val store: StoreDispatcher by lazy {
        Room.databaseBuilder(
            requireContext().applicationContext,
            RoomStore::class.java,
            RoomStore.DB_NAME
        )
            .build()
            .let { StoreDispatcher(it) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.let {
            viewModel = ViewModelProvider(it).get(MainViewModel::class.java)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_player, container, false)

        // TODO: remove action bar
        val toolbar = rootView.findViewById<Toolbar>(R.id.toolbar)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)

        setHasOptionsMenu(true)
        initWidgets(rootView)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadPreferences()
    }

    override fun onStart() {
        super.onStart()
        viewModel.mediaCtrl.registerCallback(ctrlCallback)
        viewModel.editClip = null
        viewModel.mediaCtrl.sendCommand(Const.COMMAND_UPDATE_CLIP, null, null)
        loadClips()
    }

    override fun onStop() {
        super.onStop()
        viewModel.mediaCtrl.unregisterCallback(ctrlCallback)
        saveClips()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_period -> showPeriodDialog()
            else -> return false
        }
        return true
    }

    private fun initWidgets(rootView: View) {
        filePath = rootView.findViewById(R.id.filename)
        lengthView = rootView.findViewById(R.id.length)
        mainList = rootView.findViewById(android.R.id.list)
        title = rootView.findViewById(R.id.track_title)
        progressBar = rootView.findViewById(android.R.id.progress)
        progressCtrl = ProgressController(progressBar = progressBar)

        title.text = viewModel.currentTrack.value?.displayTitle
        filePath.text = viewModel.currentTrack.value?.filename
        progressBar.max = viewModel.duration
        lengthView.text = viewModel.duration.toReadable()

        adapter = ClipAdapter(requireActivity(), data, {
            (it.tag as? Clip)?.let { clip ->
                onClipToggled(it, clip)
            }
        }, {
            (it.tag as? Clip)?.let { clip ->
                onClipClicked(it, clip)
            }
        }, {
            (it.tag as? Clip)?.let { clip ->
                onClipLongClicked(it, clip)
            }
        })

        registerForContextMenu(mainList)
        mainList.adapter = adapter
        mainList.layoutManager = LinearLayoutManager(activity)

        btnPlay = rootView.findViewById(android.R.id.button1)
        btnPlay.tag = false
        btnPlay.setOnClickListener {
            val isPlaying = it.tag as? Boolean ?: false
            if (isPlaying) {
                viewModel.mediaCtrl.transportControls?.pause()
            } else {
                viewModel.mediaCtrl.transportControls?.play()
            }
        }

        btnStop = rootView.findViewById(android.R.id.button2)
        btnStop.setOnClickListener {
            viewModel.mediaCtrl.transportControls?.stop()
        }

        rootView.findViewById<View>(R.id.button_add).setOnClickListener {
            viewModel.currentTrack.value?.let {
                // use last clip's end-time as start-time for next clip
                val startTime = if (data.isEmpty()) 0 else data.last().endTime
                viewModel.editClip = Clip(
                    startTime = startTime,
                    endTime = viewModel.duration,
                    isEnabled = true,
                    trackId = it.id
                )
                openEditClipFragment(requireActivity().supportFragmentManager)
            }
        }
    }

    private fun loadPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        period = prefs.getLong(PREF_KEY_PERIOD, 1000L)
    }

    /**
     * Save clips to database
     */
    private fun saveClips() {
        val track = viewModel.currentTrack.value ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            store.removeClipsOfTrack(track.id)
            for (c in data) {
                store.upsertClip(c)
            }
        }
    }

    /**
     * load clips from database
     */
    private fun loadClips() {
        val track = viewModel.currentTrack.value ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val clips = store.queryClips(track.id)
            data.clear()
            data.addAll(clips)
            sortClips(data)
            adapter.notifyDataSetChanged()
        }
    }

    private fun onClipToggled(view: View, clip: Clip) {
        clip.isEnabled = !clip.isEnabled
        viewLifecycleOwner.lifecycleScope.launch {
            store.upsertClip(clip)
            val extras = Bundle().also {
                it.putLong(Const.EXTRA_KEY_CLIP, clip.id)
            }
            viewModel.mediaCtrl.sendCommand(
                Const.COMMAND_UPDATE_CLIP, extras, null
            )
        }
    }

    private fun onClipClicked(view: View, clip: Clip) {
        // on clip clicked, do nothing yet
    }

    private fun onClipLongClicked(view: View, clip: Clip) {
        fun confirmDelete() {
            showConfirmDialog(
                requireContext(),
                "Delete this clip?"
            ) {
                viewLifecycleOwner.lifecycleScope.launch {
                    store.removeClip(clip.id)
                    val extras = Bundle().also { it.putLong(Const.EXTRA_KEY_CLIP, clip.id) }
                    viewModel.mediaCtrl.sendCommand(
                        Const.COMMAND_REMOVE_CLIP, extras, null
                    )
                }
            }
        }

        val popupMenu = PopupMenu(view.context, view, Gravity.CLIP_HORIZONTAL)
        popupMenu.inflate(R.menu.clip_item_menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_edit -> {
                    viewModel.editClip = clip
                    openEditClipFragment(requireActivity().supportFragmentManager)
                }
                R.id.menu_delete -> confirmDelete()
            }

            true
        }
        popupMenu.show()
    }

    private fun showPeriodDialog() {
        val periodView = showSingleInputDialog(requireActivity(), R.layout.period_dialog) {
            it?.let { str ->
                period = str.toLong()
                val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
                val editor = prefs.edit()
                editor.putLong(PREF_KEY_PERIOD, period)
                editor.apply()
            }
        }
        periodView.setText(period.toString())
    }

    inner class CtrlCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(psc: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(psc)
            psc ?: return
            val extras = psc.extras
            val playingClipId = extras?.getLong(Const.EXTRA_KEY_CLIP) ?: -1

            when (psc.state) {
                PlaybackStateCompat.STATE_PLAYING -> {
                    btnPlay.isEnabled = true
                    btnStop.isEnabled = true
                    btnPlay.setImageResource(R.drawable.ic_pause)
                    btnPlay.tag = true
                    progressCtrl.startAt(psc.position.toInt())

                    // update isPlaying for each clips
                    data.forEachIndexed { idx, it ->
                        if (it.id == playingClipId) {
                            it.isPlaying = true
                            adapter.notifyItemChanged(idx)
                        } else {
                            if (it.isPlaying) {
                                it.isPlaying = false
                                adapter.notifyItemChanged(idx)
                            }
                        }
                    }
                }
                PlaybackStateCompat.STATE_PAUSED -> {
                    btnPlay.isEnabled = true
                    btnStop.isEnabled = true
                    btnPlay.setImageResource(R.drawable.ic_play)
                    btnPlay.tag = false
                    progressCtrl.pauseAt(psc.position.toInt())
                }
                PlaybackStateCompat.STATE_STOPPED -> {
                    btnPlay.isEnabled = true
                    btnStop.isEnabled = false
                    btnPlay.tag = false
                    btnPlay.setImageResource(R.drawable.ic_play)
                    progressCtrl.stop()
                    data.forEach {
                        it.isPlaying = false
                    }
                    adapter.notifyDataSetChanged()
                }
                PlaybackStateCompat.STATE_SKIPPING_TO_NEXT -> {
                    btnPlay.isEnabled = false
                    btnStop.isEnabled = true
                    progressCtrl.pauseAt(psc.position.toInt())
                }
                PlaybackStateCompat.STATE_NONE -> {
                    btnPlay.isEnabled = false
                    btnStop.isEnabled = false
                    progressCtrl.stop()
                }
            }
        }

        override fun onSessionEvent(event: String, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            if (extras == null) {
                return
            }
            when (event) {
                Const.EVENT_UPDATE_INFO -> updateInfo(extras)
                Const.EVENT_UPDATE_CLIP -> updateClip(extras)
                Const.EVENT_UPDATE_CLIPS -> updateClips(extras)
            }
        }

        private fun updateInfo(extras: Bundle) {
            viewModel.duration = extras.getInt(Const.EXTRA_KEY_INFO_DURATION, 0)
            title.text = viewModel.currentTrack.value?.displayTitle
            filePath.text = viewModel.currentTrack.value?.filename
            progressBar.max = viewModel.duration
            lengthView.text = viewModel.duration.toReadable()
            updateClips(extras)
        }

        // a clip might be added, removed or edited
        private fun updateClip(extras: Bundle) {
            val id = extras.getLong(Const.EXTRA_KEY_CLIP)
            viewLifecycleOwner.lifecycleScope.launch {
                val clip = store.getClip(id)
                if (clip == null) {
                    // remove clip
                    var targetIdx = -1
                    data.forEachIndexed { idx, it ->
                        if (it.id == id) {
                            targetIdx = idx
                        }
                    }
                    if (targetIdx != -1) {
                        data.removeAt(targetIdx)
                        adapter.notifyItemRemoved(targetIdx)
                    }
                } else {
                    val match = data.filter { it.id == id }
                    if (match.isEmpty()) {
                        val first = data.indexOfFirst { it.startTime >= clip.startTime }
                        val index = if (first == -1) data.size else first
                        data.add(index, clip)
                        adapter.notifyItemInserted(index)
                    } else {
                        match.forEach {
                            it.isEnabled = clip.isEnabled
                            it.isPlaying = clip.isPlaying
                        }
                    }
                }

            }
        }

        private fun updateClips(extras: Bundle) {
            viewLifecycleOwner.lifecycleScope.launch {
                val clips = store.queryClips(extras.getLong(Const.EXTRA_KEY_TRACK))
                data.clear()
                data.addAll(clips)
                sortClips(data)
                adapter.notifyDataSetChanged()
            }
        }
    }
}
