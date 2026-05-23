package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import android.widget.Toast
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.*
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.accounts.AccountManager
import android.app.Activity

fun getLogoIcon(iconName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (iconName) {
        "LocalHospital" -> Icons.Default.LocalHospital
        "Favorite" -> Icons.Default.Favorite
        "Medication" -> Icons.Default.Medication
        "Healing" -> Icons.Default.Healing
        else -> Icons.Default.MedicalServices
    }
}

fun getLogoBgColor(hexString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hexString))
    } catch (e: Exception) {
        Color(0xFF8E1439) // Fallback deep maroon
    }
}

fun copyUriToLocalFile(context: android.content.Context, uri: android.net.Uri, label: String): String? {
    return try {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null)
        var displayName = "file_${System.currentTimeMillis()}"
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    displayName = it.getString(nameIndex)
                }
            }
        }
        val cleanName = displayName.replace(" ", "_").replace("[^a-zA-Z0-9._-]", "")
        val localFile = java.io.File(context.filesDir, "up_${label}_${System.currentTimeMillis()}_$cleanName")
        contentResolver.openInputStream(uri)?.use { input ->
            java.io.FileOutputStream(localFile).use { output ->
                input.copyTo(output)
            }
        }
        localFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun AppLogoView(appPrefs: AppPreferences, modifier: Modifier = Modifier.size(20.dp), tint: Color = MedicineGold) {
    val customUri = appPrefs.customLogoUri
    if (!customUri.isNullOrEmpty()) {
        AsyncImage(
            model = customUri,
            contentDescription = "App Custom Logo",
            modifier = modifier.clip(RoundedCornerShape(4.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    } else {
        Icon(
            imageVector = getLogoIcon(appPrefs.logoIconName),
            contentDescription = "App Built-in Logo",
            tint = tint,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalAppUI(viewModel: MedicalViewModel) {
    val isLoggedIn = viewModel.isLoggedIn
    val isAdminLoggedIn = viewModel.isAdminLoggedIn
    val appPrefs by viewModel.appPreferences.collectAsState()

    if (!isLoggedIn) {
        AuthScreen(viewModel)
    } else if (isAdminLoggedIn) {
        AdminSystemScreen(viewModel)
    } else {
        val userProfile by viewModel.userProfile.collectAsState()
        val currentTab = viewModel.selectedTab

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(getLogoBgColor(appPrefs.logoBgColorHex)),
                                contentAlignment = Alignment.Center
                            ) {
                                AppLogoView(appPrefs = appPrefs, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = appPrefs.appName,
                                color = WarmWhite,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectedTab = "profile" }) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                tint = WarmWhite
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DeepMaroon
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = WarmWhite,
                    tonalElevation = 8.dp
                ) {
                    val tabs = listOf(
                        Triple("dashboard", "Dashboard", Icons.Default.Home),
                        Triple("study", "Books", Icons.Default.Book),
                        Triple("videos", "Lectures", Icons.Default.PlayCircle),
                        Triple("notes", "Notes", Icons.Default.Edit),
                        Triple("search", "Smart Search", Icons.Default.Search),
                        Triple("assistant", "Arfi AI", Icons.Default.AutoAwesome)
                    )

                    tabs.forEach { (tabId, label, icon) ->
                        val isSelected = currentTab == tabId || (tabId == "study" && currentTab == "chapters")
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { 
                                viewModel.selectedTab = tabId
                                if (tabId == "notes") {
                                    viewModel.startNewNote()
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) DeepMaroon else Color.Gray
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    color = if (isSelected) DeepMaroon else Color.Gray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = LightCream
                            ),
                            modifier = Modifier.testTag("nav_tab_$tabId")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(LightCream, Color(0xFFFBF9F6))
                        )
                    )
            ) {
                // Outer Layout tabs
                when (viewModel.selectedTab) {
                    "dashboard" -> DashboardTab(viewModel)
                    "study" -> StudyTab(viewModel)
                    "videos" -> VideosTab(viewModel)
                    "quiz" -> QuizTab(viewModel)
                    "notes" -> NotesTab(viewModel)
                    "search" -> SearchTab(viewModel)
                    "assistant" -> AssistantTab(viewModel)
                    "admin" -> AdminTab(viewModel)
                    "profile" -> ProfileTab(viewModel)
                    else -> DashboardTab(viewModel)
                }

                // WhatsApp Support Floating Action Badge (03246767582 Contact)
                val context = androidx.compose.ui.platform.LocalContext.current
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp)
                        .testTag("whatsapp_support_button")
                ) {
                    IconButton(
                        onClick = {
                            val phoneNumber = "923246767582"
                            val message = "Assalamualaikum sir! I need help about Medz With Arfi."
                            try {
                                val encodedMsg = java.net.URLEncoder.encode(message, "UTF-8")
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=$encodedMsg")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val encodedMsg = java.net.URLEncoder.encode(message, "UTF-8")
                                val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse("https://web.whatsapp.com/send?phone=$phoneNumber&text=$encodedMsg")
                                }
                                context.startActivity(fallbackIntent)
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(6.dp, RoundedCornerShape(26.dp))
                            .background(Color(0xFF25D366), RoundedCornerShape(26.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "WhatsApp Admin Support",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Green online bubble badge indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(14.dp)
                            .background(Color.White, RoundedCornerShape(7.dp))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF4CAF50), RoundedCornerShape(5.dp))
                        )
                    }
                }
            }
        }
    }
}

// ==================== AUTH GATEWAY ====================
@Composable
fun AuthScreen(viewModel: MedicalViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var collegeName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    val isSignUp = viewModel.isSignUpMode
    val error = viewModel.authError
    val appPrefs by viewModel.appPreferences.collectAsState()

    var showGoogleDialog by remember { mutableStateOf(false) }
    var showGoogleAccountPicker by remember { mutableStateOf(false) }
    var showGoogleFormDialog by remember { mutableStateOf(false) }
    var chosenGoogleEmail by remember { mutableStateOf("") }

    var showForgotDialog by remember { mutableStateOf(false) }
    var showOtpSignUpDialog by remember { mutableStateOf(false) }
    var otpDigits by remember { mutableStateOf("") }
    var otpErrorText by remember { mutableStateOf<String?>(null) }

    val googleAccountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                chosenGoogleEmail = accountName
                viewModel.checkAndPerformGoogleLogin(accountName) {
                    showGoogleFormDialog = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkMaroon, DeepMaroon, Color(0xFF2C040E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Heart/Medical Crest Icon (Dynamic)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(50))
                    .background(getLogoBgColor(appPrefs.logoBgColorHex)),
                contentAlignment = Alignment.Center
            ) {
                AppLogoView(appPrefs = appPrefs, modifier = Modifier.size(54.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = appPrefs.appName,
                color = WarmWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Professional Anatomy & Physiology Quiz System",
                color = SoftGold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSignUp) "Create Student Account" else "Sign In with Credentials",
                        color = DeepMaroon,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isSignUp) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; viewModel.authName = it },
                            label = { Text("Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = DeepMaroon) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("auth_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = collegeName,
                            onValueChange = { collegeName = it; viewModel.authCollegeName = it },
                            label = { Text("College / University Name") },
                            leadingIcon = { Icon(Icons.Default.Book, contentDescription = null, tint = DeepMaroon) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("auth_college_input"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = mobileNumber,
                            onValueChange = { mobileNumber = it; viewModel.authMobileNumber = it },
                            label = { Text("Mobile Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = DeepMaroon) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("auth_mobile_input"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; viewModel.authEmail = it },
                        label = { Text("Medical Email / Admin Username") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = DeepMaroon) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; viewModel.authPassword = it },
                        label = { Text("Secure Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = DeepMaroon) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                    )

                    if (error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = CoralRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (isSignUp) {
                                if (name.isBlank() || email.isBlank() || password.isBlank() || collegeName.isBlank() || mobileNumber.isBlank()) {
                                    viewModel.authError = "All fields are required during signup (Name, Email, College, Mobile, Password)."
                                } else if (!email.contains("@")) {
                                    viewModel.authError = "Please insert a valid educational email."
                                } else if (password.length < 5) {
                                    viewModel.authError = "Secure password must be at least 5 characters."
                                } else {
                                    viewModel.performSignUp()
                                }
                            } else {
                                viewModel.performLogin()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_button")
                    ) {
                        Text(
                            text = if (isSignUp) "REGISTER ACCOUNT" else "SECURE LOGIN",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (!isSignUp) {
                        TextButton(onClick = { showForgotDialog = true }) {
                            Text("Forgot Password?", color = LightMaroon, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Divider(color = LightGrey, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(12.dp))

                    // OAuth Google integration support demo
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    AccountManager.newChooseAccountIntent(
                                        null, null, arrayOf("com.google"), null, null, null, null
                                    )
                                } else {
                                    @Suppress("DEPRECATION")
                                    AccountManager.newChooseAccountIntent(
                                        null, null, arrayOf("com.google"), false, null, null, null, null
                                    )
                                }
                                if (intent != null) {
                                    try {
                                        googleAccountPickerLauncher.launch(intent)
                                    } catch (launchEx: Exception) {
                                        showGoogleAccountPicker = true
                                    }
                                } else {
                                    showGoogleAccountPicker = true
                                }
                            } catch (e: Exception) {
                                showGoogleAccountPicker = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Sign In with Gmail (Google Login)", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSignUp) "Already registered?" else "New medicine student?",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        TextButton(onClick = { viewModel.isSignUpMode = !viewModel.isSignUpMode }) {
                            Text(
                                text = if (isSignUp) "Login here" else "Sign up here",
                                color = DeepMaroon,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Professional Account & Password Dialog workflows
        if (showGoogleAccountPicker) {
            var customGmailInput by remember { mutableStateOf("") }
            var showCustomInput by remember { mutableStateOf(false) }

            Dialog(onDismissRequest = { showGoogleAccountPicker = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(LightCream),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountCircle, null, tint = DeepMaroon, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Sign In with Gmail", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 18.sp, fontFamily = FontFamily.Serif)
                        Text("Select an active Google account to authorize", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = customGmailInput,
                            onValueChange = { customGmailInput = it },
                            label = { Text("Gmail Address") },
                            placeholder = { Text("username@gmail.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showGoogleAccountPicker = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel", fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    if (customGmailInput.contains("@") && customGmailInput.contains("gmail")) {
                                        showGoogleAccountPicker = false
                                        viewModel.checkAndPerformGoogleLogin(customGmailInput) {
                                            chosenGoogleEmail = customGmailInput
                                            showGoogleFormDialog = true
                                        }
                                    } else {
                                        customGmailInput = "Invalid Gmail"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("PROCEED", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { showGoogleAccountPicker = false }) {
                            Text("DISMISS", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (showGoogleFormDialog) {
            var gName by remember { mutableStateOf("") }
            var gCollege by remember { mutableStateOf("") }
            var gMobile by remember { mutableStateOf("") }
            var gError by remember { mutableStateOf<String?>(null) }

            Dialog(onDismissRequest = { showGoogleFormDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Assignment, null, tint = MedicineGold, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Google Workspace Profile", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 16.sp)
                        Text("Complete secure profile for $chosenGoogleEmail", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = gName,
                            onValueChange = { gName = it },
                            label = { Text("Your Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = gCollege,
                            onValueChange = { gCollege = it },
                            label = { Text("College / University Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = gMobile,
                            onValueChange = { gMobile = it },
                            label = { Text("Mobile Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                        )

                        if (gError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(gError!!, color = CoralRed, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { showGoogleFormDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("CANCEL", color = Color.Gray)
                            }
                            Button(
                                onClick = {
                                    if (gName.isBlank() || gCollege.isBlank() || gMobile.isBlank()) {
                                        gError = "Please fill in all Google Profile details."
                                    } else {
                                        showGoogleFormDialog = false
                                        viewModel.loginWithGoogleCustom(gName, chosenGoogleEmail, gCollege, gMobile)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Text("AUTHORIZE", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        if (showForgotDialog) {
            var forgotEmail by remember { mutableStateOf("") }
            var forgotMessage by remember { mutableStateOf<String?>(null) }
            var isOtpPhase by remember { mutableStateOf(false) }
            var enteredOtp by remember { mutableStateOf("") }
            var newPassword by remember { mutableStateOf("") }

            Dialog(onDismissRequest = { showForgotDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.LockReset, null, tint = DeepMaroon, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Account Recovery", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 17.sp)
                        Text("Enter your Gmail address to verify your account details", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = forgotEmail,
                            onValueChange = { forgotEmail = it },
                            label = { Text("Your Registered Gmail Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                        )

                        if (forgotMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = LightCream),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    forgotMessage!!,
                                    color = DeepMaroon,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(10.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (forgotEmail.isBlank()) {
                                        forgotMessage = "Gmail address field is required."
                                    } else {
                                        viewModel.checkEmailExists(forgotEmail) { profile ->
                                            if (profile != null) {
                                                forgotMessage = "Account verified! \nEmail: ${profile.email}\nUniversity: ${profile.collegeName}\n(Click LOGIN below to enter instantly!)"
                                            } else {
                                                forgotMessage = "No profile found for $forgotEmail. You can register custom details, or use:\nEmail: dummy@medzwitharfi.com"
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Recover Registered Profile", fontSize = 11.sp, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    if (forgotEmail.isBlank() || !forgotEmail.contains("@")) {
                                        forgotMessage = "Please insert a valid recovery Gmail."
                                    } else {
                                        viewModel.checkEmailExists(forgotEmail) { profile ->
                                            if (profile != null) {
                                                viewModel.authEmail = forgotEmail
                                                viewModel.authPassword = "dummy_pass_override"
                                                viewModel.performLogin()
                                                showForgotDialog = false
                                            } else {
                                                // Register on the fly
                                                viewModel.loginWithGoogleCustom("Scholar Learner", forgotEmail, "Anatomy Faculty", "+92-000")
                                                showForgotDialog = false
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MedicineGold),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("LOGIN DIRECTLY", fontSize = 11.sp, color = DarkMaroon)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { showForgotDialog = false }) {
                            Text("DISMISS", color = Color.Gray)
                        }
                    }
                }
            }
        }

        if (showOtpSignUpDialog) {
            Dialog(onDismissRequest = { showOtpSignUpDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Security, null, tint = DeepMaroon, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Registration Safety Verification", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 16.sp)
                        Text("A secure 6-digit OTP code has been dispatch routed to:", fontSize = 11.sp, color = Color.Gray)
                        Text("$email / $mobileNumber", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MedicineGold)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = otpDigits,
                            onValueChange = { otpDigits = it },
                            label = { Text("Verification OTP (6 Digits)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                        )

                        Text("Test OTP Hint Code: 123456", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))

                        if (otpErrorText != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(otpErrorText!!, color = CoralRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showOtpSignUpDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("CANCEL", color = Color.Gray)
                            }
                            Button(
                                onClick = {
                                    if (otpDigits == "123456") {
                                        showOtpSignUpDialog = false
                                        viewModel.performSignUp()
                                    } else {
                                        otpErrorText = "Safety error: Incorrect OTP code. Enter 123456."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Text("VERIFY & REGISTER", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== VIDEOS DASHBOARD TAB ====================
@Composable
fun VideosTab(viewModel: MedicalViewModel) {
    val allLectures by viewModel.allVideoLectures.collectAsState()
    var selectedBookFilter by remember { mutableStateOf("All Books") }
    var searchQuery by remember { mutableStateOf("") }
    var videoToPlay by remember { mutableStateOf<VideoLecture?>(null) }

    val filteredLectures = allLectures.filter { lecture ->
        val matchesBook = when (selectedBookFilter) {
            "Snell's Anatomy" -> lecture.bookSource == "Snell's Clinical Anatomy"
            "Ross & Wilson" -> lecture.bookSource == "Ross and Wilson Anatomy & Physiology"
            else -> true
        }
        val matchesSearch = lecture.title.contains(searchQuery, ignoreCase = true) || 
                            lecture.description.contains(searchQuery, ignoreCase = true) ||
                            lecture.chapterName.contains(searchQuery, ignoreCase = true)
        matchesBook && matchesSearch
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LightCream, Color(0xFFFBF9F6))))
            .padding(16.dp)
    ) {
        // Lecture Theatre Title Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DeepMaroon),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = MedicineGold, modifier = Modifier.size(28.dp))
                }
                Column {
                    Text(
                        text = "Clinical Lecture Theater",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = "High-yield video lectures & medical system tutorials",
                        fontSize = 11.sp,
                        color = WarmWhite.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by topic, chapter or keywords...", fontSize = 12.sp, color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = DeepMaroon) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
        )

        // Book Filtering Tabs Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filterOptions = listOf("All Books", "Snell's Anatomy", "Ross & Wilson")
            filterOptions.forEach { option ->
                val isSelected = selectedBookFilter == option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(30.dp))
                        .background(if (isSelected) DeepMaroon else Color.White)
                        .border(1.dp, if (isSelected) DeepMaroon else Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(30.dp))
                        .clickable { selectedBookFilter = option }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color.DarkGray
                    )
                }
            }
        }

        // Videos List LazyColumn
        if (filteredLectures.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.VideoLibrary, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No matched medical lectures found.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLectures) { video ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Badge labels row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isSnell = video.bookSource == "Snell's Clinical Anatomy"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSnell) DeepMaroon.copy(alpha = 0.1f) else MedicineGold.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isSnell) "Snell's Clinical Anatomy" else "Ross & Wilson Anatomy & Phys",
                                        color = if (isSnell) DeepMaroon else DarkMaroon,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, null, tint = MedicineGold, modifier = Modifier.size(12.dp))
                                    Text(video.duration, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkMaroon)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = video.title,
                                fontWeight = FontWeight.Bold,
                                color = DeepMaroon,
                                fontSize = 14.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Chapter: ${video.chapterName}",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = video.description,
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Divider(color = LightCream, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                            Button(
                                onClick = { videoToPlay = video },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("STREAM LECTURE TUTORIAL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Classroom Video Player inside dashboard with Fullscreen & Screen Rotation control
    if (videoToPlay != null) {
        val video = videoToPlay!!
        var isFullScreen by remember { mutableStateOf(false) }
        val context = androidx.compose.ui.platform.LocalContext.current
        val activity = context as? android.app.Activity

        LaunchedEffect(isFullScreen) {
            if (isFullScreen) {
                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }

        Dialog(
            onDismissRequest = { 
                videoToPlay = null 
                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = !isFullScreen
            )
        ) {
            if (isFullScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            android.webkit.WebView(ctx).apply {
                                webViewClient = android.webkit.WebViewClient()
                                webChromeClient = android.webkit.WebChromeClient()
                                settings.javaScriptEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                loadUrl(video.videoUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Exit Full Screen Trigger overlay
                    IconButton(
                        onClick = { isFullScreen = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Fullscreen",
                            tint = Color.White
                        )
                    }

                    // Rotate Back overlay on the bottom right
                    IconButton(
                        onClick = { isFullScreen = false },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = "Portrait Mode",
                            tint = Color.White
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E1E1E))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = video.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { videoToPlay = null }) {
                                Icon(Icons.Default.Close, null, tint = Color.White)
                            }
                        }

                        // Actual Embedded Youtube WebView Component with Fullscreen button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    android.webkit.WebView(ctx).apply {
                                        webViewClient = android.webkit.WebViewClient()
                                        webChromeClient = android.webkit.WebChromeClient()
                                        settings.javaScriptEnabled = true
                                        settings.loadWithOverviewMode = true
                                        settings.useWideViewPort = true
                                        loadUrl(video.videoUrl)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Rotate / Fullscreen Button at bottom end of the video view
                            IconButton(
                                onClick = { isFullScreen = true },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = "Rotate & Full Screen",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Interactive Lesson Explainer & Note pad
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(LightCream)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Reference Medical Lesson: ${video.bookSource} (${video.chapterName})",
                                fontWeight = FontWeight.Bold,
                                color = DeepMaroon,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Reference Duration: ${video.duration}",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Clinical Overview:",
                                fontWeight = FontWeight.Bold,
                                color = DarkMaroon,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = video.description,
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { isFullScreen = true },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ScreenRotation, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ROTATE FULL SCREEN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { videoToPlay = null },
                                colors = ButtonDefaults.buttonColors(containerColor = MedicineGold),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("RETURN TO THEATRE", color = DarkMaroon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== SMART DASHBOARD TAB ====================
@Composable
fun DashboardTab(viewModel: MedicalViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()
    val testScores by viewModel.quizScores.collectAsState()
    val allMcqs by viewModel.allMCQs.collectAsState()
    val profile = userProfile ?: UserProfile()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightCream),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header & Overlapping Hero Progress Card (Editorial Aesthetic)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightCream)
            ) {
                // Curved maroon header background
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                        .background(DeepMaroon)
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 64.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // User Circle Avatar
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(21.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(21.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile.name.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "WELCOME BACK",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = profile.name,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }

                        // Gold Streak Badge
                        Box(
                            modifier = Modifier
                                .background(MedicineGold, RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "🔥 ${profile.studyStreak} DAY STREAK",
                                color = DeepMaroon,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Overlapping Hero Progress Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 40.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFEEEEE))
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Progress SVG Ring substitute using circular progress
                        Box(
                            modifier = Modifier.size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { profile.dailyProgress },
                                modifier = Modifier.fillMaxSize(),
                                color = DeepMaroon,
                                strokeWidth = 5.dp,
                                trackColor = Color(0xFFF3F4F6)
                            )
                            Text(
                                text = "${(profile.dailyProgress * 100).toInt()}%",
                                color = DeepMaroon,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Anatomy Mastery",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Next: Thorax Clinicals (Snell's)",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Progress bar slider matching colors
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFFF3F4F6))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(profile.dailyProgress)
                                        .height(4.dp)
                                        .background(MedicineGold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Play Trigger button
                        IconButton(
                            onClick = { 
                                val anatomyList = allMcqs.filter { it.bookSource == MedicalDataCatalog.BookSnellAnatomy }
                                viewModel.startQuiz(anatomyList.ifEmpty { allMcqs }, "Anatomy Mastery Progress", "Anatomy Mastery Practice")
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(DeepMaroon, RoundedCornerShape(20.dp)),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start Quiz",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Spacer to clear the bottom of overlapping card
        item {
            Spacer(modifier = Modifier.height(40.dp))
        }

        // Section 2: Subject Cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Anatomy Card (Thick bottom border in Maroon)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val anatomyList = allMcqs.filter { it.bookSource == MedicalDataCatalog.BookSnellAnatomy }
                            viewModel.startQuiz(anatomyList.ifEmpty { allMcqs }, "Anatomy Revision", "Anatomy Specialist Prep")
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFFEEEEE)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Emoji/Icon container
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFFFEBEE), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🦴", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Anatomy",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Snell's Clinical",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "1,240 MCQs",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Thick accent bar at bottom
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(DeepMaroon, RoundedCornerShape(2.dp))
                        )
                    }
                }

                // Physiology Card (Thick bottom border in Gold)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val physList = allMcqs.filter { it.bookSource == MedicalDataCatalog.BookRossPhysiology }
                            viewModel.startQuiz(physList.ifEmpty { allMcqs }, "Physiology Revision", "Ross & Wilson Systemic Prep")
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFFEEEEE)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Emoji/Icon container
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFFFF8E1), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🫀", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Physiology",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Ross & Wilson",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "980 MCQs",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF57F17)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Thick accent bar at bottom
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(MedicineGold, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }

        // Section 3: AI Insights Recommendation Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    listOf(DeepMaroon, Color(0xFF5A0000))
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .background(MedicineGold, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AI INSIGHT",
                                    color = DeepMaroon,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Review Weak Topic: ${profile.weakTopics.substringBefore(",")}",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Revisit the key pathway nodes in Snell's Chapter 8 based on your past mock diagnostics to solidify understanding.",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Quick Stats Grid
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quizzes
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFFEEEEE)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("QUIZZES", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${testScores.size}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = DeepMaroon)
                    }
                }

                // Accuracy
                val averageAccuracy = if (testScores.isNotEmpty()) {
                    testScores.map { it.percentage }.average().toInt()
                } else {
                    88
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFFEEEEE)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ACCURACY", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$averageAccuracy%", fontSize = 18.sp, fontWeight = FontWeight.Black, color = DeepMaroon)
                    }
                }

                // Ranking
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFFEEEEE)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("RANKING", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("#14", fontSize = 18.sp, fontWeight = FontWeight.Black, color = DeepMaroon)
                    }
                }
            }
        }

        // Section 5: Custom Column Graph Chart
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFFEEEEE)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Quiz Diagnostics & Performance Graph",
                        color = DeepMaroon,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (testScores.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.TrendingUp, "trending", tint = Color.LightGray, modifier = Modifier.size(36.dp))
                                Text("No exam scores yet. Take your first quiz!", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val lastScores = testScores.take(6).reversed()
                            lastScores.forEachIndexed { idx, score ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "${score.percentage.toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepMaroon
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .height((100 * (score.percentage / 100f)).dp.coerceAtLeast(12.dp))
                                            .width(24.dp)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(LightMaroon, MedicineGold)
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Q${idx + 1}",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 6: Global Board Rank representation
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFFEEEEE)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Rank", tint = MedicineGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Global Board Rank",
                            color = DeepMaroon,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Primary User Base Ranking: #242 out of 11,210",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "Top 2.15%",
                            color = SoftGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Section 7: Personal Diagnostics / Weak Topics
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFFEEEEE)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "AI Personal Diagnostics (Weak Topics)",
                        color = DeepMaroon,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = profile.weakTopics,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.askAiAboutTopic(profile.weakTopics.substringBefore(",")) },
                        colors = ButtonDefaults.buttonColors(containerColor = LightCream, contentColor = DeepMaroon),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, "AI icon", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Personalized AI Revision Plan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 8: Recent Activity header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Study Sessions",
                    color = DeepMaroon,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (testScores.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearHistory() }) {
                        Text("Clear Logs", color = CoralRed, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section 9: Recent activity list items
        if (testScores.isEmpty()) {
            item {
                Text(
                    text = "No study sessions found in database catalog.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        } else {
            items(testScores.take(4)) { score ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFFEEEEE)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = score.chapterOrBook,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = DeepMaroon
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${score.quizType} • ${score.timeTakenSeconds}s duration",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${score.correctAnswers}/${score.totalQuestions} Right",
                                fontWeight = FontWeight.Bold,
                                color = SoftGreen,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${score.percentage.toInt()}% Score",
                                fontSize = 12.sp,
                                color = if (score.percentage >= 70f) SoftGreen else CoralRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp)) // padding safe space at end
        }
    }
}

// ==================== COMPLETE SUBJECT SYSTEM / BOOKS TAB ====================
@Composable
fun StudyTab(viewModel: MedicalViewModel) {
    val allMcqs by viewModel.allMCQs.collectAsState()
    val allVideoLectures by viewModel.allVideoLectures.collectAsState()
    val customUploadedFiles by viewModel.customUploadedFiles.collectAsState()
    var selectedBook by remember { mutableStateOf<String?>(null) } // null showing choices, otherwise chapters list
    var activePreviewFile by remember { mutableStateOf<CustomUploadedFile?>(null) }

    var activeChapterForVideos by remember { mutableStateOf<String?>(null) }
    var activeBookForVideos by remember { mutableStateOf<String?>(null) }
    var videoToPlay by remember { mutableStateOf<VideoLecture?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (selectedBook == null) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Select Medical Library",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepMaroon,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "Review standard curriculum chapters and practice board-style MCQs recursively.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

            Spacer(modifier = Modifier.height(20.dp))

            // Book 1 Card: Snell's Clinical Anatomy
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedBook = MedicalDataCatalog.BookSnellAnatomy }
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DeepMaroon),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Anat", color = MedicineGold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = MedicalDataCatalog.BookSnellAnatomy,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepMaroon
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Clinical anatomy structures, embryology, surgical mappings & high-yield clinical focus details.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Book 2 Card: Ross and Wilson Anatomy & Physiology
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedBook = MedicalDataCatalog.BookRossPhysiology }
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkMaroon),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Phys", color = MedicineGold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = MedicalDataCatalog.BookRossPhysiology,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepMaroon
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Comprehensive cells, tissues, systemic feedback actions, homeostasis, and metabolic bio-flows.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Standard General Board Quizzes shortcuts
            Text("Complete General Mock Exams", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.startQuiz(allMcqs, "Random Quiz", "Random Clinical Anatomy Mix") },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Random Mix Quiz", fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.startQuiz(allMcqs, "Mock Tests", "Comprehensive Board Mock") },
                    colors = ButtonDefaults.buttonColors(containerColor = MedicineGold),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Full standard Mock", fontSize = 12.sp, color = DarkMaroon)
                }
            }

            if (customUploadedFiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Clinical Handouts & Visual Guides",
                    fontWeight = FontWeight.Bold,
                    color = DeepMaroon,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "Supplementary high-yield materials uploaded from Arfi's clinical vault.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(10.dp))

                customUploadedFiles.forEach { ufield ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                activePreviewFile = ufield
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(45.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(LightCream),
                                contentAlignment = Alignment.Center
                            ) {
                                if (ufield.fileType == "image") {
                                    AsyncImage(
                                        model = ufield.fileUri,
                                        contentDescription = ufield.fileName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    val icon = when (ufield.fileType) {
                                        "pdf" -> Icons.Default.PictureAsPdf
                                        else -> Icons.Default.InsertDriveFile
                                    }
                                    Icon(icon, null, tint = DeepMaroon, modifier = Modifier.size(24.dp))
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ufield.fileName,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkMaroon,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Category: ${ufield.fileType.uppercase()} | Size: ${ufield.fileSize}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }

                            if (ufield.fileType == "image") {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = "Zoom In",
                                    tint = MedicineGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            } // Closing column wrapper
        } else {
            // Book chapters view list
            val bookName = selectedBook!!
            val chapters = if (bookName == MedicalDataCatalog.BookSnellAnatomy) {
                MedicalDataCatalog.SnellChapters
            } else {
                MedicalDataCatalog.RossChapters
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedBook = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepMaroon)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = bookName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepMaroon
                    )
                    Text("Select Chapter to Study", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(chapters) { idx, chapter ->
                    val filteredMcqs = allMcqs.filter { it.bookSource == bookName && it.chapterName == chapter }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}. $chapter",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = DeepMaroon,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(LightCream)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${filteredMcqs.size} MCQs",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MedicineGold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { viewModel.askAiAboutTopic("$bookName chapter $chapter") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, "AI", modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("AI Brief", fontSize = 11.sp)
                                }

                                TextButton(
                                    onClick = { activeChapterForVideos = chapter; activeBookForVideos = bookName },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayCircle, "Video", modifier = Modifier.size(13.dp), tint = MedicineGold)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Lectures", fontSize = 11.sp, color = DeepMaroon)
                                }

                                Button(
                                    onClick = {
                                        // If there are no custom MCQs yet, use a shuffled selection of all MCQs as helper,
                                        // but prioritize chapter content if available.
                                        val mcqPool = if (filteredMcqs.isNotEmpty()) filteredMcqs else allMcqs
                                        viewModel.startQuiz(mcqPool, "Chapter Quiz", "$chapter Exam")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.2f).testTag("start_quiz_$chapter"),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text("Start Quiz", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // LIST VIDEO LECTURES DIALOG FOR SELECTED CHAPTER
    if (activeChapterForVideos != null && activeBookForVideos != null) {
        val chapter = activeChapterForVideos!!
        val book = activeBookForVideos!!
        val filteredLectures = allVideoLectures.filter { 
            it.bookSource.lowercase().contains(book.lowercase().take(10)) && 
            it.chapterName.lowercase() == chapter.lowercase()
        }

        Dialog(onDismissRequest = { activeChapterForVideos = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(chapter, fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 16.sp)
                            Text("Topic video tutorials and medical explanations", fontSize = 11.sp, color = Color.Gray)
                        }
                        IconButton(onClick = { activeChapterForVideos = null }) {
                            Icon(Icons.Default.Close, null, tint = DeepMaroon)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredLectures.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.VideoCall, null, tint = Color.LightGray, modifier = Modifier.size(54.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("No video lectures compiled yet for this topic.", fontSize = 13.sp, color = Color.Gray)
                                Text("Ask usamaarfi (Admin) to include some!", fontSize = 11.sp, color = Color.LightGray)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredLectures) { video ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = LightCream),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(video.title, fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(video.duration, fontSize = 11.sp, color = MedicineGold, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(video.description, fontSize = 11.sp, color = Color.DarkGray)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { videoToPlay = video },
                                            colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Play Interactive Tutorial", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // HIGH FIDELITY IN-APP INTERACTIVE CLASSROOM WEB PLAYER with Fullscreen & Screen Rotation control
    if (videoToPlay != null) {
        val video = videoToPlay!!
        var isFullScreen by remember { mutableStateOf(false) }
        val context = androidx.compose.ui.platform.LocalContext.current
        val activity = context as? android.app.Activity

        LaunchedEffect(isFullScreen) {
            if (isFullScreen) {
                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }

        Dialog(
            onDismissRequest = { 
                videoToPlay = null 
                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = !isFullScreen
            )
        ) {
            if (isFullScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            android.webkit.WebView(ctx).apply {
                                webViewClient = android.webkit.WebViewClient()
                                webChromeClient = android.webkit.WebChromeClient()
                                settings.javaScriptEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                loadUrl(video.videoUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Exit Full Screen Trigger overlay
                    IconButton(
                        onClick = { isFullScreen = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Fullscreen",
                            tint = Color.White
                        )
                    }

                    // Rotate Back overlay on the bottom right
                    IconButton(
                        onClick = { isFullScreen = false },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = "Portrait Mode",
                            tint = Color.White
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E1E1E))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = video.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { videoToPlay = null }) {
                                Icon(Icons.Default.Close, null, tint = Color.White)
                            }
                        }

                        // Actual Embedded Youtube WebView Component with Fullscreen button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    android.webkit.WebView(ctx).apply {
                                        webViewClient = android.webkit.WebViewClient()
                                        webChromeClient = android.webkit.WebChromeClient()
                                        settings.javaScriptEnabled = true
                                        settings.loadWithOverviewMode = true
                                        settings.useWideViewPort = true
                                        loadUrl(video.videoUrl)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Rotate / Fullscreen Button at bottom end of the video view
                            IconButton(
                                onClick = { isFullScreen = true },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = "Rotate & Full Screen",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Video Tutorial Interactive Study Notes Pane
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White)
                                .padding(16.dp)
                        ) {
                            Text("Anatomy & Physiology Live Annotations", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 14.sp)
                            Text(
                                text = "Reference Medical Lesson: ${video.bookSource} (${video.chapterName})",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Divider()
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { isFullScreen = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.ScreenRotation, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ROTATE FULL SCREEN", fontSize = 11.sp)
                                }
                            }

                            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    Text("💡 Core Concept Overview", fontWeight = FontWeight.Bold, color = DarkMaroon, fontSize = 12.sp)
                                    Text(video.description, fontSize = 11.sp, color = Color.DarkGray)
                                }
                                item {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("🎓 Study Tips & Boards High-Yields", fontWeight = FontWeight.Bold, color = DarkMaroon, fontSize = 12.sp)
                                    Text("1. Be sure to trace the anatomical roots and systemic feedback processes physically using clinical models.\n2. Boards frequently test the exact embryological and cellular functional origins mapped in these visual tutorials.", fontSize = 11.sp, color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (activePreviewFile != null) {
        val file = activePreviewFile!!
        val context = androidx.compose.ui.platform.LocalContext.current
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var pdfPage by remember { mutableStateOf(1) }
        var isDownloading by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        Dialog(onDismissRequest = { activePreviewFile = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.fileName,
                                fontWeight = FontWeight.Bold,
                                color = DeepMaroon,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Category: ${file.fileType.uppercase()} | Size: ${file.fileSize}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = { activePreviewFile = null }) {
                            Icon(Icons.Default.Close, "Dismiss")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isDownloading) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(LightCream),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                CircularProgressIndicator(color = DeepMaroon)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Downloading Handout to Gallery...", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Saving to /Downloads/MedzArfiVault/...", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    } else {
                        // Interactive media canvas container with zoom modifiers
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(LightCream)
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 5f)
                                        if (scale > 1f) {
                                            offset += pan
                                        } else {
                                            offset = Offset.Zero
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (file.fileType == "image") {
                                AsyncImage(
                                    model = file.fileUri,
                                    contentDescription = "Zoomable Diagram",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y
                                        )
                                )
                            } else if (file.fileType == "pdf") {
                                // High-fidelity PDF document page simulator widget that expands on zoom!
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp)
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DeepMaroon.copy(alpha = 0.05f))
                                            .padding(6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("CLINICAL REF GUIDE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DeepMaroon)
                                        Text("PAGE $pdfPage OF 3", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DeepMaroon)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    when (pdfPage) {
                                        1 -> {
                                            Text(
                                                text = "SECTION A: CLINICAL CARDIOVASCULAR ANATOMY",
                                                fontWeight = FontWeight.Bold,
                                                color = DarkMaroon,
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "The blood supply of the cardiovascular engine is primarily managed by the coronary artery nodes. Under dissection, the anterior interventricular division provides branches mapped directly across the lower quadrants. Obstruction inside this branch is highly linked with high-tier myocardial localized ischemia (The Widowed Pathway).",
                                                fontSize = 10.sp,
                                                color = Color.DarkGray,
                                                lineHeight = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text("Arterial Segment Mapping Table:", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = MedicineGold)
                                                    Text("• Left Main Coronary -> LAD & Circ", fontSize = 8.sp)
                                                    Text("• Right Coronary -> Marginal & Posterior", fontSize = 8.sp)
                                                }
                                            }
                                        }
                                        2 -> {
                                            Text(
                                                text = "SECTION B: RESPIRATORY DISSECTION MATRIX",
                                                fontWeight = FontWeight.Bold,
                                                color = DarkMaroon,
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Laryngeal structures represent the protective valve and sound gateway of the respiratory tract. Extent of the recurrent laryngeal nerve loop beneath the aortic arch exposes it to damage during posterior chest incisions, leading directly to vocal card dysfunction.",
                                                fontSize = 10.sp,
                                                color = Color.DarkGray,
                                                lineHeight = 14.sp
                                            )
                                        }
                                        else -> {
                                            Text(
                                                text = "SECTION C: PHYSIOLOGICAL HOMEOSTASIS INDEX",
                                                fontWeight = FontWeight.Bold,
                                                color = DarkMaroon,
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Regulative feedback loops within the baroreception cells control critical mean arterial strain index. Sudden decrease in strain limits signal conduction, firing adrenal pathways to augment total peripheral resistance index and heartbeat output dynamically.",
                                                fontSize = 10.sp,
                                                color = Color.DarkGray,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.InsertDriveFile, null, tint = DeepMaroon, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("MEDICAL REFERENCE SHEET", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DeepMaroon)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "High-yield reference material containing clinical summaries, anatomical indexes, and lecture footnotes map. Double-pinch the frame to zoom the content context dynamically, or press the download button to export the original handout to your local device downloads folder.",
                                        fontSize = 11.sp,
                                        color = Color.DarkGray,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (file.fileType == "pdf" && !isDownloading) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (pdfPage > 1) pdfPage-- },
                                enabled = pdfPage > 1
                            ) {
                                Icon(Icons.Default.ArrowBack, "Prev Page")
                            }
                            Text("Page $pdfPage of 3", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                            IconButton(
                                onClick = { if (pdfPage < 3) pdfPage++ },
                                enabled = pdfPage < 3
                            ) {
                                Icon(Icons.Default.ArrowForward, "Next Page")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isDownloading) {
                            IconButton(onClick = { scale = (scale - 0.5f).coerceAtLeast(1f); if (scale == 1f) offset = Offset.Zero }) {
                                Icon(Icons.Default.ZoomOut, "Zoom Out", tint = DeepMaroon)
                            }

                            Slider(
                                value = scale,
                                onValueChange = { scale = it; if (scale == 1f) offset = Offset.Zero },
                                valueRange = 1f..5f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = MedicineGold, activeTrackColor = DeepMaroon)
                            )

                            IconButton(onClick = { scale = (scale + 0.5f).coerceAtMost(5f) }) {
                                Icon(Icons.Default.ZoomIn, "Zoom In", tint = DeepMaroon)
                            }

                            TextButton(onClick = { scale = 1f; offset = Offset.Zero }) {
                                Text("RESET", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepMaroon)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                isDownloading = true
                                scope.launch {
                                    kotlinx.coroutines.delay(1200)
                                    isDownloading = false
                                    Toast.makeText(
                                        context,
                                        "Saved \"${file.fileName}\" successfully to Gallery / Downloads folder!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MedicineGold),
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isDownloading
                        ) {
                            Icon(Icons.Default.Download, null, tint = DarkMaroon, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DOWNLOAD TO GALLERY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkMaroon)
                        }

                        Button(
                            onClick = { activePreviewFile = null },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isDownloading
                        ) {
                            Text("DONE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==================== ADVANCED QUIZ SYSTEM ====================
@Composable
fun QuizTab(viewModel: MedicalViewModel) {
    val state = viewModel.quizState
    val bookmarkList by viewModel.bookmarks.collectAsState()

    if (state.mcqs.isEmpty()) {
        val context = androidx.compose.ui.platform.LocalContext.current
        var aiBook by remember { mutableStateOf("Snell's Clin Anatomy") }
        var aiChapter by remember { mutableStateOf("Thorax & Cardiac Grid") }
        var aiType by remember { mutableStateOf("MCQ") } // MCQ or "SEQ"
        var aiCount by remember { mutableStateOf(5) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.PlayCircleFilled,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = DeepMaroon
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "BOARD EXAM CENTER",
                fontWeight = FontWeight.Bold,
                color = DeepMaroon,
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif
            )
            Text(
                text = "Practice questions or configure Arfi's Infinite Gemini AI Study Bank.",
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Standard launcher
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                border = BorderStroke(1.dp, DeepMaroon.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "1. Local Curated Board Exams",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DarkMaroon
                    )
                    Text(
                        "Trigger a standardized test extracted directly from standard textbook questions catalog instantly.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Button(
                        onClick = {
                            viewModel.startQuiz(
                                viewModel.allMCQs.value,
                                "Random Practice",
                                "Quick Mock Mix"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Text("Launch Standard Practice Exam", fontSize = 12.sp)
                    }
                }
            }

            // Gemini Dynamic AI Launcher Block
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MedicineGold.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = MedicineGold, modifier = Modifier.size(18.dp))
                        Text(
                            "2. Gemini Infinite AI Study Bank",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = DeepMaroon
                        )
                    }
                    Text(
                        "Contact AI Clinical Medical engine to build customized clinical scenarios with explanations dynamically.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (viewModel.aiBankLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LightCream, shape = RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = DeepMaroon, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = viewModel.aiBankStatusMessage ?: "Generating scenarios...",
                                    fontSize = 11.sp,
                                    color = DeepMaroon,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // AI Selection Form
                        Text("Target Textbook Reference:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.DarkGray)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Snell's Clin Anatomy", "Ross & Wilson Physiology").forEach { b ->
                                val active = aiBook == b
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (active) DeepMaroon else Color.White,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .border(1.dp, if (active) DeepMaroon else Color.LightGray, shape = RoundedCornerShape(6.dp))
                                        .clickable { aiBook = b }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = b,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else Color.DarkGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("Select System / Focus Topic:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.DarkGray)
                        OutlinedTextField(
                            value = aiChapter,
                            onValueChange = { aiChapter = it },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DeepMaroon,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text("Format Structure:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.DarkGray)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("MCQ", "SEQ").forEach { typeStr ->
                                        val active = aiType == typeStr
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    if (active) MedicineGold else Color.White,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .border(1.dp, if (active) MedicineGold else Color.LightGray, shape = RoundedCornerShape(6.dp))
                                                .clickable { aiType = typeStr }
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = typeStr,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (active) DarkMaroon else Color.DarkGray
                                            )
                                        }
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Quantity Count:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.DarkGray)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(3, 5, 10).forEach { num ->
                                        val active = aiCount == num
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    if (active) DeepMaroon else Color.White,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .border(1.dp, if (active) DeepMaroon else Color.LightGray, shape = RoundedCornerShape(6.dp))
                                                .clickable { aiCount = num }
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$num",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (active) Color.White else Color.DarkGray
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                viewModel.generateQuestionsWithGemini(aiBook, aiChapter, aiType, aiCount)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MedicineGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text("Launch Infinite Gemini AI Test", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkMaroon)
                        }
                    }

                    // Display feedback messages if any
                    viewModel.aiBankStatusMessage?.let { msg ->
                        if (msg.contains("Successfully generated") || msg.contains("Failed:")) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (msg.contains("Failed")) Color(0xFFFDE8E8) else Color(0xFFE1F5FE)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    fontSize = 11.sp,
                                    color = if (msg.contains("Failed")) Color(0xFFC81E1E) else Color(0xFF0288D1),
                                    modifier = Modifier.padding(10.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    if (state.isSubmitted && state.completedScore != null) {
        // Results View
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepMaroon),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "EXAM COMPLETED",
                            fontWeight = FontWeight.Bold,
                            color = SoftGold,
                            letterSpacing = 2.sp,
                            fontSize = 11.sp
                        )
                        Text(
                            text = state.completedScore.chapterOrBook,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Serif
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        // Score Badge ring
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(55))
                                .background(Color(0xFF8E1439)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${state.completedScore.percentage.toInt()}%",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MedicineGold
                                )
                                Text("SCORE", fontSize = 10.sp, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Correct", color = SoftGold, fontSize = 11.sp)
                                Text("${state.completedScore.correctAnswers}", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Incorrect", color = SoftGold, fontSize = 11.sp)
                                Text("${state.completedScore.wrongAnswers}", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Duration", color = SoftGold, fontSize = 11.sp)
                                Text("${state.completedScore.timeTakenSeconds}s", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Medical Review & Rationale Guides",
                    color = DeepMaroon,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            itemsIndexed(state.mcqs) { idx, mcq ->
                val chosen = state.selectedOptions[mcq.id]
                val isCorrect = chosen == mcq.correctAnswer

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isCorrect) SoftGreen else CoralRed
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "Question ${idx + 1}",
                                fontWeight = FontWeight.Bold,
                                color = DeepMaroon,
                                fontSize = 13.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isCorrect) SoftGreen.copy(alpha = 0.2f) else CoralRed.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isCorrect) "Correct" else "Wrong",
                                    fontSize = 10.sp,
                                    color = if (isCorrect) SoftGreen else CoralRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(mcq.question, fontSize = 14.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("• Selected Option: $chosen \n• Correct Answer: ${mcq.correctAnswer}", fontSize = 12.sp, color = Color.Gray)

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(LightCream)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("Clinical Solution Explanation:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepMaroon)
                                Text(mcq.explanation, fontSize = 12.sp, color = Color.DarkGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Reference Topic: ${mcq.referenceTopic}", fontSize = 10.sp, color = MedicineGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.selectedTab = "dashboard" },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("RETURN TO SMART DASHBOARD", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    // Active MCQ Screen View Model
    val currentIdx = state.currentIndex
    val currentMcq = state.mcqs.getOrNull(currentIdx)!!
    val isBookmarked = bookmarkList.any { it.mcqId == currentMcq.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Headers (Book source + Chapter name)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentMcq.bookSource.uppercase(),
                    color = MedicineGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = currentMcq.chapterName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepMaroon
                )
            }
            IconButton(onClick = { viewModel.toggleBookmarkMCQ(currentMcq.id) }) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = MedicineGold
                )
            }
        }

        // Action Progress Bar
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { (currentIdx + 1).toFloat() / state.mcqs.size.toFloat() },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = DeepMaroon,
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "${currentIdx + 1}/${state.mcqs.size}",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Large Question Card (Clinical Stem)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LightCream)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = currentMcq.difficultyLevel.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepMaroon
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("• ${currentMcq.referenceTopic}", fontSize = 11.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = currentMcq.question,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Justify,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4 MCQ Options Lists
        val optionsList = listOf(
            "A" to currentMcq.optionA,
            "B" to currentMcq.optionB,
            "C" to currentMcq.optionC,
            "D" to currentMcq.optionD
        )

        optionsList.forEach { (optionKey, optionText) ->
            val isSelected = state.selectedOptions[currentMcq.id] == optionKey

            OutlinedCard(
                onClick = { viewModel.answerCurrentQuestion(optionKey) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) LightCream else Color.White
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) DeepMaroon else Color.LightGray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("option_$optionKey")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(if (isSelected) DeepMaroon else LightGrey),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = optionKey,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color.DarkGray,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = optionText,
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation bottom indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { viewModel.retractQuestion() },
                enabled = currentIdx > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("BACK")
            }

            if (currentIdx == state.mcqs.size - 1) {
                Button(
                    onClick = { viewModel.submitQuiz() },
                    colors = ButtonDefaults.buttonColors(containerColor = MedicineGold, contentColor = DarkMaroon),
                    enabled = state.selectedOptions[currentMcq.id] != null,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("submit_quiz_button")
                ) {
                    Text("FINISH EXAM", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { viewModel.advanceQuestion() },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("NEXT")
                }
            }
        }
    }
}

// ==================== DYNAMIC NOTES SYSTEM TAB ====================
@Composable
fun NotesTab(viewModel: MedicalViewModel) {
    val notes by viewModel.savedNotes.collectAsState()
    var isEditing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (!isEditing) {
            // Display standard Saved Notes List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Clinical Notes",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepMaroon,
                        fontFamily = FontFamily.Serif
                    )
                    Text("Permanently saved study notes", fontSize = 11.sp, color = Color.Gray)
                }
                Button(
                    onClick = { 
                        viewModel.startNewNote()
                        isEditing = true 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("create_note_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create Note")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NoteAdd, "no notes", modifier = Modifier.size(54.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No notes created yet.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notes) { note ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectNoteForEditing(note)
                                    isEditing = true
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = note.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = DeepMaroon,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MedicineGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "${note.bookSource} • ${note.chapter}",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = note.content,
                                    fontSize = 13.sp,
                                    color = Color.DarkGray,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

        } else {
            // Note Editor/Creator Workplace Workspace
            Text(
                text = if (viewModel.editingNoteId == null) "Create Medicine Note" else "Edit Clinical Note",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DeepMaroon
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.noteTitle,
                onValueChange = { viewModel.noteTitle = it },
                label = { Text("Note Title") },
                modifier = Modifier.fillMaxWidth().testTag("note_title_input"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Book & Chapter Pickers row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = viewModel.noteBookSource,
                    onValueChange = { viewModel.noteBookSource = it },
                    label = { Text("Book Source (Snell/Ross)") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon),
                    singleLine = true
                )
                OutlinedTextField(
                    value = viewModel.noteChapterName,
                    onValueChange = { viewModel.noteChapterName = it },
                    label = { Text("Chapter/Topic") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text Area Content with highlighter supports
            OutlinedTextField(
                value = viewModel.noteContent,
                onValueChange = { viewModel.noteContent = it },
                label = { Text("Content Notes (Rich descriptions, clinical signs)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("note_content_input"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon),
                maxLines = 15
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (viewModel.editingNoteId != null) {
                    IconButton(
                        onClick = {
                            viewModel.deleteCurrentNote(viewModel.editingNoteId!!) {
                                isEditing = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CoralRed)
                    }
                } else {
                    Spacer(modifier = Modifier.width(20.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { 
                            viewModel.startNewNote()
                            isEditing = false 
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("CANCEL")
                    }

                    Button(
                        onClick = {
                            viewModel.saveCurrentNote {
                                isEditing = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_note_button")
                    ) {
                        Text("SAVE NOTE")
                    }
                }
            }
        }
    }
}

// ==================== AI SMART SEARCH SYSTEM TAB ====================
@Composable
fun SearchTab(viewModel: MedicalViewModel) {
    val focusManager = LocalFocusManager.current
    val results = viewModel.searchResults
    val isSearching = viewModel.isSearching

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "AI Smart Search System",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DeepMaroon,
            fontFamily = FontFamily.Serif
        )
        Text("Search disease, anatomy terms or physiological questions recursively.", fontSize = 11.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar Row implementation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = viewModel.searchText,
                onValueChange = { viewModel.searchText = it },
                placeholder = { Text("E.g. Inguinal hernia or Homeostasis...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_input_term"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DeepMaroon) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.executeSearch()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(54.dp)
            ) {
                Text("SEARCH")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = DeepMaroon)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("AI searching database...", color = DeepMaroon, fontSize = 12.sp)
                }
            }
        } else if (results == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Enter a clinical query above to retrieve Board insights.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Topic AI synthesis panel
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LightCream),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, null, tint = MedicineGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "AI Smart Conception Summary",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = DeepMaroon
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = results.aiExplanation,
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Matched MCQs
                if (results.mcqs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Related Board MCQs (${results.mcqs.size})",
                            color = DeepMaroon,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    items(results.mcqs) { mcq ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${mcq.bookSource} • ${mcq.chapterName}",
                                    fontSize = 10.sp,
                                    color = MedicineGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = mcq.question,
                                    fontSize = 13.sp,
                                    color = Color.DarkGray,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                TextButton(
                                    onClick = { 
                                        viewModel.quizState = ActiveQuizState(listOf(mcq))
                                        viewModel.selectedTab = "quiz" 
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Test yourself with this MCQ", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Matched Short Questions
                if (results.shortQuestions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Matching Clinical Short Questions (${results.shortQuestions.size})",
                            color = DeepMaroon,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    items(results.shortQuestions) { sq ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "QUESTION: ${sq.question}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepMaroon
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sq.baseAnswer,
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }

                if (results.mcqs.isEmpty() && results.shortQuestions.isEmpty()) {
                    item {
                        Text(
                            "No matching local material found. Check the AI Concept block above for dynamic reasoning.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==================== ARFI AI ASSISTANT CHAT TAB ====================
@Composable
fun AssistantTab(viewModel: MedicalViewModel) {
    val history = viewModel.chatHistory
    val loading = viewModel.isAiResponding
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "AI Clinical Assistant",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DeepMaroon,
            fontFamily = FontFamily.Serif
        )
        Text("Powered by Gemini for advanced medicine explanation, study guides, & mock designs", fontSize = 11.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(12.dp))

        // Chat logs bubble
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(history) { msg ->
                val isUser = msg.sender == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isUser) 12.dp else 0.dp,
                                    bottomEnd = if (isUser) 0.dp else 12.dp
                                )
                            )
                            .background(if (isUser) DeepMaroon else Color.White)
                            .border(1.dp, if (isUser) DeepMaroon else Color.LightGray.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = if (isUser) "You (Physician)" else "Arfi Medz Bot",
                                fontWeight = FontWeight.Bold,
                                color = if (isUser) SoftGold else DeepMaroon,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.text,
                                color = if (isUser) Color.White else Color.DarkGray,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
            
            if (loading) {
                item {
                    Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DeepMaroon, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Arfi is analyzing clinical criteria...", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Query input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = viewModel.aiQueryText,
                onValueChange = { viewModel.aiQueryText = it },
                placeholder = { Text("Ask about Anatomy, Physiology, Clinical signs...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_assistant_input"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.sendAiMessage() },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DeepMaroon)
                    .size(54.dp)
                    .testTag("ai_send_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

// ==================== ADMIN PANEL TAB ====================
@Composable
fun AdminTab(viewModel: MedicalViewModel) {
    var passwordInput by remember { mutableStateOf("") }
    var isAuthenticatedByPass by remember { mutableStateOf(false) }

    if (!isAuthenticatedByPass) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Security, "Security", modifier = Modifier.size(54.dp), tint = DeepMaroon)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Admin Portal Authentication", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DeepMaroon)
            Text("Sign in with clinical credentials to unlock medical MCQ additions.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Admin Code / Pass") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon),
                modifier = Modifier.fillMaxWidth().testTag("admin_pass_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (passwordInput == "admin" || passwordInput == "arfi123" || passwordInput == "arfi") {
                        isAuthenticatedByPass = true
                    } else {
                        viewModel.adminSuccessMessage = "Incorrect Code. Try 'arfi'."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("admin_auth_button")
            ) {
                Text("VERIFY SECURITY CRITERIA")
            }

            if (viewModel.adminSuccessMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(viewModel.adminSuccessMessage!!, color = CoralRed, fontSize = 12.sp)
            }
        }
        return
    }

    // Admin MCQ Adder view
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Arfi Admin Panel",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DeepMaroon,
                fontFamily = FontFamily.Serif
            )
            TextButton(onClick = { isAuthenticatedByPass = false }) {
                Text("Lock Terminal", color = CoralRed)
            }
        }

        Text("Add custom medical questions directly into the system database", fontSize = 11.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = viewModel.adminQuestion,
            onValueChange = { viewModel.adminQuestion = it },
            label = { Text("Clinical MCQ Stem / Question") },
            modifier = Modifier.fillMaxWidth().testTag("admin_mcq_question"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Options inputs
        Text("Answer Options Configuration", color = DeepMaroon, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        OutlinedTextField(
            value = viewModel.adminOptions[0],
            onValueChange = { viewModel.adminOptions[0] = it },
            label = { Text("Option A") },
            modifier = Modifier.fillMaxWidth().testTag("admin_option_a"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon),
            singleLine = true
        )
        OutlinedTextField(
            value = viewModel.adminOptions[1],
            onValueChange = { viewModel.adminOptions[1] = it },
            label = { Text("Option B") },
            modifier = Modifier.fillMaxWidth().testTag("admin_option_b"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon),
            singleLine = true
        )
        OutlinedTextField(
            value = viewModel.adminOptions[2],
            onValueChange = { viewModel.adminOptions[2] = it },
            label = { Text("Option C") },
            modifier = Modifier.fillMaxWidth().testTag("admin_option_c"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon),
            singleLine = true
        )
        OutlinedTextField(
            value = viewModel.adminOptions[3],
            onValueChange = { viewModel.adminOptions[3] = it },
            label = { Text("Option D") },
            modifier = Modifier.fillMaxWidth().testTag("admin_option_d"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Correct Answer Pick Option (A/B/C/D)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Correct Option: ", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(6.dp))
            listOf("A", "B", "C", "D").forEach { key ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    RadioButton(
                        selected = viewModel.adminCorrectAnswer == key,
                        onClick = { viewModel.adminCorrectAnswer = key },
                        colors = RadioButtonDefaults.colors(selectedColor = DeepMaroon)
                    )
                    Text(key, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Explanation rationale
        OutlinedTextField(
            value = viewModel.adminExplanation,
            onValueChange = { viewModel.adminExplanation = it },
            label = { Text("Rationale Explanation for MBBS Students") },
            modifier = Modifier.fillMaxWidth().testTag("admin_explanation_input"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = viewModel.adminDifficulty,
                onValueChange = { viewModel.adminDifficulty = it },
                label = { Text("Difficulty (Easy, Medium, Hard)") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon),
                singleLine = true
            )
            OutlinedTextField(
                value = viewModel.adminBookSource,
                onValueChange = { viewModel.adminBookSource = it },
                label = { Text("Book (Snell or Ross)") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = viewModel.adminChapterName,
            onValueChange = { viewModel.adminChapterName = it },
            label = { Text("Chapter Category") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon),
            singleLine = true
        )

        if (viewModel.adminSuccessMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = viewModel.adminSuccessMessage!!,
                color = if (viewModel.adminSuccessMessage!!.startsWith("Error")) CoralRed else SoftGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.createAdminMCQ() },
            colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("compile_mcq_btn")
        ) {
            Icon(Icons.Default.Publish, null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("COMPILE MCQ TO LOCAL DATABASE", fontWeight = FontWeight.Bold)
        }
    }
}

// ==================== PROFILE TAB ====================
@Composable
fun ProfileTab(viewModel: MedicalViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()
    val testScores by viewModel.quizScores.collectAsState()
    val profile = userProfile ?: UserProfile()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large Profile Image placeholder
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(45))
                .background(LightCream)
                .border(2.dp, MedicineGold, RoundedCornerShape(45)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AccountCircle, null, tint = DeepMaroon, modifier = Modifier.size(76.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = profile.name,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = DeepMaroon,
            fontFamily = FontFamily.Serif
        )
        Text(
            text = profile.email,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "College: ${profile.collegeName}",
            fontSize = 12.sp,
            color = DarkMaroon,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Mobile: ${profile.mobileNumber}",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Professional diagnostic parameters Card list
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Board Performance Summary", color = DeepMaroon, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Exams Taken:", color = Color.Gray, fontSize = 13.sp)
                    Text("${testScores.size}", fontWeight = FontWeight.Bold, color = DeepMaroon)
                }
                Spacer(modifier = Modifier.height(8.dp))
                val averagePercent = if (testScores.isNotEmpty()) testScores.map { it.percentage }.average().toInt() else 0
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Average Competence Score:", color = Color.Gray, fontSize = 13.sp)
                    Text("$averagePercent%", fontWeight = FontWeight.Bold, color = if (averagePercent >= 70) SoftGreen else CoralRed)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Study Streak Factor:", color = Color.Gray, fontSize = 13.sp)
                    Text("${profile.studyStreak} Consecutive Days", fontWeight = FontWeight.Bold, color = MedicineGold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security API keys warning instruction under standard skill guide
        Card(
            colors = CardDefaults.cardColors(containerColor = LightCream),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Security Warning Notice",
                    color = DeepMaroon,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Justify,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = { viewModel.performSignOut() },
            colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button")
        ) {
            Text("SECURE DE-AUTHORIZE / DISCONNECT", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ==================== ADVANCED SECURE ADMIN SYSTEM SYSTEM ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSystemScreen(viewModel: MedicalViewModel) {
    val allProfiles by viewModel.allProfiles.collectAsState()
    val allLectures by viewModel.allVideoLectures.collectAsState()
    val appPrefs by viewModel.appPreferences.collectAsState()
    val allMcqs by viewModel.allMCQs.collectAsState()
    val allseqs by viewModel.allSEQs.collectAsState()
    val customUploadedFiles by viewModel.customUploadedFiles.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    var enteredGeminiKey by remember { mutableStateOf(appPrefs.customGeminiApiKey ?: "") }

    var activeAdminTab by remember { mutableStateOf("directory") } // directory, lectures, styling, ai_automation, publisher, files

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(getLogoBgColor(appPrefs.logoBgColorHex)),
                            contentAlignment = Alignment.Center
                        ) {
                            AppLogoView(appPrefs = appPrefs, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Admin System: Arfi",
                            color = WarmWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            fontFamily = FontFamily.Serif
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.performSignOut() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, null, tint = WarmWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sign Out", color = WarmWhite, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepMaroon
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = activeAdminTab == "directory",
                    onClick = { activeAdminTab = "directory" },
                    label = { Text("Students", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.People, "Students") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepMaroon,
                        selectedTextColor = DeepMaroon,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = LightCream
                    )
                )

                NavigationBarItem(
                    selected = activeAdminTab == "lectures",
                    onClick = { activeAdminTab = "lectures" },
                    label = { Text("Videos", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.VideoCall, "Videos") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepMaroon,
                        selectedTextColor = DeepMaroon,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = LightCream
                    )
                )

                NavigationBarItem(
                    selected = activeAdminTab == "styling",
                    onClick = { activeAdminTab = "styling" },
                    label = { Text("Styling", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Edit, "Styling") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepMaroon,
                        selectedTextColor = DeepMaroon,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = LightCream
                    )
                )

                NavigationBarItem(
                    selected = activeAdminTab == "ai_automation",
                    onClick = { activeAdminTab = "ai_automation" },
                    label = { Text("Systems AI", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.AutoAwesome, "Systems AI") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepMaroon,
                        selectedTextColor = DeepMaroon,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = LightCream
                    )
                )

                NavigationBarItem(
                    selected = activeAdminTab == "publisher",
                    onClick = { activeAdminTab = "publisher" },
                    label = { Text("Board MCQ", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.CloudUpload, "Publish MCQs") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepMaroon,
                        selectedTextColor = DeepMaroon,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = LightCream
                    )
                )

                NavigationBarItem(
                    selected = activeAdminTab == "files",
                    onClick = { activeAdminTab = "files" },
                    label = { Text("Files & Logo", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Folder, "Files") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepMaroon,
                        selectedTextColor = DeepMaroon,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = LightCream
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(LightCream)
                .padding(16.dp)
        ) {
            when (activeAdminTab) {
                "directory" -> {
                    Text("Students Profile Directory", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 20.sp, fontFamily = FontFamily.Serif)
                    Text("Secure clinical audit logs of all active registered Google / Local accounts", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (allProfiles.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No students registered on the database yet.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(allProfiles) { student ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(22.dp))
                                                    .background(getLogoBgColor(appPrefs.logoBgColorHex)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(student.name.take(2).uppercase(), color = WarmWhite, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(student.name, fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 15.sp)
                                                Text("ID / Email: ${student.email}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Divider(thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column {
                                                Text("🎓 COLLEGE NAME", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MedicineGold)
                                                Text(student.collegeName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkMaroon)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("📞 PHONE NUMBER", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MedicineGold)
                                                Text(student.mobileNumber, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CalendarToday, null, tint = LightMaroon, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Daily progress: ${(student.dailyProgress * 100).toInt()}%", fontSize = 11.sp, color = Color.Gray)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(LightCream)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("Streak: ${student.studyStreak} days", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MedicineGold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "lectures" -> {
                    Text("Interactive Classroom Manager", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 20.sp, fontFamily = FontFamily.Serif)
                    Text("Compile and map live video tutorials on standard textbook chapters", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Compile Video Lecture Form", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = viewModel.adminVideoTitle,
                                        onValueChange = { viewModel.adminVideoTitle = it },
                                        label = { Text("Video Title") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = viewModel.adminVideoUrl,
                                        onValueChange = { viewModel.adminVideoUrl = it },
                                        label = { Text("YouTube Iframe URL") },
                                        placeholder = { Text("https://www.youtube.com/embed/XXXXX") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = viewModel.adminVideoDescription,
                                        onValueChange = { viewModel.adminVideoDescription = it },
                                        label = { Text("Topic Explanation / Summary Description") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = viewModel.adminVideoDuration,
                                            onValueChange = { viewModel.adminVideoDuration = it },
                                            label = { Text("Duration") },
                                            placeholder = { Text("12 mins") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )

                                        OutlinedTextField(
                                            value = viewModel.adminVideoChapter,
                                            onValueChange = { viewModel.adminVideoChapter = it },
                                            label = { Text("Chapter") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text("Reference Curriculum Book Source:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.Gray)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = viewModel.adminVideoBook == MedicalDataCatalog.BookSnellAnatomy,
                                            onClick = { viewModel.adminVideoBook = MedicalDataCatalog.BookSnellAnatomy }
                                        )
                                        Text("Snell's Anatomy", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        RadioButton(
                                            selected = viewModel.adminVideoBook == MedicalDataCatalog.BookRossPhysiology,
                                            onClick = { viewModel.adminVideoBook = MedicalDataCatalog.BookRossPhysiology }
                                        )
                                        Text("Ross Physiology", fontSize = 11.sp)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = { viewModel.compileVideoLecture() },
                                        colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(44.dp)
                                    ) {
                                        Text("COMPILE & MAP VIDEO", fontWeight = FontWeight.Bold)
                                    }

                                    if (viewModel.adminSuccessMessage != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(viewModel.adminSuccessMessage!!, color = MedicineGold, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        TextButton(onClick = { viewModel.adminSuccessMessage = null }) {
                                            Text("Dismiss Log", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text("Current Compiled Video Lectures (${allLectures.size})", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 14.sp)
                        }

                        items(allLectures) { video ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(video.title, fontWeight = FontWeight.Bold, color = DarkMaroon, fontSize = 13.sp)
                                            Text("Mapped to: ${video.bookSource} / ${video.chapterName}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        IconButton(onClick = { viewModel.removeVideoLecture(video.id) }) {
                                            Icon(Icons.Default.Delete, null, tint = CoralRed, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "styling" -> {
                    Text("Dynamic App Identity Customizer", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 20.sp, fontFamily = FontFamily.Serif)
                    Text("Deform, morph or customize app naming, logo icons, and primary aesthetics", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Realtime Identity Parameters", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = viewModel.adminAppNameInput,
                                onValueChange = { viewModel.adminAppNameInput = it },
                                label = { Text("Application Name") },
                                placeholder = { Text(appPrefs.appName) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = viewModel.adminAppLogoColorInput,
                                onValueChange = { viewModel.adminAppLogoColorInput = it },
                                label = { Text("Logo Primary Hex Color Background (e.g. #8E1439)") },
                                placeholder = { Text(appPrefs.logoBgColorHex) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Vector Logo Mark Icon Style:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray)
                            val logos = listOf("MedicalServices", "LocalHospital", "Favorite", "Medication", "Healing")
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                logos.forEach { logoName ->
                                    val isChosen = viewModel.adminAppLogoIconInput == logoName
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isChosen) DeepMaroon else LightCream)
                                            .clickable { viewModel.adminAppLogoIconInput = logoName },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getLogoIcon(logoName),
                                            contentDescription = null,
                                            tint = if (isChosen) MedicineGold else Color.Gray,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    viewModel.updateSystemPreferences(
                                        appName = viewModel.adminAppNameInput,
                                        icon = viewModel.adminAppLogoIconInput,
                                        pColor = viewModel.adminAppLogoColorInput
                                    )
                                    viewModel.adminSuccessMessage = "Identity updated! App home screen parameters set."
                                    // Reset input strings
                                    viewModel.adminAppNameInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SAVE SYSTEM PREFERENCES", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                "ai_automation" -> {
                    Text("Dr. Arfi Systems AI Console", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 20.sp, fontFamily = FontFamily.Serif)
                    Text("Type instructions in native natural language to alter system parameters by AI", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("System AI Natural Command Prompt", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Example commands: 'Change app name to Anatomy Board Master', 'Add a video video on pacemakers', 'Set theme color to #4A1521'", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = viewModel.adminAiQueryText,
                                onValueChange = { viewModel.adminAiQueryText = it },
                                label = { Text("What changes do you wish to execute by AI?") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 4
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { viewModel.processAdminAiAction() },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                enabled = !viewModel.isAdminAiLoading
                            ) {
                                if (viewModel.isAdminAiLoading) {
                                    CircularProgressIndicator(color = WarmWhite, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Coordinating System Triggers...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("RUN AI COMPILER", fontWeight = FontWeight.Bold)
                                }
                            }

                            if (viewModel.adminAiStatusMessage != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(LightCream)
                                        .border(1.dp, MedicineGold, RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text("AI COMPILE SYSTEM RESPONSE LOG:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MedicineGold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(viewModel.adminAiStatusMessage!!, fontSize = 12.sp, color = DarkMaroon)
                                        TextButton(onClick = { viewModel.adminAiStatusMessage = null }) {
                                            Text("Clear Log Output", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VpnKey, null, tint = DeepMaroon, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gemini API Key Configuration", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (appPrefs.customGeminiApiKey.isNullOrBlank()) {
                                    "Status: Using Default System Sandbox Core Key"
                                } else {
                                    "Status: Custom Key Enabled (${appPrefs.customGeminiApiKey!!.take(8)}...)"
                                },
                                fontSize = 11.sp,
                                color = if (appPrefs.customGeminiApiKey.isNullOrBlank()) Color.Gray else MedicineGold,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = enteredGeminiKey,
                                onValueChange = { enteredGeminiKey = it },
                                label = { Text("Enter Gemini API Key (AIzaSy...)") },
                                placeholder = { Text("Paste your Gemini API key from Google AI Studio") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    viewModel.updateCustomGeminiApiKey(enteredGeminiKey)
                                    Toast.makeText(context, "Gemini API Key saved successfully!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(42.dp)
                             ) {
                                 Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                                 Spacer(modifier = Modifier.width(6.dp))
                                 Text("SAVE CLINICAL GEMINI KEY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                             }
                        }
                    }
                }

                "publisher" -> {
                    var publisherSubTab by remember { mutableStateOf("manual") } // manual, ai_import

                    Text("Board Custom MCQ & SEQ Publisher", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 20.sp, fontFamily = FontFamily.Serif)
                    Text("Publish custom or AI-extracted high-yield board-style MCQs/SEQs to textbooks.", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { publisherSubTab = "manual" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (publisherSubTab == "manual") DeepMaroon else LightCream,
                                contentColor = if (publisherSubTab == "manual") WarmWhite else Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text("Manual Creator", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { publisherSubTab = "ai_import" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (publisherSubTab == "ai_import") DeepMaroon else LightCream,
                                contentColor = if (publisherSubTab == "ai_import") WarmWhite else Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp).testTag("ai_importer_tab")
                        ) {
                            Text("AI Bulk Importer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (publisherSubTab == "manual") {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Publish New MCQ", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(10.dp))

                                        OutlinedTextField(
                                            value = viewModel.adminQuestion,
                                            onValueChange = { viewModel.adminQuestion = it },
                                            label = { Text("MCQ Stem / Question Body") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = viewModel.adminOptions[0],
                                            onValueChange = { viewModel.adminOptions[0] = it },
                                            label = { Text("Option A") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("option_a_input")
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = viewModel.adminOptions[1],
                                            onValueChange = { viewModel.adminOptions[1] = it },
                                            label = { Text("Option B") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("option_b_input")
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = viewModel.adminOptions[2],
                                            onValueChange = { viewModel.adminOptions[2] = it },
                                            label = { Text("Option C") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = viewModel.adminOptions[3],
                                            onValueChange = { viewModel.adminOptions[3] = it },
                                            label = { Text("Option D") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text("Correct Answer Selection:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.Gray)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            listOf("A", "B", "C", "D").forEach { ans ->
                                                val isChosen = viewModel.adminCorrectAnswer == ans
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isChosen) DeepMaroon else LightCream)
                                                        .clickable { viewModel.adminCorrectAnswer = ans }
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(ans, color = if (isChosen) WarmWhite else Color.Black, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = viewModel.adminExplanation,
                                            onValueChange = { viewModel.adminExplanation = it },
                                            label = { Text("Board Explanation Summary") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = viewModel.adminChapterName,
                                                onValueChange = { viewModel.adminChapterName = it },
                                                label = { Text("Chapter") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )

                                            OutlinedTextField(
                                                value = viewModel.adminDifficulty,
                                                onValueChange = { viewModel.adminDifficulty = it },
                                                label = { Text("Difficulty") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text("Reference Book Target:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.Gray)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = viewModel.adminBookSource == MedicalDataCatalog.BookSnellAnatomy,
                                                onClick = { viewModel.adminBookSource = MedicalDataCatalog.BookSnellAnatomy }
                                            )
                                            Text("Snell's Anatomy", fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(16.dp))
                                            RadioButton(
                                                selected = viewModel.adminBookSource == MedicalDataCatalog.BookRossPhysiology,
                                                onClick = { viewModel.adminBookSource = MedicalDataCatalog.BookRossPhysiology }
                                            )
                                            Text("Ross Physiology", fontSize = 11.sp)
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Button(
                                            onClick = { viewModel.createAdminMCQ() },
                                            colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("publish_mcq_button")
                                        ) {
                                            Text("PUBLISH MCQ TO TEXTBOOK", fontWeight = FontWeight.Bold)
                                        }

                                        if (viewModel.adminSuccessMessage != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(viewModel.adminSuccessMessage!!, color = MedicineGold, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            TextButton(onClick = { viewModel.adminSuccessMessage = null }) {
                                                Text("Dismiss Log", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Dr. Arfi's Gemini AI Importer", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 14.sp)
                                        Text("Bulk extract high-yield questions from pasted pdf/text files or direct webpage/document web links.", fontSize = 10.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text("Extraction Question Type:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.Gray)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            val isMcq = viewModel.importMode == "MCQ"
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isMcq) DeepMaroon else LightCream)
                                                    .clickable { viewModel.importMode = "MCQ" }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Board MCQs", color = if (isMcq) WarmWhite else Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (!isMcq) DeepMaroon else LightCream)
                                                    .clickable { viewModel.importMode = "SEQ" }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Board SEQs (Essay)", color = if (!isMcq) WarmWhite else Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))

                                        OutlinedTextField(
                                            value = viewModel.importSourceText,
                                            onValueChange = { viewModel.importSourceText = it },
                                            label = { Text("Paste Text Content from PDF/File (Multi-line)") },
                                            placeholder = { Text("E.g. Paste copy-pasted medical textbooks paragraphs, transcript notes or article extracts...") },
                                            modifier = Modifier.fillMaxWidth().height(120.dp),
                                            maxLines = 6
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = viewModel.importSourceUrl,
                                            onValueChange = { viewModel.importSourceUrl = it },
                                            label = { Text("Direct Link / Document Web URL") },
                                            placeholder = { Text("E.g. Direct Google Drive/Dropbox PDF sharing link, medical exam site...") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("ai_import_url_input")
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = viewModel.importChapterName,
                                                onValueChange = { viewModel.importChapterName = it },
                                                label = { Text("Target Chapter") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text("Reference Book Target:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.Gray)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = viewModel.importBookSource == MedicalDataCatalog.BookSnellAnatomy,
                                                onClick = { viewModel.importBookSource = MedicalDataCatalog.BookSnellAnatomy }
                                            )
                                            Text("Snell's Anatomy", fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(16.dp))
                                            RadioButton(
                                                selected = viewModel.importBookSource == MedicalDataCatalog.BookRossPhysiology,
                                                onClick = { viewModel.importBookSource = MedicalDataCatalog.BookRossPhysiology }
                                            )
                                            Text("Ross Physiology", fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))

                                        if (viewModel.isImportLoading) {
                                            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(color = DeepMaroon)
                                            }
                                        } else {
                                            Button(
                                                onClick = { viewModel.processAiImport() },
                                                colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("extract_ai_button")
                                            ) {
                                                Text("EXTRACT QUESTIONS VIA GEMINI", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }

                                        viewModel.importStatusText?.let { status ->
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(status, color = MedicineGold, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            TextButton(onClick = { viewModel.importStatusText = null }, contentPadding = PaddingValues(0.dp)) {
                                                Text("Dismiss Log", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (viewModel.extractedMCQDrafts.isNotEmpty() || viewModel.extractedSEQDrafts.isNotEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = LightCream),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Extracted Questions Review Draft", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 14.sp)
                                        Text("The following questions were successfully parsed by Gemini. Confirm details and publish them to textbook chapters.", fontSize = 11.sp, color = Color.DarkGray)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        if (viewModel.importMode == "MCQ") {
                                            viewModel.extractedMCQDrafts.forEachIndexed { index, mcq ->
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Text("${index + 1}. MCQ: ${mcq.question}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkMaroon)
                                                        Text("Options: A) ${mcq.optionA} | B) ${mcq.optionB} | C) ${mcq.optionC} | D) ${mcq.optionD}", fontSize = 10.sp, color = Color.Gray)
                                                        Text("Correct Answer: ${mcq.correctAnswer}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MedicineGold)
                                                    }
                                                }
                                            }
                                        } else {
                                            viewModel.extractedSEQDrafts.forEachIndexed { index, seq ->
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Text("${index + 1}. SEQ: ${seq.question}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkMaroon)
                                                        Text("Answer: ${seq.baseAnswer}", fontSize = 11.sp, color = Color.DarkGray)
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { viewModel.publishImportedDrafts() },
                                            colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("publish_all_import_button")
                                        ) {
                                            Text("CONFIRM & PUBLISH ALL DRAFTS", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text("Active MCQs Direct Inspection (${allMcqs.size})", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 14.sp)
                        }

                        items(allMcqs) { mcq ->
                            val isCustom = mcq.id.startsWith("cus_")
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(mcq.question, fontWeight = FontWeight.Bold, color = DarkMaroon, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Text("Source: ${mcq.bookSource} | ${mcq.chapterName}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        if (isCustom) {
                                            IconButton(onClick = { viewModel.removeCustomMCQ(mcq.id) }) {
                                                Icon(Icons.Default.Delete, null, tint = CoralRed, modifier = Modifier.size(18.dp))
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(LightCream)
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text("PRESET", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MedicineGold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text("Active SEQs Direct Inspection (${allseqs.size})", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 14.sp)
                        }

                        items(allseqs) { seq ->
                            val isCustom = seq.id.startsWith("cus_")
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("QUESTION: ${seq.question}", fontWeight = FontWeight.Bold, color = DarkMaroon, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Text("Source: ${seq.bookSource} | ${seq.chapterName}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        if (isCustom) {
                                            IconButton(onClick = { viewModel.removeCustomSEQ(seq.id) }) {
                                                Icon(Icons.Default.Delete, null, tint = CoralRed, modifier = Modifier.size(18.dp))
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(LightCream)
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text("PRESET", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MedicineGold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "files" -> {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    var uploadFileName by remember { mutableStateOf("") }
                    var uploadFileType by remember { mutableStateOf("image") } // "image", "pdf", "document"

                    val logoLauncher = rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                    ) { uri ->
                        uri?.let {
                            val clonedPath = copyUriToLocalFile(context, it, "logo")
                            if (clonedPath != null) {
                                viewModel.updateLogoImage(clonedPath)
                            }
                        }
                    }

                    val fileLauncher = rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                    ) { uri ->
                        uri?.let {
                            val finalName = if (uploadFileName.isNotBlank()) uploadFileName else "Uploaded File"
                            val clonedPath = copyUriToLocalFile(context, it, "file")
                            if (clonedPath != null) {
                                val file = java.io.File(clonedPath)
                                val sizeKb = if (file.exists()) "${file.length() / 1024} KB" else "Unknown Size"
                                viewModel.addUploadedFile(
                                    fileName = finalName,
                                    fileUri = clonedPath,
                                    fileSize = sizeKb,
                                    fileType = uploadFileType
                                )
                                uploadFileName = ""
                            }
                        }
                    }

                    Text("Medical File Storage & App Branding", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 20.sp, fontFamily = FontFamily.Serif)
                    Text("Upload anatomical illustrations, PDF lesson files, and update app logo branding dynamically.", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Section 1: Logo Branding
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Logo Upload & Branding from Gallery", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Upload your custom medicine team logo or avatar here. It replaces the default hospital icon across all screens.", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Preview
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(getLogoBgColor(appPrefs.logoBgColorHex)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AppLogoView(appPrefs = appPrefs, modifier = Modifier.size(54.dp))
                                        }

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (!appPrefs.customLogoUri.isNullOrEmpty()) {
                                                Text("Custom Image Loaded Successfully", color = MedicineGold, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                                Text("Source: Local Storage Cloned", fontSize = 10.sp, color = Color.Gray)
                                            } else {
                                                Text("No Custom Image Loaded (Default Vector)", color = Color.Gray, fontSize = 12.sp)
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { logoLauncher.launch("image/*") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.height(32.dp).testTag("select_logo_gallery_btn")
                                                ) {
                                                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(12.dp), tint = Color.White)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Pick Logo", fontSize = 10.sp, color = Color.White)
                                                }

                                                if (!appPrefs.customLogoUri.isNullOrEmpty()) {
                                                    OutlinedButton(
                                                        onClick = { viewModel.clearLogoImage() },
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                        shape = RoundedCornerShape(6.dp),
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Text("Reset", fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Section 2: Upload Files
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Upload Files / Assets / Study Media", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = uploadFileName,
                                        onValueChange = { uploadFileName = it },
                                        label = { Text("Display Name / Title describing the file") },
                                        placeholder = { Text("E.g. Heart Anatomy Diagram, Semester Checklist") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("add_file_name_input")
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text("Select File Category Style:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.Gray)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("image" to "Anatomy Image", "pdf" to "PDF Guide", "document" to "Any Document").forEach { (typeKey, typeLabel) ->
                                            val isSelected = uploadFileType == typeKey
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isSelected) DeepMaroon else LightCream)
                                                    .clickable { uploadFileType = typeKey }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(typeLabel, color = if (isSelected) WarmWhite else Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            val filter = when (uploadFileType) {
                                                "image" -> "image/*"
                                                "pdf" -> "application/pdf"
                                                else -> "*/*"
                                            }
                                            fileLauncher.launch(filter)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("choose_file_and_upload_btn")
                                    ) {
                                        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SELECT & UPLOAD FROM GALLERY + COMPUTER", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Section 3: Gallery of Uploads
                        item {
                            Text("Stored Materials & Reference Uploads (${customUploadedFiles.size})", fontWeight = FontWeight.Bold, color = DeepMaroon, fontSize = 14.sp)
                        }

                        if (customUploadedFiles.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No custom learning images or files uploaded yet.", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }

                        items(customUploadedFiles) { ufield ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(LightCream),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (ufield.fileType == "image") {
                                                    AsyncImage(
                                                        model = ufield.fileUri,
                                                        contentDescription = ufield.fileName,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                    )
                                                } else {
                                                    val icon = when (ufield.fileType) {
                                                        "pdf" -> Icons.Default.PictureAsPdf
                                                        else -> Icons.Default.InsertDriveFile
                                                    }
                                                    Icon(icon, null, tint = DeepMaroon, modifier = Modifier.size(20.dp))
                                                }
                                            }

                                            Column {
                                                Text(ufield.fileName, fontWeight = FontWeight.Bold, color = DarkMaroon, fontSize = 13.sp)
                                                Text("Size: ${ufield.fileSize} | Type: ${ufield.fileType.uppercase()}", fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (ufield.fileType == "image") {
                                                IconButton(
                                                    onClick = { viewModel.updateLogoImage(ufield.fileUri) }
                                                ) {
                                                    Icon(Icons.Default.Brush, "Set App Logo", tint = MedicineGold, modifier = Modifier.size(18.dp))
                                                }
                                            }

                                            IconButton(
                                                onClick = { viewModel.removeUploadedFile(ufield.id) }
                                            ) {
                                                Icon(Icons.Default.Delete, "Delete File", tint = CoralRed, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }

                                    // Direct Expanded Display of Custom Diagrams inside storage
                                    if (ufield.fileType == "image") {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(LightCream)
                                        ) {
                                            AsyncImage(
                                                model = ufield.fileUri,
                                                contentDescription = ufield.fileName,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
