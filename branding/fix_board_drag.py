from pathlib import Path

p = Path(r"C:\Users\김쥬\woohaeng-board\apps\android\app\src\main\java\com\woohaeng\board\ui\CaptureScreen.kt")
text = p.read_text(encoding="utf-8")

# --- imports ---
if "detectDragGestures" not in text:
    text = text.replace(
        "import androidx.compose.foundation.clickable\n",
        "import androidx.compose.foundation.clickable\n"
        "import androidx.compose.foundation.gestures.detectDragGestures\n"
        "import androidx.compose.foundation.gestures.detectTapGestures\n"
        "import androidx.compose.foundation.layout.BoxWithConstraints\n"
        "import androidx.compose.foundation.layout.offset\n",
    )
if "onSizeChanged" not in text:
    text = text.replace(
        "import androidx.compose.ui.layout.ContentScale\n",
        "import androidx.compose.ui.layout.ContentScale\n"
        "import androidx.compose.ui.layout.onSizeChanged\n"
        "import androidx.compose.ui.input.pointer.pointerInput\n"
        "import androidx.compose.ui.unit.IntOffset\n",
    )
if "import kotlin.math.roundToInt" not in text:
    text = text.replace(
        "import java.util.concurrent.atomic.AtomicBoolean\n",
        "import java.util.concurrent.atomic.AtomicBoolean\n"
        "import kotlin.math.roundToInt\n",
    )

text = text.replace(
    "import com.woohaeng.board.util.BoardCorner\n",
    "",
)

# CaptureScreen state
old_state = """    var corner by remember { mutableStateOf(BoardCorner.BottomLeft) }
    var widthRatio by remember { mutableFloatStateOf(0.42f) }"""
new_state = """    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(1f) }
    var widthRatio by remember { mutableFloatStateOf(0.42f) }"""
if old_state not in text:
    raise SystemExit("state block missing")
text = text.replace(old_state, new_state)

text = text.replace(
    "val layout = BoardLayout(corner = corner, widthRatio = widthRatio)",
    "val layout = BoardLayout(offsetX = offsetX, offsetY = offsetY, widthRatio = widthRatio)",
)

old_live_call = """        LiveCameraWithBoard(
            fields = fields,
            corner = corner,
            widthRatio = widthRatio,
            onCornerChange = { corner = it },
            onWidthRatioChange = { widthRatio = it },
            onCaptured = { bitmap ->
                source = bitmap
                fromCamera = true
                showLiveCamera = false
            },
            onClose = { showLiveCamera = false }
        )"""
new_live_call = """        LiveCameraWithBoard(
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
        )"""
if old_live_call not in text:
    raise SystemExit("live call missing")
text = text.replace(old_live_call, new_live_call)

old_format_call = """                FormatPanel(
                    corner = corner,
                    widthRatio = widthRatio,
                    onCorner = { corner = it },
                    onRatio = { widthRatio = it },
                    boxMod = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )"""
new_format_call = """                FormatPanel(
                    widthRatio = widthRatio,
                    onRatio = { widthRatio = it },
                    boxMod = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )"""
if old_format_call not in text:
    raise SystemExit("format call missing")
text = text.replace(old_format_call, new_format_call)

# Replace FormatPanel function
start = text.index("@Composable\nprivate fun FormatPanel(")
end = text.index("private fun displayDate(")
new_format = '''@Composable
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

'''
text = text[:start] + new_format + text[end:]

# Replace LiveCameraWithBoard signature and overlay section via markers
# First replace function signature through flash setup - do carefully

old_sig = """fun LiveCameraWithBoard(
    fields: BoardFields,
    corner: BoardCorner,
    widthRatio: Float,
    onCornerChange: (BoardCorner) -> Unit,
    onWidthRatioChange: (Float) -> Unit,
    onCaptured: (Bitmap) -> Unit,
    onClose: () -> Unit
)"""
new_sig = """fun LiveCameraWithBoard(
    fields: BoardFields,
    offsetX: Float,
    offsetY: Float,
    widthRatio: Float,
    onOffsetXChange: (Float) -> Unit,
    onOffsetYChange: (Float) -> Unit,
    onWidthRatioChange: (Float) -> Unit,
    onCaptured: (Bitmap) -> Unit,
    onClose: () -> Unit
)"""
if old_sig not in text:
    raise SystemExit("sig missing")
text = text.replace(old_sig, new_sig)

# Remove overlayAlign and replace overlay + panel block
old_align = """    val safeRatio = widthRatio.coerceIn(0.25f, 0.65f)
    val overlayAlign = when (corner) {
        BoardCorner.BottomLeft -> Alignment.BottomStart
        BoardCorner.BottomRight -> Alignment.BottomEnd
        BoardCorner.TopLeft -> Alignment.TopStart
        BoardCorner.TopRight -> Alignment.TopEnd
    }
    val flashIcon = when (flashOption) {"""
new_align = """    val safeRatio = widthRatio.coerceIn(0.25f, 0.65f)
    val flashIcon = when (flashOption) {"""
if old_align not in text:
    raise SystemExit("align missing")
text = text.replace(old_align, new_align)

old_overlay = """            Box(
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
            }"""

new_overlay = """            BoxWithConstraints(
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
                val px = (offsetX.coerceIn(0f, 1f) * travelX)
                val py = (offsetY.coerceIn(0f, 1f) * travelY)

                LiveBoardOverlay(
                    fields = fields,
                    widthFraction = safeRatio,
                    modifier = Modifier
                        .offset { IntOffset(px.roundToInt(), py.roundToInt()) }
                        .onSizeChanged { boardH = it.height.toFloat().coerceAtLeast(1f) }
                        .pointerInput(travelX, travelY, offsetX, offsetY) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val nx = (offsetX * travelX + dragAmount.x).coerceIn(0f, travelX)
                                val ny = (offsetY * travelY + dragAmount.y).coerceIn(0f, travelY)
                                onOffsetXChange(nx / travelX)
                                onOffsetYChange(ny / travelY)
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { showBoardPanel = !showBoardPanel },
                                onLongPress = { showBoardPanel = true }
                            )
                        }
                )

                Text(
                    text = "보드판을 드래그해 위치 이동 · 탭하면 크기 조절",
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
            }"""

if old_overlay not in text:
    raise SystemExit("overlay block missing")
text = text.replace(old_overlay, new_overlay)

# Update LiveBoardOverlay to take modifier instead of onClick
old_live_board = """@Composable
private fun LiveBoardOverlay(
    fields: BoardFields,
    widthFraction: Float,
    onClick: () -> Unit
) {
    val rows = listOf(
        "공사명" to fields.workName,
        "공종" to fields.workType,
        "위치" to fields.location,
        "내용" to fields.content,
        "일자" to fields.workDate.replace('-', '.'),
        "작성자" to fields.author
    )
    Column(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .background(Color(0xD9FFFFFF), RoundedCornerShape(4.dp))
            .border(1.5.dp, Color(0xFF145037), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
    ) {"""

new_live_board = """@Composable
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
        "일자" to fields.workDate.replace('-', '.'),
        "작성자" to fields.author
    )
    Column(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .background(Color(0xD9FFFFFF), RoundedCornerShape(4.dp))
            .border(1.5.dp, Color(0xFF145037), RoundedCornerShape(4.dp))
    ) {"""

if old_live_board not in text:
    raise SystemExit("LiveBoardOverlay missing")
text = text.replace(old_live_board, new_live_board)

# Update preview area to allow drag on composed? Keep composed preview but also need drag after capture.
# Replace preview Image section with source + overlay when source present.
old_preview = """            Box(
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
            }"""

new_preview = """            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PreviewBg),
                contentAlignment = Alignment.Center
            ) {
                val src = source
                if (src != null) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    ) {
                        Image(
                            bitmap = src.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Fit 영역과 맞추기 어려워 합성 미리보기도 함께 표시하되,
                        // 위치는 드래그로 바꾸고 즉시 반영되도록 합성본을 갱신한다.
                        Image(
                            bitmap = (preview ?: src).asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    // 드래그 위치 조정 레이어 (투명 보드)
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    ) {
                        val parentW = constraints.maxWidth.toFloat()
                        val parentH = constraints.maxHeight.toFloat()
                        val safe = widthRatio.coerceIn(0.25f, 0.65f)
                        val boardW = parentW * safe
                        var boardH by remember { mutableFloatStateOf(parentH * 0.28f) }
                        val travelX = (parentW - boardW).coerceAtLeast(1f)
                        val travelY = (parentH - boardH).coerceAtLeast(1f)
                        val px = offsetX.coerceIn(0f, 1f) * travelX
                        val py = offsetY.coerceIn(0f, 1f) * travelY
                        LiveBoardOverlay(
                            fields = fields,
                            widthFraction = safe,
                            modifier = Modifier
                                .offset { IntOffset(px.roundToInt(), py.roundToInt()) }
                                .onSizeChanged { boardH = it.height.toFloat().coerceAtLeast(1f) }
                                .pointerInput(travelX, travelY, offsetX, offsetY) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val nx = (offsetX * travelX + dragAmount.x).coerceIn(0f, travelX)
                                        val ny = (offsetY * travelY + dragAmount.y).coerceIn(0f, travelY)
                                        offsetX = nx / travelX
                                        offsetY = ny / travelY
                                    }
                                }
                        )
                    }
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
            }"""

# Actually the preview with double Image is messy - better: only show composed preview OR only drag overlay on raw.
# Simpler preview: just composed bitmap (updates when offset changes via remember), and add a separate "위치 드래그" mode.
# Even simpler for main screen: keep composed preview only (drag mainly in camera). Revert preview to simple composed.

# I'll use simpler preview - composed only - drag in camera is the main UX
new_preview_simple = """            Box(
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
            }"""

# Keep original preview (no change) - skip replace if we use same
# Don't replace preview - leave as is

# Remove unused FilterChip import if FormatPanel no longer uses it - optional

p.write_text(text, encoding="utf-8")
print("patch applied")
# sanity
for needle in ["BoardCorner", "onCornerChange", "overlayAlign", "PhotoCamera"]:
    if needle in text:
        print("WARN still has", needle)
print("offsetX count", text.count("offsetX"))
