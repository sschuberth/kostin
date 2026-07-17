package dev.schuberth.kostin.client

import dev.schuberth.kostin.client.models.AuthServerFinal
import dev.schuberth.kostin.client.models.AuthServerFirst

expect fun CryptoHelper.Companion.get(): CryptoHelper

interface CryptoHelper {
    companion object

    fun getAuthMessage(user: User, nonce: String, auth: AuthServerFirst): ByteArray = listOf(
        "n=${user.name}",
        "r=$nonce",
        "r=${auth.nonce}",
        "s=${auth.salt}",
        "i=${auth.rounds}",
        "c=biws",
        "r=${auth.nonce}"
    ).joinToString(",").encodeToByteArray()

    fun getSaltedPassword(auth: AuthServerFirst, password: String): ByteArray
    fun getProof(authMessage: ByteArray, saltedPassword: ByteArray): Proof
    fun checkServerSignature(auth: AuthServerFinal, saltedPassword: ByteArray, authMessage: ByteArray): Boolean
    fun encryptSessionToken(token: String, proof: Proof): SealedBox
}

@Suppress("EnumEntryName")
enum class User { user, master }

class Proof(
    val authMessage: ByteArray,
    val clientKey: ByteArray,
    val storedKey: ByteArray,
    val proof: ByteArray
)

class SealedBox(
    val nonce: ByteArray,
    val authTag: ByteArray,
    val encryptedData: ByteArray
)
