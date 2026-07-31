package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entities.ProductEntity
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryBlue

@Composable
fun ProductCard(
    product: ProductEntity,
    onProductClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onProductClick() }
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Product Photo Header with Category & Condition Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFFE2E8F0))
            ) {
                AsyncImage(
                    model = product.photoUrls.split(",").firstOrNull() ?: "",
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Condition Badge Top Left
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = product.condition,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Category Badge Top Right
                Surface(
                    color = PrimaryBlue,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Text(
                        text = product.category,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Price and Original Price Strike-through
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${product.price.toInt()}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryBlue
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "₹${product.originalPrice.toInt()}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = TextDecoration.LineThrough,
                            color = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SAVE ${((1 - product.price / product.originalPrice) * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Location & Seller
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${product.pickupLocation}, ${product.city} • ${product.college}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // WhatsApp Contact Button
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
                        .testTag("whatsapp_button_${product.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "WhatsApp",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Contact Seller on WhatsApp",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
