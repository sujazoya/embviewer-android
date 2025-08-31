package com.example.embviewer.ui.preview

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import com.example.embviewer.ui.screens.MainScreen
import com.example.embviewer.ui.viewmodel.ConverterViewModel
import androidx.lifecycle.viewmodel.compose.viewModel


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
    // Note: This preview won't work with JNI calls
    // but will show the UI layout
    MainScreen(viewModel = ConverterViewModel())
}