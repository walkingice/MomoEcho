package net.julianchu.momoecho.file

import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.julianchu.momoecho.model.Clip
import net.julianchu.momoecho.model.Track
import net.julianchu.momoecho.utils.readCsv
import net.julianchu.momoecho.utils.writeCsv
import java.io.File

//private fun ensureDir(dir: File): Boolean {
//    return if (dir.mkdirs()) {
//        true
//    } else {
//        return dir.exists() && dir.isDirectory() && dir.canWrite()
//    }
//
//}

class FileController {
    private val filename = "momoecho.csv"
    private val file = File(Environment.getExternalStorageDirectory(), filename)

    fun saveToCsv(
        tracks: List<Track>, clips: List<Clip>?,
        callback: (Boolean, File) -> Unit = { _, _ -> }
    ) {

        GlobalScope.launch(context = Dispatchers.IO) {
            val result = writeCsv(file, tracks, clips)
            GlobalScope.launch(context = Dispatchers.Main) {
                callback(result, file)
            }
        }
    }

    fun readFromCsv(
        callback: (File, List<Track>, List<Clip>) -> Unit = { _, _, _ -> }
    ) {

        GlobalScope.launch(context = Dispatchers.IO) {
            val result = readCsv(file)
            GlobalScope.launch(context = Dispatchers.Main) {
                callback(file, result.second, result.third)
            }
        }
    }
}