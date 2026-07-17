package dev.schuberth.kostin.client

import at.asitplus.signum.indispensable.Digest as SignumDigest
import at.asitplus.signum.indispensable.HMAC
import at.asitplus.signum.indispensable.kdf.PBKDF2
import at.asitplus.signum.indispensable.symmetric.SymmetricEncryptionAlgorithm
import at.asitplus.signum.indispensable.symmetric.authTag
import at.asitplus.signum.indispensable.symmetric.keyFrom
import at.asitplus.signum.indispensable.symmetric.nonce
import at.asitplus.signum.supreme.hash.digest
import at.asitplus.signum.supreme.kdf.deriveKey
import at.asitplus.signum.supreme.mac.mac
import at.asitplus.signum.supreme.symmetric.encrypt

import dev.schuberth.kostin.client.models.AuthServerFinal
import dev.schuberth.kostin.client.models.AuthServerFirst

import kotlin.experimental.xor
import kotlin.io.encoding.Base64

actual fun CryptoHelper.Companion.get(): CryptoHelper = CryptoHelperSignum

object CryptoHelperSignum : CryptoHelper {
    override suspend fun getSaltedPassword(auth: AuthServerFirst, password: String): ByteArray {
        val kdf = PBKDF2.HMAC_SHA256(auth.rounds)
        return kdf.deriveKey(
            Base64.decode(auth.salt),
            password.encodeToByteArray(),
            kdf.pbkdf2.prf.digest.outputLength
        ).getOrThrow()
    }

    override suspend fun getProof(authMessage: ByteArray, saltedPassword: ByteArray): Proof {
        val clientKey = HMAC.SHA256.mac(saltedPassword, "Client Key".encodeToByteArray()).getOrThrow()
        val storedKey = SignumDigest.SHA256.digest(clientKey)
        val clientSignature = HMAC.SHA256.mac(storedKey, authMessage).getOrThrow()

        val proof = clientKey.zip(clientSignature) { a, b -> a xor b }.toByteArray()

        return Proof(authMessage, clientKey, storedKey, proof)
    }

    override suspend fun checkServerSignature(
        auth: AuthServerFinal,
        saltedPassword: ByteArray,
        authMessage: ByteArray
    ): Boolean {
        val serverKey = HMAC.SHA256.mac(saltedPassword, "Server Key".encodeToByteArray()).getOrThrow()
        val serverSignature = HMAC.SHA256.mac(serverKey, authMessage).getOrThrow()
        return serverSignature.contentEquals(Base64.decode(auth.signature))
    }

    override suspend fun encryptSessionToken(token: String, proof: Proof): SealedBox {
        val msg = "Session Key".encodeToByteArray() + proof.authMessage + proof.clientKey
        val protocolKey = HMAC.SHA256.mac(proof.storedKey, msg).getOrThrow()
        val key = SymmetricEncryptionAlgorithm.AES_256.GCM.keyFrom(protocolKey).getOrThrow()
        val box = key.encrypt(token.encodeToByteArray()).getOrThrow()
        return SealedBox(box.nonce, box.authTag, box.encryptedData)
    }
}
