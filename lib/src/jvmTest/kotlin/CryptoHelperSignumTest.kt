package dev.schuberth.kostin.client

import de.infix.testBalloon.framework.core.testSuite

import io.kotest.matchers.shouldBe

val cryptoHelperSignumTest by testSuite {
    test("encryptSessionToken()") {
        val box = CryptoHelperSignum.encryptSessionToken(
            "26b0ccb8c0e76a91773a883dfa52fd27fa159912c4c2689fb12839216de7d48f",
            Proof(
                authMessage = byteArrayOf(
                    110, 61, 117, 115, 101, 114, 44, 114, 61, 112, 79, 99, 90, 89, 82, 102, 103, 86, 67, 49, 67, 51, 72,
                    98, 111, 44, 114, 61, 112, 79, 99, 90, 89, 82, 102, 103, 86, 67, 49, 67, 51, 72, 98, 111, 82, 114,
                    121, 113, 78, 54, 80, 111, 121, 65, 50, 97, 111, 80, 70, 104, 44, 115, 61, 85, 75, 70, 104, 104, 71,
                    56, 105, 55, 118, 101, 106, 118, 80, 85, 111, 44, 105, 61, 50, 57, 48, 48, 48, 44, 99, 61, 98, 105,
                    119, 115, 44, 114, 61, 112, 79, 99, 90, 89, 82, 102, 103, 86, 67, 49, 67, 51, 72, 98, 111, 82, 114,
                    121, 113, 78, 54, 80, 111, 121, 65, 50, 97, 111, 80, 70, 104
                ),
                clientKey = byteArrayOf(
                    62, 14, 61, 117, 37, -46, -42, -34, -3, 106, 74, -55, 10, 31, -15, -96, -61, -75, 25, -40, -83, 36,
                    -113, -119, 69, 70, 94, -125, -81, -80, -124, -19
                ),
                storedKey = byteArrayOf(
                    24, -115, -37, -26, 4, 0, -56, 108, -11, -118, 83, -4, -100, -90, 96, 12, -19, -24, -6, 39, -9, 50,
                    121, -62, 71, -90, 11, 108, 24, -66, 68, -25
                ),
                proof = byteArrayOf(
                    -118, 92, 86, 11, -85, 98, 1, 71, 109, 116, 111, -63, 21, 45, 54, 33, -116, 65, -33, -63, -124, -12,
                    97, 5, 38, 72, -65, 116, 102, -21, -8, 72
                )
            )
        )

        with(box) {
            nonce shouldBe byteArrayOf(-36, 74, -83, -78, 19, 64, -92, 64, -96, -93, 56, 78)
            authTag shouldBe byteArrayOf()
            encryptedData shouldBe byteArrayOf()
        }
    }
}
