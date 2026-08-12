package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UploadModalDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var uploadType by remember { mutableStateOf("Notes") } // "Notes" or "Physical"

    // Upload Progress & Status from ViewModel
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val uploadStatusText by viewModel.uploadStatusText.collectAsState()

    LaunchedEffect(uploadStatusText) {
        if (uploadStatusText?.contains("successfully!", ignoreCase = true) == true) {
            kotlinx.coroutines.delay(1000)
            onDismiss()
        }
    }

    // Form fields for Notes
    var noteType by remember { mutableStateOf("Free") } // "Free" or "Premium"
    var titleInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var subjectInput by remember { mutableStateOf("Computer Science") }
    var courseInput by remember { mutableStateOf("B.Tech CS") }
    var semesterInput by remember { mutableStateOf("Semester 5") }
    var collegeInput by remember { mutableStateOf("IIT Delhi") }
    var tagsInput by remember { mutableStateOf("Handwritten, Exam Prep, Topper Notes") }
    var priceInput by remember { mutableStateOf("49") }
    var copyrightDeclared by remember { mutableStateOf(false) }

    // File pickers state for Notes
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfName by remember { mutableStateOf("") }
    var selectedPreviewUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Form fields for Physical Item Marketplace
    var productTitleInput by remember { mutableStateOf("") }
    var productDescInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Books") }
    var conditionInput by remember { mutableStateOf("Like New") }
    var productPriceInput by remember { mutableStateOf("250") }
    var stateInput by remember { mutableStateOf("Delhi") }
    var cityInput by remember { mutableStateOf("New Delhi") }
    var mobileInput by remember { mutableStateOf("+91 98765 43210") }
    var whatsappInput by remember { mutableStateOf("+91 98765 43210") }
    var pickupLocationInput by remember { mutableStateOf("Hostel Campus Gate 2") }
    var productPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val physicalCategories = listOf(
        "Books", "Notebooks", "Digital Notes", "Handwritten Notes",
        "School Shoes", "School Uniform", "College Uniform",
        "Bag", "Backpack", "Stationery", "Calculator", "Electronics", "Others"
    )

    val conditions = listOf("New", "Like New", "Good", "Fair")
    val quickPrices = listOf("10", "20", "50", "99", "199", "499")

    // Activity Result Launchers
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPdfUri = uri
            selectedPdfName = uri.lastPathSegment?.substringAfterLast('/') ?: "Uploaded_Document.pdf"
            if (!selectedPdfName.endsWith(".pdf", ignoreCase = true)) {
                selectedPdfName += ".pdf"
            }
        }
    }

    val previewImagesPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedPreviewUris = (selectedPreviewUris + uris).take(5)
        }
    }

    val productPhotosPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            productPhotoUris = (productPhotoUris + uris).take(5)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        modifier = Modifier.testTag("upload_modal_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upload & Publish",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, enabled = !isUploading) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (isUploading) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = uploadStatusText ?: "Uploading to Storage...",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { uploadProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(uploadProgress * 100).toInt()}% Complete",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Selector: Digital Notes vs Physical Item
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { uploadType = "Notes" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uploadType == "Notes") PrimaryBlue else Color.LightGray
                            ),
                            modifier = Modifier.weight(1f).testTag("upload_type_notes")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Digital Notes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { uploadType = "Physical" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uploadType == "Physical") PrimaryBlue else Color.LightGray
                            ),
                            modifier = Modifier.weight(1f).testTag("upload_type_physical")
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Physical Item", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (uploadType == "Notes") {
                        // Free vs Premium Toggle
                        Text("Note Type & Pricing", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = noteType == "Free",
                                onClick = { noteType = "Free" },
                                label = { Text("Free Note (₹0)") },
                                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = noteType == "Premium",
                                onClick = { noteType = "Premium" },
                                label = { Text("Premium Note (Paid)") },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (noteType == "Premium") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Quick Price Suggestions:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                quickPrices.forEach { priceVal ->
                                    SuggestionChip(
                                        onClick = { priceInput = priceVal },
                                        label = { Text("₹$priceVal", fontSize = 11.sp) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = priceInput,
                                onValueChange = { priceInput = it },
                                label = { Text("Set Price (₹)") },
                                modifier = Modifier.fillMaxWidth().testTag("upload_price_input"),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // PDF File Upload Picker
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pdfPickerLauncher.launch("application/pdf") },
                            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PrimaryBlue,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (selectedPdfUri != null) selectedPdfName else "Select PDF File (Required)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (selectedPdfUri != null) AccentGreen else PrimaryBlue
                                    )
                                    Text(
                                        text = if (selectedPdfUri != null) "File attached cleanly" else "Pick from File Manager, Gallery, Google Drive",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = PrimaryBlue)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Preview Images Picker
                        Text("Preview Images / Sample Pages (JPG, PNG, WEBP)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { previewImagesPickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Sample Note Photos (${selectedPreviewUris.size}/5)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }

                        if (selectedPreviewUris.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(selectedPreviewUris) { uri ->
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Sample Note Preview",
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, PrimaryBlue, RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Notes Title / Chapter Name") },
                            modifier = Modifier.fillMaxWidth().testTag("upload_title_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = descriptionInput,
                            onValueChange = { descriptionInput = it },
                            label = { Text("Description & Syllabus Covered") },
                            modifier = Modifier.fillMaxWidth().testTag("upload_description_input"),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = subjectInput,
                                onValueChange = { subjectInput = it },
                                label = { Text("Subject") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = semesterInput,
                                onValueChange = { semesterInput = it },
                                label = { Text("Semester") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = courseInput,
                                onValueChange = { courseInput = it },
                                label = { Text("Course / Branch") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = collegeInput,
                                onValueChange = { collegeInput = it },
                                label = { Text("College Name") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = tagsInput,
                            onValueChange = { tagsInput = it },
                            label = { Text("Search Tags (e.g., PYQ, Unit 1, Topper)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Copyright Declaration Box
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.12f))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = copyrightDeclared,
                                    onCheckedChange = { copyrightDeclared = it },
                                    modifier = Modifier.testTag("copyright_declaration_checkbox")
                                )
                                Column {
                                    Text("Original Content Declaration", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AccentGreen)
                                    Text("I declare that these notes are my original handwritten work.", fontSize = 10.sp, color = Color.DarkGray)
                                }
                            }
                        }

                    } else {
                        // Physical Item Marketplace Form
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pickup-Only Campus Marketplace. Direct student-to-student handover.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Category", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(physicalCategories) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = productTitleInput,
                            onValueChange = { productTitleInput = it },
                            label = { Text("Item Title (e.g. Casio fx-991EX Calculator)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = productDescInput,
                            onValueChange = { productDescInput = it },
                            label = { Text("Description & Condition Details") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Condition", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            conditions.forEach { cond ->
                                FilterChip(
                                    selected = conditionInput == cond,
                                    onClick = { conditionInput = cond },
                                    label = { Text(cond, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = productPriceInput,
                            onValueChange = { productPriceInput = it },
                            label = { Text("Price (₹)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Photos Picker
                        Text("Item Photos (JPG, PNG, WEBP)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { productPhotosPickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Photos (${productPhotoUris.size}/5)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }

                        if (productPhotoUris.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(productPhotoUris) { uri ->
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Product Photo",
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, PrimaryBlue, RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = stateInput,
                                onValueChange = { stateInput = it },
                                label = { Text("State") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = cityInput,
                                onValueChange = { cityInput = it },
                                label = { Text("City") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = pickupLocationInput,
                            onValueChange = { pickupLocationInput = it },
                            label = { Text("Pickup Location / Hostel Gate") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = mobileInput,
                                onValueChange = { mobileInput = it },
                                label = { Text("Mobile Number") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = whatsappInput,
                                onValueChange = { whatsappInput = it },
                                label = { Text("WhatsApp Number") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isUploading) {
                Button(
                    onClick = {
                        if (uploadType == "Notes") {
                            if (selectedPdfUri == null) {
                                viewModel.uiMessage.value = "Please select a valid PDF file to upload."
                                return@Button
                            }
                            if (!copyrightDeclared) {
                                viewModel.uiMessage.value = "Mandatory: You must check the Original Content Declaration box."
                                return@Button
                            }

                            val price = priceInput.toDoubleOrNull() ?: 0.0
                            val isFreeNote = noteType == "Free"
                            val finalPdfUri = selectedPdfUri!!.toString()
                            val finalPdfName = if (selectedPdfName.isNotBlank()) selectedPdfName else "HandwrittenNotes.pdf"
                            val previewStrings = selectedPreviewUris.map { it.toString() }

                            viewModel.lastSelectedPdfUriForDiag = finalPdfUri
                            viewModel.lastSelectedPdfNameForDiag = finalPdfName

                            viewModel.createHandwrittenNotes(
                                context = context,
                                title = titleInput.ifEmpty { "Handwritten Lecture Notes" },
                                description = descriptionInput.ifEmpty { "Complete high-yield handwritten exam notes." },
                                subject = subjectInput,
                                semester = semesterInput,
                                course = courseInput,
                                college = collegeInput,
                                tags = tagsInput,
                                isFree = isFreeNote,
                                price = if (isFreeNote) 0.0 else price,
                                pdfUri = finalPdfUri,
                                pdfFileName = finalPdfName,
                                previewImagesList = previewStrings,
                                copyrightDeclared = copyrightDeclared
                            )
                        } else {
                            val price = productPriceInput.toDoubleOrNull() ?: 250.0
                            val photoStrings = productPhotoUris.map { it.toString() }

                            viewModel.createPhysicalProduct(
                                title = productTitleInput.ifEmpty { "Used Scientific Calculator" },
                                description = productDescInput.ifEmpty { "Good working condition for semester exams." },
                                category = selectedCategory,
                                condition = conditionInput,
                                price = price,
                                pickupLocation = pickupLocationInput,
                                city = cityInput,
                                state = stateInput,
                                mobileNumber = mobileInput,
                                whatsappNumber = whatsappInput,
                                photoUrlsList = photoStrings
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.testTag("submit_upload_button")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Publish Listing")
                }
            }
        },
        dismissButton = {
            if (!isUploading) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
