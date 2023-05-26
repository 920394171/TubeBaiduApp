package com.android.buaa.tubebaiduapp.fragments

import android.content.DialogInterface
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.buaa.tubebaiduapp.R
import com.android.buaa.tubebaiduapp.classes.Node
import com.android.buaa.tubebaiduapp.classes.Tube
import com.android.buaa.tubebaiduapp.utils.ClientModel
import com.android.buaa.tubebaiduapp.viewModels.MapViewModel
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException


class MapAnnotationFragment : Fragment() {

    private var textureIndex: Int = 0
    private lateinit var adjustBtn: Button
    private lateinit var clientModel: ClientModel
    private lateinit var mCurrentPointPlace: LatLng
    private lateinit var mTouchType: String
    private val TAG = "TTZZ"
    private lateinit var mLocationClient: LocationClient

    private lateinit var mMapView: MapView
    private lateinit var locateBtn: Button
    private lateinit var beginBtn: Button
    private lateinit var cleanBtn: Button
    private lateinit var popBtn: Button
    private lateinit var mStateBar: TextView
    private lateinit var mBaiduMap: BaiduMap

    private var beginLatLng: LatLng? = null
    private var endLatLng: LatLng? = null

    //    private var selectedMarker: MarkerOptions? = null
    private var pointMarkers = mutableListOf<MarkerOptions>()
    private var polylineMarkers = mutableListOf<PolylineOptions>()

    private lateinit var nowLocationMsg: MyLocationData
    private val mbitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_marka)

    private var isRecording: Boolean = false

    private val allNodes = mutableListOf<Node>()
    private val firstNodesIndexOfTubes: ArrayList<Int> = ArrayList()
    private val polylinePoints: ArrayList<LatLng> = ArrayList()
    private val pointsForSinglePolyline: ArrayList<LatLng> = ArrayList()
    private val pointsForAllPolyline: ArrayList<ArrayList<LatLng>> = ArrayList()

    // 顺序节点latlng信息
//    private var overlaysArray: ArrayList<Overlay> = ArrayList()

    // 管道tube和节点node列表
    private var nodes: ArrayList<Node> = ArrayList()
    private var tube: Tube = Tube()
    private var autoTubeId = 0

    companion object {
        fun newInstance() = MapAnnotationFragment()
    }

    private lateinit var viewModel: MapViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_map_annotation, container, false)

        //获取地图控件引用
        mMapView = view.findViewById(R.id.bmapView)
        mBaiduMap = mMapView.map
        mStateBar = view.findViewById(R.id.state)

        viewModel = ViewModelProvider(requireActivity())[MapViewModel::class.java]
        clientModel = viewModel.clientModel

        viewModel.initMapSetting(mBaiduMap, mMapView)
        initListener()
        beginToLocate()

        // map刷新
        viewModel.refreshMapAnnotationLiveData.observe(viewLifecycleOwner) {
            mBaiduMap.clear()
            pointMarkers.forEach { mBaiduMap.addOverlay(it) }
            polylineMarkers.forEach { mBaiduMap.addOverlay(it) }
            val stringBuilder = StringBuilder()
            for (i in 0 until nodes.size) {
                stringBuilder.append("lat:").append(nodes[i].latLng.latitude).append("--lng:").append(nodes[i].latLng.longitude).append("\n")
            }
            Log.e(TAG, "onCreateView: markers={${stringBuilder.toString()}}")
        }

        // 设置marker点击事件
        mBaiduMap.setOnMarkerClickListener { marker ->
            if (viewModel.isAdjustingMarkerLiveData.value == true) {
                viewModel.selectedMarkerLiveData.value = pointMarkers.find { it.position == marker.position }
                viewModel.selectedNodeLiveData.value = nodes.find { it.latLng == marker.position }

                viewModel.selectedPolylineMarkerLiveData.value = polylineMarkers.find { polyline ->
                    (polyline.points.find { it == marker.position })?.let { it ->
                        viewModel.selectedPolylineNodeIndexLiveData.value = polyline.points.indexOf(it)
                    }
                    polyline.points.find { it == marker.position } != null
                }
                return@setOnMarkerClickListener true
            }
            false
        }

        // 定位按钮
        locateBtn = view.findViewById(R.id.locateBtn)
        locateBtn.setOnClickListener {
            mBaiduMap.setMyLocationData(nowLocationMsg)
            Log.e(TAG, "onReceiveLocation: ${mBaiduMap.locationData.latitude}, ${mBaiduMap.locationData.longitude}")
            val currentLatLng = LatLng(nowLocationMsg.latitude, nowLocationMsg.longitude)
            val builder: MapStatus.Builder = MapStatus.Builder()
            builder.target(currentLatLng).zoom(21.0f)
            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()))
        }

        // 清空按钮
        cleanBtn = view.findViewById(R.id.cleanBtn)
        cleanBtn.setOnClickListener {
            mBaiduMap.clear()
            pointMarkers.clear()
            nodes.clear()
            beginLatLng = null
            endLatLng = null
        }

        // pop按钮
        popBtn = view.findViewById(R.id.popBtn)
        popBtn.setOnClickListener {
            popOoaArray()
        }

        // 开始记录节点按钮
        beginBtn = view.findViewById(R.id.beginBtn)
        beginBtn.setOnClickListener {
            isRecording = !isRecording
            if (isRecording) {
                beginBtn.text = resources.getString(R.string.BUTTON_RECORDING)
                if (nodes.isNotEmpty()) {
                    nodes.clear()
                }
                tube.clear()
                pointsForSinglePolyline.clear()
                pointsForAllPolyline.add(ArrayList())
                showTubeInputDialog()
            } else {
                beginBtn.text = resources.getString(R.string.BUTTON_END_RECORDING)
                try {
                    uploadAnchors()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                updatePolyline()
                refreshTextureIndexByMod100Add()
                endLatLng = null
                beginLatLng = null
            }
        }

        // 调整按钮
        adjustBtn = view.findViewById(R.id.adjustBtn)
        adjustBtn.setOnClickListener {
            viewModel.isAdjustingMarkerLiveData.value = !viewModel.isAdjustingMarkerLiveData.value!!

            if (viewModel.isAdjustingMarkerLiveData.value == true) {
                adjustBtn.text = "finish"
                // 调整marker位置

            } else {
                adjustBtn.text = "adjust"
                // 结束调整marker

            }
        }

        return view
    }


    private fun showNodeInputDialog(node: Node) {
        requireActivity().runOnUiThread {

            // 创建AlertDialog.Builder对象
            val builder = AlertDialog.Builder(requireActivity())
            builder.setTitle("请输入当前节点信息：")

            // 定义下拉框选项
            val items = arrayOf("起始节点", "结束节点", "中间节点")

            // 创建下拉框
            val spinner = Spinner(requireActivity())
            val adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, items)
            spinner.adapter = adapter

            // 创建输入框
            val editText = EditText(requireActivity())
            editText.hint = "周边环境描述"

            // 创建布局容器
            val container = LinearLayout(requireActivity())
            container.orientation = LinearLayout.VERTICAL
            container.addView(spinner)
            container.addView(editText)

            // 设置AlertDialog的内容视图
            builder.setView(container)

            // 设置完成按钮
            builder.setPositiveButton("完成") { dialog, which ->
                node.setNodeType(spinner.selectedItem.toString())
                Log.e(TAG, "showNodeInputDialog: nodeType:${spinner.selectedItem.toString()}")
                node.nodeMsg = editText.text.toString()
                Toast.makeText(requireActivity().applicationContext, "提交成功！", Toast.LENGTH_SHORT).show()
            }

            // 设置取消按钮
            builder.setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }

            // 显示AlertDialog
            builder.show()
        }
    }

    private fun showTubeInputDialog() {
        requireActivity().runOnUiThread {

            // 创建AlertDialog.Builder对象
            val builder = AlertDialog.Builder(requireActivity())
            builder.setTitle("请输入当前地标信息：")

            // 创建输入框
            val tubeName = EditText(requireActivity())
            tubeName.hint = "地标名称"

            // 创建输入框
            val tubeType = EditText(requireActivity())
            tubeType.hint = "地标类型"

            // 创建输入框
            val tubeMsg = EditText(requireActivity())
            tubeMsg.hint = "地标描述"

            // 创建布局容器
            val container = LinearLayout(requireActivity())
            container.orientation = LinearLayout.VERTICAL
            container.addView(tubeName)
            container.addView(tubeType)
            container.addView(tubeMsg)

            // 设置AlertDialog的内容视图
            builder.setView(container)

            // 设置完成按钮
            builder.setPositiveButton("完成") { dialog: DialogInterface?, which: Int ->
                tube.tubeName = tubeName.text.toString()
                tube.tubeMsg = tubeMsg.text.toString()
                tube.tubeTypeStr = tubeType.text.toString()
                Toast.makeText(requireActivity().applicationContext, "提交成功！", Toast.LENGTH_SHORT).show()
            }

            // 设置取消按钮
            builder.setNegativeButton("取消") { dialog: DialogInterface, which: Int -> dialog.dismiss() }

            // 显示AlertDialog
            builder.show()
        }
    }

    @Throws(IOException::class)
    private fun uploadAnchors() {
        // 先传tube，得到autoTubeId，再传nodes
        val tubeParamsMap = HashMap<String, String?>()
        tubeParamsMap["Tube_name"] = tube.tubeName
        tubeParamsMap["Tube_type"] = tube.tubeTypeStr
        tubeParamsMap["Surrounding_message"] = tube.tubeMsg
        Log.e(TAG, "new PostTubeTask().execute(tubeParamsMap);")
        val call = clientModel.postTube(tubeParamsMap)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Log.d(TAG, response.message())
                val responseStr: String
                try {
                    responseStr = response.body()?.string() ?: ""
                    Log.d(
                        TAG,
                        "-------------------------TUBE MSG!!!!-------------------\n" +
                                responseStr +
                                "-------------------------MSG!!!!-------------------"
                    )
                    autoTubeId = responseStr.toInt()
                    Log.d(TAG, "autoTubeId = $autoTubeId")

                    // 传nodes
                    uploadNodes()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.d(TAG, "onFailure: MSG Failure!")
            }
        })
//        new PostTubeTask().execute(tubeParamsMap);
    }

    private fun uploadNodes() {
        for (node: Node in nodes) {
            val nodeParamsMap = HashMap<String, String>()
            nodeParamsMap["Node_index"] = nodes.indexOf(node).toString()
            nodeParamsMap["Tube_id"] = autoTubeId.toString()
            nodeParamsMap["Latitude"] = node.latLng.latitude.toString()
            nodeParamsMap["Longitude"] = node.latLng.longitude.toString()
            nodeParamsMap["Altitude"] = node.altitude.toString()
            nodeParamsMap["Node_type"] = node.nodeType.toString()
            nodeParamsMap["Surrounding_message"] = node.nodeMsg ?: ""
            val callNode: Call<ResponseBody> = clientModel.postNode(nodeParamsMap)
            callNode.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.d(TAG, response.message())
                    response.body()?.let {
                        Log.d(
                            TAG,
                            "-------------------------NODE MSG!!!!-------------------\n" +
                                    it.string() +
                                    "-------------------------MSG!!!!-------------------"
                        )
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    // 处理请求失败情况
                }
            })
        }
    }

    private fun popOoaArray() {
        if (pointMarkers.isNotEmpty()) {
            pointMarkers.removeAt(pointMarkers.size - 1)
            refreshMapMarkers()
//            mBaiduMap.removeOverLays(arrayListOf(overlaysArray.removeAt(overlaysArray.size - 1)))
        }
        if (nodes.isNotEmpty()) {
            nodes.removeAt(nodes.size - 1)
        }
    }

    private fun refreshMapMarkers() {
        mBaiduMap.clear()
        pointMarkers.forEach { mBaiduMap.addOverlay(it) }
    }

    /**
     * 更新地图状态显示面板
     */
    private fun updateMapState() {
        if (!isRecording) return
        if (endLatLng == null) {
            endLatLng = mCurrentPointPlace
        } else {
            beginLatLng = endLatLng
            endLatLng = mCurrentPointPlace
        }

        pointsForSinglePolyline.add(mCurrentPointPlace)
        pointsForAllPolyline[pointsForAllPolyline.size - 1].add(mCurrentPointPlace)

        val state: String = String.format("$mTouchType,当前经度： %f 当前纬度：%f", mCurrentPointPlace.longitude, mCurrentPointPlace.latitude)
        val ooA = MarkerOptions().position(mCurrentPointPlace).icon(mbitmap).draggable(true)
        pointMarkers.add(ooA)
        mBaiduMap.addOverlay(ooA)
        mStateBar.text = state


        val node = Node()
        showNodeInputDialog(node)
        node.latLng = mCurrentPointPlace
        node.altitude = 0.0
        node.nodeIndex = nodes.size + 1
        nodes.add(node)
    }

    private fun updatePolyline(){
        //构建折线点坐标
        val points: MutableList<LatLng> = ArrayList()
        pointsForSinglePolyline.forEach { points.add(it) }

        //添加纹理图片
        val textureList: MutableList<BitmapDescriptor> = ArrayList()
        val goAheadTextureBitmap = BitmapFactory.decodeResource(resources, R.drawable.goahead)
        val goAheadTexture = BitmapDescriptorFactory.fromBitmap(goAheadTextureBitmap)
        textureList.add(goAheadTexture)

        //添加纹理索引
        val indexList: MutableList<Int> = ArrayList()
        indexList.add(textureIndex)


        //设置折线的属性
        val mOverlayOptions: PolylineOptions = PolylineOptions()
            .width(20)
            .dottedLine(true)
            .points(points)
//                .customTextureList(textureList)
//                .textureIndex(indexList) //设置纹理列表
        polylineMarkers.add(mOverlayOptions)

        //在地图上绘制折线
        //mPloyline 折线对象
        val mPolyline = mBaiduMap.addOverlay(mOverlayOptions)
    }



    /**
     * 对地图事件的消息响应
     */
    private fun initListener() {
        mBaiduMap.setOnMapClickListener(object : BaiduMap.OnMapClickListener {
            /**
             * 单击地图
             */
            override fun onMapClick(point: LatLng) {
                if (!viewModel.isAdjustingMarkerLiveData.value!!) {
                    mTouchType = "单击地图"
                    mCurrentPointPlace = point
                    updateMapState()
                }
            }

            /**
             * 单击地图中的POI点
             */
            override fun onMapPoiClick(poi: MapPoi) {
//                mTouchType = "单击POI点"
//                mCurrentPointPlace = poi.position
//                updateMapState()
            }
        })

//        mBaiduMap.setOnMapLongClickListener { point ->
//
//            /**
//             * 长按地图
//             */
//            mTouchType = "长按"
//            mCurrentPointPlace = point
//            updateMapState()
//        }
//
//        mBaiduMap.setOnMapDoubleClickListener { point ->
//
//            /**
//             * 双击地图
//             */
//            mTouchType = "双击"
//            mCurrentPointPlace = point
//            updateMapState()
//        }
    }


    private fun beginToLocate() {
        LocationClient.setAgreePrivacy(true)
        //定位初始化
        mLocationClient = LocationClient(requireContext().applicationContext)


        //通过LocationClientOption设置LocationClient相关参数
        val option = LocationClientOption()
        option.isOpenGps = true // 打开gps
        option.setCoorType("bd09ll") // 设置坐标类型
        option.setScanSpan(1000)


        //设置locationClientOption
        mLocationClient.locOption = option

        //注册LocationListener监听器
        val myLocationListener = MyLocationListener()
        mLocationClient.registerLocationListener(myLocationListener)

        //开启地图定位图层
        mLocationClient.start()
    }

    fun refreshTextureIndexByMod100Add() {
        textureIndex = (textureIndex + 1) % 100
    }

    override fun onResume() {
        super.onResume()
        mMapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mMapView.onPause()
    }

    override fun onDestroy() {
        mLocationClient.stop()
        mBaiduMap.isMyLocationEnabled = false
        mMapView.onDestroy()
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
            Log.e(TAG, "onReceiveLocation: ${p0.radius},${p0.direction},${p0.latitude},${p0.longitude}")
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

}