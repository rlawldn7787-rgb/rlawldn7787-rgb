package com.woohaeng.board.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        containerColor = Brand.Bg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Brand.Panel,
                    titleContentColor = Brand.Navy,
                    navigationIconContentColor = Brand.Navy
                ),
                title = { Text("기록 상세", style = MaterialTheme.typography.titleLarge) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val item = record
            if (item == null) {
                Text("불러오는 중...", style = MaterialTheme.typography.bodyMedium, color = Brand.Muted)
            } else {
                Surface(
                    color = Brand.NavyDeep,
                    shape = SoftShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = resolveMediaUrl(item.photoUrl),
                        contentDescription = item.workName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(SoftShape)
                    )
                }
                Surface(
                    color = Brand.Panel,
                    shape = SoftShape,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(item.workName, style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(14.dp))
                        MetaRow("일자", item.workDate.take(10))
                        MetaRow("공종", item.workType.ifBlank { "-" })
                        MetaRow("위치", item.location.ifBlank { "-" })
                        MetaRow("내용", item.content.ifBlank { "-" })
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Brand.Muted,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Brand.CyanSoft)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}
