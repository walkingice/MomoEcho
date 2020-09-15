package net.julianchu.momoecho.editclip

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.julianchu.momoecho.Const
import net.julianchu.momoecho.R
import net.julianchu.momoecho.StoreDispatcher
import net.julianchu.momoecho.db.room.RoomStore
import net.julianchu.momoecho.model.Track
import net.julianchu.momoecho.utils.AudioUtil
import net.julianchu.momoecho.utils.calculateMd5
import net.julianchu.momoecho.widget.WaveformView
import java.io.File

private const val TAG = "VisualEditorFragment"

class VisualEditorFragment : Fragment() {

    private lateinit var store: StoreDispatcher

    private lateinit var fileInfoTextView: TextView
    private lateinit var loading: View
    private lateinit var waveformView: WaveformView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        store = Room.databaseBuilder(
            context.applicationContext,
            RoomStore::class.java,
            RoomStore.DB_NAME
        )
            .build()
            .let { StoreDispatcher(it) }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_visual_editor, container, false)
        fileInfoTextView = rootView.findViewById(R.id.file_info)
        loading = rootView.findViewById(R.id.loading_spinner)
        waveformView = rootView.findViewById(R.id.waveform_view)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val trackId = prefs.getLong(Const.PREF_KEY_TRACK_ID, -1)
        viewLifecycleOwner.lifecycleScope.launch {
            val ts = store.getTracks()
            val tracks = ts.filter { it.id == trackId }
            val track = if (tracks.isNotEmpty()) tracks[0] else null
            track?.let {
                setTrackInfo(it)
                loadPcmData(track)
            }
        }
    }

    private fun loadPcmData(track: Track) {
        viewLifecycleOwner.lifecycleScope.launch {
            loading.isVisible = true
            val md5 = getFileMd5(track.uri)
            if (md5 != null) {
                val cache = prepareAmplitude(md5, track.uri)
                Log.d(TAG, "${cache?.size}")
                val mediaFormatData = AudioUtil.getMediaFormatData(requireContext(), track.uri)
                if (mediaFormatData != null) {
                    Log.d(TAG, "channels: ${mediaFormatData.channelCount}")
                }

                if (mediaFormatData != null && cache != null) {
                    val amplitude = AudioUtil.amplitudeToDiagram(md5, mediaFormatData, cache)
                    waveformView.setDiagram(amplitude)
                }
            }
            loading.isVisible = false
        }
    }

    // TODO: ensure read, write data is correct, I need write some utils and unit test
    // ensure each Functions are correct
    private suspend fun prepareAmplitude(
        md5: String,
        uri: Uri
    ): ShortArray? = withContext(Dispatchers.IO) {
        val cacheFile = File(requireContext().cacheDir, "$md5.pcm")
        // val cacheFile = File("/sdcard/Music/my_$md5.pcm")
        val useCache = true
        if (useCache && cacheFile.exists() && cacheFile.length() > 0) {
            val byteArray = cacheFile.readBytes()
            AudioUtil.pcmToAmplitude(byteArray)
        } else {
            val data = AudioUtil.decodeToAmplitude(requireContext(), uri)?.also {
                val pcm = AudioUtil.amplitudeToPcm(it)
                cacheFile.writeBytes(pcm)
            }
            data
        }
    }

    private suspend fun getFileMd5(uri: Uri): String? = withContext(Dispatchers.IO) {
        calculateMd5(requireContext(), uri)
    }

    private fun setTrackInfo(track: Track) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(requireContext(), track.uri)
        fileInfoTextView.text = "${track.displayTitle}" +
            " (${retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)})"
    }

    companion object {
        fun createFragment(): VisualEditorFragment {
            val args = Bundle().apply {
            }
            return VisualEditorFragment().apply { arguments = args }
        }
    }
}
