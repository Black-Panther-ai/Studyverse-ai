package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onDarkThemeToggle: () -> Unit,
    isDarkTheme: Boolean
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    if (!isLoggedIn || currentUser == null) {
        AuthScreen(
            viewModel = viewModel,
            onAuthSuccess = { /* Navigation handled via state */ }
        )
        return
    }

    val user = currentUser!!

    var nameInput by remember(user) { mutableStateOf(user.name) }
    var collegeInput by remember(user) { mutableStateOf(user.collegeName) }
    var courseInput by remember(user) { mutableStateOf(user.course) }
    var semesterInput by remember(user) { mutableStateOf(user.semester) }
    var stateInput by remember(user) { mutableStateOf(user.state) }
    var whatsappInput by remember(user) { mutableStateOf(user.phoneWhatsApp) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        contentPadding = PaddingValues(16.dp, bottom = 80.dp)
    ) {
        item {
            Text(
                text = "My Profile & Account Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // User Profile Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        if (user.profilePicture.isNotEmpty()) {
                            AsyncImage(
                                model = user.profilePicture,
                                contentDescription = "Profile Picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(40.dp).align(Alignment.Center)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${user.collegeName} • ${user.course}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = AccentGreen,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Role: ${user.role}",
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Joined: ${user.createdDate}",
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Edit Profile Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = collegeInput,
                onValueChange = { collegeInput = it },
                label = { Text("College Name") },
                modifier = Modifier.fillMaxWidth().testTag("profile_college_input"),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = courseInput,
                    onValueChange = { courseInput = it },
                    label = { Text("Course (e.g. B.Tech CS)") },
                    modifier = Modifier.weight(1f).testTag("profile_course_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = semesterInput,
                    onValueChange = { semesterInput = it },
                    label = { Text("Semester") },
                    modifier = Modifier.weight(1f).testTag("profile_semester_input"),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = stateInput,
                    onValueChange = { stateInput = it },
                    label = { Text("State") },
                    modifier = Modifier.weight(1f).testTag("profile_state_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = whatsappInput,
                    onValueChange = { whatsappInput = it },
                    label = { Text("WhatsApp Number") },
                    modifier = Modifier.weight(1f).testTag("profile_whatsapp_input"),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.updateUserProfile(nameInput, collegeInput, courseInput, semesterInput, stateInput, whatsappInput)
                },
                modifier = Modifier.fillMaxWidth().testTag("save_profile_button"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Profile Changes", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Account Dashboard & Shortcuts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Section Links
            ProfileSectionItem("My Orders", Icons.Default.ReceiptLong, "View your active orders & purchases") {
                viewModel.switchTab(com.example.ui.viewmodel.AppTab.BUYER_DASHBOARD)
            }
            ProfileSectionItem("Transaction History", Icons.Default.History, "Instamojo transaction logs") {
                viewModel.switchTab(com.example.ui.viewmodel.AppTab.BUYER_DASHBOARD)
            }
            ProfileSectionItem("Payment History", Icons.Default.Payment, "Detailed payment invoices") {
                viewModel.switchTab(com.example.ui.viewmodel.AppTab.BUYER_DASHBOARD)
            }
            ProfileSectionItem("My Downloads", Icons.Default.Download, "Access watermarked PDF study notes") {
                viewModel.switchTab(com.example.ui.viewmodel.AppTab.BUYER_DASHBOARD)
            }
            ProfileSectionItem("Saved Items & Wishlist", Icons.Default.Favorite, "Bookmarked books & notes") {
                viewModel.switchTab(com.example.ui.viewmodel.AppTab.WISHLIST)
            }
            ProfileSectionItem("Settings & Preferences", Icons.Default.Settings, "Theme mode, alerts & security") {
                viewModel.switchTab(com.example.ui.viewmodel.AppTab.SETTINGS)
            }
            ProfileSectionItem("Notifications", Icons.Default.Notifications, "Manage push notifications") {
                viewModel.switchTab(com.example.ui.viewmodel.AppTab.SETTINGS)
            }
            ProfileSectionItem("Help & Support", Icons.Default.HelpOutline, "FAQs & 24/7 Student Desk") {
                viewModel.switchTab(com.example.ui.viewmodel.AppTab.HELP_SUPPORT)
            }
            ProfileSectionItem("Privacy Policy", Icons.Default.Lock, "Read our data & privacy policy") {
                viewModel.switchTab(com.example.ui.viewmodel.AppTab.PRIVACY_POLICY)
            }
            ProfileSectionItem("Terms & Conditions", Icons.Default.Gavel, "User agreement & marketplace rules") {
                viewModel.switchTab(com.example.ui.viewmodel.AppTab.TERMS_CONDITIONS)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dark Theme Switcher
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Theme", fontWeight = FontWeight.Bold)
                        Text("Switch between Light and Modern Dark mode", fontSize = 11.sp, color = Color.Gray)
                    }

                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onDarkThemeToggle() },
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logout Button
            OutlinedButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.fillMaxWidth().testTag("logout_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign Out", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfileSectionItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, fontSize = 11.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
    }
}
