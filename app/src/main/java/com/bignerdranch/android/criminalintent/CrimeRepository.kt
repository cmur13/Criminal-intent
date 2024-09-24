package com.bignerdranch.android.criminalintent

import android.content.Context
import androidx.room.Room
import database.CrimeDatabase
import kotlinx.coroutines.flow.Flow
import java.lang.IllegalStateException
import java.util.UUID

private const val DATABASE_NAME = "crime-database"

class CrimeRepository private constructor(context: Context) {

    // add a private property to store a reference to your database
    private val database: CrimeDatabase = Room.databaseBuilder(
        context.applicationContext,
        CrimeDatabase::class.java,
        DATABASE_NAME
    )
        .createFromAsset(DATABASE_NAME)
        .build()
    fun getCrimes(): Flow<List<Crime>> = database.crimeDao().getCrimes()

    suspend fun getCrime(id: UUID): Crime = database.crimeDao().getCrime(id)

    companion object{
        private var INSTANCE: CrimeRepository?= null

        fun initialize(context: Context){
            if(INSTANCE == null){
                INSTANCE = CrimeRepository(context)
            }
        }

        fun get(): CrimeRepository {
            return INSTANCE ?:
            throw IllegalStateException("Crime Repository must be initialized")
        }
    }
}