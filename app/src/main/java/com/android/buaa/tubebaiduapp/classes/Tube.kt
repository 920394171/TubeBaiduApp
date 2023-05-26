package com.android.buaa.tubebaiduapp.classes

internal class Tube {
    var tubeName: String? = null
    var tubeTypeStr: String? = null
    var tubeMsg: String? = null

    fun clear() {
        tubeName = null
        tubeMsg = null
        tubeTypeStr = null
    }

    fun setTubeName(tubeName: String): Tube {
        this.tubeName = tubeName
        return this
    }

    fun setTubeTypeStr(tubeTypeStr: String): Tube {
        this.tubeTypeStr = tubeTypeStr
        return this
    }

    fun setTubeMsg(tubeMsg: String): Tube {
        this.tubeMsg = tubeMsg
        return this
    }
}