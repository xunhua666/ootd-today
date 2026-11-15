package com.ootd.today.data.local

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 单品图片存储。
 *
 * 相册选择返回的是 `content://` URI，其读取授权是**临时的**：
 * 进程重启后系统会回收授权，届时直接用旧 URI 加载会失败。
 * 因此选中图片后立刻拷贝进应用私有目录，数据库里只存私有路径，
 * 保证任何时候都能读到。
 *
 * 拍照走 FileProvider 私有目录，本身就是持久的，无需拷贝。
 */
@Singleton
class ImageStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val photoDir: File
        get() = File(context.filesDir, "photos").apply { mkdirs() }

    /**
     * 把任意 URI 指向的图片拷贝进私有目录。
     *
     * @return 拷贝后的绝对路径；读取失败返回 null，调用方应回退到色卡占位。
     */
    fun copyToPrivate(uri: Uri): String? = runCatching {
        val target = File(photoDir, "item_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output) }
        } ?: return null
        target.absolutePath
    }.getOrNull()

    /** 拍照目标文件：在私有目录预建一张空文件，供系统相机写入。 */
    fun createCameraTarget(): File = File(photoDir, "item_${System.currentTimeMillis()}.jpg")
}
