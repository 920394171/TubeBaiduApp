package com.android.buaa.tubebaiduapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.buaa.tubebaiduapp.R
import com.android.buaa.tubebaiduapp.classes.Node
import com.android.buaa.tubebaiduapp.classes.Tube
import com.android.buaa.tubebaiduapp.utils.ClientModel
import com.android.buaa.tubebaiduapp.viewModels.MapViewModel
import com.baidu.lbsapi.panoramaview.ImageMarker
import com.baidu.lbsapi.panoramaview.PanoramaRequest
import com.baidu.lbsapi.panoramaview.PanoramaView
import com.baidu.lbsapi.panoramaview.PanoramaViewListener
import com.baidu.lbsapi.tools.CoordinateConverter
import com.baidu.lbsapi.tools.Point
import com.baidu.location.LocationClient
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.BitmapDescriptorFactory
import com.baidu.mapapi.map.MapView
import com.baidu.mapapi.map.MyLocationData
import com.baidu.mapapi.model.LatLng
import com.baidu.mapframework.nirvana.Utils
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PanoLocateFragment : Fragment(), PanoramaViewListener {
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


    companion object {
        private var instance: PanoLocateFragment? = null

        fun newInstance(): PanoLocateFragment {
            return instance ?: synchronized(this) {
                instance ?: PanoLocateFragment().also { instance = it }
            }
        }
    }

    private lateinit var viewModel: MapViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view: View = inflater.inflate(R.layout.fragment_pano_locate, container, false)


        return view
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
                if (responseStr == "-1") return
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
            Utils.loge(TAG, "Node ${i + 1}: $rowStr")

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
            Utils.loge(TAG, "$tube -> $mutableMap")
            for ((node, _) in mutableMap) {
                val marker = ImageMarker()
                marker.setMarkerPosition(Point(node.latLng.longitude, node.latLng.latitude))
//                marker.setMarkerHeight(2.3f)
                marker.setMarker(resources.getDrawable(R.drawable.icon_marka))
                marker.setOnTabMarkListener {
                    Toast.makeText(
                        requireActivity(),
                        "标注已被点击", Toast.LENGTH_SHORT
                    ).show()
                }
                mPanoView.addMarker(marker);
            }
        }
    }

    /**
     * 定位初始化
     */
//    private fun initLocation() {
//        LocationClient.setAgreePrivacy(true)
//        try {
//            // 定位初始化
//            mLocationClient = LocationClient(requireActivity().applicationContext)
//            val myListener = MyLocationListener()
//            mLocationClient.registerLocationListener(myListener)
//            val option = LocationClientOption()
//            // 打开gps
//            option.isOpenGps = true
//            // 设置坐标类型
//            option.setCoorType("bd09ll")
//            option.setScanSpan(1000)
//            mLocationClient.locOption = option
//            mLocationClient.start()
//        } catch (_: Exception) {
//        }
//    }

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

//    inner class MyLocationListener() : BDAbstractLocationListener() {
//        override fun onReceiveLocation(p0: BDLocation?) {
//            //mapView 销毁后不再处理新接收的位置
//            if (p0 == null) {
//                return
//            }
//            nowLocationMsg = MyLocationData.Builder()
//                .accuracy(p0.radius) // 此处设置开发者获取到的方向信息，顺时针0-360
//                .direction(p0.direction).latitude(p0.latitude)
//                .longitude(p0.longitude).build()
////            Log.e(TAG, "onReceiveLocation: ${p0.radius},${p0.direction},${p0.latitude},${p0.longitude}")
//        }
//    }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[MapViewModel::class.java]
        clientModel = viewModel.clientModel

//        val intent = intent
        mCurrentPanoLatLng = LatLng(40.0, 116.0)

        // 初始化全景请求器
        panoramaRequest = PanoramaRequest.getInstance(requireActivity().applicationContext)


        mPanoView = requireView().findViewById(R.id.panorama)
        mPanoView.setPanoramaViewListener(this)
//        val lat = 40.01205502490338
//        val lon = 116.35542987734043
        mPanoView.setPanorama(mCurrentPanoLatLng.longitude, mCurrentPanoLatLng.latitude)

//        mPanoView.setPanorama("0900220000150519082140960T5")

//        mPanoView.setPanorama("0100220000130817164838355J5")
        mPanoView.setPanoramaImageLevel(PanoramaView.ImageDefinition.ImageDefinitionHigh)

        // 开始定位
//        initLocation()

        // 定位按钮
        locateBtn = requireView().findViewById(R.id.locateBtn)
        locateBtn.setOnClickListener {
            mPanoView.setPanorama(nowLocationMsg.longitude, nowLocationMsg.latitude)
        }

        // 清除按钮
        cleanBtn = requireView().findViewById(R.id.cleanBtn)
        cleanBtn.setOnClickListener {
            mPanoView.removeAllMarker()
        }

        // 开始恢复按钮
        beginBtn = requireView().findViewById(R.id.beginBtn)
        beginBtn.setOnClickListener {
            getNodeByDistance(LatLng(mCurrentPanoLatLng.latitude, mCurrentPanoLatLng.longitude), detectRadius)
        }

        viewModel.nowLocMsgLiveData.value?.let { nowLocationMsg = it }

        viewModel.nowLocMsgLiveData.observe(viewLifecycleOwner) {
            viewModel.nowLocMsgLiveData.value?.let { nowLocationMsg = it }
        }

        viewModel.needLocatePanoLiveData.observe(viewLifecycleOwner) {
            viewModel.nowLocMsgLiveData.value?.let {
                mPanoView.setPanorama(it.longitude, it.latitude)
                Log.e(TAG, "onActivityCreated: setPanorama! ${it.longitude} --- ${it.latitude}")
            }
        }
    }

}