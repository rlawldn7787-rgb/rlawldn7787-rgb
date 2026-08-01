package com.woohaeng.board.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.woohaeng.board.R

@Composable
fun LoginScreen(vm: AppViewModel, onSuccess: () -> Unit) {
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    val message by vm.message.collectAsState()
    val busy by vm.busy.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Brand.NavyDeep, Brand.Navy, Color(0xFF123A5C))
                )
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 12.dp)
                .size(220.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Brand.Cyan.copy(alpha = 0.12f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SoftShape)
                    .background(Brand.Panel.copy(alpha = 0.97f))
                    .padding(horizontal = 22.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_woohaeng),
                    contentDescription = "우행통신",
                    modifier = Modifier.size(84.dp)
                )
                Text("우행통신", style = MaterialTheme.typography.headlineLarge)
                SoftField(
                    value = username,
                    onValueChange = { username = it },
                    label = "아이디"
                )
                SoftField(
                    value = password,
                    onValueChange = { password = it },
                    label = "비밀번호",
                    password = true
                )

                if (!message.isNullOrBlank()) {
                    Text(
                        text = message!!,
                        color = Brand.Danger,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brand.Danger.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }

                Button(
                    onClick = { vm.login(username, password) { if (it) onSuccess() } },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Brand.Cyan,
                        contentColor = Brand.NavyDeep,
                        disabledContainerColor = Brand.Cyan.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(50.dp)
                ) {
                    Text(
                        if (busy) "로그인 중..." else "로그인",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun SoftField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    password: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Brand.Cyan,
            unfocusedBorderColor = Brand.Line,
            focusedLabelColor = Brand.Navy,
            cursorColor = Brand.Cyan,
            focusedContainerColor = Brand.Panel,
            unfocusedContainerColor = Brand.Panel
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    )
}
