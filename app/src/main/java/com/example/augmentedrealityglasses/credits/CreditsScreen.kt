package com.example.augmentedrealityglasses.credits

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun CreditScreen() {
    val theme = MaterialTheme.colorScheme

    Column {
        Text(
            text = "Credit screen",
            color = theme.primary
        )
    }
}