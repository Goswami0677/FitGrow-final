package com.example.ui.screens.coach

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

import com.example.ui.theme.Indigo500

@Composable
fun CoachScreen(viewModel: MainViewModel, onBack: () -> Unit = {}) {
    CoachChatScreen(viewModel, onBack)
}

@Composable
fun CoachChatScreen(viewModel: MainViewModel, onBack: () -> Unit = {}) {
    val messages = viewModel.chatMessages.collectAsState().value
    var input by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            attachedImageUri = uri
        }
    }
    
    fun launchCamera() {
        imagePickerLauncher.launch("image/*")
    }
    
    LaunchedEffect(Unit) {
        viewModel.markCoachMessagesRead()
        if (messages.isEmpty()) {
            viewModel.sendUserMessage("Hi Coach, let's start!")
        }
        viewModel.triggerCheckInPromptIfNeeded()
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F12))) {
        
        Column(modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                androidx.compose.material3.IconButton(onClick = onBack) {
                    Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF222328), CircleShape)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFF9E82FF).copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.coach_avatar),
                        contentDescription = "Coach Avatar",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Personal Coach", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { msg ->
                val isUser = msg.sender == "USER"
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isUser) Color(0xFF128C7E) else Color(0xFF202C33),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .fillMaxWidth(if (msg.imageUri != null) 0.8f else 0.75f)
                    ) {
                        Column {
                            if (msg.imageUri != null) {
                                val mappedUri = if (msg.imageUri.startsWith("/")) "file://${msg.imageUri}" else msg.imageUri
                                AsyncImage(
                                    model = mappedUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().height(200.dp).padding(bottom = 8.dp).background(Color.Black, RoundedCornerShape(8.dp))
                                )
                            }
                            Text(msg.text, color = Color.White, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
        
        // Attachment Preview
        if (attachedImageUri != null) {
             Box(modifier = Modifier.padding(vertical = 8.dp).height(80.dp).background(Color.Black, RoundedCornerShape(8.dp))) {
                  val mappedUri = if (attachedImageUri!!.toString().startsWith("/")) "file://${attachedImageUri}" else attachedImageUri
                  AsyncImage(model = mappedUri, contentDescription = null, modifier = Modifier.fillMaxHeight())
             }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = { launchCamera() }) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Camera", tint = Color.Gray)
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { 
                    Text(
                        text = "Tap & tell your meal (e.g., 'Ate 4 eggs & 2 roti')", 
                        color = Color.Gray, 
                        fontSize = 13.sp, 
                        maxLines = 1, 
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    ) 
                },
                trailingIcon = {
                    Icon(androidx.compose.material.icons.Icons.Default.Mic, contentDescription = "Mic", tint = Color.Gray)
                },
                shape = RoundedCornerShape(24.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color(0xFF1E1E24),
                    unfocusedContainerColor = Color(0xFF1E1E24),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            IconButton(onClick = {
                if (input.isNotBlank() || attachedImageUri != null) {
                    viewModel.sendUserMessage(input, attachedImageUri?.toString())
                    input = ""
                    attachedImageUri = null
                }
            }) {
                 Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color(0xFF128C7E))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
    }
}
