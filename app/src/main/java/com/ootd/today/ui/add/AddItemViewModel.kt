package com.ootd.today.ui.add

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ootd.today.data.local.ImageStore
import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.ItemStatus
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.Season
import com.ootd.today.data.model.StyleTag
import com.ootd.today.data.model.WardrobeItem
import com.ootd.today.data.repository.WardrobeRepository
import com.ootd.today.domain.ItemRecognizer
import com.ootd.today.data.model.GarmentShape
import com.ootd.today.data.prefs.SettingsRepository
import com.ootd.today.data.prefs.UserSettings
import com.ootd.today.data.repository.CloudGarmentVision
import com.ootd.today.domain.vision.GarmentVisionResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 录入/编辑表单状态。全部字段可编辑，保存前做校验。 */
data class AddItemUiState(
    val savedItemId: Long? = null,
    val recommendAfterSave: Boolean = false,
    val shape: GarmentShape = GarmentShape(),
    val isAnalyzingShape: Boolean = false,
    val visionDraft: GarmentVisionResult? = null,
    val visionError: String? = null,
    val name: String = "",
    val category: ClothingCategory = ClothingCategory.TOP,
    val subCategory: String = "T恤",
    val imagePath: String? = null,
    val colorArgb: Long = 0xFFB0A8B9,
    val colorName: String = "",
    val warmthLevel: Int = 2,
    val seasons: Set<Season> = setOf(Season.SPRING),
    val occasions: Set<Occasion> = setOf(Occasion.COMMUTE),
    val style: StyleTag? = null,
    val brand: String = "",
    val price: String = "",
    val note: String = "",
    /** 正在编辑的单品 id；null 表示新建。 */
    val editingId: Long? = null,
    /** 用户是否手动改过品类；改过后 AI 预填不再覆盖。 */
    val categoryTouched: Boolean = false,
    /** 用户是否手动改过主色；改过后 AI 预填不再覆盖。 */
    val colorTouched: Boolean = false,
    /** 编辑模式下是否已加载完成，避免表单先闪一帧空值。 */
    val editLoaded: Boolean = false,
    /** 保存成功标记，UI 观察它来关闭页面。 */
    val saved: Boolean = false,
    /** AI 识别进行中，UI 可展示 loading 提示。 */
    val isRecognizing: Boolean = false,
    /** AI 识别给出的提示文案，null 表示无。 */
    val recognitionHint: String? = null,
    /** 校验失败提示，null 表示无错误。 */
    val error: String? = null,
    val isSaving: Boolean = false
) {
    val isEditing: Boolean get() = editingId != null
}

/**
 * 「录入单品」页 ViewModel。
 *
 * 表单状态全部集中在 [AddItemUiState] 一个数据类里，
 * 这样旋转屏幕、进程重建时只需恢复这一个对象，不会出现字段散落各处对不上的情况。
 */
@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val wardrobeRepository: WardrobeRepository,
    private val imageStore: ImageStore,
    private val recognizer: ItemRecognizer,
    private val vision: CloudGarmentVision,
    settings: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddItemUiState())
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()
    val settings = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())
    private var visionJob: Job? = null
    private var recognitionJob: Job? = null
    private var photoVersion = 0
    private var visionToken = 0
    fun onShapeChange(value: GarmentShape) = _uiState.update { it.copy(shape = value.copy(confirmed = true).forCategory(it.category), categoryTouched = true) }
    fun discardVisionDraft() = _uiState.update { it.copy(visionDraft = null) }
    fun applyVisionDraft() {
        val draft = _uiState.value.visionDraft ?: return
        recognitionJob?.cancel()
        _uiState.update { state ->
            val category = draft.category ?: state.category
            state.copy(category = category, categoryTouched = true, subCategory = if (category == state.category) state.subCategory else category.subCategories.first(),
                name = state.name.ifBlank { draft.name }, shape = draft.shape.copy(confirmed = true).forCategory(category), visionDraft = null, isRecognizing = false)
        }
    }
    fun analyzeShape() {
        val path = _uiState.value.imagePath ?: return
        if (_uiState.value.isAnalyzingShape) return
        val version = photoVersion
        val token = ++visionToken
        _uiState.update { it.copy(isAnalyzingShape = true, visionError = null, visionDraft = null) }
        visionJob = viewModelScope.launch {
            try {
                val result = vision.analyze(path)
                if (version == photoVersion && token == visionToken) _uiState.update { it.copy(visionDraft = result) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (version == photoVersion && token == visionToken) _uiState.update { it.copy(visionError = when(e) {
                    is java.net.SocketTimeoutException -> "识别超时，请稍后重试"
                    is java.io.IOException -> "无法连接视觉服务，请检查网络和接口地址"
                    is org.json.JSONException -> "识别结果格式无效，请重试"
                    else -> e.message ?: "识别失败，可以手动填写版型"
                }) }
            } finally {
                if (version == photoVersion && token == visionToken) _uiState.update { it.copy(isAnalyzingShape = false) }
            }
        }
    }
    fun cancelShapeAnalysis() {
        visionJob?.cancel(); visionToken++
        _uiState.update { it.copy(isAnalyzingShape = false, visionDraft = null) }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, error = null) }

    fun onColorNameChange(value: String) = _uiState.update { it.copy(colorName = value) }

    fun onBrandChange(value: String) = _uiState.update { it.copy(brand = value) }

    fun onPriceChange(value: String) =
        // 只允许数字与一个小数点，避免解析失败。
        _uiState.update {
            val filtered = value.filterIndexed { index, c ->
                c.isDigit() || (c == '.' && index > 0 && !value.substring(0, index).contains('.'))
            }
            it.copy(price = filtered)
        }

    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }

    /**
     * 编辑模式：把已有单品填进表单。
     *
     * 只加载一次（[AddItemUiState.editLoaded] 守卫），
     * 避免用户改到一半被重新加载覆盖。
     */
    fun loadForEdit(itemId: Long) {
        if (_uiState.value.editLoaded) return
        viewModelScope.launch {
            val item = wardrobeRepository.getItem(itemId) ?: return@launch
            _uiState.update {
                it.copy(
                    editingId = item.id,
                    name = item.name,
                    shape = item.shape,
                    category = item.category,
                    subCategory = item.subCategory,
                    imagePath = item.imagePath,
                    colorArgb = item.colorArgb,
                    colorName = item.colorName,
                    warmthLevel = item.warmthLevel,
                    seasons = item.seasons,
                    occasions = item.occasions,
                    style = item.style,
                    brand = item.brand.orEmpty(),
                    price = item.price?.let { p -> if (p == p.toLong().toDouble()) p.toLong().toString() else p.toString() }.orEmpty(),
                    note = item.note.orEmpty(),
                    editLoaded = true
                )
            }
        }
    }

    /** 切换大类时，把细分款式重置为该大类的第一个选项，否则会残留上一个品类的款式。 */
    fun onCategoryChange(category: ClothingCategory) = _uiState.update {
        it.copy(
            category = category,
            subCategory = category.subCategories.first(),
            shape = if (category == it.category) it.shape else GarmentShape(),
            // 用户手动改过品类后，AI 不再覆盖。
            categoryTouched = true
        )
    }

    fun onSubCategoryChange(value: String) = _uiState.update { it.copy(subCategory = value) }

    fun onWarmthChange(level: Int) = _uiState.update {
        it.copy(warmthLevel = level.coerceIn(WardrobeItem.WARMTH_RANGE))
    }

    fun onColorPicked(argb: Long) = _uiState.update {
        it.copy(colorArgb = argb, colorTouched = true)
    }

    fun onImagePicked(path: String?) {
        photoVersion++; visionJob?.cancel(); recognitionJob?.cancel()
        _uiState.update { it.copy(imagePath = path, shape = GarmentShape(), isAnalyzingShape = false, visionDraft = null, visionError = null, isRecognizing = false) }
    }

    /**
     * 移除已选照片。
     *
     * 只清掉表单里的引用，**不删除磁盘上的文件**：
     * 编辑场景下用户可能反悔又想用回原图；而且单品的照片也可能已被
     * 打卡历史/社区动态的快照引用，删文件会让那些地方变成破图。
     */
    fun onRemovePhoto() = onImagePicked(null)

    /**
     * 图片确定后触发 AI 预填。
     *
     * 识别在后台跑，不阻塞用户继续填其它字段；
     * 结果只预填「品类 + 主色」两个字段，且**不覆盖用户已手动改过的值**
     * （用 categoryTouched / colorTouched 标记跟踪）。
     */
    fun recognizeAndPrefill(path: String) {
        recognitionJob?.cancel()
        val version = photoVersion
        recognitionJob = viewModelScope.launch {
            _uiState.update { it.copy(isRecognizing = true, recognitionHint = null) }
            val result = withContext(Dispatchers.IO) { recognizer.recognize(path) }
            if (version != photoVersion) return@launch
            _uiState.update { state ->
                if (result == null) {
                    state.copy(
                        isRecognizing = false,
                        recognitionHint = "AI 没看出品类，手动选一下就好"
                    )
                } else {
                    val category = result.suggestedCategory
                    val color = result.dominantColorArgb
                    state.copy(
                        isRecognizing = false,
                        category = if (category != null && !state.categoryTouched) category else state.category,
                        subCategory = if (category != null && !state.categoryTouched)
                            category.subCategories.first() else state.subCategory,
                        colorArgb = if (color != null && !state.colorTouched) color else state.colorArgb,
                        recognitionHint = buildHint(result.rawLabels, category != null, color != null)
                    )
                }
            }
        }
    }

    private fun buildHint(labels: List<String>, hasCategory: Boolean, hasColor: Boolean): String {
        val seen = labels.take(2).joinToString("、")
        return when {
            hasCategory && hasColor -> "AI 识别：$seen，已预填品类与主色，可修改"
            hasCategory -> "AI 识别：$seen，已预填品类"
            hasColor -> "AI 提取了主色，品类请手动选"
            else -> "AI 没看出品类，手动选一下就好"
        }
    }

    /**
     * 相册选图回调。
     *
     * 相册返回的 content:// URI 授权是临时的，进程重启即失效，
     * 所以这里立刻把图片拷贝进应用私有目录，数据库只存私有路径。
     * 拷贝在 IO 线程异步执行，避免大图阻塞主线程。
     */
    fun onGalleryImagePicked(uri: Uri) {
        viewModelScope.launch {
            // 拷贝是 IO 密集操作，切到 IO 线程；结果回主线程更新状态。
            val path = withContext(Dispatchers.IO) { imageStore.copyToPrivate(uri) }
            onImagePicked(path)
            path?.let { recognizeAndPrefill(it) }
        }
    }

    /** 拍照成功回调：记录路径并触发 AI 预填。 */
    fun onCameraImagePicked(path: String) {
        onImagePicked(path)
        recognizeAndPrefill(path)
    }



    /** 季节/场合/风格都是多选，点一下切换选中态。 */
    fun toggleSeason(season: Season) = _uiState.update {
        it.copy(seasons = it.seasons.toggle(season))
    }

    fun toggleOccasion(occasion: Occasion) = _uiState.update {
        it.copy(occasions = it.occasions.toggle(occasion))
    }

    fun toggleStyle(style: StyleTag) = _uiState.update {
        it.copy(style = if (it.style == style) null else style)
    }

    /**
     * 校验并保存。
     *
     * 校验规则：名称必填；至少选一个季节；至少选一个场合。
     * 场合尤其关键——推荐算法把「场合不匹配」当作硬性淘汰条件，
     * 一件没有任何场合标签的单品将永远不会被推荐出来。
     */
    fun save(buildOutfit: Boolean = false) {
        val s = _uiState.value
        if (s.isSaving || s.isAnalyzingShape) return
        when {
            s.name.isBlank() -> {
                _uiState.update { it.copy(error = "给这件单品起个名字吧") }
                return
            }
            s.seasons.isEmpty() -> {
                _uiState.update { it.copy(error = "至少选一个适用季节") }
                return
            }
            s.occasions.isEmpty() -> {
                _uiState.update { it.copy(error = "至少选一个适用场合，否则不会被推荐") }
                return
            }
        }

        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            // 编辑模式先取回原记录，保留表单不承载的字段
            // （穿着次数、最后穿着日期、购入日期、在用状态），
            // 否则一次编辑就会把穿着历史清零。
            val existing = s.editingId?.let { wardrobeRepository.getItem(it) }

            val item = WardrobeItem(
                // 编辑模式保留原 id，新建模式传 0 由仓库分配。
                id = s.editingId ?: 0L,
                name = s.name.trim(),
                category = s.category,
                subCategory = s.subCategory,
                imagePath = s.imagePath,
                colorArgb = s.colorArgb,
                colorName = s.colorName.ifBlank { defaultColorName(s.colorArgb) },
                warmthLevel = s.warmthLevel,
                seasons = s.seasons,
                occasions = s.occasions,
                style = s.style,
                brand = s.brand.trim().ifEmpty { null },
                price = s.price.toDoubleOrNull(),
                purchaseDate = existing?.purchaseDate,
                lastWornDate = existing?.lastWornDate,
                status = existing?.status ?: ItemStatus.ACTIVE,
                note = s.note.trim().ifEmpty { null },
                wearCount = existing?.wearCount ?: 0,
                shape = s.shape.forCategory(s.category)
            )
            try {
                val id = wardrobeRepository.upsertItem(item)
                _uiState.update { it.copy(isSaving = false, saved = true, savedItemId = id, recommendAfterSave = buildOutfit) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _uiState.update { it.copy(isSaving = false, error = "保存失败，请重试") } }
        }
    }

    /** 表单复用（连续录入）时重置，但保留品类，方便批量录入同类单品。 */
    fun resetKeepCategory() {
        val keepCategory = _uiState.value.category
        val keepSub = _uiState.value.subCategory
        _uiState.value = AddItemUiState(category = keepCategory, subCategory = keepSub)
    }

    private fun <T> Set<T>.toggle(element: T): Set<T> =
        if (contains(element)) this - element else this + element

    private fun defaultColorName(argb: Long): String {
        val r = (argb shr 16 and 0xFF).toInt()
        val g = (argb shr 8 and 0xFF).toInt()
        val b = (argb and 0xFF).toInt()
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        return when {
            max - min < 20 -> when {
                max > 235 -> "白"
                max < 45 -> "黑"
                else -> "灰"
            }
            max == r && min == b -> "红调"
            max == g -> "绿调"
            max == b -> "蓝调"
            else -> "混色"
        }
    }
}
