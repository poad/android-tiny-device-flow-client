package com.github.poad.test.deviceflowexample

import android.app.Application
import android.util.Log
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.plugin.Plugin

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        try {
            Amplify.addPlugin<Plugin<*>>(AWSCognitoAuthPlugin())
            Amplify.configure(applicationContext)
            Log.i("Device Flow Example", "Initialized Amplify")
        } catch (error: AmplifyException) {
            Log.e("Device Flow Example", "Could not initialize Amplify", error)
        }
    }
}
