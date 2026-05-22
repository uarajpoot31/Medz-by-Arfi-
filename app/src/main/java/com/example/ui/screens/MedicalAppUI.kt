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
import androidx.compose.ui.draw.clip
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
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalAppUI(viewModel: MedicalViewModel) {
    val isLoggedIn = viewModel.isLoggedIn

    if (!isLoggedIn) {
        AuthScreen(viewModel)
    } else {
        val userProfile by viewModel.userProfile.collectAsState()
        val currentTab = viewModel.selectedTab

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = MedicineGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Medz with Arfi",
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
                        IconButton(onClick = { viewModel.selectedTab = "admin" }) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Admin Portal",
                                tint = MedicineGold
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
                when (viewModel.selectedTab) {
                    "dashboard" -> DashboardTab(viewModel)
                    "study" -> StudyTab(viewModel)
                    "quiz" -> QuizTab(viewModel)
                    "notes" -> NotesTab(viewModel)
                    "search" -> SearchTab(viewModel)
                    "assistant" -> AssistantTab(viewModel)
                    "admin" -> AdminTab(viewModel)
                    "profile" -> ProfileTab(viewModel)
                    else -> DashboardTab(viewModel)
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
    val isSignUp = viewModel.isSignUpMode
    val error = viewModel.authError

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
            // Heart/Medical Crest Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF8E1439)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = "Medical Services",
                    tint = MedicineGold,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Medz with Arfi",
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
                        text = if (isSignUp) "Create Physician Account" else "Sign In to Clinic",
                        color = DeepMaroon,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isSignUp) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; viewModel.authName = it },
                            label = { Text("Physician Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = DeepMaroon) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("auth_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepMaroon)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; viewModel.authEmail = it },
                        label = { Text("Medical Email") },
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
                                viewModel.performSignUp()
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
                        TextButton(onClick = { viewModel.triggerForgotPassword() }) {
                            Text("Forgot Password?", color = LightMaroon, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Divider(color = LightGrey, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(12.dp))

                    // OAuth Google integration support demo
                    OutlinedButton(
                        onClick = { viewModel.loginWithGoogle() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Sign In with Gmail", fontWeight = FontWeight.SemiBold)
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
    var selectedBook by remember { mutableStateOf<String?>(null) } // null showing choices, otherwise chapters list

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (selectedBook == null) {
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
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(
                                    onClick = { viewModel.askAiAboutTopic("$bookName chapter $chapter") }
                                ) {
                                    Icon(Icons.Default.AutoAwesome, "AI", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AI Briefing", fontSize = 12.sp)
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
                                    modifier = Modifier.testTag("start_quiz_$chapter")
                                ) {
                                    Text("Start Quiz", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PlayCircleFilled, null, modifier = Modifier.size(64.dp), tint = DeepMaroon)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No Active Study Exam Session",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = DeepMaroon
                )
                Text(
                    "Go to the 'Books' tab to trigger specific chapters, or tap below to execute a quick random mock exam instantly.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        viewModel.startQuiz(
                            viewModel.allMCQs.value,
                            "Random Practice",
                            "Quick Mock Mix"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepMaroon),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Launch Random Board Exam")
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
