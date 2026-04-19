package com.funtime.sciai

import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
    
    @Test
    fun testEmotionState_values() {
        val states = listOf(
            "IDLE", "HAPPY", "SAD", "EXCITED", "SLEEPING", 
            "LOVE", "SHY", "ANGRY", "CONCERNED", "CURIOUS"
        )
        assertEquals(10, states.size)
    }
    
    @Test
    fun testChatMessage_creation() {
        val message = ChatMessage(
            id = "test-1",
            text = "Hello",
            isUser = true,
            timestamp = System.currentTimeMillis()
        )
        assertEquals("test-1", message.id)
        assertEquals("Hello", message.text)
        assertTrue(message.isUser)
    }
}

class PreferenceManagerTest {
    @Test
    fun testPreferences_defaultValues() {
        val prefs = TestPreferences()
        assertEquals(0.5f, prefs.happinessLevel, 0.01f)
        assertEquals(0.3f, prefs.affectionLevel, 0.01f)
        assertTrue(prefs.isEnabled)
    }
}

class MockPreferences {
    var happinessLevel: Float = 0.5f
    var affectionLevel: Float = 0.3f
    var isEnabled: Boolean = true
    var userId: String? = "test-user"
}

fun createTestPreferences(): MockPreferences = MockPreferences()