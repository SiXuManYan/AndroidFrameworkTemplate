package com.example.template.data.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Room 示例属于 App，不由框架初始化；业务扩展应继承 RoomDatabase 而非另一个数据库。 */
@Entity(tableName = "example_note")
data class ExampleNote(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String)

@Dao
interface ExampleNoteDao {
    @Query("SELECT * FROM example_note ORDER BY id DESC")
    fun observeAll(): Flow<List<ExampleNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(note: ExampleNote): Long

    @Query("DELETE FROM example_note WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [ExampleNote::class], version = 1, exportSchema = false)
abstract class ExampleDatabase : RoomDatabase() {
    abstract fun noteDao(): ExampleNoteDao

    companion object {
        @Volatile
        private var instance: ExampleDatabase? = null

        /** 首次使用才打开数据库；升级时必须提供 Migration，不自动删除数据。 */
        fun getInstance(context: Context): ExampleDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, ExampleDatabase::class.java, "example_database"
            ).build().also { instance = it }
        }
    }
}
