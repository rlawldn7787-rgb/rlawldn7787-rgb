package com.woohaeng.board.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Logout
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.woohaeng.board.util.resolveMediaUrl
import coil.compose.AsyncImage
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
    val records by vm.records.collectAsState()
    val pending by vm.pendingCount.collectAsState()
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
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("우행통신 보드판")
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
                            vm.exportExcel(from, to, workName)
                        }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "엑셀")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "로그아웃")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCapture) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = yearExpanded,
                    onExpandedChange = { yearExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "${year}년",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("연도") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = yearExpanded,
                        onDismissRequest = { yearExpanded = false }
                    ) {
                        yearOptions.forEach { y ->
                            DropdownMenuItem(
                                text = { Text("${y}년") },
                                onClick = {
                                    year = y
                                    yearExpanded = false
                                }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = monthExpanded,
                    onExpandedChange = { monthExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "${month}월",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("월") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = monthExpanded,
                        onDismissRequest = { monthExpanded = false }
                    ) {
                        monthOptions.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m}월") },
                                onClick = {
                                    month = m
                                    monthExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = workName,
                onValueChange = { workName = it },
                label = { Text("공사명 검색") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            Text(
                "${year}년 ${month}월 · 공사명 검색 후 새로고침. 엑셀도 같은 조건입니다.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(records, key = { it.id }) { item ->
                    Surface(
                        tonalElevation = 2.dp,
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
                                    .height(160.dp)
                            )
                            Text(
                                text = item.workName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp)
                            )
                            Text(
                                text = "${item.workDate.take(10)} · ${item.authorName}",
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = "${item.workType} / ${item.location}",
                                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
