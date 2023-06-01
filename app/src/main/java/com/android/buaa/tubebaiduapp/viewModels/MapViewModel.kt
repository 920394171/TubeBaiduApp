package com.android.buaa.tubebaiduapp.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.buaa.tubebaiduapp.classes.Node
import com.android.buaa.tubebaiduapp.utils.ClientModel
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng

class MapViewModel : ViewModel() {
    lateinit var clientModel: ClientModel

    lateinit var mapChooseLocateLatLng: LatLng

    val nowLocLatLiveData: MutableLiveData<Double> = MutableLiveData(0.0)
    val nowLocLngLiveData: MutableLiveData<Double> = MutableLiveData(0.0)
    val nowLocRadiusLiveData: MutableLiveData<Float> = MutableLiveData(0.0f)
    val nowLocDirectionLiveData: MutableLiveData<Float> = MutableLiveData(0.0f)
    val nowLocLocTypeLiveData: MutableLiveData<Int> = MutableLiveData(0)
    val nowLocMsgLiveData: MutableLiveData<MyLocationData> = MutableLiveData()

    val mCurrentPointPlaceLiveData:MutableLiveData<LatLng> = MutableLiveData()

    val isAdjustingMarkerLiveData: MutableLiveData<Boolean> = MutableLiveData(false)
//    val selectedMarkerLiveData: MutableLiveData<MarkerOptions> = MutableLiveData()
    val selectedNodeLiveData: MutableLiveData<Node> = MutableLiveData()

    val refreshMapAnnotationLiveData:MutableLiveData<Boolean> = MutableLiveData(true)
    val needLocatePanoLiveData:MutableLiveData<Boolean> = MutableLiveData(true)

    /**
     * 定位初始化
     */
    fun initMapSetting(mBaiduMap: BaiduMap, mMapView: MapView) {
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
}