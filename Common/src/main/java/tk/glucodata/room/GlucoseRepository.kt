package tk.glucodata.room

import kotlinx.coroutines.flow.Flow

class GlucoseRepository(private val dao: GlucoseDao) {

    suspend fun insert(reading: GlucoseReading) = dao.insert(reading)
    suspend fun update(reading: GlucoseReading) = dao.update(reading)
    suspend fun delete(reading: GlucoseReading) = dao.delete(reading)

    fun getAllReadings(): Flow<List<GlucoseReading>> = dao.getAllReadings()
    fun getReadingsBySerial(serial: String): Flow<List<GlucoseReading>> = dao.getReadingsBySerial(serial)
}
