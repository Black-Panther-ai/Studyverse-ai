package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.data.local.entities.ProductEntity
import com.example.ui.components.AdBanner
import com.example.ui.components.AdNative
import com.example.ui.components.AppFooter
import com.example.ui.components.CategoryChip
import com.example.ui.components.ProductCard
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.MainViewModel

@Composable
fun MarketplaceScreen(
    viewModel: MainViewModel,
    onProductSelect: (ProductEntity) -> Unit
) {
    val products by viewModel.allProducts.collectAsState()
    var searchInput by remember { mutableStateOf("") }
    var activeCategory by remember { mutableStateOf("All") }

    val categories = listOf(
        "All", "Books", "Notebooks", "Digital Notes", "Handwritten Notes",
        "School Shoes", "School Uniform", "College Uniform", "Bag", "Backpack",
        "Stationery", "Calculator", "Electronics", "Others"
    )

    val filteredProducts = remember(products, searchInput, activeCategory) {
        products.filter { product ->
            val matchesCategory = (activeCategory == "All" || product.category.equals(activeCategory, ignoreCase = true))
            val matchesSearch = searchInput.isBlank() ||
                    product.title.contains(searchInput, ignoreCase = true) ||
                    product.college.contains(searchInput, ignoreCase = true) ||
                    product.description.contains(searchInput, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("marketplace_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Search & Filter Header
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Student Marketplace",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Buy & sell physical study items, uniforms, shoes, and gear locally.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    placeholder = { Text("Search items, college, location...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
                    trailingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, tint = PrimaryBlue) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("marketplace_search_input"),
                    singleLine = true
                )
            }
        }

        // Category Horizontal Chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(categories) { category ->
                    CategoryChip(
                        label = category,
                        isSelected = activeCategory == category,
                        onClick = { activeCategory = category }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (filteredProducts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (products.isEmpty()) "No products available" else "No listings found matching your search.",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (products.isEmpty()) "Be the first student to publish a product listing!" else "Try clearing filters or search terms.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            itemsIndexed(filteredProducts) { index, product ->
                Column {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        ProductCard(
                            product = product,
                            onProductClick = { onProductSelect(product) }
                        )
                    }

                    if ((index + 1) % 4 == 0 && (index + 1) % 8 != 0) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            AdNative()
                        }
                    } else if ((index + 1) % 8 == 0) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            AdBanner()
                        }
                    }
                }
            }
        }

        item {
            AppFooter()
        }
    }
}
