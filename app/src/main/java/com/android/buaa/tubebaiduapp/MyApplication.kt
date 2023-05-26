package com.android.buaa.tubebaiduapp

import android.app.Application
import com.baidu.mapapi.CoordType
import com.baidu.mapapi.SDKInitializer

open class MyApplication : Application() {
    val TAG: String = "TTZZ"
    companion object{
        private var mInstance: MyApplication? = null

        fun getInstance(): MyApplication? {
            return mInstance
        }
    }

    override fun onCreate() {
        super.onCreate()
        mInstance = this
    }
}