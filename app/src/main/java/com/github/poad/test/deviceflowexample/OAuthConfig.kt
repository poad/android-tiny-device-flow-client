package com.github.poad.test.deviceflowexample

import android.content.res.Resources
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader


data class OAuthConfig(
    val clientId: String,
    val endpoint: String,
    val deviceCodePath: String,
    val tokenPath: String,
    val scope: String,
    val apiEndpoint: String,
    val userInfoApi: String,
    val provider: String,
    val audience: String,
    val useAmplify: Boolean
) {
    companion object {
        fun load(resources: Resources): OAuthConfig {
            val config = JSONObject(BufferedReader(InputStreamReader(resources.openRawResource(R.raw.oauth))).readText())
            return OAuthConfig(
                config.getString("client_id"),
                config.getString("endpoint"),
                config.getString("device_code_path"),
                config.getString("token_path"),
                config.getString("scope"),
                config.getString("api_endpoint"),
                config.getString("user_info_api"),
                config.getString("provider"),
                when (config.has("audience")) {
                    true -> config.getString("audience")
                    else -> ""
                },
                when (config.has("use_amplify")) {
                    true -> config.getBoolean("use_amplify")
                    else -> false
                }
            )
        }
    }
}
