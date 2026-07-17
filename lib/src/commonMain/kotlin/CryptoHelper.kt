package dev.schuberth.kostin.client

import dev.schuberth.kostin.client.models.AuthServerFinal
import dev.schuberth.kostin.client.models.AuthServerFirst

interface CryptoHelper {
    fun getSaltedPassword(auth: AuthServerFirst, password: String): ByteArray
    fun getProof(user: User, nonce: String, auth: AuthServerFirst, saltedPassword: ByteArray): Proof
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
