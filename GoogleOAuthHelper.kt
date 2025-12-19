package com.example.bookletscanner

import android.util.Base64
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

object GoogleOAuthHelper {

    fun getAccessToken(clientEmail: String, privateKeyPem: String): String {
        val privateKey = loadPrivateKey(privateKeyPem)

        val header = JSONObject()
        header.put("alg", "RS256")
        header.put("typ", "JWT")

        val nowSeconds = System.currentTimeMillis() / 1000
        val claim = JSONObject()
        claim.put("iss", clientEmail)
        claim.put("scope", "https://www.googleapis.com/auth/cloud-platform")
        claim.put("aud", "https://oauth2.googleapis.com/token")
        claim.put("exp", nowSeconds + 3600)
        claim.put("iat", nowSeconds)

        val headerBase64 = base64UrlEncode(header.toString().toByteArray(StandardCharsets.UTF_8))
        val claimBase64 = base64UrlEncode(claim.toString().toByteArray(StandardCharsets.UTF_8))
        val unsignedToken = "$headerBase64.$claimBase64"

        val signature = sign(unsignedToken, privateKey)
        val signedJwt = "$unsignedToken.${base64UrlEncode(signature)}"

        // 🔹 Exchange JWT for access token
        val url = URL("https://oauth2.googleapis.com/token")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        connection.doOutput = true

        val params = "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=$signedJwt"
        val writer = OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8)
        writer.write(params)
        writer.flush()
        writer.close()

        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()

        val json = JSONObject(response.toString())
        return json.getString("access_token")
    }

    private fun loadPrivateKey(pem: String): PrivateKey {
        val cleaned = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val decoded = Base64.decode(cleaned, Base64.DEFAULT)
        val spec = PKCS8EncodedKeySpec(decoded)
        return KeyFactory.getInstance("RSA").generatePrivate(spec)
    }

    private fun sign(data: String, privateKey: PrivateKey): ByteArray {
        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(privateKey)
        signer.update(data.toByteArray(StandardCharsets.UTF_8))
        return signer.sign()
    }

    private fun base64UrlEncode(input: ByteArray): String {
        return Base64.encodeToString(input, Base64.NO_WRAP)
            .replace("+", "-")
            .replace("/", "_")
            .replace("=", "")
    }
}
