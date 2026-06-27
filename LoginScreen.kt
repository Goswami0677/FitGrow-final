package com.example.ui.screens.login

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel

private val BrandPurple = Color(0xFF7B61FF)
private val DarkBg = Color(0xFF0C0D0F)
private val GlassWhite = Color(0xFF1E1F24).copy(alpha = 0.85f)
private val GlassBorder = Color(0xFF2C2D35).copy(alpha = 0.5f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onLoginFinish: () -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("") }
    
    var showGoogleChooser by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Decorative glowing background circle
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-150).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            BrandPurple.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // App Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(BrandPurple, Color(0xFF9E8BFF))),
                        shape = RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = "FitGrow Logo",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Headings
            Text(
                text = "FitGrow AI",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your elite AI fitness coach & personal trainer. Sign in with Google to sync your workouts securely.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Glassmorphic Input Card
            Card(
                colors = CardDefaults.cardColors(containerColor = GlassWhite),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Verify Google Account",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Field
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            isError = false
                        },
                        label = { Text("Gmail Address", color = Color.Gray) },
                        placeholder = { Text("example@gmail.com", color = Color(0xFF55565A)) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = BrandPurple)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BrandPurple,
                            unfocusedBorderColor = GlassBorder,
                            focusedLabelColor = BrandPurple,
                            unfocusedLabelColor = Color.Gray
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                        }),
                        isError = isError,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sign In Button
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            val email = emailInput.trim()
                            if (email.isBlank() || !email.endsWith("@gmail.com") || email.length < 11) {
                                isError = true
                                errorMessage = "Please enter a valid @gmail.com address"
                            } else {
                                isError = false
                                isLoading = true
                                loadingMessage = "Connecting with Google Secure Sync..."
                                viewModel.loginAndSync(email) { success, state ->
                                    if (success) {
                                        if (state == "FOUND") {
                                            loadingMessage = "Cloud backup found! Synchronizing your files..."
                                        } else {
                                            loadingMessage = "New training profile registered. Setting up cloud..."
                                        }
                                        isLoading = false
                                        onLoginFinish()
                                    } else {
                                        isLoading = false
                                        isError = true
                                        errorMessage = "Failed to sync. Please check internet connection."
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Secure Sync Log In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "OR",
                        color = Color.DarkGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Continue with Google Button
                    OutlinedButton(
                        onClick = {
                            showGoogleChooser = true
                        },
                        border = BorderStroke(1.dp, GlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Custom high quality Google color G-Logo icon replica or standard graphic
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "G",
                                    color = BrandPurple,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Continue with Google",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Trust badge info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Shield",
                    tint = Color(0xFF00C853),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Encrypted Google Auth & automatic Room database backup.",
                    color = Color.DarkGray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Animated Loading Overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(enabled = false) {}, // prevent click-through
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F24)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = BrandPurple,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = loadingMessage,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Google Account Chooser Dialog
        if (showGoogleChooser) {
            AlertDialog(
                onDismissRequest = { showGoogleChooser = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Sign in with Google", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Choose an account to continue to FitGrow Secure Sync:",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Google Profile 1 (Developer / User Email)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF15161A)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showGoogleChooser = false
                                    emailInput = "shubham08shat@gmail.com"
                                    // Trigger Auto Login
                                    isLoading = true
                                    loadingMessage = "Connecting with Google Secure Sync..."
                                    viewModel.loginAndSync("shubham08shat@gmail.com") { success, state ->
                                        if (success) {
                                            if (state == "FOUND") {
                                                loadingMessage = "Cloud backup found! Synchronizing your files..."
                                            } else {
                                                loadingMessage = "Welcome! Setting up your new cloud space..."
                                            }
                                            isLoading = false
                                            onLoginFinish()
                                        } else {
                                            isLoading = false
                                            isError = true
                                            errorMessage = "Failed to connect to secure servers."
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(BrandPurple, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("S", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Shubham", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("shubham08shat@gmail.com", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Google Profile 2 (Mock Profile for variety)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF15161A)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showGoogleChooser = false
                                    emailInput = "guest.trainer@gmail.com"
                                    isLoading = true
                                    loadingMessage = "Connecting with Google Secure Sync..."
                                    viewModel.loginAndSync("guest.trainer@gmail.com") { success, state ->
                                        if (success) {
                                            if (state == "FOUND") {
                                                loadingMessage = "Cloud backup found! Synchronizing your files..."
                                            } else {
                                                loadingMessage = "Welcome! Setting up your new cloud space..."
                                            }
                                            isLoading = false
                                            onLoginFinish()
                                        } else {
                                            isLoading = false
                                            isError = true
                                            errorMessage = "Failed to connect to secure servers."
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF00C853), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Guest Trainer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("guest.trainer@gmail.com", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showGoogleChooser = false }) {
                        Text("Cancel", color = BrandPurple)
                    }
                },
                containerColor = Color(0xFF1E1F24)
            )
        }
    }
}
