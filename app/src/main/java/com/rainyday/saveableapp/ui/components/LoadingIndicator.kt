package com.rainyday.saveableapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingIndicatorPreview() {
    SaveableAppTheme {
        LoadingIndicator(modifier = Modifier.height(Dimens.d200))
    }
}
