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
import com.github.poad.test.deviceflowexample.api.UserInfoApiClient
import com.github.poad.test.deviceflowexample.api.Client
import com.amazonaws.mobile.client.AWSMobileClient
import com.amplifyframework.core.Amplify
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
        config.useAmplify
        val token = this.intent.getStringExtra(
            if (config.useAmplify) { R.string.extra_key_access_token.toString() } else { R.string.extra_key_id_token.toString() })
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

        CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
            try {
                if (config.useAmplify) {
                    val cognitoAuthPlugin = Amplify.Auth.getPlugin("awsCognitoAuthPlugin")
                    val mobileClient = cognitoAuthPlugin.escapeHatch as AWSMobileClient
                    val userInfo = mobileClient.federatedSignIn(config.provider, token)

                    val name = userInfo.userState.name
                    userIndoModel.userInfo.postValue(name)
                } else {
                    val client = Client(config.apiEndpoint, UserInfoApiClient::class.java, token)
                        .createService()
                    val apiPath = config.userInfoApi
                    val userInfo = client.userInfo(apiPath)
                    val name = userInfo.username ?: userInfo.name
                    userIndoModel.userInfo.postValue(name)
                }
            } catch (e: Exception) {
                Log.e("[ERROR]", e.toString())
            }
        }
    }

}
