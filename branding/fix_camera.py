from pathlib import Path

p = Path(r"C:\Users\김쥬\woohaeng-board\apps\android\app\src\main\java\com\woohaeng\board\ui\CaptureScreen.kt")
text = p.read_text(encoding="utf-8")

text = text.replace(
    "import java.util.concurrent.atomic.AtomicBoolean\nprivate val",
    "import java.util.concurrent.atomic.AtomicBoolean\n\nprivate val",
)

text = text.replace(
    "ToolButton(Icons.Default.PhotoCamera, Modifier.weight(1f))",
    "ToolButton(Icons.Default.PhotoLibrary, Modifier.weight(1f))",
)

old_perm = """    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showLiveCamera = true
    }"""
new_perm = """    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showLiveCamera = true
        } else {
            Toast.makeText(context, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }"""
if old_perm not in text:
    raise SystemExit("permission block not found")
text = text.replace(old_perm, new_perm)

start = text.index("@Composable\nfun LiveCameraWithBoard(")
end = text.index("@Composable\nprivate fun SideControlButton(")

new_fn = r'''@Composable
fun LiveCameraWithBoard(
    fields: BoardFields,
    corner: BoardCorner,
    widthRatio: Float,
    onCornerChange: (BoardCorner) -> Unit,
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
    val overlayAlign = when (corner) {
        BoardCorner.BottomLeft -> Alignment.BottomStart
        BoardCorner.BottomRight -> Alignment.BottomEnd
        BoardCorner.TopLeft -> Alignment.TopStart
        BoardCorner.TopRight -> Alignment.TopEnd
    }
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

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = overlayAlign
            ) {
                LiveBoardOverlay(
                    fields = fields,
                    widthFraction = safeRatio,
                    onClick = { showBoardPanel = !showBoardPanel }
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
                    Text("보드판 위치", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "좌하" to BoardCorner.BottomLeft,
                            "우하" to BoardCorner.BottomRight,
                            "좌상" to BoardCorner.TopLeft,
                            "우상" to BoardCorner.TopRight
                        ).forEach { (label, value) ->
                            val selected = corner == value
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) UiBlue else Color(0xFF333333))
                                    .clickable { onCornerChange(value) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
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

        Column(
            modifier = Modifier
                .width(72.dp)
                .fillMaxHeight()
                .background(Color(0xFF111111))
                .padding(vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SideControlButton(
                icon = Icons.Default.ScreenRotation,
                label = if (shootOrientation == ShootOrientation.Landscape) "가로" else "세로",
                onClick = {
                    shootOrientation =
                        if (shootOrientation == ShootOrientation.Landscape) {
                            ShootOrientation.Portrait
                        } else {
                            ShootOrientation.Landscape
                        }
                }
            )
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
    }
}

'''

text = text[:start] + new_fn + text[end:]
p.write_text(text, encoding="utf-8")
print("updated ok")
