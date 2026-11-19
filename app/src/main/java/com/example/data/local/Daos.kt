package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY isActive DESC, lastUsedAt DESC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 LIMIT 1")
    fun getActiveAccount(): Flow<AccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE accounts SET isActive = 0")
    suspend fun deactivateAllAccounts()

    @Query("UPDATE accounts SET isActive = 1, lastUsedAt = :now WHERE id = :id")
    suspend fun activateAccount(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)
}

@Dao
interface InstanceDao {
    @Query("SELECT * FROM instances ORDER BY isSelected DESC, lastPlayedAt DESC")
    fun getAllInstances(): Flow<List<InstanceEntity>>

    @Query("SELECT * FROM instances WHERE isSelected = 1 LIMIT 1")
    fun getSelectedInstance(): Flow<InstanceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstance(instance: InstanceEntity): Long

    @Update
    suspend fun updateInstance(instance: InstanceEntity)

    @Query("UPDATE instances SET isSelected = 0")
    suspend fun clearSelectedInstance()

    @Query("UPDATE instances SET isSelected = 1, lastPlayedAt = :now WHERE id = :id")
    suspend fun selectInstance(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM instances WHERE id = :id")
    suspend fun deleteInstanceById(id: Long)
}

@Dao
interface InstalledModDao {
    @Query("SELECT * FROM installed_mods WHERE instanceId = :instanceId ORDER BY installedAt DESC")
    fun getModsForInstance(instanceId: Long): Flow<List<InstalledModEntity>>

    @Query("SELECT * FROM installed_mods ORDER BY installedAt DESC")
    fun getAllInstalledMods(): Flow<List<InstalledModEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMod(mod: InstalledModEntity): Long

    @Query("UPDATE installed_mods SET isEnabled = :enabled WHERE id = :id")
    suspend fun setModEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM installed_mods WHERE id = :id")
    suspend fun deleteModById(id: Long)
}

@Dao
interface TouchLayoutDao {
    @Query("SELECT * FROM touch_layouts ORDER BY isActive DESC, updatedAt DESC")
    fun getAllLayouts(): Flow<List<TouchLayoutEntity>>

    @Query("SELECT * FROM touch_layouts WHERE isActive = 1 LIMIT 1")
    fun getActiveLayout(): Flow<TouchLayoutEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayout(layout: TouchLayoutEntity): Long

    @Update
    suspend fun updateLayout(layout: TouchLayoutEntity)

    @Query("UPDATE touch_layouts SET isActive = 0")
    suspend fun clearActiveLayout()

    @Query("UPDATE touch_layouts SET isActive = 1, updatedAt = :now WHERE id = :id")
    suspend fun activateLayout(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM touch_layouts WHERE id = :id")
    suspend fun deleteLayoutById(id: Long)
}
