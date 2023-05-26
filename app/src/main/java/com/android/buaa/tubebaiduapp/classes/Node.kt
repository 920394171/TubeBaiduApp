package com.android.buaa.tubebaiduapp.classes

import com.baidu.mapapi.model.LatLng


class Node {
    lateinit var latLng: LatLng
    var altitude = 0.0
    var nodeType = 0
    var nodeMsg: String? = null
    var nodeIndex = 0

    fun setLatLng(latLng: LatLng): Node {
        this.latLng = latLng
        return this
    }

    fun setAltitude(altitude: Double): Node {
        this.altitude = altitude
        return this
    }

    fun setNodeType(nodeTypeStr: String?): Node {
        nodeType = when (nodeTypeStr) {
            "起始节点" -> 1
            "结束节点" -> 2
            else -> 3
        }
        return this
    }

    fun getNodeTypeStr():String {
        return when(nodeType){
            1-> "起始节点"
            2->"结束节点"
            else->"中间节点"
        }
    }

    fun setNodeMsg(nodeMsg: String): Node {
        this.nodeMsg = nodeMsg
        return this
    }

    fun setNodeIndex(nodeIndex: Int): Node {
        this.nodeIndex = nodeIndex
        return this
    }
}