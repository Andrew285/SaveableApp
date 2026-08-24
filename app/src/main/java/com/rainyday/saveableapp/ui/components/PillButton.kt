package com.rainyday.saveableapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.rainyday.saveableapp.ui.theme.AppAlpha
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.PillShape
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

/** Color scheme for an outlined or filled pill button — see [PillButtonPaletteDefaults]. */
@Immutable
data class PillButtonPalette(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
    val borderColor: Color = Color.Transparent,
)

object PillButtonPaletteDefaults {

    /** Outlined mint — primary but non-destructive actions ("Add List", "Save Task"). */
    @Composable
    fun primary(): PillButtonPalette {
        val colors = MaterialTheme.colorScheme
        return PillButtonPalette(
            containerColor = Color.Transparent,
            contentColor = colors.primary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = colors.primary.copy(alpha = AppAlpha.a38),
            borderColor = colors.primary
        )
    }

    /** Outlined red — destructive actions ("Delete Task"). */
    @Composable
    fun danger(): PillButtonPalette {
        val colors = MaterialTheme.colorScheme
        return PillButtonPalette(
            containerColor = Color.Transparent,
            contentColor = colors.error,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = colors.error.copy(alpha = AppAlpha.a38),
            borderColor = colors.error
        )
    }

    /** Solid mint — the one prominent call-to-action on a screen. */
    @Composable
    fun filled(): PillButtonPalette {
        val colors = MaterialTheme.colorScheme
        return PillButtonPalette(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.primary.copy(alpha = AppAlpha.a38),
            disabledContentColor = colors.onPrimary.copy(alpha = AppAlpha.a38)
        )
    }
}

/** Outlined pill — primary but non-destructive actions ("Add List", "Save Task"). */
@Composable
fun PillButtonPrimary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    palette: PillButtonPalette = PillButtonPaletteDefaults.primary()
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        border = BorderStroke(Dimens.d1, palette.borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = palette.contentColor,
            disabledContentColor = palette.disabledContentColor
        ),
        contentPadding = PaddingValues(vertical = Dimens.d14),
        modifier = modifier
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Outlined pill — destructive actions ("Delete Task"). */
@Composable
fun PillButtonDanger(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    palette: PillButtonPalette = PillButtonPaletteDefaults.danger()
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        border = BorderStroke(Dimens.d1, palette.borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = palette.contentColor,
            disabledContentColor = palette.disabledContentColor
        ),
        contentPadding = PaddingValues(vertical = Dimens.d14),
        modifier = modifier
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Solid pill — the one prominent call-to-action on a screen. */
@Composable
fun PillButtonFilled(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    palette: PillButtonPalette = PillButtonPaletteDefaults.filled()
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.containerColor,
            contentColor = palette.contentColor,
            disabledContainerColor = palette.disabledContainerColor,
            disabledContentColor = palette.disabledContentColor
        ),
        contentPadding = PaddingValues(vertical = Dimens.d14),
        modifier = modifier
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun PillButtonsPreview() {
    SaveableAppTheme {
        Column(modifier = Modifier.padding(Dimens.d16), verticalArrangement = Arrangement.spacedBy(Dimens.d12)) {
            PillButtonPrimary(text = "Add List", onClick = {})
            PillButtonDanger(text = "Delete Task", onClick = {})
            PillButtonFilled(text = "Save Task", onClick = {})
            PillButtonFilled(text = "Save Task", onClick = {}, enabled = false)
        }
    }
}
