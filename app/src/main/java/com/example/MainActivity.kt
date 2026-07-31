package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.NoteEntity
import com.example.data.local.entities.ProductEntity
import com.example.ui.screens.*
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.StudySwapTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.data.firebase.FirebaseManager.initialize(applicationContext)

        intent?.data?.let { uri ->
            viewModel.handleDeepLink(uri)
        }

        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }

            StudySwapTheme(darkTheme = isDarkTheme) {
                MainAppContent(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onDarkThemeToggle = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { uri ->
            viewModel.handleDeepLink(uri)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    isDarkTheme: Boolean,
    onDarkThemeToggle: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val context = LocalContext.current

    var selectedNoteForDetail by remember { mutableStateOf<NoteEntity?>(null) }
    var selectedProductForDetail by remember { mutableStateOf<ProductEntity?>(null) }
    var showUploadModal by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUiMessage()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .testTag("app_navigation_drawer")
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryBlue)
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "StudySwap AI",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (currentUser != null) {
                            Text(
                                currentUser!!.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                currentUser!!.email,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                "Student Workspace",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Items
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)
                ) {
                    DrawerMenuItem("Home", Icons.Default.Home, currentTab == AppTab.HOME) {
                        viewModel.switchTab(AppTab.HOME)
                        scope.launch { drawerState.close() }
                    }

                    DrawerMenuItem("Categories", Icons.Default.Category, currentTab == AppTab.CATEGORIES) {
                        viewModel.switchTab(AppTab.CATEGORIES)
                        scope.launch { drawerState.close() }
                    }

                    DrawerMenuItem("Marketplace", Icons.Default.Storefront, currentTab == AppTab.MARKETPLACE) {
                        viewModel.switchTab(AppTab.MARKETPLACE)
                        scope.launch { drawerState.close() }
                    }

                    DrawerMenuItem("Free Notes", Icons.Default.MenuBook, currentTab == AppTab.FREE_NOTES) {
                        viewModel.switchTab(AppTab.FREE_NOTES)
                        scope.launch { drawerState.close() }
                    }

                    DrawerMenuItem("Notes Store", Icons.Default.ShoppingBag, currentTab == AppTab.DIGITAL_STORE) {
                        viewModel.switchTab(AppTab.DIGITAL_STORE)
                        scope.launch { drawerState.close() }
                    }

                    DrawerMenuItem("Sell Item / Upload Note", Icons.Default.AddCircle, currentTab == AppTab.SELL) {
                        showUploadModal = true
                        scope.launch { drawerState.close() }
                    }

                    DrawerMenuItem("My Downloads", Icons.Default.Download, currentTab == AppTab.BUYER_DASHBOARD) {
                        viewModel.switchTab(AppTab.BUYER_DASHBOARD)
                        scope.launch { drawerState.close() }
                    }

                    DrawerMenuItem("Wishlist & Saved", Icons.Default.Favorite, currentTab == AppTab.WISHLIST) {
                        viewModel.switchTab(AppTab.WISHLIST)
                        scope.launch { drawerState.close() }
                    }

                    DrawerMenuItem("My Orders", Icons.Default.ReceiptLong, currentTab == AppTab.ORDERS) {
                        viewModel.switchTab(AppTab.BUYER_DASHBOARD)
                        scope.launch { drawerState.close() }
                    }

                    DrawerMenuItem("Seller Dashboard", Icons.Default.CloudUpload, currentTab == AppTab.SELLER_DASHBOARD) {
                        viewModel.switchTab(AppTab.SELLER_DASHBOARD)
                        scope.launch { drawerState.close() }
                    }

                    DrawerMenuItem("AI Tutor", Icons.Default.AutoAwesome, currentTab == AppTab.AI_ASSISTANT) {
                        viewModel.switchTab(AppTab.AI_ASSISTANT)
                        scope.launch { drawerState.close() }
                    }

                    if (currentUser?.role == "Admin" || currentUser?.userType == "Admin") {
                        DrawerMenuItem("Admin Panel", Icons.Default.Security, currentTab == AppTab.ADMIN_PANEL, isHighlight = true) {
                            viewModel.switchTab(AppTab.ADMIN_PANEL)
                            scope.launch { drawerState.close() }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    DrawerMenuItem("My Profile", Icons.Default.Person, currentTab == AppTab.PROFILE) {
                        viewModel.switchTab(AppTab.PROFILE)
                        scope.launch { drawerState.close() }
                    }

                    DrawerMenuItem("Settings", Icons.Default.Settings, currentTab == AppTab.SETTINGS) {
                        viewModel.switchTab(AppTab.SETTINGS)
                        scope.launch { drawerState.close() }
                    }

                    DrawerMenuItem("Help & Support", Icons.Default.HelpOutline, currentTab == AppTab.HELP_SUPPORT) {
                        viewModel.switchTab(AppTab.HELP_SUPPORT)
                        scope.launch { drawerState.close() }
                    }

                    DrawerMenuItem("Privacy Policy", Icons.Default.Lock, currentTab == AppTab.PRIVACY_POLICY) {
                        viewModel.switchTab(AppTab.PRIVACY_POLICY)
                        scope.launch { drawerState.close() }
                    }

                    DrawerMenuItem("Terms & Conditions", Icons.Default.Gavel, currentTab == AppTab.TERMS_CONDITIONS) {
                        viewModel.switchTab(AppTab.TERMS_CONDITIONS)
                        scope.launch { drawerState.close() }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    DrawerMenuItem("Sign Out", Icons.Default.Logout, false, isLogout = true) {
                        viewModel.logout()
                        scope.launch { drawerState.close() }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (currentTab) {
                                AppTab.HOME -> "StudySwap AI"
                                AppTab.CATEGORIES -> "Browse Categories"
                                AppTab.MARKETPLACE -> "Book Marketplace"
                                AppTab.FREE_NOTES -> "Free Study Notes"
                                AppTab.DIGITAL_STORE -> "Premium Digital Notes"
                                AppTab.SELL -> "Sell & List Item"
                                AppTab.BUYER_DASHBOARD -> "My Downloads & Orders"
                                AppTab.ORDERS -> "My Orders"
                                AppTab.WISHLIST, AppTab.SAVED_ITEMS -> "Wishlist & Saved"
                                AppTab.SELLER_DASHBOARD -> "Seller Dashboard"
                                AppTab.ADMIN_PANEL -> "Admin Portal"
                                AppTab.PROFILE -> "My Profile"
                                AppTab.SETTINGS, AppTab.NOTIFICATIONS -> "Settings & Alerts"
                                AppTab.HELP_SUPPORT -> "Help & Support"
                                AppTab.PRIVACY_POLICY -> "Privacy Policy"
                                AppTab.TERMS_CONDITIONS -> "Terms & Conditions"
                                AppTab.AI_ASSISTANT -> "AI Study Assistant"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Drawer Navigation")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.switchTab(AppTab.AI_ASSISTANT) }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant", tint = PrimaryBlue)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (selectedNoteForDetail != null) {
                    NoteDetailScreen(
                        note = selectedNoteForDetail!!,
                        viewModel = viewModel,
                        onBack = { selectedNoteForDetail = null }
                    )
                } else if (selectedProductForDetail != null) {
                    ProductDetailScreen(
                        product = selectedProductForDetail!!,
                        viewModel = viewModel,
                        onBack = { selectedProductForDetail = null }
                    )
                } else {
                    when (currentTab) {
                        AppTab.HOME -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToTab = { viewModel.switchTab(it) },
                            onNoteSelect = { selectedNoteForDetail = it },
                            onProductSelect = { selectedProductForDetail = it }
                        )
                        AppTab.CATEGORIES -> CategoriesScreen(
                            viewModel = viewModel,
                            onCategorySelected = { viewModel.switchTab(AppTab.MARKETPLACE) }
                        )
                        AppTab.MARKETPLACE -> MarketplaceScreen(
                            viewModel = viewModel,
                            onProductSelect = { selectedProductForDetail = it }
                        )
                        AppTab.FREE_NOTES -> FreeNotesScreen(
                            viewModel = viewModel,
                            onNoteSelect = { selectedNoteForDetail = it }
                        )
                        AppTab.DIGITAL_STORE -> DigitalNotesStoreScreen(
                            viewModel = viewModel,
                            onNoteSelect = { selectedNoteForDetail = it }
                        )
                        AppTab.SELL -> MarketplaceScreen(
                            viewModel = viewModel,
                            onProductSelect = { selectedProductForDetail = it }
                        )
                        AppTab.AI_ASSISTANT -> AiStudyAssistantScreen(
                            viewModel = viewModel
                        )
                        AppTab.BUYER_DASHBOARD, AppTab.ORDERS -> BuyerDashboardScreen(
                            viewModel = viewModel
                        )
                        AppTab.WISHLIST, AppTab.SAVED_ITEMS -> MarketplaceScreen(
                            viewModel = viewModel,
                            onProductSelect = { selectedProductForDetail = it }
                        )
                        AppTab.SELLER_DASHBOARD -> SellerDashboardScreen(
                            viewModel = viewModel,
                            onOpenUploadModal = { showUploadModal = true }
                        )
                        AppTab.ADMIN_PANEL -> AdminPanelScreen(
                            viewModel = viewModel
                        )
                        AppTab.PROFILE -> ProfileScreen(
                            viewModel = viewModel,
                            isDarkTheme = isDarkTheme,
                            onDarkThemeToggle = onDarkThemeToggle
                        )
                        AppTab.SETTINGS, AppTab.NOTIFICATIONS -> SettingsScreen(
                            isDarkTheme = isDarkTheme,
                            onDarkThemeToggle = onDarkThemeToggle
                        )
                        AppTab.HELP_SUPPORT -> HelpSupportScreen()
                        AppTab.PRIVACY_POLICY -> PrivacyPolicyScreen()
                        AppTab.TERMS_CONDITIONS -> TermsConditionsScreen()
                    }
                }
            }
        }
    }

    if (showUploadModal) {
        UploadModalDialog(
            viewModel = viewModel,
            onDismiss = { showUploadModal = false }
        )
    }
}

@Composable
fun DrawerMenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    isHighlight: Boolean = false,
    isLogout: Boolean = false,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                fontWeight = if (isSelected || isHighlight) FontWeight.Bold else FontWeight.Medium,
                color = if (isLogout) Color.Red else if (isHighlight) AccentGreen else MaterialTheme.colorScheme.onSurface
            )
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isLogout) Color.Red else if (isHighlight) AccentGreen else if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
