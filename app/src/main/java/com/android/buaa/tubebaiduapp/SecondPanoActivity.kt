package com.android.buaa.tubebaiduapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.buaa.tubebaiduapp.classes.Node
import com.android.buaa.tubebaiduapp.classes.Tube
import com.android.buaa.tubebaiduapp.utils.ClientModel
import com.baidu.lbsapi.model.BaiduPanoData
import com.baidu.lbsapi.panoramaview.ImageMarker
import com.baidu.lbsapi.panoramaview.PanoramaRequest
import com.baidu.lbsapi.panoramaview.PanoramaView
import com.baidu.lbsapi.panoramaview.PanoramaViewListener
import com.baidu.lbsapi.tools.CoordinateConverter
import com.baidu.lbsapi.tools.Point
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.baidu.mapframework.nirvana.Utils.loge
import com.baidu.pano.platform.comjni.MessageProxy.*
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.*


class SecondPanoActivity : AppCompatActivity(), PanoramaViewListener{

    private lateinit var mCurrentPanoLatLng: LatLng
    private lateinit var mCurrentPanoPid: String
    private lateinit var nowLocationPid: String
    private lateinit var panoramaRequest: PanoramaRequest
    private val detectRadius: Double = 100.0
    private val TAG: String = "TTZZ"
    private lateinit var mPanoView: PanoramaView
    private lateinit var nowLocationMsg: MyLocationData
    private lateinit var mLocationClient: LocationClient
    private lateinit var locateBtn: Button
    private lateinit var beginBtn: Button
    private lateinit var cleanBtn: Button
    private val EARTH_RADIUS = 6371009.0

    private val tubeMap: MutableMap<Int, Tube> = HashMap()
    private val nodeMap: MutableMap<Tube, MutableMap<Node, Double>> = HashMap()

    private lateinit var mMapView: MapView
    private lateinit var mBaiduMap: BaiduMap

    // 服务器model
    private lateinit var clientModel: ClientModel

    private lateinit var mCurrentPointPlace: LatLng
    private lateinit var mTouchType: String
    private val mbitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_marka)
    private lateinit var mStateBar: TextView


    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_second_pano)

        val intent = intent
        mCurrentPanoLatLng = LatLng(intent.getDoubleExtra("lat", 0.0), intent.getDoubleExtra("lon", 0.0))

        // 初始化服务器的客户端model
        clientModel = ClientModel(assets)



        // 初始化全景请求器
        panoramaRequest = PanoramaRequest.getInstance(applicationContext)


        mPanoView = findViewById(R.id.panorama)
        mPanoView.setPanoramaViewListener(this)
//        val lat = 40.01205502490338
//        val lon = 116.35542987734043
        mPanoView.setPanorama(mCurrentPanoLatLng.longitude, mCurrentPanoLatLng.latitude)

//        mPanoView.setPanorama("0900220000150519082140960T5")

//        mPanoView.setPanorama("0100220000130817164838355J5")
        mPanoView.setPanoramaImageLevel(PanoramaView.ImageDefinition.ImageDefinitionHigh)

        // 开始定位
        initLocation()

        // 定位按钮
        locateBtn = findViewById(R.id.locateBtn)
        locateBtn.setOnClickListener {
            mPanoView.setPanorama(nowLocationMsg.longitude, nowLocationMsg.latitude)
        }

        // 清除按钮
        cleanBtn = findViewById(R.id.cleanBtn)
        cleanBtn.setOnClickListener {
            mPanoView.removeAllMarker()
        }

        // 开始恢复按钮
        beginBtn = findViewById(R.id.beginBtn)
        beginBtn.setOnClickListener {
            getNodeByDistance(LatLng(mCurrentPanoLatLng.latitude, mCurrentPanoLatLng.longitude), detectRadius)
        }
    }

    private fun getNodeByDistance(currentLatLng: LatLng, dist: Double) {
        val nodeParamsMap = HashMap<String, String>()
        nodeParamsMap["Latitude"] = currentLatLng.latitude.toString()
        nodeParamsMap["Longitude"] = currentLatLng.longitude.toString()
        nodeParamsMap["dist"] = dist.toString()
        val callNode: Call<ResponseBody> = clientModel.getNodeByDistance(nodeParamsMap)
        callNode.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Log.d(TAG, response.message())
                val responseStr: String = response.body()?.string() ?: ""
                Log.d(
                    TAG,
                    "-------------------------NODE MSG!!!!-------------------\n" +
                            responseStr +
                            "-------------------------MSG!!!!-------------------"
                )
                if(responseStr == "-1") return
                resolveNodes(responseStr)
                resolveMarkers()
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // 处理请求失败情况
            }
        })
    }

    private fun resolveNodes(responseStr: String) {
        val str = responseStr.trimIndent()

        // 将字符串按行切分
        val lines = str.split("\n")

        // 遍历每行，并将每行的字符串按逗号切分，得到整数数组
        val nodes = lines.map { line ->
            line.split(",")
        }

        // 打印结果，每个节点对应一个列表
        nodes.forEachIndexed { i, rowStr ->
            loge(TAG, "Node ${i + 1}: $rowStr")

            val index = rowStr[0].toInt()
            val tube = Tube()
            tube.tubeName = rowStr[1]
            tube.tubeMsg = rowStr[2]
            tube.tubeTypeStr = rowStr[3]
            val node = Node()
            node.setNodeIndex(rowStr[4].toInt())
                .setLatLng(LatLng(rowStr[5].toDouble(), rowStr[6].toDouble()))
                .setNodeMsg(rowStr[7])
                .setNodeType(rowStr[8])
            val dist = rowStr[9].toDouble()
            tubeMap[index] = tube
            if (nodeMap[tube] == null) nodeMap[tube] = mutableMapOf(node to dist)
            else nodeMap[tube]?.put(node, dist)
        }
    }

    private fun resolveMarkers() {
        for ((tube, mutableMap) in nodeMap) {
            loge(TAG, "$tube -> $mutableMap")
            for((node, _) in mutableMap){
                val marker = ImageMarker()
                marker.setMarkerPosition(Point(node.latLng.longitude, node.latLng.latitude))
//                marker.setMarkerHeight(2.3f)
                marker.setMarker(resources.getDrawable(R.drawable.icon_marka))
                marker.setOnTabMarkListener {
                    Toast.makeText(
                        this,
                        "标注已被点击", Toast.LENGTH_SHORT
                    ).show()
                }
                mPanoView.addMarker(marker);
            }
        }
    }

    /**
     * 计算两个经纬度点之间的距离
     * @param pointA LatLng
     * @param pointB LatLng
     * @return Double
     */
    fun haversineDistance(pointA: LatLng, pointB: LatLng): Double {
        val lat1 = Math.toRadians(pointA.latitude)
        val lon1 = Math.toRadians(pointA.longitude)
        val lat2 = Math.toRadians(pointB.latitude)
        val lon2 = Math.toRadians(pointB.longitude)
        val dLat = lat2 - lat1
        val dLon = lon2 - lon1
        val a = sin(dLat / 2).pow(2.0) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS * c
    }

    /**
     * 定位初始化
     */
    private fun initLocation() {
        LocationClient.setAgreePrivacy(true)
        try {
            // 定位初始化
            mLocationClient = LocationClient(applicationContext)
            val myListener = MyLocationListener()
            mLocationClient.registerLocationListener(myListener)
            val option = LocationClientOption()
            // 打开gps
            option.isOpenGps = true
            // 设置坐标类型
            option.setCoorType("bd09ll")
            option.setScanSpan(1000)
            mLocationClient.locOption = option
            mLocationClient.start()
        } catch (_: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
        mPanoView.onPause()
    }

    override fun onResume() {
        super.onResume()
        mPanoView.onResume()
    }

    override fun onDestroy() {
        mPanoView.destroy()
        super.onDestroy()
    }

    inner class MyLocationListener() : BDAbstractLocationListener() {
        override fun onReceiveLocation(p0: BDLocation?) {
            //mapView 销毁后不再处理新接收的位置
            if (p0 == null) {
                return
            }
            nowLocationMsg = MyLocationData.Builder()
                .accuracy(p0.radius) // 此处设置开发者获取到的方向信息，顺时针0-360
                .direction(p0.direction).latitude(p0.latitude)
                .longitude(p0.longitude).build()

            val mPanoDataWithLatLon: BaiduPanoData = panoramaRequest.getPanoramaInfoByLatLon(p0.longitude, p0.latitude)
            nowLocationPid = mPanoDataWithLatLon.pid
//            Log.e(TAG, "onReceiveLocation: ${p0.radius},${p0.direction},${p0.latitude},${p0.longitude}")
        }
    }

    override fun onDescriptionLoadEnd(json: String?) {
        Log.e(TAG, "onDescriptionLoadEnd: $json")
    }

    override fun onLoadPanoramaBegin() {
    }

    override fun onLoadPanoramaEnd(keyMsg: String?) {
        Log.e(TAG, "onLoadPanoramaEnd: $keyMsg")
        keyMsg?.let {
            val jsonObject = JSONObject(it)
//            val iterator = jsonObject.keys()
//            while (iterator.hasNext()){
//                val key = iterator.next()
//                Log.e(TAG, "onLoadPanoramaEnd: key: $key")
//            }

            mCurrentPanoPid = jsonObject.getString("ID")
            val x = jsonObject.getDouble("X")
            val y = jsonObject.getDouble("Y")
            val llPoint = CoordinateConverter.MCConverter2LL(x, y)
            mCurrentPanoLatLng = LatLng(llPoint.y, llPoint.x)
            Log.e(TAG, "onLoadPanoramaEnd: ${llPoint.x}, ${llPoint.y}")
        }
//        panoramaRequest.getPanoramaInfoByUid()
    }

    override fun onLoadPanoramaError(json: String?) {
        Log.e(TAG, "onLoadPanoramaError: $json")
    }

    override fun onMessage(msgName: String?, msgType: Int) {
//        Log.e(TAG, "onMessage: $msgName, $msgType")
    }

    override fun onCustomMarkerClick(key: String?) {
        Log.e(TAG, "onCustomMarkerClick: $key")
    }

    override fun onMoveStart() {
    }

    override fun onMoveEnd() {
    }
}