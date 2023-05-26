package com.android.buaa.tubebaiduapp.utils


import android.content.res.AssetManager
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import java.io.IOException
import java.security.KeyManagementException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


class ClientModel(assets: AssetManager) {
    /*调试头*/
    private val TAG = "TTZZ"

    /*连接服务器域名*/ //    private Retrofit retrofit  = new Retrofit.Builder().baseUrl("http://116.63.227.32:10000/").build();  // 公网ip
    private var retrofit // 本地ip，需要调用ipconfig手动更新
            : Retrofit? = null

    /*service实例，用来向服务器通信的接口*/
    private var retrofitService: RetrofitService? = null

    init {
        val clientBuilder = OkHttpClient.Builder()

        // 添加自签名的SSL证书到信任证书列表中
        try {
            val inputStream = assets.open("server.crt")
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val certificate = certificateFactory.generateCertificate(inputStream) as X509Certificate

            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            keyStore.setCertificateEntry("mycert", certificate)

            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(keyStore)
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustManagerFactory.trustManagers, null)
            clientBuilder.sslSocketFactory(sslContext.socketFactory, trustManagerFactory.trustManagers[0] as X509TrustManager)
            retrofit = Retrofit.Builder().baseUrl("http://47.97.228.41:10000/").build() // 本地ip，需要调用ipconfig手动更新
            retrofitService = retrofit!!.create(RetrofitService::class.java)
        } catch (e: CertificateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: KeyStoreException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        }
    }

    fun postTube(paramMap: Map<String, String?>): Call<ResponseBody> {
        val partMap: MutableMap<String, RequestBody> = HashMap()
        partMap["Tube_name"] = RequestBody.create(MediaType.parse("text/plain"), paramMap["Tube_name"] ?: "")
        partMap["Tube_type"] = RequestBody.create(MediaType.parse("text/plain"), paramMap["Tube_type"] ?: "")
        partMap["Surrounding_message"] = RequestBody.create(MediaType.parse("text/plain"), paramMap["Surrounding_message"] ?: "")
        return retrofitService!!.uploadTube(partMap)
    }

    fun postNode(paramMap: Map<String, String?>): Call<ResponseBody> {
        val partMap: MutableMap<String, RequestBody> = HashMap()
        partMap["Node_index"] = RequestBody.create(MediaType.parse("text/plain"), paramMap["Node_index"] ?: "")
        partMap["Tube_id"] = RequestBody.create(MediaType.parse("text/plain"), paramMap["Tube_id"] ?: "")
        partMap["Latitude"] = RequestBody.create(MediaType.parse("text/plain"), paramMap["Latitude"] ?: "")
        partMap["Longitude"] = RequestBody.create(MediaType.parse("text/plain"), paramMap["Longitude"] ?: "")
        partMap["Altitude"] = RequestBody.create(MediaType.parse("text/plain"), paramMap["Altitude"] ?: "")
        partMap["Node_type"] = RequestBody.create(MediaType.parse("text/plain"), paramMap["Node_type"] ?: "")
        partMap["Surrounding_message"] = RequestBody.create(MediaType.parse("text/plain"), paramMap["Surrounding_message"] ?: "")
        return retrofitService!!.uploadNode(partMap)
    }

    fun getNodeByDistance(paramMap: Map<String, String?>): Call<ResponseBody>{
        val partMap: MutableMap<String, RequestBody> = HashMap()
        partMap["Latitude"] = RequestBody.create(MediaType.parse("text/plain"), paramMap["Latitude"] ?: "")
        partMap["Longitude"] = RequestBody.create(MediaType.parse("text/plain"), paramMap["Longitude"] ?: "")
        partMap["dist"] = RequestBody.create(MediaType.parse("text/plain"), paramMap["dist"] ?: "")
        return retrofitService!!.getNodeByDistance(partMap)
    }
}
