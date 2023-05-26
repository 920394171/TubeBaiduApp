package com.android.buaa.tubebaiduapp.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.buaa.tubebaiduapp.R
import com.android.buaa.tubebaiduapp.SecondPanoActivity
import com.android.buaa.tubebaiduapp.utils.ClientModel
import com.android.buaa.tubebaiduapp.utils.OnSendPointListener
import com.android.buaa.tubebaiduapp.viewModels.MapViewModel
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng

class MapLocateFragment : Fragment() {

    private lateinit var mCurrentPointPlace: LatLng
    private lateinit var mTouchType: String
    private val TAG = "TTZZ"
    private lateinit var mLocationClient: LocationClient

    private lateinit var mMapView: MapView
    private lateinit var locateBtn: Button
    private lateinit var beginBtn: Button
    private lateinit var cleanBtn: Button
    private lateinit var mStateBar: TextView
    private lateinit var mBaiduMap: BaiduMap

    private lateinit var nowLocationMsg: MyLocationData
    private val mbitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_marka)

    private var isRecording: Boolean = false

    // 顺序节点latlng信息
    private var overlaysArray: ArrayList<MarkerOptions> = ArrayList()

    // 服务器model
    private lateinit var clientModel: ClientModel

    companion object {
        private var instance: MapLocateFragment? = null

        fun newInstance(): MapLocateFragment {
            return instance ?: synchronized(this) {
                instance ?: MapLocateFragment().also { instance = it }
            }
        }
    }

    // ViewModel对象
    private lateinit var viewModel: MapViewModel

    // 切换fragment的监听器
    private lateinit var onSendPointListener: OnSendPointListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map_locate, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // 初始化onSendPointListener
        if (context is OnSendPointListener) {
            onSendPointListener = context
        } else {
            throw RuntimeException("$context must implement OnSendPointListener")
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MapViewModel::class.java]
        clientModel = viewModel.clientModel

        //获取地图控件引用
        mMapView = requireView().findViewById(R.id.bmapView)
        mBaiduMap = mMapView.map
        mStateBar = requireView().findViewById(R.id.state)

        initMapSetting()
        initListener()
        beginToLocate()

        // 定位按钮
        locateBtn = requireView().findViewById(R.id.locateBtn)
        locateBtn.setOnClickListener {
            mBaiduMap.setMyLocationData(nowLocationMsg)
            Log.e(TAG, "onReceiveLocation: ${mBaiduMap.locationData.latitude}, ${mBaiduMap.locationData.longitude}")
            val currentLatLng = LatLng(nowLocationMsg.latitude, nowLocationMsg.longitude)
            val builder: MapStatus.Builder = MapStatus.Builder()
            builder.target(currentLatLng).zoom(21.0f)
            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()))
        }

        // 清空按钮
        cleanBtn = requireView().findViewById(R.id.cleanBtn)
        cleanBtn.setOnClickListener {
            mBaiduMap.clear()
            overlaysArray.clear()
        }

        // 开始记录节点按钮
        beginBtn = requireView().findViewById(R.id.beginBtn)
        beginBtn.isEnabled = false
        beginBtn.setOnClickListener {
            sentPoint()
        }

    }

    private fun sentPoint() {
        // 保存经纬度到viewmodel
        viewModel.mapChooseLocateLatLng = LatLng(overlaysArray[0].position.latitude, overlaysArray[0].position.longitude)
        // 切换fragment
        onSendPointListener.switchFragment()
    }

    /**
     * 更新地图状态显示面板
     */
    private fun updateMapState() {
        beginBtn.isEnabled = true
        mBaiduMap.clear()
        overlaysArray.clear()
        val state: String = String.format("$mTouchType,当前经度： %f 当前纬度：%f", mCurrentPointPlace.longitude, mCurrentPointPlace.latitude)
        val ooA = MarkerOptions().position(mCurrentPointPlace).icon(mbitmap).draggable(true)
        overlaysArray.add(ooA)
        mBaiduMap.addOverlay(ooA)
        mStateBar.text = state

        viewModel.mCurrentPointPlaceLiveData.value = mCurrentPointPlace
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
        mLocationClient = LocationClient(requireActivity().applicationContext)


        //通过LocationClientOption设置LocationClient相关参数
        val option = LocationClientOption()
        option.locationMode = LocationClientOption.LocationMode.Hight_Accuracy
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
            Log.e(TAG, "onReceiveLocation: locType=${p0?.locType ?: 0}")
            //mapView 销毁后不再处理新接收的位置
            if (p0 == null) {
                return
            }
            nowLocationMsg = MyLocationData.Builder()
                .accuracy(p0.radius) // 此处设置开发者获取到的方向信息，顺时针0-360
                .direction(p0.direction).latitude(p0.latitude)
                .longitude(p0.longitude).build()
            Log.e(TAG, "onReceiveLocation: ${p0.radius},${p0.direction},${p0.latitude},${p0.longitude}")

            viewModel.nowLocLocTypeLiveData.value = p0.locType
            viewModel.nowLocLngLiveData.value = p0.longitude
            viewModel.nowLocLatLiveData.value = p0.latitude
            viewModel.nowLocRadiusLiveData.value = p0.radius
            viewModel.nowLocDirectionLiveData.value = p0.direction
            viewModel.nowLocMsgLiveData.value = nowLocationMsg
        }

        override fun onLocDiagnosticMessage(p0: Int, p1: Int, p2: String?) {
            Log.e(TAG, "onLocDiagnosticMessage: $p0, $p1, $p2")
        }
    }
}