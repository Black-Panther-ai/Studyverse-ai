package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryBlue

@Composable
fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("category_chip_$label"),
        color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
        )
    }
}

