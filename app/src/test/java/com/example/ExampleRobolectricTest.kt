package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.RustCoreBridge
import com.example.data.model.TouchControlDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RustCraft", appName)
    }

    @Test
    fun `offline uuid matches minecraft rfc4122 v3 format`() {
        val uuid1 = RustCoreBridge.generateOfflineUuid("RustBuilder")
        val uuid2 = RustCoreBridge.generateOfflineUuid("RustBuilder")
        assertEquals(uuid1, uuid2)
        assertEquals(36, uuid1.length)
        assertEquals('3', uuid1[14])
    }

    @Test
    fun `skin dimension validator accepts 64x64 and 64x32`() {
        assertTrue(RustCoreBridge.validateSkinDimensions(64, 64))
        assertTrue(RustCoreBridge.validateSkinDimensions(64, 32))
        assertFalse(RustCoreBridge.validateSkinDimensions(128, 64))
    }

    @Test
    fun `touch layout json roundtrip preserves buttons`() {
        val preset = TouchControlDefaults.pvpPreset()
        val encoded = TouchControlDefaults.encodeButtons(preset)
        val decoded = TouchControlDefaults.decodeButtons(encoded)
        assertEquals(preset.size, decoded.size)
        assertEquals(preset.first().id, decoded.first().id)
    }
}
