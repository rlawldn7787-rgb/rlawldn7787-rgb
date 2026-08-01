package com.woohaeng.board.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.Intent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.woohaeng.board.util.resolveMediaUrl
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    vm: AppViewModel,
    onCapture: () -> Unit,
    onOpen: (Int) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val records by vm.records.collectAsState()
    val pending by vm.pendingCount.collectAsState()
    val message by vm.message.collectAsState()
    val userName by vm.userName.collectAsState()
    val now = remember { LocalDate.now() }
    var year by remember { mutableIntStateOf(now.year) }
    var month by remember { mutableIntStateOf(now.monthValue) }
    var workName by remember { mutableStateOf("") }
    var yearExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }

    val yearOptions = remember(now.year) {
        ((now.year + 1) downTo (now.year - 6)).toList()
    }
    val monthOptions = remember { (1..12).toList() }

    fun monthBounds(): Pair<String, String> {
        val ym = YearMonth.of(year, month)
        return ym.atDay(1).toString() to ym.atEndOfMonth().toString()
    }

    fun reload() {
        val (from, to) = monthBounds()
        vm.flushQueue()
        vm.loadRecords(from, to, workName)
    }

    LaunchedEffect(year, month) {
        val (from, to) = monthBounds()
        vm.loadRecords(from, to, workName)
    }

    Scaffold(
        containerColor = Brand.Bg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Brand.Panel,
                    titleContentColor = Brand.Navy,
                    actionIconContentColor = Brand.Navy
                ),
                title = {
                    Column {
                        Text("우행통신 보드판", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${userName ?: ""} · 대기 ${pending}건",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                    }
                    IconButton(
                        onClick = {
                            val (from, to) = monthBounds()
                            vm.exportExcel(from, to, workName) { uri ->
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type =
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(send, "엑셀 공유")
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "엑셀")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "로그아웃")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCapture,
                containerColor = Brand.Cyan,
                contentColor = Brand.NavyDeep,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "촬영")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            if (pending > 0 || !message.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF4E5))
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (pending > 0) "업로드 대기 ${pending}건" else "알림",
                        color = Color(0xFF8A4B00),
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (!message.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message!!,
                            color = Color(0xFF5C3A00),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "새로고침을 누르면 대기 건을 다시 전송합니다.",
                        color = Color(0xFF7A5A2A),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            Surface(
                color = Brand.Panel,
                shape = SoftShape,
                tonalElevation = 0.dp,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "${year}년 ${month}월 현장 기록",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "연·월을 고르고 공사명으로 좁혀보세요",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SoftSelect(
                            label = "연도",
                            value = "${year}년",
                            expanded = yearExpanded,
                            onExpandedChange = { yearExpanded = it },
                            options = yearOptions.map { it to "${it}년" },
                            onSelect = {
                                year = it
                                yearExpanded = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                        SoftSelect(
                            label = "월",
                            value = "${month}월",
                            expanded = monthExpanded,
                            onExpandedChange = { monthExpanded = it },
                            options = monthOptions.map { it to "${it}월" },
                            onSelect = {
                                month = it
                                monthExpanded = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = workName,
                        onValueChange = { workName = it },
                        label = { Text("공사명 검색") },
                        singleLine = true,
                        shape = SoftShapeSm,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Text(
                "${records.size}건 · 검색 후 새로고침",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 14.dp, bottom = 10.dp)
            )

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SoftShape)
                        .background(Brand.Panel)
                        .padding(vertical = 40.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${year}년 ${month}월 기록이 없습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Brand.Muted
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(records, key = { it.id }) { item ->
                    Surface(
                        color = Brand.Panel,
                        shape = SoftShape,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(item.id) }
                    ) {
                        Column {
                            AsyncImage(
                                model = resolveMediaUrl(item.photoThumbUrl ?: item.photoUrl),
                                contentDescription = item.workName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(168.dp)
                                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                            )
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(item.workName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    item.workDate.take(10),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                if (item.workType.isNotBlank()) {
                                    Text(
                                        item.workType,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Brand.Navy,
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Brand.CyanSoft)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    item.location.ifBlank { "위치 미입력" },
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoftSelect(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<Pair<Int, String>>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = SoftShapeSm,
            colors = fieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { (key, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = { onSelect(key) }
                )
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Brand.Cyan,
    unfocusedBorderColor = Brand.Line,
    focusedLabelColor = Brand.Navy,
    cursorColor = Brand.Cyan,
    focusedContainerColor = Brand.BgTop,
    unfocusedContainerColor = Brand.BgTop
)
