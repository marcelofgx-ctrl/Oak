package com.oakandember.admin

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class PhotoRef(
    val path: String,
    val size: Long,
    val type: String
)

data class RequestPayload(
    val name: String,
    val phone: String,
    val email: String,
    val address: String,
    val zip: String,
    val service: String,
    val appliance: String,
    val reason: String,
    val details: String,
    val contact: String,
    val date: String,
    val time: String
)

data class OakRequest(
    val id: String,
    val createdAt: String,
    val ready: Boolean,
    val payload: RequestPayload,
    val status: String,
    val notes: String,
    val quote: JSONObject?,
    val visit: JSONObject?,
    val photos: List<PhotoRef>
)

data class OakSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long,
    val email: String
)

class OakApiException(
    message: String,
    val statusCode: Int = 0
) : Exception(message)

class OakRepository(context: Context) {
    private val appContext = context.applicationContext
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            "oak_secure_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun hasStoredSession(): Boolean =
        !prefs.getString(KEY_REFRESH_TOKEN, null).isNullOrBlank()

    fun lastEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""

    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }

    suspend fun restoreSession(): Boolean = withContext(Dispatchers.IO) {
        if (!hasStoredSession()) return@withContext false
        try {
            val session = ensureSession()
            assertOperator(session.accessToken)
            true
        } catch (error: OakApiException) {
            if (error.statusCode == 401 || error.statusCode == 403) clearSession()
            false
        } catch (_: Exception) {
            false
        }
    }

    suspend fun signIn(email: String, password: String): OakSession = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("email", email.trim())
            .put("password", password)
            .toString()
            .toRequestBody(jsonMedia)

        val request = Request.Builder()
            .url("${SupabaseConfig.URL}/auth/v1/token?grant_type=password")
            .header("apikey", SupabaseConfig.API_KEY)
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        val json = executeJson(request)
        val session = sessionFromAuth(json, email.trim())
        saveSession(session)
        try {
            assertOperator(session.accessToken)
        } catch (error: Exception) {
            clearSession()
            throw error
        }
        session
    }

    suspend fun fetchRequests(): List<OakRequest> = withContext(Dispatchers.IO) {
        val body = authenticatedText { token ->
            Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/oak_requests?select=id,created_at,ready,payload,status,notes,quote,visit,photos&ready=eq.true&order=created_at.desc")
                .header("apikey", SupabaseConfig.API_KEY)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .get()
                .build()
        }
        parseRequests(body)
    }

    suspend fun updateRequest(id: String, status: String, notes: String): OakRequest = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("status", status)
            .put("notes", notes.trim())
            .toString()
            .toRequestBody(jsonMedia)

        val encodedId = URLEncoder.encode(id, StandardCharsets.UTF_8.toString())
        val body = authenticatedText { token ->
            Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/oak_requests?id=eq.$encodedId&select=id,created_at,ready,payload,status,notes,quote,visit,photos")
                .header("apikey", SupabaseConfig.API_KEY)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .patch(payload)
                .build()
        }
        val rows = parseRequests(body)
        rows.firstOrNull() ?: throw OakApiException("The request was saved, but the updated record could not be read back.")
    }

    suspend fun loadPhoto(path: String): ByteArray = withContext(Dispatchers.IO) {
        var session = ensureSession()
        var response = executeRaw(photoRequest(path, session.accessToken)).second
        if (response.code == 401) {
            response.close()
            session = refreshSession(session.refreshToken, session.email)
            response = executeRaw(photoRequest(path, session.accessToken)).second
        }
        response.use { http ->
            if (!http.isSuccessful) {
                val text = http.body?.string().orEmpty()
                throw apiError(text, http.code)
            }
            http.body?.bytes() ?: throw OakApiException("The photo could not be loaded.")
        }
    }

    private fun photoRequest(path: String, token: String): Request {
        val safePath = path.split('/').joinToString("/") { segment ->
            URLEncoder.encode(segment, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        }
        return Request.Builder()
            .url("${SupabaseConfig.URL}/storage/v1/object/authenticated/${SupabaseConfig.PHOTO_BUCKET}/$safePath")
            .header("apikey", SupabaseConfig.API_KEY)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
    }

    private fun saveSession(session: OakSession) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_EXPIRES_AT, session.expiresAtMillis)
            .putString(KEY_EMAIL, session.email)
            .apply()
    }

    private fun loadSession(): OakSession? {
        val access = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refresh = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val expires = prefs.getLong(KEY_EXPIRES_AT, 0L)
        return OakSession(access, refresh, expires, lastEmail())
    }

    private fun ensureSession(): OakSession {
        val existing = loadSession() ?: throw OakApiException("Please sign in.", 401)
        if (existing.accessToken.isNotBlank() && existing.expiresAtMillis > System.currentTimeMillis() + 60_000L) {
            return existing
        }
        return refreshSession(existing.refreshToken, existing.email)
    }

    private fun refreshSession(refreshToken: String, email: String): OakSession {
        val body = JSONObject()
            .put("refresh_token", refreshToken)
            .toString()
            .toRequestBody(jsonMedia)

        val request = Request.Builder()
            .url("${SupabaseConfig.URL}/auth/v1/token?grant_type=refresh_token")
            .header("apikey", SupabaseConfig.API_KEY)
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        val json = executeJson(request)
        val session = sessionFromAuth(json, email)
        saveSession(session)
        return session
    }

    private fun sessionFromAuth(json: JSONObject, fallbackEmail: String): OakSession {
        val access = json.optString("access_token")
        val refresh = json.optString("refresh_token")
        if (access.isBlank() || refresh.isBlank()) {
            throw OakApiException("Supabase did not return a valid session.", 401)
        }
        val expiresAtSeconds = json.optLong("expires_at", 0L)
        val expiresInSeconds = json.optLong("expires_in", 3600L)
        val expiresAtMillis = if (expiresAtSeconds > 0L) {
            expiresAtSeconds * 1000L
        } else {
            System.currentTimeMillis() + expiresInSeconds * 1000L
        }
        val userEmail = json.optJSONObject("user")?.optString("email").orEmpty().ifBlank { fallbackEmail }
        return OakSession(access, refresh, expiresAtMillis, userEmail)
    }

    private fun assertOperator(token: String) {
        val request = Request.Builder()
            .url("${SupabaseConfig.URL}/rest/v1/oak_operators?select=user_id&limit=1")
            .header("apikey", SupabaseConfig.API_KEY)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .get()
            .build()
        val text = executeText(request)
        if (JSONArray(text).length() == 0) {
            throw OakApiException("This account is not authorized to manage Oak & Ember.", 403)
        }
    }

    private fun authenticatedText(build: (String) -> Request): String {
        var session = ensureSession()
        var response = executeRaw(build(session.accessToken)).second
        if (response.code == 401) {
            response.close()
            session = refreshSession(session.refreshToken, session.email)
            response = executeRaw(build(session.accessToken)).second
        }
        response.use { http ->
            val text = http.body?.string().orEmpty()
            if (!http.isSuccessful) throw apiError(text, http.code)
            return text
        }
    }

    private fun executeJson(request: Request): JSONObject = JSONObject(executeText(request))

    private fun executeText(request: Request): String {
        executeRaw(request).second.use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw apiError(text, response.code)
            return text
        }
    }

    private fun executeRaw(request: Request): Pair<Int, okhttp3.Response> {
        val response = try {
            client.newCall(request).execute()
        } catch (_: Exception) {
            throw OakApiException("Could not reach Oak & Ember. Check the internet connection and try again.")
        }
        return response.code to response
    }

    private fun apiError(text: String, status: Int): OakApiException {
        val message = try {
            val json = JSONObject(text)
            json.optString("msg")
                .ifBlank { json.optString("message") }
                .ifBlank { json.optString("error_description") }
                .ifBlank { json.optString("error") }
                .ifBlank { "Request failed ($status)." }
        } catch (_: Exception) {
            "Request failed ($status)."
        }
        return OakApiException(message, status)
    }

    private fun parseRequests(text: String): List<OakRequest> {
        val rows = JSONArray(text)
        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                val payload = row.optJSONObject("payload") ?: JSONObject()
                val photosJson = row.optJSONArray("photos") ?: JSONArray()
                val photos = buildList {
                    for (photoIndex in 0 until photosJson.length()) {
                        val photo = photosJson.optJSONObject(photoIndex) ?: continue
                        val path = photo.optString("path")
                        if (path.isNotBlank()) {
                            add(PhotoRef(path, photo.optLong("size", 0L), photo.optString("type")))
                        }
                    }
                }
                add(
                    OakRequest(
                        id = row.optString("id"),
                        createdAt = row.optString("created_at"),
                        ready = row.optBoolean("ready", false),
                        payload = RequestPayload(
                            name = payload.optString("name"),
                            phone = payload.optString("phone"),
                            email = payload.optString("email"),
                            address = payload.optString("address"),
                            zip = payload.optString("zip"),
                            service = payload.optString("service"),
                            appliance = payload.optString("appliance"),
                            reason = payload.optString("reason"),
                            details = payload.optString("details"),
                            contact = payload.optString("contact"),
                            date = payload.optString("date"),
                            time = payload.optString("time")
                        ),
                        status = row.optString("status", "Nueva"),
                        notes = row.optString("notes"),
                        quote = row.optJSONObject("quote"),
                        visit = row.optJSONObject("visit"),
                        photos = photos
                    )
                )
            }
        }
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_EMAIL = "email"
    }
}

val allowedStatuses = listOf(
    "Nueva",
    "Por contactar",
    "Esperando información",
    "Presupuesto enviado",
    "Aceptada",
    "Cerrada"
)

fun statusLabel(status: String): String = when (status) {
    "Nueva" -> "New"
    "Por contactar" -> "To contact"
    "Esperando información" -> "Waiting for info"
    "Presupuesto enviado" -> "Quote sent"
    "Aceptada" -> "Accepted"
    "Cerrada" -> "Closed"
    else -> status
}

fun serviceLabel(service: String): String = when (service) {
    "cleaning" -> "Chimney cleaning"
    "inspection" -> "Chimney inspection"
    "repair" -> "Repairs & maintenance"
    "fireplace" -> "Fireplace installation"
    "stove" -> "Stove installation"
    "unsure" -> "Help me choose"
    else -> service.ifBlank { "Service request" }
}

fun OakRequest.customerKey(): String = when {
    payload.email.isNotBlank() -> payload.email.trim().lowercase()
    payload.phone.isNotBlank() -> payload.phone.filter(Char::isDigit)
    else -> payload.name.trim().lowercase()
}

fun OakRequest.scheduledDate(): String = visit?.let { json ->
    sequenceOf("date", "scheduled_date", "day")
        .map { json.optString(it) }
        .firstOrNull { it.isNotBlank() }
}.orEmpty().ifBlank { payload.date }

fun OakRequest.scheduledTime(): String = visit?.let { json ->
    sequenceOf("time", "scheduled_time")
        .map { json.optString(it) }
        .firstOrNull { it.isNotBlank() }
}.orEmpty().ifBlank { payload.time }

fun formatCreatedAt(value: String): String {
    if (value.isBlank()) return ""
    return try {
        val instant = OffsetDateTime.parse(value).toInstant()
        val zoned = instant.atZone(ZoneId.of("America/New_York"))
        DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a").format(zoned)
    } catch (_: Exception) {
        value
    }
}

fun formatDateOnly(value: String): String {
    if (value.isBlank()) return "Unscheduled"
    return try {
        val date = java.time.LocalDate.parse(value)
        DateTimeFormatter.ofPattern("EEE, MMM d").format(date)
    } catch (_: Exception) {
        value
    }
}
