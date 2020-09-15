package net.julianchu.momoecho.player

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.julianchu.momoecho.Const
import net.julianchu.momoecho.Const.Companion.PREF_KEY_TRACK_ID
import net.julianchu.momoecho.R
import net.julianchu.momoecho.StoreDispatcher
import net.julianchu.momoecho.db.room.RoomStore
import net.julianchu.momoecho.file.FileController
import net.julianchu.momoecho.model.Track
import net.julianchu.momoecho.openPlayerFragment
import net.julianchu.momoecho.player.ui.TrackAdapter
import net.julianchu.momoecho.utils.TrackUtil
import net.julianchu.momoecho.utils.calculateMd5
import net.julianchu.momoecho.utils.showConfirmDialog
import net.julianchu.momoecho.viewmodel.MainViewModel

private const val REQ_OPEN_AUDIO = 0xAA01

fun createBrowserFragment(): BrowserFragment {
    return BrowserFragment()
}

class BrowserFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: TrackAdapter
    private lateinit var mainList: RecyclerView
    private lateinit var fab: ImageButton
    private lateinit var bottomBar: View
    private lateinit var bottomBarShadow: View
    private val tracks = mutableListOf<Track>()

    private val store: StoreDispatcher by lazy {
        Room.databaseBuilder(
            requireContext().applicationContext,
            RoomStore::class.java,
            RoomStore.DB_NAME
        )
            .build()
            .let { StoreDispatcher(it) }
    }

    private val contentResolver: ContentResolver by lazy {
        requireContext().applicationContext.contentResolver
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
        val rootView = inflater.inflate(R.layout.fragment_browser, container, false)

        activity?.let {
            val toolbar = rootView.findViewById<Toolbar>(R.id.toolbar)
            toolbar.setTitle(R.string.frg_browser_title)
            (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        }

        initWidgets(rootView)

        setHasOptionsMenu(true)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadTrackFromStore()
    }

    override fun onResume() {
        super.onResume()
        refreshTracks()
    }

    override fun onActivityResult(reqCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(reqCode, resultCode, data)
        if (reqCode == REQ_OPEN_AUDIO && resultCode == Activity.RESULT_OK) {
            val uri: Uri = data?.data ?: return
            onFileChoose(uri)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater?.inflate(R.menu.browser_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_export -> exportToCsv()
            R.id.menu_import -> importFromCsv()
            else -> return false
        }
        return true
    }

    private fun exportToCsv() {
        GlobalScope.launch(context = Dispatchers.IO) {
            val tracks = store.getTracks()
            val clips = store.getClips()
            FileController().saveToCsv(tracks, clips) { result, file ->
                Toast.makeText(
                    requireContext(),
                    "$result: Save to ${file.path}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun importFromCsv() {
        GlobalScope.launch(context = Dispatchers.IO) {
            // FIXME: should call refresh tracks after IO
            val triple = FileController().readFromCsv()
            val tracks = triple.second
            val clips = triple.third
            for (track in tracks) {
                store.addTrack(track)
            }
            for (clip in clips) {
                store.upsertClip(clip)
            }
        }
    }

    private fun initWidgets(root: View) {
        mainList = root.findViewById(android.R.id.list)
        fab = root.findViewById(R.id.fab)
        adapter = TrackAdapter(requireActivity(), viewLifecycleOwner.lifecycleScope, tracks, {
            (it.tag as? Track)?.let { clip ->
                onTrackClicked(it, clip)
            }
        }, {
            (it.tag as? Track)?.let { clip ->
                onTrackLongClicked(it, clip)
            }
        })

        fab.setOnClickListener { chooseFile() }

        bottomBar = root.findViewById(R.id.bottom_bar)
        bottomBarShadow = root.findViewById(R.id.bottom_bar_shadow)
        viewModel.currentTrack.observe(viewLifecycleOwner, Observer<Track> {
            if (it == null) hideBottomBar() else showBottomBar(it)
        })
        loadTrackFromStore()

        mainList.adapter = adapter
        mainList.layoutManager = LinearLayoutManager(requireActivity())
    }

    private fun loadTrackFromStore() {
        // init track from previous stored preferences
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val trackId = prefs.getLong(PREF_KEY_TRACK_ID, -1)
        viewLifecycleOwner.lifecycleScope.launch {
            val ts = store.getTracks()
            val tracks = ts.filter { it.id == trackId }
            if (tracks.isNotEmpty() && tracks[0] != viewModel.currentTrack.value) {
                viewModel.currentTrack.postValue(tracks[0])
            }
        }
    }

    private fun showBottomBar(track: Track) {
        bottomBar.isVisible = true
        bottomBarShadow.isVisible = true
        bottomBar.setOnClickListener {
            onTrackClicked(it, track)
        }
        bottomBar.findViewById<TextView>(R.id.bottom_bar_album).text =
            "${track.album}"
        bottomBar.findViewById<TextView>(R.id.bottom_bar_title).text =
            "${track.displayTitle}"
        bottomBar.findViewById<TextView>(R.id.bottom_bar_filename).text =
            "${track.filename}"
    }

    private fun hideBottomBar() {
        bottomBar.isVisible = false
        bottomBarShadow.isVisible = false
    }

    private fun chooseFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "audio/*"
        startActivityForResult(intent, REQ_OPEN_AUDIO)
    }

    private fun onFileChoose(uri: Uri) {
        if (tracks.any { it.uri == uri }) {
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val md5 = calculateMd5(requireContext(), uri)
            val fileName = TrackUtil.resolveFilename(requireContext(), uri)
            if (fileName != null && md5 != null) {
                val metadata = TrackUtil.retrieveMetaData(requireContext(), uri, md5)
                val track = Track(
                    uri = uri,
                    filename = fileName,
                    title = metadata.title ?: fileName,
                    duration = metadata.duration,
                    album = metadata.album ?: "",
                    author = metadata.author ?: "",
                    thumbnailFilePath = metadata.thumbnailFilePath,
                    md5 = md5
                )
                store.addTrack(track)
                refreshTracks()
            }
        }
    }

    private fun onTrackClicked(view: View, track: Track) {
        viewModel.currentTrack.postValue(track)
        val extras = Bundle().also { it.putLong(Const.EXTRA_KEY_TRACK, track.id) }
        viewModel.mediaCtrl.sendCommand(
            Const.COMMAND_SET_TRACK, extras, null
        )

        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val editor = prefs.edit()
        editor.putLong(PREF_KEY_TRACK_ID, track.id)
        editor.commit()
        activity?.supportFragmentManager?.let { openPlayerFragment(it) }
    }

    private fun onTrackLongClicked(view: View, track: Track) {
        fun confirmDelete() {
            showConfirmDialog(
                requireActivity(),
                "Delete this track?"
            ) {
                asyncRemoveTrack(track) {
                    refreshTracks()
                }
            }
        }

        val popupMenu = PopupMenu(view.context, view, Gravity.CLIP_HORIZONTAL)
        popupMenu.inflate(R.menu.track_item_menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_delete -> confirmDelete()
            }
            true
        }
        popupMenu.show()
    }

    private fun refreshTracks() {
        // count clips number of each track
        val map = mutableMapOf<Long, Int>()
        viewLifecycleOwner.lifecycleScope.launch {
            val clips = store.getClips()
            for (clip in clips) {
                map[clip.trackId] = (map[clip.trackId] ?: 0) + 1
            }
            val storedTracks = store.getTracks()
            for (track in storedTracks) {
                track.clipsNumber = map[track.id] ?: 0
            }
            tracks.clear()
            tracks.addAll(storedTracks)
            adapter.notifyDataSetChanged()
        }
    }

    private fun asyncRemoveTrack(track: Track, callback: () -> Unit) {
        GlobalScope.launch {
            store.removeTrack(track.id)
            callback()
        }
    }
}
