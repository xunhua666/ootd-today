package com.ootd.today.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.Executor

/**
 * 应用内相机页。
 *
 * 用 CameraX 实现实时取景 + 拍照，相比调用系统相机的好处：
 * - 不跳出应用，拍完直接回到录入流程；
 * - 可自定义取景框（本页加了四角构图辅助线）与按钮布局；
 * - 能控制输出尺寸，避免系统相机拍出几十 MB 的原图拖慢列表加载。
 *
 * 生命周期：相机与 [LocalLifecycleOwner] 绑定，离开页面时 CameraX 会自动释放，
 * 因此不需要在 ViewModel 里持有任何相机对象。
 *
 * @param onCaptured 拍照成功回调，参数是写入私有目录后的文件路径。
 */
@Composable
fun CameraScreen(
    onCaptured: (String) -> Unit,
    onCancel: () -> Unit,
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 权限状态。已授权则直接进入取景。
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    // 未授权时立即请求，而不是等用户按下快门才报错。
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) {
        CameraPermissionDenied(onCancel = onCancel)
        return
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashEnabled by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }

    // imageCapture 会在预览绑定时创建；用 state 让快门按钮能访问到。
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        CameraPreview(
            lensFacing = lensFacing,
            flashEnabled = flashEnabled,
            lifecycleOwner = lifecycleOwner,
            onImageCaptureReady = { imageCapture = it },
            modifier = Modifier.fillMaxSize()
        )

        // --- 顶部：取消 + 闪光灯 ---
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "取消",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Text(
                text = if (flashEnabled) "⚡️ 闪光灯开" else "⚡️ 闪光灯关",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable { flashEnabled = !flashEnabled }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // --- 底部：切换镜头 + 快门 ---
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 占位，让快门保持居中。
            Spacer(Modifier.size(56.dp))

            ShutterButton(
                enabled = !isCapturing,
                onClick = {
                    val capture = imageCapture ?: return@ShutterButton
                    isCapturing = true
                    capturePhoto(
                        context = context,
                        imageCapture = capture,
                        outputFile = viewModel.createOutputFile(),
                        onSuccess = { path ->
                            isCapturing = false
                            onCaptured(path)
                        },
                        onError = { isCapturing = false }
                    )
                }
            )

            // 前后置切换。多数设备前置不支持闪光灯，切换时顺手关掉。
            Text(
                text = "🔄",
                fontSize = 24.sp,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                        flashEnabled = false
                    }
                    .padding(14.dp)
            )
        }
    }
}

/**
 * CameraX 预览视图。
 *
 * 用 [AndroidView] 承载 [PreviewView]：CameraX 的输出目标是 Android View，
 * 无法直接画进 Compose 画布，这是官方推荐的桥接方式。
 */
@Composable
private fun CameraPreview(
    lensFacing: Int,
    flashEnabled: Boolean,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onImageCaptureReady: (ImageCapture) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 每次镜头切换或闪光灯状态变化都重新绑定，保证设置即时生效。
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val capture = ImageCapture.Builder()
                    // 衣物照片不需要 4K 原图：限制分辨率可显著降低文件体积
                    // 与后续列表加载开销。用 ResolutionSelector（新的推荐 API）
                    // 而非已废弃的 setTargetResolution。
                    .setResolutionSelector(
                        androidx.camera.core.resolutionselector.ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                androidx.camera.core.resolutionselector.ResolutionStrategy(
                                    android.util.Size(1080, 1920),
                                    androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                )
                            )
                            .build()
                    )
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setFlashMode(
                        if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                    )
                    .build()

                val selector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                runCatching {
                    // 先解绑再绑定：重复绑定同一生命周期会抛异常。
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                    onImageCaptureReady(capture)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

/**
 * 执行拍照并写入指定文件。
 *
 * CameraX 的 [ImageCapture.takePicture] 需要外部传入 [Executor]，
 * 这里用主线程执行器 —— 回调只做状态更新，不涉及耗时操作。
 */
private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    outputFile: File,
    onSuccess: (String) -> Unit,
    onError: () -> Unit
) {
    val options = ImageCapture.OutputFileOptions.Builder(outputFile).build()
    val executor: Executor = ContextCompat.getMainExecutor(context)

    imageCapture.takePicture(
        options,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val uri: Uri? = output.savedUri
                // 优先用 savedUri（某些设备会改写路径），否则回退到预建文件。
                onSuccess(uri?.path ?: outputFile.absolutePath)
            }

            override fun onError(exception: ImageCaptureException) {
                // 拍照失败通常是设备被占用或存储不可写；清理空文件避免留下垃圾。
                runCatching { if (outputFile.exists()) outputFile.delete() }
                onError()
            }
        }
    )
}

/** 快门按钮：外圈白环 + 内圆，按下时内圆缩小以示反馈。 */
@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .border(width = 4.dp, color = Color.White, shape = CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (enabled) 58.dp else 48.dp)
                .clip(CircleShape)
                .background(if (enabled) Color.White else Color.White.copy(alpha = 0.5f))
        )
    }
}

/** 权限被拒时的引导页。 */
@Composable
private fun CameraPermissionDenied(onCancel: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "📷", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "需要相机权限才能拍照",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "你也可以返回后从相册选择图片",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "返回",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 28.dp, vertical = 12.dp)
            )
        }
    }
}