package net.julianchu.momoecho.player

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
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
import net.julianchu.momoecho.player.ui.TrackAdapter
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)
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

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
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
            store.getTracks { tracks ->
                store.getClips { clips ->
                    FileController().saveToCsv(tracks, clips) { result, file ->
                        Toast.makeText(
                            requireContext(),
                            "$result: Save to ${file.path}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun importFromCsv() {
        GlobalScope.launch(context = Dispatchers.IO) {
            // FIXME: should call refresh tracks after IO
            FileController().readFromCsv { _, tracks, clips ->
                for (track in tracks) {
                    store.addTrack(track)
                }
                for (clip in clips) {
                    store.upsertClip(clip)
                }
            }
        }
    }

    private fun initWidgets(root: View) {
        mainList = root.findViewById(android.R.id.list)
        fab = root.findViewById(R.id.fab)
        adapter = TrackAdapter(requireActivity(), tracks, {
            (it.tag as? Track)?.let { clip ->
                onTrackClicked(it, clip)
            }
        }, {
            (it.tag as? Track)?.let { clip ->
                onTrackLongClicked(it, clip)
            }
        })

        fab.setOnClickListener { chooseFile() }

        mainList.adapter = adapter
        mainList.layoutManager = LinearLayoutManager(requireActivity())
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
        resolveFilename(uri) { name ->
            val track = Track(uri = uri, name = name)
            asyncAddTrack(track) {
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
        activity?.supportFragmentManager?.popBackStack()
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
        store.getClips { clips ->
            for (clip in clips) {
                map[clip.trackId] = (map[clip.trackId] ?: 0) + 1
            }
        }
        store.getTracks { storedTracks ->
            for (track in storedTracks) {
                track.clipsNumber = map[track.id] ?: 0
            }
            this.tracks.clear()
            this.tracks.addAll(storedTracks)
            adapter.notifyDataSetChanged()
        }
    }

    private fun asyncAddTrack(track: Track, callback: () -> Unit) {
        GlobalScope.launch {
            store.addTrack(track) {
                callback()
            }
        }
    }

    private fun asyncRemoveTrack(track: Track, callback: () -> Unit) {
        GlobalScope.launch {
            store.removeTrack(track.id)
            callback()
        }
    }

    private fun resolveFilename(uri: Uri, callback: (String) -> Unit) {
        GlobalScope.launch {
            contentResolver.query(uri, null, null, null, null)
                ?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    val name = cursor.getString(nameIndex)
                    launch(context = Dispatchers.Main) {
                        callback(name)
                    }
                }
        }
    }
}