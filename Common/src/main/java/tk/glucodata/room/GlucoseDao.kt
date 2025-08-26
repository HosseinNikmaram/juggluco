package tk.glucodata.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GlucoseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: GlucoseReading)

    @Update
    suspend fun update(reading: GlucoseReading)

    @Delete
    suspend fun delete(reading: GlucoseReading)

    @Query("SELECT * FROM glucose_readings ORDER BY timmsec DESC")
    fun getAllReadings(): Flow<List<GlucoseReading>>

    @Query("SELECT * FROM glucose_readings WHERE serialNumber = :serial ORDER BY timmsec DESC")
    fun getReadingsBySerial(serial: String): Flow<List<GlucoseReading>>
}
