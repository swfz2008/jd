package com.example.network

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class QinglongApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun testConnection(url: String, clientId: String, clientSecret: String): Result<String> {
        return try {
            val tokenResult = getToken(url, clientId, clientSecret)
            if (tokenResult.isSuccess) {
                Result.success("连接成功！Token 获取成功")
            } else {
                Result.failure(tokenResult.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getToken(baseUrl: String, clientId: String, clientSecret: String): Result<Pair<String, String>> {
        val formattedUrl = formatUrl(baseUrl)
        val url = "$formattedUrl/open/auth/token?client_id=$clientId&client_secret=$clientSecret"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        
        return try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d("Qinglong", "Token Response: code=${response.code}, body=$bodyStr")
                if (!response.isSuccessful) {
                    return Result.failure(IOException("HTTP Error: ${response.code}"))
                }
                val json = JSONObject(bodyStr)
                val dataObj = json.optJSONObject("data")
                if (dataObj != null) {
                    val token = dataObj.optString("token", "")
                    val tokenType = dataObj.optString("token_type", "Bearer")
                    if (token.isNotEmpty()) {
                        Result.success(Pair(tokenType, token))
                    } else {
                        Result.failure(Exception("接口返回不含 token 字段"))
                    }
                } else {
                    Result.failure(Exception("接口返回不含 data 字段"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendCookie(
        baseUrl: String,
        tokenType: String,
        token: String,
        ptPin: String,
        cookieValue: String
    ): Result<String> {
        val formattedBaseUrl = formatUrl(baseUrl)
        val authHeader = "$tokenType $token"
        
        // 1. Get existing envs to check if pt_pin already exists
        // URL is: GET /open/envs?searchValue={pt_pin}
        val encodedPin = java.net.URLEncoder.encode(ptPin, "UTF-8")
        val getUrl = "$formattedBaseUrl/open/envs?searchValue=$encodedPin"
        
        val getRequest = Request.Builder()
            .url(getUrl)
            .header("Authorization", authHeader)
            .get()
            .build()
            
        return try {
            var existingEnv: JSONObject? = null
            client.newCall(getRequest).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d("Qinglong", "Get Env Response: code=${response.code}, body=$bodyStr")
                if (!response.isSuccessful) {
                    return Result.failure(IOException("查询环境变量失败, HTTP ${response.code}"))
                }
                
                val jsonObj = JSONObject(bodyStr)
                val dataArr = jsonObj.optJSONArray("data")
                if (dataArr != null) {
                    for (i in 0 until dataArr.length()) {
                        val env = dataArr.getJSONObject(i)
                        val name = env.optString("name")
                        val value = env.optString("value")
                        // Match either exact decoded or encoded pin inside JD_COOKIE value
                        if (name == "JD_COOKIE" && (value.contains("pt_pin=$ptPin") || value.contains("pt_pin=$encodedPin"))) {
                            existingEnv = env
                            break
                        }
                    }
                }
            }
            
            val mediaType = "application/json; charset=utf-8".toMediaType()
            if (existingEnv != null) {
                // Update environment variable
                val envId = existingEnv!!.optString("id").takeIf { it.isNotEmpty() } 
                    ?: existingEnv!!.optString("_id")
                    
                val remarks = existingEnv!!.optString("remarks", ptPin)
                
                val putBody = JSONObject().apply {
                    put("id", envId) // provide both key names for version compatibility
                    put("_id", envId)
                    put("name", "JD_COOKIE")
                    put("value", cookieValue)
                    put("remarks", remarks)
                }
                
                val putUrl = "$formattedBaseUrl/open/envs"
                val putRequest = Request.Builder()
                    .url(putUrl)
                    .header("Authorization", authHeader)
                    .put(putBody.toString().toRequestBody(mediaType))
                    .build()
                    
                client.newCall(putRequest).execute().use { response ->
                    val responseStr = response.body?.string() ?: ""
                    Log.d("Qinglong", "PUT Env Response: code=${response.code}, body=$responseStr")
                    if (!response.isSuccessful) {
                        return Result.failure(IOException("更新环境变量失败, HTTP ${response.code}"))
                    }
                }
                
                // Now enable it
                val enableUrl = "$formattedBaseUrl/open/envs/enable"
                val enableArray = JSONArray().apply {
                    val longId = envId.toLongOrNull()
                    if (longId != null) {
                        put(longId)
                    } else {
                        put(envId)
                    }
                }
                
                val enableRequest = Request.Builder()
                    .url(enableUrl)
                    .header("Authorization", authHeader)
                    .put(enableArray.toString().toRequestBody(mediaType))
                    .build()
                    
                client.newCall(enableRequest).execute().use { response ->
                    val resStr = response.body?.string() ?: ""
                    Log.d("Qinglong", "Enable Env Response: code=${response.code}, body=$resStr")
                    if (!response.isSuccessful) {
                        return Result.failure(IOException("启用环境变量失败, HTTP ${response.code}"))
                    }
                }
                
                Result.success("更新并启用成功")
            } else {
                // Create environment variable (POST expects Array)
                val postArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("name", "JD_COOKIE")
                        put("value", cookieValue)
                        put("remarks", ptPin)
                    })
                }
                
                val postUrl = "$formattedBaseUrl/open/envs"
                val postRequest = Request.Builder()
                    .url(postUrl)
                    .header("Authorization", authHeader)
                    .post(postArray.toString().toRequestBody(mediaType))
                    .build()
                    
                client.newCall(postRequest).execute().use { response ->
                    val responseStr = response.body?.string() ?: ""
                    Log.d("Qinglong", "POST Env Response: code=${response.code}, body=$responseStr")
                    if (!response.isSuccessful) {
                        return Result.failure(IOException("新增环境变量失败, HTTP ${response.code}"))
                    }
                }
                
                Result.success("新增并默认启用成功")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatUrl(url: String): String {
        var trimmed = url.trim()
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length - 1)
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "http://$trimmed"
        }
        return trimmed
    }
}
