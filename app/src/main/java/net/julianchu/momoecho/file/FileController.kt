package net.julianchu.momoecho.file

import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    suspend fun readFromCsv(): Triple<File, List<Track>, List<Clip>> = withContext(Dispatchers.IO) {
        val result = readCsv(file)
        Triple(file, result.second, result.third)
    }
}
