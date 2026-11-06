package com.app_health.app.feature.integrated

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

fun photoFeature() = AppFeature("photo", "Chụp món ăn") { vm, _ -> PhotoScreen(vm) }

private fun normalizedPhoto(context: Context, uri: Uri): ByteArray {
    val resolver = context.contentResolver
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri).use { requireNotNull(it); BitmapFactory.decodeStream(it, null, options) }
    require(options.outWidth > 0 && options.outHeight > 0 && options.outWidth.toLong() * options.outHeight <= 50_000_000) { "Ảnh không hợp lệ hoặc quá lớn." }
    var sample = 1
    while (maxOf(options.outWidth, options.outHeight) / sample > 1600) sample *= 2
    val bitmap = resolver.openInputStream(uri).use {
        requireNotNull(it); requireNotNull(BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }))
    }
    val orientation = resolver.openInputStream(uri).use { stream ->
        if (stream == null) ExifInterface.ORIENTATION_NORMAL else runCatching { ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    }
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f,1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f,-1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f,1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f,1f) }
        }
    }
    val rotated = Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)
    return ByteArrayOutputStream().use { output ->
        rotated.compress(Bitmap.CompressFormat.JPEG,88,output)
        if (rotated !== bitmap) rotated.recycle()
        bitmap.recycle()
        output.toByteArray().also { require(it.size <= 8*1024*1024) { "Ảnh vượt giới hạn 8 MB." } }
    }
}

@Composable
fun MealInputs(meal: String, time: String, enabled: Boolean, setMeal: (String) -> Unit, setTime: (String) -> Unit) {
    Choice("Bữa ăn", meal, mealChoices, enabled, setMeal)
    OutlinedTextField(time, setTime, label = { Text("Giờ ăn (HH:mm)") }, singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth())
}

fun validMealTime(time: String) = Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(time)

fun PhotoDraft.toPhotoItem(): PhotoItemDto? {
    val g=inputNumber(grams) ?: return null
    val cal=inputNumber(calories) ?: return null
    val p=inputNumber(protein) ?: return null
    val c=inputNumber(carbs) ?: return null
    val f=inputNumber(fat) ?: return null
    if(name.isBlank() || name.length>160 || g<0.01 || g>10000 || cal !in 0.0..20000.0 || p !in 0.0..2000.0 || c !in 0.0..3000.0 || f !in 0.0..2000.0)return null
    return PhotoItemDto(name.trim(),g,cal,p,c,f,confidence)
}

@Composable
fun PhotoScreen(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val previewView = remember { PreviewView(context) }
    val capture = remember { ImageCapture.Builder().build() }
    var permission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var cameraReady by remember { mutableStateOf(false) }
    var imageVersion by remember { mutableIntStateOf(0) }
    var meal by rememberSaveable { mutableStateOf("snack") }
    var time by rememberSaveable { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    val enabled = !state.busy && !state.loading && !state.uncertain
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    fun select(uri: Uri) {
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { normalizedPhoto(context,uri) }
                vm.setPhoto(bytes,"image/jpeg")
                imageVersion++
            } catch (e: Exception) { vm.showError(e.message ?: "Không đọc được ảnh.") }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::select) }
    DisposableEffect(permission, owner) {
        var active = true
        var provider: ProcessCameraProvider? = null
        val preview = Preview.Builder().build()
        if (permission) {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                if (active) try {
                    provider = future.get()
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                    provider?.bindToLifecycle(owner,CameraSelector.DEFAULT_BACK_CAMERA,preview,capture)
                    cameraReady = true
                } catch (_: Exception) { vm.showError("Không mở được camera. Bạn có thể chọn ảnh từ thư viện.") }
            },executor)
        }
        onDispose { active = false; cameraReady = false; provider?.unbind(preview,capture) }
    }
    Page {
        Text("Ngày ghi: ${state.date}")
        if (permission) AndroidView(factory = { previewView }, modifier = Modifier.fillMaxWidth().height(220.dp))
        else Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Cho phép sử dụng camera") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = enabled && cameraReady, onClick = {
                val file = File(context.cacheDir,"meal-${UUID.randomUUID()}.jpg")
                capture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(),executor,object: ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        scope.launch {
                            try { val bytes = withContext(Dispatchers.IO) { normalizedPhoto(context,Uri.fromFile(file)) }; vm.setPhoto(bytes,"image/jpeg"); imageVersion++ }
                            catch (e: Exception) { vm.showError(e.message ?: "Không đọc được ảnh chụp.") }
                            finally { file.delete() }
                        }
                    }
                    override fun onError(exception: ImageCaptureException) { file.delete(); vm.showError("Không chụp được ảnh.") }
                })
            }) { Text("Chụp ảnh") }
            OutlinedButton(enabled = enabled, onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Chọn ảnh") }
        }
        val bytes = vm.photoBytes
        val bitmap = remember(bytes,state.photoRevision,imageVersion) { bytes?.let { BitmapFactory.decodeByteArray(it,0,it.size)?.asImageBitmap() } }
        bitmap?.let { Image(it,"Ảnh bữa ăn đã chọn",Modifier.fillMaxWidth().height(200.dp)) }
        Button(enabled = enabled && bytes != null && !state.photoConfirmed, onClick = {
            val upload = bytes ?: return@Button
            vm.write { ctx ->
                val part = MultipartBody.Part.createFormData("image","meal.jpg",upload.toRequestBody(vm.photoMime.toMediaType()))
                val result = vm.api.analyze(ctx.owner,ctx.date,part)
                vm.publish(ctx) { it.copy(photo = result, photoConfirmed = false, photoRevision = it.photoRevision+1,
                    photoDrafts=result.items.map { item -> PhotoDraft(item.name,item.estimated_grams.display(),item.calories_kcal.display(),item.protein_g.display(),item.carb_g.display(),item.fat_g.display(),item.confidence) }) }
            }
        }) { Text("Phân tích ảnh") }
        state.photo?.let { photo ->
            Text(photo.notes)
            Text(photo.disclaimer, style = MaterialTheme.typography.bodySmall)
            state.photoDrafts.forEachIndexed { index, item ->
                Panel("Món ${index+1} • độ tin cậy ${(item.confidence*100).toInt()}%") {
                    fun update(next: PhotoDraft) { val ctx = vm.context(); vm.publish(ctx) { it.copy(photoDrafts = it.photoDrafts.toMutableList().apply { set(index,next) }) } }
                    OutlinedTextField(item.name,{ update(item.copy(name=it.take(160))) },label={ Text("Tên món") },enabled=enabled && !state.photoConfirmed)
                    NumberInput("Khối lượng (g)",item.grams,enabled && !state.photoConfirmed) { update(item.copy(grams=it)) }
                    NumberInput("Calo của khẩu phần",item.calories,enabled && !state.photoConfirmed) { update(item.copy(calories=it)) }
                    NumberInput("Protein (g)",item.protein,enabled && !state.photoConfirmed) { update(item.copy(protein=it)) }
                    NumberInput("Carb (g)",item.carbs,enabled && !state.photoConfirmed) { update(item.copy(carbs=it)) }
                    NumberInput("Fat (g)",item.fat,enabled && !state.photoConfirmed) { update(item.copy(fat=it)) }
                }
            }
            MealInputs(meal,time,enabled && !state.photoConfirmed,{meal=it},{time=it})
            val items=state.photoDrafts.mapNotNull{it.toPhotoItem()}
            val valid = items.isNotEmpty() && items.size==state.photoDrafts.size
            Button(enabled=enabled && valid && validMealTime(time) && !state.photoConfirmed,onClick={
                vm.write { ctx ->
                    vm.api.confirm(ctx.owner,mapOf("items" to items,"meal_type" to meal,"meal_time" to time,"log_date" to ctx.date))
                    vm.publish(ctx) { it.copy(photoConfirmed=true) }
                }
            }) { Text(if (state.photoConfirmed) "Đã lưu vào nhật ký" else "Xác nhận và lưu nhật ký") }
        }
    }
}
