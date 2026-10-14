package org.privacyrom.dialer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NicknamesTest {

    private val nicknames = Nicknames("device-secret-1".toByteArray())

    @Test
    fun sameNumberSameNickname() {
        assertEquals(nicknames.of("+39 333 123 4567"), nicknames.of("+39 333 123 4567"))
    }

    @Test
    fun formattingAndCountryCodeDoNotMatter() {
        assertEquals(nicknames.of("+39 333 123 4567"), nicknames.of("333-123-4567"))
        assertEquals(nicknames.of("(555) 010-9999"), nicknames.of("5550109999"))
    }

    @Test
    fun differentNumbersDifferentNicknames() {
        assertNotEquals(nicknames.of("3331234567"), nicknames.of("3331234568"))
    }

    @Test
    fun otherDeviceSecretGivesOtherNickname() {
        val other = Nicknames("device-secret-2".toByteArray())
        assertNotEquals(nicknames.of("3331234567"), other.of("3331234567"))
    }

    @Test
    fun nicknameNeverContainsDigitsOfTheNumber() {
        val nick = nicknames.of("3331234567")
        assertFalse(nick.contains("3331234567"))
        assertTrue(nick.matches(Regex("[A-Z][a-z]+ [A-Z][a-z]+ \\d{1,2}")))
    }

    @Test
    fun hiddenNumbers() {
        assertEquals("Unknown caller", nicknames.of(null))
        assertEquals("Unknown caller", nicknames.of(""))
        assertNull(Nicknames.normalize("Private"))
    }
}
