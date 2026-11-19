package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.RustCoreBridge
import com.example.data.model.TouchControlDefaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AccountEntity::class,
        InstanceEntity::class,
        InstalledModEntity::class,
        TouchLayoutEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class RustCraftDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun instanceDao(): InstanceDao
    abstract fun installedModDao(): InstalledModDao
    abstract fun touchLayoutDao(): TouchLayoutDao

    companion object {
        @Volatile
        private var INSTANCE: RustCraftDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Remove any pre-seeded placeholder versions; keep only scanned external ready versions
                db.execSQL("DELETE FROM instances WHERE externalPathUri = ''")
            }
        }

        fun getInstance(context: Context): RustCraftDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): RustCraftDatabase {
            lateinit var db: RustCraftDatabase
            db = Room.databaseBuilder(
                context,
                RustCraftDatabase::class.java,
                "rustcraft_launcher.db",
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(object : Callback() {
                    override fun onCreate(database: SupportSQLiteDatabase) {
                        super.onCreate(database)
                        CoroutineScope(Dispatchers.IO).launch {
                            seedInitialData(db)
                        }
                    }
                })
                .build()
            return db
        }

        private suspend fun seedInitialData(db: RustCraftDatabase) {
            val defaultUser = "RustBuilder"
            db.accountDao().insertAccount(
                AccountEntity(
                    username = defaultUser,
                    uuid = RustCoreBridge.generateOfflineUuid(defaultUser),
                    accountType = AccountType.OFFLINE.name,
                    skinModel = SkinModel.CLASSIC.name,
                    capeStyle = "EMERALD_CAPE",
                    skinServerUrl = "https://authserver.ely.by/api/authlib-injector",
                    isActive = true,
                )
            )

            // No pre-bundled Minecraft versions are seeded.
            // Versions are only populated by scanning ready installations from external launchers (Zalith / Mojo / Pojav).

            db.touchLayoutDao().insertLayout(
                TouchLayoutEntity(
                    name = "الوضع القتالي PvP",
                    isActive = true,
                    buttonsJson = TouchControlDefaults.encodeButtons(TouchControlDefaults.pvpPreset()),
                    globalOpacity = 0.85f,
                    hapticEnabled = true,
                )
            )
            db.touchLayoutDao().insertLayout(
                TouchLayoutEntity(
                    name = "وضع البناء والإبداع",
                    isActive = false,
                    buttonsJson = TouchControlDefaults.encodeButtons(TouchControlDefaults.builderPreset()),
                    globalOpacity = 0.78f,
                    hapticEnabled = true,
                )
            )
        }
    }
}
