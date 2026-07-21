package com.woohaeng.board.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.woohaeng.board.util.resolveMediaUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(vm: AppViewModel, id: Int, onBack: () -> Unit) {
    val record by vm.selected.collectAsState()
    LaunchedEffect(id) { vm.loadDetail(id) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("기록 상세") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val item = record
            if (item == null) {
                Text("불러오는 중...")
            } else {
                AsyncImage(
                    model = resolveMediaUrl(item.photoUrl),
                    contentDescription = item.workName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                )
                Text(item.workName, modifier = Modifier.padding(top = 12.dp))
                Text("일자: ${item.workDate.take(10)}")
                Text("공종: ${item.workType}")
                Text("위치: ${item.location}")
                Text("내용: ${item.content}")
                Text("작성자: ${item.authorName}")
            }
        }
    }
}
