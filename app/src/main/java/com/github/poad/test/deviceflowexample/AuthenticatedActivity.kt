package com.github.poad.test.deviceflowexample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.FederatedSignInOptions
import com.amplifyframework.core.Amplify
import com.github.poad.test.deviceflowexample.api.UserInfoApiClient
import com.github.poad.test.deviceflowexample.api.Client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class UserInfoViewModel : ViewModel() {
    internal val userInfo: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
}

class AuthenticatedActivity : AppCompatActivity() {
    private val userIndoModel by lazy {
        ViewModelProvider(this).get(UserInfoViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authenticated)

        val config = OAuthConfig.load(resources)
        val token = if (config.useAmplify) {
            this.intent.getStringExtra(R.string.extra_key_id_token.toString())
        } else {
            this.intent.getStringExtra(R.string.extra_key_access_token.toString())
        }
        if (token == null) {
            startActivity(
                Intent(
                    applicationContext,
                    MainActivity::class.java
                )
            )
            return
        }
        val userInfoObserver = Observer<String> { userInfo ->
            val userInfoTextView = findViewById<TextView>(R.id.userInfo)
            userInfoTextView.text = userInfo
            userInfoTextView.visibility = View.VISIBLE
        }
        userIndoModel.userInfo.observe(this, userInfoObserver)

        Log.i("token", token)

        CoroutineScope(Dispatchers.Main + SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (config.useAmplify) {
                    // not working https://github.com/aws-amplify/aws-sdk-android/issues/1484#issuecomment-589772131
                    val cognitoAuthPlugin = Amplify.Auth.getPlugin("awsCognitoAuthPlugin")
                    val mobileClient = cognitoAuthPlugin.escapeHatch as AWSMobileClient

                    val idPoolId = mobileClient.configuration
                        ?.let {
                            it.optJsonObject("CredentialsProvider")
                                ?.let { credentialsProvider ->
                                    credentialsProvider.optJSONObject("CognitoIdentity")
                                        ?.let { cognitoIdentity ->
                                            cognitoIdentity.optJSONObject("Default")
                                                ?.optString("PoolId")
                                        }
                                }
                        }

                    val options = FederatedSignInOptions.builder()

                    val userInfo = mobileClient.federatedSignIn(
                        mobileClient.configuration
                            ?.let {
                                it.optJsonObject("Auth")
                                    ?.let { auth ->
                                        auth.optJSONObject("Default")
                                            ?.let { authDefault ->
                                                authDefault.optJSONObject("OAuth")
                                                    ?.optString("IdentityProvider")
                                            }
                                    }
                            } ?: config.provider,
                        token,
                        if (idPoolId != null) {
                            options.cognitoIdentityId(idPoolId)
                        } else {
                            options
                        }.build()
                    )

                    val name = userInfo.userState.name
                    userIndoModel.userInfo.postValue(name)
                } else {
                    val client = Client(config.apiEndpoint, UserInfoApiClient::class.java, token)
                        .createService()
                    val apiPath = config.userInfoApi
                    val userInfo = if (config.httpGet) {
                        client.userInfoGet(apiPath)
                    } else {
                        client.userInfoPost(apiPath)
                    }
                    val name = userInfo.username ?: userInfo.name
                    userIndoModel.userInfo.postValue(name)
                }
            } catch (e: Exception) {
                Log.e("[ERROR]", "", e)
            }
        }
    }

}
