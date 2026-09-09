package com.ndev.moodyroutine.util

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ndev.moodyroutine.data.db.MoodyRoutineDatabase
import com.ndev.moodyroutine.data.db.entity.ModeEntity
import com.ndev.moodyroutine.data.db.entity.RoutineEntity
import com.ndev.moodyroutine.data.model.Mode
import com.ndev.moodyroutine.data.model.Routine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class BackupPayload(
    val version: Int = 1,
    val app: String = "MoodyRoutine",
    val timestamp: Long = System.currentTimeMillis(),
    val modes: List<Mode> = emptyList(),
    val routines: List<Routine> = emptyList()
)

object BackupManager {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportBackup(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val db = MoodyRoutineDatabase.getInstance(context)
            val modeEntities = db.modeDao().getAll().first()
            val routineEntities = db.routineDao().getAll().first()

            val modes = modeEntities.map { it.toDomainModel() }
            val routines = routineEntities.map { it.toDomainModel() }

            val payload = BackupPayload(
                version = 1,
                app = "MoodyRoutine",
                timestamp = System.currentTimeMillis(),
                modes = modes,
                routines = routines
            )

            val json = gson.toJson(payload)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            } ?: return@withContext Result.failure(Exception("Failed to open output stream for export"))

            AppLogger.i("BackupManager", "Successfully exported ${modes.size} modes and ${routines.size} routines")
            Result.success("Exported ${modes.size} modes and ${routines.size} routines successfully")
        } catch (e: Exception) {
            AppLogger.e("BackupManager", "Error exporting backup", e)
            Result.failure(e)
        }
    }

    suspend fun readBackupPayload(context: Context, uri: Uri): Result<BackupPayload> = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
            } ?: return@withContext Result.failure(Exception("Could not read backup file"))

            val payload = gson.fromJson(content, BackupPayload::class.java)
                ?: return@withContext Result.failure(Exception("Invalid backup format"))

            Result.success(payload)
        } catch (e: Exception) {
            AppLogger.e("BackupManager", "Error reading backup file", e)
            Result.failure(e)
        }
    }

    suspend fun importBackup(
        context: Context,
        payload: BackupPayload,
        overwrite: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val db = MoodyRoutineDatabase.getInstance(context)

            if (overwrite) {
                // Remove all existing modes and routines
                val existingModes = db.modeDao().getAll().first()
                existingModes.forEach { db.modeDao().delete(it) }

                val existingRoutines = db.routineDao().getAll().first()
                existingRoutines.forEach { db.routineDao().delete(it) }
            }

            // Insert modes with fresh IDs
            payload.modes.forEach { mode ->
                val entity = ModeEntity.fromDomainModel(mode.copy(id = 0, isActive = false))
                db.modeDao().insert(entity)
            }

            // Insert routines with fresh IDs
            payload.routines.forEach { routine ->
                val entity = RoutineEntity.fromDomainModel(routine.copy(id = 0))
                db.routineDao().insert(entity)
            }

            val summary = "Imported ${payload.modes.size} modes and ${payload.routines.size} routines"
            AppLogger.i("BackupManager", "Successfully imported: $summary (overwrite=$overwrite)")
            Result.success(summary)
        } catch (e: Exception) {
            AppLogger.e("BackupManager", "Error importing backup", e)
            Result.failure(e)
        }
    }
}
