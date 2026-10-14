package org.privacyrom.dialer

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Turns phone numbers into stable, human-friendly nicknames ("Quiet Otter").
 *
 * The nickname is an HMAC of the normalized number keyed with a per-device secret, so the same
 * number always gets the same nickname on this phone, but nicknames can't be reversed or
 * matched across devices. Pure Kotlin/JVM so it can be unit-tested.
 */
class Nicknames(private val secret: ByteArray) {

    fun of(number: String?): String {
        val key = normalize(number) ?: return "Unknown caller"
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(secret, "HmacSHA256")) }
        val h = mac.doFinal(key.toByteArray())
        val adjective = ADJECTIVES[(h[0].toInt() and 0xFF) % ADJECTIVES.size]
        val animal = ANIMALS[(h[1].toInt() and 0xFF) % ANIMALS.size]
        // 2 more bytes as a short tag keep collisions between contacts rare (~1 in 2.6M).
        val tag = ((h[2].toInt() and 0xFF) shl 8 or (h[3].toInt() and 0xFF)) % 100
        return "$adjective $animal $tag"
    }

    companion object {
        /**
         * Last 9 digits, so "+39 333 123 4567" and "3331234567" match. Null for hidden/empty numbers.
         */
        fun normalize(number: String?): String? {
            val digits = number?.filter { it.isDigit() } ?: return null
            if (digits.isEmpty()) return null
            return digits.takeLast(9)
        }

        private val ADJECTIVES = listOf(
            "Quiet", "Brave", "Calm", "Swift", "Gentle", "Bright", "Clever", "Lucky", "Sunny", "Misty",
            "Silver", "Golden", "Rapid", "Patient", "Bold", "Kind", "Witty", "Noble", "Mellow", "Frosty",
            "Amber", "Crimson", "Velvet", "Cosmic", "Polar", "Rustic", "Shy", "Stormy", "Tidy", "Zesty",
            "Jolly", "Lively",
        )
        private val ANIMALS = listOf(
            "Otter", "Falcon", "Badger", "Heron", "Lynx", "Panda", "Gecko", "Bison", "Koala", "Raven",
            "Walrus", "Marten", "Ibex", "Puffin", "Tapir", "Okapi", "Lemur", "Quokka", "Moose", "Stork",
            "Beaver", "Coyote", "Dolphin", "Ferret", "Gazelle", "Hedgehog", "Jaguar", "Kiwi", "Llama",
            "Narwhal", "Owl", "Pelican",
        )
    }
}
