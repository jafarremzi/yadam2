package app.yadavar.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** عنوان کار (برای یادآوری‌ها؛ برای یادداشت ساده خالی است). */
    val title: String = "",
    val text: String,
    val createdAt: Long,
    /** زمان یادآوری (میلی‌ثانیه UTC)؛ null یعنی یادداشت ساده. */
    val remindAt: Long? = null,
    val repeat: Int = Repeat.NONE,
    val done: Boolean = false,
    /** آیا اعلان این نوبت نمایش داده شده است؟ */
    val fired: Boolean = false,
) {
    val displayTitle: String get() = title.ifBlank { text }
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE remindAt IS NOT NULL AND done = 0")
    suspend fun getActiveReminders(): List<NoteEntity>

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)
}

/** مهاجرت‌ها: هنگام افزایش version باید Migration صریح اضافه شود (از fallbackToDestructiveMigration عمداً استفاده نشده). */
@Database(entities = [NoteEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "yadavar.db",
                ).build().also { instance = it }
            }
    }
}
