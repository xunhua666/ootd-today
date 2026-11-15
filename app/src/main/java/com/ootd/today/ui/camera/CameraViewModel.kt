package com.ootd.today.ui.camera

import androidx.lifecycle.ViewModel
import com.ootd.today.data.local.ImageStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject

/**
 * 相机页 ViewModel。
 *
 * 职责刻意很窄：只负责产出「照片要写到哪个文件」。
 * 真正的取景、对焦、拍照都在 Composable 层用 CameraX 完成 —— 那些
 * 与 View 生命周期强绑定，放在 ViewModel 里反而会引入泄漏风险。
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val imageStore: ImageStore
) : ViewModel() {

    /**
     * 创建本次拍照的输出文件。
     *
     * 每次按下快门前调用一次，保证连拍多张时各自独立、不会互相覆盖。
     */
    fun createOutputFile(): File = imageStore.createCameraTarget()
}