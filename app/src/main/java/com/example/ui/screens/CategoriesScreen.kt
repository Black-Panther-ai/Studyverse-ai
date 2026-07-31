package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel

data class CategoryItem(
    val name: String,
    val icon: ImageVector,
    val description: String
)

@Composable
fun CategoriesScreen(
    viewModel: MainViewModel,
    onCategorySelected: (String) -> Unit
) {
    val allProducts by viewModel.allProducts.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()

    val categoriesList = listOf(
        CategoryItem("Books", Icons.Default.MenuBook, "Textbooks, Reference Books, Guides"),
        CategoryItem("E-Books", Icons.Default.FileDownload, "PDF E-Books & Reference Guides"),
        CategoryItem("Handwritten Notes", Icons.Default.Edit, "Classroom Lecture Notes"),
        CategoryItem("Notebooks", Icons.Default.Book, "Blank & Practice Register Notebooks"),
        CategoryItem("School Shoes", Icons.Default.RollerSkating, "Black Shoes & Sports Shoes"),
        CategoryItem("School Uniform", Icons.Default.Checkroom, "Shirts, Trousers, Skirts, Blazers"),
        CategoryItem("College Uniform", Icons.Default.DryCleaning, "Formal Shirts, Blazers, Ties"),
        CategoryItem("Lab Coat", Icons.Default.Biotech, "Science, Medical & Chemistry Coats"),
        CategoryItem("Bags", Icons.Default.ShoppingBag, "College Shoulder Bags & Totes"),
        CategoryItem("Backpacks", Icons.Default.Backpack, "Laptop Bags & Heavy Backpacks"),
        CategoryItem("Stationery", Icons.Default.Create, "Pens, Geometry Sets, Markers"),
        CategoryItem("Calculator", Icons.Default.Calculate, "Scientific & Standard Calculators"),
        CategoryItem("Scientific Instruments", Icons.Default.Science, "Drawing Boards, Drafters, Kits"),
        CategoryItem("Electronics", Icons.Default.Devices, "Tablets, Pen Drives, Headphones"),
        CategoryItem("Other Study Materials", Icons.Default.FolderSpecial, "Syllabus, Question Banks"),
        CategoryItem("Other", Icons.Default.Category, "Miscellaneous Campus Items")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("categories_screen")
    ) {
        Text(
            text = "Browse Categories",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Select a category to filter marketplace listings and digital study notes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(categoriesList) { item ->
                val count = allProducts.count { it.category.equals(item.name, ignoreCase = true) } +
                        allNotes.count { it.subject.equals(item.name, ignoreCase = true) || it.course.equals(item.name, ignoreCase = true) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clickable {
                            viewModel.selectedCategory.value = item.name
                            onCategorySelected(item.name)
                        }
                        .testTag("category_card_${item.name}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = PrimaryBlue.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.name,
                                    tint = PrimaryBlue,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(24.dp)
                                )
                            }

                            Surface(
                                color = AccentGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$count items",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
