package dev.schuberth.kostin.client

import dev.schuberth.kostin.client.models.AuthServerFinal
import dev.schuberth.kostin.client.models.AuthServerFirst

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.HMAC
import dev.whyoleg.cryptography.algorithms.PBKDF2
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.random.CryptographyRandom

import kotlin.experimental.xor
import kotlin.io.encoding.Base64

object CryptoHelperKotlin : CryptoHelper {
    private val provider = CryptographyProvider.Default

    override suspend fun getSaltedPassword(auth: AuthServerFirst, password: String): ByteArray {
        val pbkdf2 = provider.get(PBKDF2)

        val derivation = pbkdf2.secretDerivation(
            digest = SHA256,
            iterations = auth.rounds,
            outputSize = 256.bits,
            salt = Base64.decode(auth.salt)
        )

        return derivation.deriveSecretToByteArray(password.encodeToByteArray())
    }

    override suspend fun getProof(authMessage: ByteArray, saltedPassword: ByteArray): Proof {
        val hmac = provider.get(HMAC)
        val sha256 = provider.get(SHA256)

        val clientKey = hmac.keyDecoder(SHA256)
            .decodeFromByteArray(HMAC.Key.Format.RAW, saltedPassword)
            .signatureGenerator()
            .generateSignature("Client Key".encodeToByteArray())

        val storedKey = sha256.hasher().hash(clientKey)

        val clientSignature = hmac.keyDecoder(SHA256)
            .decodeFromByteArray(HMAC.Key.Format.RAW, storedKey)
            .signatureGenerator()
            .generateSignature(authMessage)

        val proof = clientKey.zip(clientSignature) { a, b -> a xor b }.toByteArray()

        return Proof(authMessage, clientKey, storedKey, proof)
    }

    override suspend fun checkServerSignature(
        auth: AuthServerFinal,
        saltedPassword: ByteArray,
        authMessage: ByteArray
    ): Boolean {
        val hmac = provider.get(HMAC)

        val serverKey = hmac.keyDecoder(SHA256)
            .decodeFromByteArray(HMAC.Key.Format.RAW, saltedPassword)
            .signatureGenerator()
            .generateSignature("Server Key".encodeToByteArray())

        val serverSignature = hmac.keyDecoder(SHA256)
            .decodeFromByteArray(HMAC.Key.Format.RAW, serverKey)
            .signatureGenerator()
            .generateSignature(authMessage)

        return serverSignature.contentEquals(Base64.decode(auth.signature))
    }

    @OptIn(DelicateCryptographyApi::class)
    override suspend fun encryptSessionToken(token: String, proof: Proof): SealedBox {
        val hmac = provider.get(HMAC)
        val aesGcm = provider.get(AES.GCM)

        val msg = "Session Key".encodeToByteArray() + proof.authMessage + proof.clientKey

        val protocolKey = hmac.keyDecoder(SHA256)
            .decodeFromByteArray(HMAC.Key.Format.RAW, proof.storedKey)
            .signatureGenerator()
            .generateSignature(msg)

        val key = aesGcm.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, protocolKey)
        val cipher = key.cipher()
        val nonce = CryptographyRandom.nextBytes(12)
        val ciphertext = cipher.encryptWithIv(nonce, token.encodeToByteArray())

        val authTag = ciphertext.takeLast(16).toByteArray()
        val encryptedData = ciphertext.dropLast(16).toByteArray()

        return SealedBox(nonce, authTag, encryptedData)
    }
}
