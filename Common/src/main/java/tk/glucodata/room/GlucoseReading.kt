package tk.glucodata.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "glucose_readings")
data class GlucoseReading(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serialNumber: String,
    val mgdl: Int,
    val gl: Float,
    val rate: Float,
    val alarm: Int,
    val timmsec: Long,
    val sensorStartMsec: Long,
    val showTime: Long,
    val sensorGenModel: Int
)
