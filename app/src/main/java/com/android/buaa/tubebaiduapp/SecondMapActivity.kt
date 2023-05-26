package com.android.buaa.tubebaiduapp

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.buaa.tubebaiduapp.classes.Node
import com.android.buaa.tubebaiduapp.classes.Tube
import com.android.buaa.tubebaiduapp.utils.ClientModel
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.baidu.mapapi.map.*
import com.baidu.mapapi.map.BaiduMap.OnMapClickListener
import com.baidu.mapapi.model.LatLng
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.*


class SecondMapActivity : AppCompatActivity() {
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

    private lateinit var nowLocationMsg: MyLocationData
    private val mbitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_marka)

    private var isRecording:Boolean = false

    // 顺序节点latlng信息
    private var overlaysArray: ArrayList<Overlay> = ArrayList()

    // 管道tube和节点node列表
    private var nodes: ArrayList<Node> = ArrayList()
    private var tube: Tube = Tube()
    private var autoTubeId = 0

    // 服务器model
    private lateinit var clientModel: ClientModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second_map)

        //获取地图控件引用
        mMapView = findViewById(R.id.bmapView)
        mBaiduMap = mMapView.map
        mStateBar = findViewById(R.id.state)

        // 初始化服务器的客户端model
        clientModel = ClientModel(assets)


        initMapSetting()
        initListener()
        beginToLocate()

        // 定位按钮
        locateBtn = findViewById(R.id.locateBtn)
        locateBtn.setOnClickListener {
            mBaiduMap.setMyLocationData(nowLocationMsg)
            Log.e(TAG, "onReceiveLocation: ${mBaiduMap.locationData.latitude}, ${mBaiduMap.locationData.longitude}")
            val currentLatLng = LatLng(nowLocationMsg.latitude, nowLocationMsg.longitude)
            val builder: MapStatus.Builder = MapStatus.Builder()
            builder.target(currentLatLng).zoom(21.0f)
            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()))
        }

        // 清空按钮
        cleanBtn = findViewById(R.id.cleanBtn)
        cleanBtn.setOnClickListener {
            mBaiduMap.clear()
            overlaysArray.clear()
            nodes.clear()
        }

        popBtn = findViewById(R.id.popBtn)
        popBtn.setOnClickListener {
            popOoaArray()
        }

        // 开始记录节点按钮
        beginBtn = findViewById(R.id.beginBtn)
        beginBtn.setOnClickListener {
            isRecording = !isRecording
            if (isRecording) {
                beginBtn.text = resources.getString(R.string.BUTTON_RECORDING)
                if (nodes.isNotEmpty()) {
                    nodes.clear()
                }
                tube.clear()
                showTubeInputDialog()
            } else {
                beginBtn.text = resources.getString(R.string.BUTTON_END_RECORDING)
                try {
                    uploadAnchors()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showNodeInputDialog(node: Node) {
        runOnUiThread {

            // 创建AlertDialog.Builder对象
            val builder = AlertDialog.Builder(this)
            builder.setTitle("请输入当前节点信息：")

            // 定义下拉框选项
            val items = arrayOf("起始节点", "结束节点", "中间节点")

            // 创建下拉框
            val spinner = Spinner(this)
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
            spinner.adapter = adapter

            // 创建输入框
            val editText = EditText(this)
            editText.hint = "周边环境描述"

            // 创建布局容器
            val container = LinearLayout(this)
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
                Toast.makeText(applicationContext, "提交成功！", Toast.LENGTH_SHORT).show()
            }

            // 设置取消按钮
            builder.setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }

            // 显示AlertDialog
            builder.show()
        }
    }

    private fun showTubeInputDialog() {
        runOnUiThread {

            // 创建AlertDialog.Builder对象
            val builder = AlertDialog.Builder(this)
            builder.setTitle("请输入当前地标信息：")

            // 创建输入框
            val tubeName = EditText(this)
            tubeName.hint = "地标名称"

            // 创建输入框
            val tubeType = EditText(this)
            tubeType.hint = "地标类型"

            // 创建输入框
            val tubeMsg = EditText(this)
            tubeMsg.hint = "地标描述"

            // 创建布局容器
            val container = LinearLayout(this)
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
                Toast.makeText(applicationContext, "提交成功！", Toast.LENGTH_SHORT).show()
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
        if (overlaysArray.isNotEmpty()) {
            mBaiduMap.removeOverLays(arrayListOf(overlaysArray.removeAt(overlaysArray.size - 1)))
        }
        if(nodes.isNotEmpty()){
            nodes.removeAt(nodes.size - 1)
        }
    }

    /**
     * 更新地图状态显示面板
     */
    private fun updateMapState() {
        val state: String = String.format("$mTouchType,当前经度： %f 当前纬度：%f", mCurrentPointPlace.longitude, mCurrentPointPlace.latitude)
        val ooA = MarkerOptions().position(mCurrentPointPlace).icon(mbitmap).draggable(true)
        overlaysArray.add(mBaiduMap.addOverlay(ooA))
        mStateBar.text = state


        val node = Node()
        showNodeInputDialog(node)
        node.latLng = mCurrentPointPlace
        node.altitude = 0.0
        node.nodeIndex = nodes.size + 1
        nodes.add(node)

    }

    /**
     * 对地图事件的消息响应
     */
    private fun initListener() {
        mBaiduMap.setOnMapClickListener(object : OnMapClickListener {
            /**
             * 单击地图
             */
            override fun onMapClick(point: LatLng) {
                mTouchType = "单击地图"
                mCurrentPointPlace = point
                updateMapState()
            }

            /**
             * 单击地图中的POI点
             */
            override fun onMapPoiClick(poi: MapPoi) {
                mTouchType = "单击POI点"
                mCurrentPointPlace = poi.position
                updateMapState()
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

    /**
     * 定位初始化
     */
    private fun initMapSetting() {
        // 修改地图类型为卫星定位
        mBaiduMap.mapType = BaiduMap.MAP_TYPE_SATELLITE
        // 修改比例尺为最大21
        val builder: MapStatus.Builder = MapStatus.Builder()
        builder.zoom(21.0f)
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()))
        // 开启定位图层
        mBaiduMap.isMyLocationEnabled = true
        //实例化UiSettings类对象
        val mUiSettings: UiSettings = mBaiduMap.uiSettings
        //通过设置enable为true或false 选择是否显示指南针
        mUiSettings.isCompassEnabled = true
        //通过设置enable为true或false 选择是否显示缩放按钮
        mMapView.showZoomControls(false)
    }

    private fun beginToLocate() {
        LocationClient.setAgreePrivacy(true)
        //定位初始化
        mLocationClient = LocationClient(applicationContext)


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
//            Log.e(TAG, "onReceiveLocation: ${p0.radius},${p0.direction},${p0.latitude},${p0.longitude}")
        }
    }
}