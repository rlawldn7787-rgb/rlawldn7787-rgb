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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.woohaeng.board.util.resolveMediaUrl

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
    var workName by remember { mutableStateOf("") }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }

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
                    IconButton(onClick = { vm.flushQueue(); vm.loadRecords(from, to, workName) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                    }
                    IconButton(onClick = { vm.exportExcel(from, to, workName) }) {
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
                OutlinedTextField(
                    value = from,
                    onValueChange = { from = it },
                    label = { Text("시작일 YYYY-MM-DD") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = to,
                    onValueChange = { to = it },
                    label = { Text("종료일") },
                    modifier = Modifier.weight(1f)
                )
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
                "필터 적용은 새로고침 버튼을 누르세요. 엑셀은 현재 필터 기준입니다.",
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
