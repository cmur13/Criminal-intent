package database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bignerdranch.android.criminalintent.Crime
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CrimeDao{
    @Query("SELECT * FROM crime")
    fun getCrimes(): Flow<List<Crime>>
    @Query("SELECT * FROM crime WHERE id = (:id)")
    suspend fun getCrime(id: UUID): Crime
    @Update
    suspend fun updateCrime(crime: Crime)
    // added a new crime to the database
    @Insert
    suspend fun addCrime(crime: Crime)
    @Delete
    suspend fun deleteCrime(crime:Crime)
}