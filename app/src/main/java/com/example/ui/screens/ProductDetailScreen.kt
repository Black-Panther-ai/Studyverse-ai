package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entities.ProductEntity
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ProductDetailScreen(
    product: ProductEntity,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("product_detail_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back_button_product_detail")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Physical Item Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(Color(0xFFE2E8F0))
            ) {
                AsyncImage(
                    model = product.photoUrls.split(",").firstOrNull() ?: "",
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Surface(
                    color = PrimaryBlue,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = product.category,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = product.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${product.price.toInt()}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "₹${product.originalPrice.toInt()}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = TextDecoration.LineThrough,
                            color = Color.Gray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${product.pickupLocation}, ${product.city} • ${product.college}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Item Description",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val message = "Hi ${product.sellerName}, I am interested in buying '${product.title}' listed on StudySwap AI for ₹${product.price.toInt()}."
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://api.whatsapp.com/send?phone=${product.sellerWhatsApp}&text=${Uri.encode(message)}")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${product.sellerWhatsApp.replace("+", "")}?text=${Uri.encode(message)}"))
                            context.startActivity(webIntent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("product_detail_whatsapp_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "WhatsApp")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Contact Seller (${product.sellerName}) on WhatsApp",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
