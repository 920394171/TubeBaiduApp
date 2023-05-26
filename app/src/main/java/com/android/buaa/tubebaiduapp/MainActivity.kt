package com.android.buaa.tubebaiduapp

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import com.android.buaa.tubebaiduapp.fragments.MapAnnotationFragment
import com.android.buaa.tubebaiduapp.utils.ClientModel
import com.android.buaa.tubebaiduapp.utils.OnSendPointListener
import com.android.buaa.tubebaiduapp.viewModels.MapViewModel
import com.baidu.lbsapi.BMapManager
import com.baidu.location.LocationClient
import com.baidu.mapapi.CoordType
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.SDKInitializer.setHttpsEnable
import com.baidu.mapapi.common.BaiduMapSDKException
import com.baidu.mapapi.model.LatLng
import com.google.android.material.navigation.NavigationView


class MainActivity : AppCompatActivity(), OnSendPointListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private var isDrawerOpen = false

    private val directionBtns: ArrayList<ImageView> = ArrayList()
    private lateinit var upBtn: ImageView
    private lateinit var downBtn: ImageView
    private lateinit var leftBtn: ImageView
    private lateinit var rightBtn: ImageView

    //    private lateinit var panoBtn: Button
//    private lateinit var mapBtn: Button
    private val MY_PERMISSIONS_REQUEST_READ_PHONE_STATE: Int = 1001
//    private lateinit var panoLocateFragment: PanoLocateFragment
//    private lateinit var mapLocateFragment: MapLocateFragment
    private lateinit var mapAnnotationFragment: MapAnnotationFragment
    private lateinit var viewModel: MapViewModel
    val TAG: String = "TTZZ"

    private val REQUEST_CODE_ACCESS_COARSE_LOCATION: Int = 1000

    //    private lateinit var panoBtn: Button
//    private lateinit var mapBtn: Button
    private lateinit var mBMapManager: BMapManager

    // 服务器model
    private lateinit var clientModel: ClientModel

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)

        // 请求定位权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//如果 API level 是大于等于 23(Android 6.0) 时
            //判断是否具有权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                //判断
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Toast.makeText(this, "KARL-Dujinyang 是否需要打开位置权限", Toast.LENGTH_SHORT).show()
                }
                //请求权限
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_CODE_ACCESS_COARSE_LOCATION
                )
            }
        }

//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                this, arrayOf(Manifest.permission.READ_PHONE_STATE),
//                MY_PERMISSIONS_REQUEST_READ_PHONE_STATE
//            )
//        }

        viewModel = ViewModelProvider(this)[MapViewModel::class.java]

        //context为applicationContext
        mBMapManager = BMapManager(applicationContext)
        //设置用户是否同意Android全景SDK隐私政策，自v2.9.2版本起增加了隐私合规接口，请务必确保用户同意隐私政策后调用setAgreePrivacy接口
        //true，表示用户同意隐私合规政策
        //false，表示用户不同意隐私合规政策
        mBMapManager.setAgreePrivacy(this, true)


        /*暂时注释，为了使用模拟机*/
        // Android地图SDK隐私合规接口
        // 是否同意隐私政策，默认为false
        SDKInitializer.setAgreePrivacy(applicationContext, true)
        try {
            //在使用SDK各组件之前初始化context信息，传入ApplicationContext
            SDKInitializer.initialize(applicationContext)
            //自4.3.0起，百度地图SDK所有接口均支持百度坐标和国测局坐标，用此方法设置您使用的坐标类型.
            //包括BD09LL和GCJ02两种坐标，默认是BD09LL坐标。
            SDKInitializer.setCoordType(CoordType.BD09LL)
        } catch (e: BaiduMapSDKException) {
            e.printStackTrace()
        }

        // Android定位SDK隐私合规接口
        LocationClient.setAgreePrivacy(true)

        // 地图SDK V5.3.2版本之后（包含）默认使用https协议
//        setHttpsEnable(true)


        // 初始化服务器的客户端model
        clientModel = ClientModel(assets)
        viewModel.clientModel = clientModel
        setContentView(R.layout.fragment_loader)

        // 设置初始化监听
        // 常用事件监听，用来处理通常的网络错误，授权验证错误等  在监听成功后去设置加载全景
        mBMapManager.init { iError ->
            // 非零值表示key验证未通过
            val context = MyApplication.getInstance()?.applicationContext
            context?.let { it ->
                if (iError != 0) {
                    // 授权Key错误：
                    Toast.makeText(
                        it,
                        "请在AndoridManifest.xml中输入正确的授权Key,并检查您的网络连接是否正常！error: $iError", Toast.LENGTH_LONG
                    ).show()
                } else {
                    Log.e(TAG, "onGetPermissionState: 1!")
                    Toast.makeText(it, "key认证成功", Toast.LENGTH_LONG).show()

                }
            }


            setContentView(R.layout.activity_main)
            Log.e(TAG, "onCreate: 2!")
            // 获取 FragmentManager
            val fragmentManager = supportFragmentManager

            // 创建 FragmentTransaction
            val transaction = fragmentManager.beginTransaction()

            // 添加 MapAnnotationFragment 到 fragment_container_top 中
            mapAnnotationFragment = MapAnnotationFragment()
            transaction.add(R.id.fragment_container_top, mapAnnotationFragment)

            // 添加 PanoLocateFragment 到 fragment_container_bottom 中
//            mapLocateFragment = MapLocateFragment.newInstance()
//            panoLocateFragment = PanoLocateFragment.newInstance()
//            transaction.add(R.id.fragment_container_bottom_1, mapLocateFragment)
//            transaction.add(R.id.fragment_container_bottom_2, panoLocateFragment)

//            transaction.show(mapLocateFragment)
//            transaction.hide(panoLocateFragment)
//            测试用，显示pano
//            transaction.hide(mapLocateFragment)
//            transaction.show(panoLocateFragment)

            // 提交 FragmentTransaction
            transaction.commit()
            Log.e(TAG, "onCreate: 3!")


            // 设置方向键按钮的点击事件
            upBtn = findViewById(R.id.upBtn)
            downBtn = findViewById(R.id.downBtn)
            leftBtn = findViewById(R.id.leftBtn)
            rightBtn = findViewById(R.id.rightBtn)
            directionBtns.add(upBtn)
            directionBtns.add(downBtn)
            directionBtns.add(leftBtn)
            directionBtns.add(rightBtn)

            directionBtns.forEach { it.visibility = View.INVISIBLE; it.isEnabled = false }

            // 设置方向键显示的时机
            viewModel.isAdjustingMarkerLiveData.observe(this) { isAdjustingMarker ->
                if (isAdjustingMarker) {
                    directionBtns.forEach {
                        it.visibility = View.VISIBLE
                        it.isEnabled = true
                    }
                } else {
                    directionBtns.forEach {
                        it.visibility = View.INVISIBLE
                        it.isEnabled = false
                    }
                }
            }

            // 点击事件
            upBtn.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // 按钮被按下，设置其背景资源为被按压的图标，并标记为已按下状态
                        v.setBackgroundResource(R.drawable.up)
                        viewModel.selectedMarkerLiveData.value?.let {
                            moveSelectedMarkerAndNode(it.position.latitude + 0.000001, it.position.longitude)
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.performClick()
                        // 按钮被释放或取消，恢复其背景资源为未被按压的图标，并标记为未按下状态
                        v.setBackgroundResource(R.drawable.up_kong)
                    }
                }
                true
            }

            downBtn.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.setBackgroundResource(R.drawable.down)
                        viewModel.selectedMarkerLiveData.value?.let {
                            moveSelectedMarkerAndNode(it.position.latitude - 0.000001, it.position.longitude)
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.performClick()
                        // 按钮被释放或取消，恢复其背景资源为未被按压的图标，并标记为未按下状态
                        v.setBackgroundResource(R.drawable.down_kong)
                    }
                }
                true
            }

            leftBtn.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // 按钮被按下，设置其背景资源为被按压的图标，并标记为已按下状态
                        v.setBackgroundResource(R.drawable.left)
                        viewModel.selectedMarkerLiveData.value?.let {
                            moveSelectedMarkerAndNode(it.position.latitude, it.position.longitude - 0.000001)
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.performClick()
                        // 按钮被释放或取消，恢复其背景资源为未被按压的图标，并标记为未按下状态
                        v.setBackgroundResource(R.drawable.left_kong)
                    }
                }
                true
            }

            rightBtn.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // 按钮被按下，设置其背景资源为被按压的图标，并标记为已按下状态
                        v.setBackgroundResource(R.drawable.right)
                        viewModel.selectedMarkerLiveData.value?.let {
                            moveSelectedMarkerAndNode(it.position.latitude, it.position.longitude + 0.000001)
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.performClick()
                        // 按钮被释放或取消，恢复其背景资源为未被按压的图标，并标记为未按下状态
                        v.setBackgroundResource(R.drawable.right_kong)
                    }
                }
                true
            }

            // 侧边栏按钮
            drawerLayout = findViewById(R.id.drawerLayout)

            val _this = this
            // 设置菜单图标的点击事件
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setHomeAsUpIndicator(R.drawable.menu_white) // 菜单图标
                // 隐藏actionBar中的app名称
                setDisplayShowTitleEnabled(false)
            }
            // 给出supportActionBar的用法以及注释
//            supportActionBar?.setDisplayHomeAsUpEnabled(true)
//            supportActionBar?.setHomeAsUpIndicator(R.drawable.manu)
//            supportActionBar?.setDisplayShowTitleEnabled(false)
//            supportActionBar?.title = ""
//            supportActionBar?.setDisplayShowHomeEnabled(true)
//            supportActionBar?.setHomeButtonEnabled(true)
//            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            navigationView = findViewById(R.id.navigationView)
            val menu = navigationView.menu
            val n = 4 // 要添加的菜单项数量
            val itemIcons = IntArray(n + 1)
            itemIcons[1] = (R.drawable.icon_nav_item_1)
            itemIcons[2] = (R.drawable.icon_nav_item_2)
            itemIcons[3] = (R.drawable.icon_nav_item_3)
            itemIcons[4] = (R.drawable.icon_nav_item_4)

            val itemTitles = ArrayList<String>()
            itemTitles.add("")
            itemTitles.add("调整")
            itemTitles.add("开始")
            itemTitles.add("弹出")
            itemTitles.add("清除")



            for (itemId in 1..n) {

                // 添加菜单项
                val menuItem = menu.add(Menu.NONE, itemId, Menu.NONE, itemTitles[itemId])
                menuItem.setIcon(itemIcons[itemId])

                // 为菜单项设置点击事件监听器
                menuItem.setOnMenuItemClickListener { item ->
                    // 处理菜单项点击事件
                    when (item.itemId) {
                        // 根据菜单项的唯一标识符进行处理
                        1 -> {
                            // 调整
                        }
                        2 -> {
                            // 开始
                        }
                        // 处理其他菜单项的点击逻辑
                    }

                    true // 返回 true 表示已处理点击事件
                }
            }

            // 设置侧边栏菜单项的点击事件
            navigationView.setNavigationItemSelectedListener { menuItem ->



                // 处理侧边栏菜单项点击事件
                drawerLayout.closeDrawers() // 点击后关闭侧边栏
                true
            }
        }
        /*暂时注释，为了使用模拟机*/
    }

    private fun moveSelectedMarkerAndNode(latitude: Double, longitude: Double) {
        viewModel.selectedMarkerLiveData.value?.position(LatLng(latitude, longitude))
        viewModel.selectedNodeLiveData.value?.setLatLng(LatLng(latitude, longitude))

        viewModel.selectedPolylineMarkerLiveData.value?.let { polylineMark->
            viewModel.selectedPolylineNodeIndexLiveData.value?.let{index->
                polylineMark.points[index] = LatLng(latitude, longitude)
            }
        }

        viewModel.refreshMapAnnotationLiveData.value = !viewModel.refreshMapAnnotationLiveData.value!!
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // 点击菜单图标时展开/收起侧边栏
                if (isDrawerOpen) {
                    closeDrawerWithAnimation()
                } else {
                    openDrawerWithAnimation()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openDrawerWithAnimation() {
        isDrawerOpen = true
        drawerLayout.openDrawer(GravityCompat.START)

        val animatorSet = AnimatorSet()
        val rotationAnimator = ObjectAnimator.ofFloat(
            supportActionBar?.customView,
            "rotation",
            0f,
            90f
        )
        rotationAnimator.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.play(rotationAnimator)
        animatorSet.start()
    }

    private fun closeDrawerWithAnimation() {
        isDrawerOpen = false
        drawerLayout.closeDrawer(GravityCompat.START)

        val animatorSet = AnimatorSet()
        val rotationAnimator = ObjectAnimator.ofFloat(
            supportActionBar?.customView,
            "rotation",
            90f,
            0f
        )
        rotationAnimator.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.play(rotationAnimator)
        animatorSet.start()
    }

    override fun switchFragment() {
//        val fragmentManager = supportFragmentManager
//
//        // 切换 Fragment
//        val transaction = fragmentManager.beginTransaction()
//        if (mapLocateFragment.isHidden) {
//            transaction.show(mapLocateFragment)
//            transaction.hide(panoLocateFragment)
////            mapLocateFragment.onBackToMap()
//        } else {
//            transaction.show(panoLocateFragment)
//            transaction.hide(mapLocateFragment)
//            viewModel.needLocatePanoLiveData.value = !viewModel.needLocatePanoLiveData.value!!
////            panoLocateFragment.onBackToMap()
//        }
//        transaction.commit()
    }
}