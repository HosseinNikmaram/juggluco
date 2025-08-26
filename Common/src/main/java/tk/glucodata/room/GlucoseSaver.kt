package tk.glucodata.room

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GlucoseSaver(private val context: Context) {

    fun saveReading(
        serialNumber: String,
        mgdl: Int,
        gl: Float,
        rate: Float,
        alarm: Int,
        timmsec: Long,
        sensorStartMsec: Long,
        showTime: Long,
        sensorGenModel: Int
    ) {
        val dao = AppDatabase.getDatabase(context).glucoseDao()

        val reading = GlucoseReading(
            serialNumber = serialNumber,
            mgdl = mgdl,
            gl = gl,
            rate = rate,
            alarm = alarm,
            timmsec = timmsec,
            sensorStartMsec = sensorStartMsec,
            showTime = showTime,
            sensorGenModel = sensorGenModel
        )

        CoroutineScope(Dispatchers.IO).launch {
            dao.insert(reading)
        }
    }

    fun getAllReadings(callback: (List<GlucoseReading>) -> Unit) {
        val dao = AppDatabase.getDatabase(context).glucoseDao()

        CoroutineScope(Dispatchers.IO).launch {
            val data = dao.getAllReadings()
            // Return on main thread
            CoroutineScope(Dispatchers.Main).launch {
               //    callback(data)
            }
        }
    }
}
