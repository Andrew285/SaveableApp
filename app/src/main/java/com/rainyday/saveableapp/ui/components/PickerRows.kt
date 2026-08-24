package com.rainyday.saveableapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.rainyday.saveableapp.ui.theme.AccentColors
import com.rainyday.saveableapp.ui.theme.AppAlpha
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

@Composable
fun ColorPickerRow(
    selectedHex: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimens.d12)
    ) {
        items(AccentColors.palette) { hex ->
            val color = parseHexColor(hex)
            val selected = hex.equals(selectedHex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(Dimens.d40)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (selected) Dimens.d3 else Dimens.d0,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape
                    )
                    .clickable { onSelect(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun IconPickerRow(
    selectedKey: String,
    accentHex: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = parseHexColor(accentHex)
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimens.d12)
    ) {
        items(IconCatalog.icons.keys.toList()) { key ->
            val selected = key == selectedKey
            Box(
                modifier = Modifier
                    .size(Dimens.d44)
                    .clip(RoundedCornerShape(Dimens.d14))
                    .background(if (selected) accent.copy(alpha = AppAlpha.a18) else MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = if (selected) Dimens.d2 else Dimens.d0,
                        color = accent,
                        shape = RoundedCornerShape(Dimens.d14)
                    )
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = IconCatalog.resolve(key),
                    contentDescription = null,
                    tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ColorPickerRowPreview() {
    SaveableAppTheme {
        ColorPickerRow(selectedHex = AccentColors.palette.first(), onSelect = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun IconPickerRowPreview() {
    SaveableAppTheme {
        IconPickerRow(selectedKey = IconCatalog.defaultKey, accentHex = AccentColors.palette.first(), onSelect = {})
    }
}
