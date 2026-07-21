package com.woohaeng.board.ui

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsEndWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.woohaeng.board.util.BoardCompositor
import com.woohaeng.board.util.BoardFields
import com.woohaeng.board.util.BoardLayout
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt
private val UiBlue = Color(0xFF2F78E8)
private val UiBg = Color(0xFFE8E8E8)
private val PreviewBg = Color(0xFFD8D8D8)
private val TableBorder = Color(0xFF222222)
private val ButtonBg = Color(0xFFEEEEEE)

private enum class EditField {
    WorkName, WorkType, Location, Content, WorkDate
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    vm: AppViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var workName by remember { mutableStateOf("") }
    var workType by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var workDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var fromCamera by remember { mutableStateOf(false) }
    var showLiveCamera by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(1f) }
    var widthRatio by remember { mutableFloatStateOf(0.42f) }
    var showFormat by remember { mutableStateOf(false) }
    var editingField by remember { mutableStateOf<EditField?>(null) }
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()

    val layout = BoardLayout(offsetX = offsetX, offsetY = offsetY, widthRatio = widthRatio)
    val fields = BoardFields(
        workName = workName,
        workType = workType,
        location = location,
        content = content,
        workDate = workDate
    )
    val preview = remember(source, fields, layout) {
        source?.let { BoardCompositor.compose(it, fields, layout) }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        fromCamera = false
        source = vm.decodeUri(uri)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showLiveCamera = true
        } else {
            Toast.makeText(context, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openCamera() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            showLiveCamera = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun doUpload() {
        val bmp = source ?: return
        if (workName.isBlank()) {
            editingField = EditField.WorkName
            return
        }
        vm.uploadComposed(
            sourceBitmap = bmp,
            workName = workName,
            workType = workType,
            location = location,
            content = content,
            workDate = workDate,
            layout = layout,
            saveToGallery = fromCamera
        ) { onDone() }
    }

    if (showLiveCamera) {
        LiveCameraWithBoard(
            fields = fields,
            offsetX = offsetX,
            offsetY = offsetY,
            widthRatio = widthRatio,
            onOffsetXChange = { offsetX = it },
            onOffsetYChange = { offsetY = it },
            onWidthRatioChange = { widthRatio = it },
            onCaptured = { bitmap ->
                source = bitmap
                fromCamera = true
                showLiveCamera = false
            },
            onClose = { showLiveCamera = false }
        )
    } else {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UiBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(8.dp))

            BoardTable(
                workName = workName,
                workType = workType,
                location = location,
                content = content,
                workDate = displayDate(workDate),
                onEdit = { editingField = it },
                boxMod = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )
            Text(
                text = "각 칸을 누르면 글자를 수정할 수 있습니다",
                fontSize = 12.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToolButton(Icons.Default.CameraAlt, Modifier.weight(1f)) { openCamera() }
                ToolButton(Icons.Default.PhotoLibrary, Modifier.weight(1f)) {
                    galleryLauncher.launch("image/*")
                }
                ToolButton(
                    Icons.Default.Upload,
                    Modifier.weight(1f),
                    enabled = !busy && source != null
                ) { doUpload() }
                ToolButton(Icons.Default.Settings, Modifier.weight(1f)) {
                    showFormat = !showFormat
                }
                ToolButton(Icons.AutoMirrored.Filled.List, Modifier.weight(1f), onClick = onBack)
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (showFormat) {
                FormatPanel(
                    widthRatio = widthRatio,
                    onRatio = { widthRatio = it },
                    boxMod = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PreviewBg),
                contentAlignment = Alignment.Center
            ) {
                if (preview != null) {
                    Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color(0xFF9A9A9A),
                        modifier = Modifier.size(56.dp)
                    )
                }

                if (!message.isNullOrBlank()) {
                    Text(
                        text = message!!,
                        color = Color(0xFF333333),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                            .background(Color(0xCCFFFFFF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        SideTab(
            text = "보드판서식",
            boxMod = Modifier.align(Alignment.CenterStart),
            onClick = { showFormat = !showFormat }
        )
        SideTab(
            text = "목록으로",
            boxMod = Modifier.align(Alignment.CenterEnd),
            onClick = onBack
        )
    }
    } // end else (!showLiveCamera)

    editingField?.let { field ->
        val title = when (field) {
            EditField.WorkName -> "공사명"
            EditField.WorkType -> "공종"
            EditField.Location -> "위치"
            EditField.Content -> "내용"
            EditField.WorkDate -> "일자"
        }
        val value = when (field) {
            EditField.WorkName -> workName
            EditField.WorkType -> workType
            EditField.Location -> location
            EditField.Content -> content
            EditField.WorkDate -> workDate
        }
        var draft by remember(field) { mutableStateOf(value) }
        AlertDialog(
            onDismissRequest = { editingField = null },
            title = { Text("$title 수정") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = field != EditField.Content,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(if (field == EditField.WorkDate) "YYYY-MM-DD" else "내용을 입력하세요")
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (field) {
                            EditField.WorkName -> workName = draft.trim()
                            EditField.WorkType -> workType = draft.trim()
                            EditField.Location -> location = draft.trim()
                            EditField.Content -> content = draft.trim()
                            EditField.WorkDate -> workDate = normalizeDate(draft)
                        }
                        editingField = null
                    }
                ) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { editingField = null }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun BoardTable(
    workName: String,
    workType: String,
    location: String,
    content: String,
    workDate: String,
    onEdit: (EditField) -> Unit,
    boxMod: Modifier
) {
    Column(
        modifier = boxMod
            .background(Color.White)
            .border(1.dp, TableBorder)
    ) {
        TableRow("공사명", workName.ifBlank { " " }) { onEdit(EditField.WorkName) }
        TableRow("공종", workType.ifBlank { " " }) { onEdit(EditField.WorkType) }
        TableRow("위치", location.ifBlank { " " }) { onEdit(EditField.Location) }
        TableRow("내용", content.ifBlank { " " }) { onEdit(EditField.Content) }
        TableRow("일자", workDate.ifBlank { " " }) { onEdit(EditField.WorkDate) }
    }
}

@Composable
private fun TableRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(72.dp)
                .fillMaxHeight()
                .border(1.dp, TableBorder)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF222222))
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(1.dp, TableBorder)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = value,
                fontSize = 13.sp,
                color = Color(0xFF111111),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    boxMod: Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = boxMod
            .height(52.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(ButtonBg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) UiBlue else Color(0xFF99AACC),
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun SideTab(
    text: String,
    onClick: () -> Unit,
    boxMod: Modifier
) {
    Box(
        modifier = boxMod
            .width(28.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(UiBlue)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.toCharArray().joinToString("\n"),
            color = Color.White,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FormatPanel(
    widthRatio: Float,
    onRatio: (Float) -> Unit,
    boxMod: Modifier
) {
    Column(
        modifier = boxMod
            .background(Color.White, RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(6.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("보드판 위치", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(
            "카메라(또는 미리보기)에서 보드판을 손가락으로 드래그해 위치를 정하세요.",
            fontSize = 12.sp,
            color = Color(0xFF555555)
        )
        Text("보드판 크기 ${(widthRatio * 100).toInt()}%", fontSize = 13.sp)
        Slider(
            value = widthRatio,
            onValueChange = onRatio,
            valueRange = 0.25f..0.65f
        )
    }
}

private fun displayDate(iso: String): String = iso.replace('-', '.')

private fun normalizeDate(raw: String): String {
    val cleaned = raw.trim().replace('.', '-')
    return if (cleaned.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) cleaned
    else LocalDate.now().toString()
}

private enum class ShootOrientation { Portrait, Landscape }

private enum class FlashOption {
    Off,
    OnCapture,
    Torch
}

@Composable
private fun LiveCameraWithBoard(
    fields: BoardFields,
    offsetX: Float,
    offsetY: Float,
    widthRatio: Float,
    onOffsetXChange: (Float) -> Unit,
    onOffsetYChange: (Float) -> Unit,
    onWidthRatioChange: (Float) -> Unit,
    onCaptured: (Bitmap) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var capturing by remember { mutableStateOf(false) }
    var showBoardPanel by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    // 현재 화면 방향과 맞춰 시작 → 열자마자 회전하며 Activity가 재생성되는 깜빡임 방지
    var shootOrientation by remember {
        mutableStateOf(
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                ShootOrientation.Landscape
            } else {
                ShootOrientation.Portrait
            }
        )
    }
    var flashOption by remember { mutableStateOf(FlashOption.Off) }
    val bound = remember { AtomicBoolean(false) }

    BackHandler(onBack = onClose)

    // 카메라 화면을 떠날 때만 방향 잠금 해제
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            cameraExecutor.shutdown()
        }
    }

    DisposableEffect(shootOrientation) {
        activity?.requestedOrientation = when (shootOrientation) {
            ShootOrientation.Portrait -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            ShootOrientation.Landscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose { }
    }

    // 방향이 바뀌면 카메라 재연결
    DisposableEffect(lifecycleOwner, shootOrientation) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
                bound.set(true)
                cameraError = null
            }.onFailure { err ->
                bound.set(false)
                cameraError = err.message ?: "카메라를 열 수 없습니다."
                camera = null
            }
        }
        cameraProviderFuture.addListener(listener, mainExecutor)
        onDispose {
            runCatching {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            }
            bound.set(false)
            camera = null
        }
    }

    LaunchedEffect(flashOption, camera) {
        val cam = camera ?: return@LaunchedEffect
        when (flashOption) {
            FlashOption.Off -> {
                cam.cameraControl.enableTorch(false)
                imageCapture.flashMode = ImageCapture.FLASH_MODE_OFF
            }
            FlashOption.OnCapture -> {
                cam.cameraControl.enableTorch(false)
                imageCapture.flashMode = ImageCapture.FLASH_MODE_ON
            }
            FlashOption.Torch -> {
                imageCapture.flashMode = ImageCapture.FLASH_MODE_OFF
                cam.cameraControl.enableTorch(true)
            }
        }
    }

    val safeRatio = widthRatio.coerceIn(0.25f, 0.65f)
    val flashIcon = when (flashOption) {
        FlashOption.Off -> Icons.Default.FlashOff
        FlashOption.OnCapture -> Icons.Default.FlashOn
        FlashOption.Torch -> Icons.Outlined.FlashlightOn
    }
    val flashLabel = when (flashOption) {
        FlashOption.Off -> "플래시 끄기"
        FlashOption.OnCapture -> "촬영 시 켜짐"
        FlashOption.Torch -> "상시 켜짐"
    }

    fun takePhoto() {
        if (capturing || !bound.get()) return
        capturing = true
        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    ContextCompat.getMainExecutor(context).execute {
                        capturing = false
                        onCaptured(bitmap)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    ContextCompat.getMainExecutor(context).execute {
                        capturing = false
                        Toast.makeText(
                            context,
                            "촬영 실패: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    val isLandscape = shootOrientation == ShootOrientation.Landscape

    @Composable
    fun PreviewArea(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .statusBarsPadding()
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                val parentW = constraints.maxWidth.toFloat()
                val parentH = constraints.maxHeight.toFloat()
                val boardW = parentW * safeRatio
                var boardH by remember { mutableFloatStateOf(parentH * 0.28f) }
                val travelX = (parentW - boardW).coerceAtLeast(1f)
                val travelY = (parentH - boardH).coerceAtLeast(1f)

                var posX by remember { mutableFloatStateOf(offsetX.coerceIn(0f, 1f) * travelX) }
                var posY by remember { mutableFloatStateOf(offsetY.coerceIn(0f, 1f) * travelY) }
                var dragging by remember { mutableStateOf(false) }
                val onOffsetXChangeLatest by rememberUpdatedState(onOffsetXChange)
                val onOffsetYChangeLatest by rememberUpdatedState(onOffsetYChange)

                LaunchedEffect(offsetX, offsetY, travelX, travelY, dragging) {
                    if (!dragging) {
                        posX = offsetX.coerceIn(0f, 1f) * travelX
                        posY = offsetY.coerceIn(0f, 1f) * travelY
                    }
                }

                fun commitOffset() {
                    onOffsetXChangeLatest((posX / travelX).coerceIn(0f, 1f))
                    onOffsetYChangeLatest((posY / travelY).coerceIn(0f, 1f))
                }

                LiveBoardOverlay(
                    fields = fields,
                    widthFraction = safeRatio,
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = posX
                            translationY = posY
                        }
                        .onSizeChanged { size ->
                            boardH = size.height.toFloat().coerceAtLeast(1f)
                        }
                        .pointerInput(travelX, travelY) {
                            detectDragGestures(
                                onDragStart = { dragging = true },
                                onDragEnd = {
                                    dragging = false
                                    commitOffset()
                                },
                                onDragCancel = {
                                    dragging = false
                                    commitOffset()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    posX = (posX + dragAmount.x).coerceIn(0f, travelX)
                                    posY = (posY + dragAmount.y).coerceIn(0f, travelY)
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { showBoardPanel = !showBoardPanel },
                                onLongPress = { showBoardPanel = true }
                            )
                        }
                )

                Text(
                    text = "보드판을 잡고 드래그해 자유롭게 이동 · 탭=크기",
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .background(Color(0x66000000), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            if (showBoardPanel) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(0.78f)
                        .padding(12.dp)
                        .background(Color(0xCC111111), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "보드판을 드래그해 위치를 정하세요",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("크기 ${(safeRatio * 100).toInt()}%", color = Color.White, fontSize = 13.sp)
                    Slider(
                        value = safeRatio,
                        onValueChange = onWidthRatioChange,
                        valueRange = 0.25f..0.65f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            IconButton(
                onClick = {
                    camera?.cameraControl?.enableTorch(false)
                    onClose()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color(0x66000000), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
            }

            Text(
                text = flashLabel,
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
                    .background(Color(0x66000000), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )

            cameraError?.let { err ->
                Text(
                    text = err,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color(0xAA000000), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )
            }
        }
    }

    @Composable
    fun ControlButtons(horizontal: Boolean) {
        val rotationBtn = @Composable {
            SideControlButton(
                icon = Icons.Default.ScreenRotation,
                label = if (isLandscape) "가로" else "세로",
                onClick = {
                    shootOrientation =
                        if (isLandscape) ShootOrientation.Portrait
                        else ShootOrientation.Landscape
                }
            )
        }
        val flashBtn = @Composable {
            SideControlButton(
                icon = flashIcon,
                label = when (flashOption) {
                    FlashOption.Off -> "끄기"
                    FlashOption.OnCapture -> "촬영"
                    FlashOption.Torch -> "상시"
                },
                onClick = {
                    flashOption = when (flashOption) {
                        FlashOption.Off -> FlashOption.OnCapture
                        FlashOption.OnCapture -> FlashOption.Torch
                        FlashOption.Torch -> FlashOption.Off
                    }
                }
            )
        }
        val shutterBtn = @Composable {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color.White, CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(if (capturing) Color.LightGray else Color.White)
                    .clickable(enabled = !capturing) { takePhoto() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "촬영",
                    tint = Color.Black,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        if (horizontal) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rotationBtn()
                shutterBtn()
                flashBtn()
            }
        } else {
            Column(
                modifier = Modifier
                    .width(76.dp)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                rotationBtn()
                flashBtn()
                shutterBtn()
            }
        }
    }

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            PreviewArea(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(Color(0xFF111111))
                    .windowInsetsPadding(
                        WindowInsets.systemBars
                            .union(WindowInsets.displayCutout)
                            .union(WindowInsets.navigationBars)
                            .only(WindowInsetsSides.Top + WindowInsetsSides.Bottom)
                    )
            ) {
                ControlButtons(horizontal = false)
                Spacer(
                    modifier = Modifier
                        .windowInsetsEndWidth(
                            WindowInsets.navigationBars.union(WindowInsets.displayCutout)
                        )
                        .fillMaxHeight()
                )
                Spacer(modifier = Modifier.width(10.dp).fillMaxHeight())
            }
        }
    } else {
        // 세로: 촬영 버튼바를 하단에 두고 홈 버튼 영역과 겹치지 않게
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            PreviewArea(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111))
            ) {
                ControlButtons(horizontal = true)
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsBottomHeight(
                            WindowInsets.navigationBars.union(WindowInsets.displayCutout)
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SideControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(label, color = Color(0xFFDDDDDD), fontSize = 11.sp)
    }
}

@Composable
private fun LiveBoardOverlay(
    fields: BoardFields,
    widthFraction: Float,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        "공사명" to fields.workName,
        "공종" to fields.workType,
        "위치" to fields.location,
        "내용" to fields.content,
        "일자" to fields.workDate.replace('-', '.')
    )
    Column(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .background(Color(0xD9FFFFFF), RoundedCornerShape(4.dp))
            .border(1.5.dp, Color(0xFF145037), RoundedCornerShape(4.dp))
    ) {
        rows.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF283C32)
                    )
                }
                Text(
                    text = value.ifBlank { "-" },
                    fontSize = 11.sp,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                )
            }
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val rotation = image.imageInfo.rotationDegrees
    if (rotation == 0) return decoded
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
}
