package com.example.flux.feature.trash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentManagerScreen(
    onNavigateUp: () -> Unit,
    viewModel: AttachmentManagerViewModel = hiltViewModel()
) {
    val orphanFiles by viewModel.orphanFiles.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val freedSpaceBytes by viewModel.freedSpaceBytes.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.scan(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("附件清理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isScanning) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("正在扫描未引用附件...")
            } else if (freedSpaceBytes > 0) {
                Text(
                    text = "清理成功，共释放 ${freedSpaceBytes / 1024} KB 空间。",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (orphanFiles.isEmpty()) {
                Text(
                    text = "没有发现未引用附件。",
                    style = MaterialTheme.typography.titleMedium
                )
            } else {
                val totalSize = orphanFiles.sumOf { it.length() } / 1024
                Text(
                    text = "扫描到 ${orphanFiles.size} 个未被任何日记引用的附件。",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "总计占用：$totalSize KB",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.clean(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("清理未引用附件")
                }
            }
        }
    }
}
