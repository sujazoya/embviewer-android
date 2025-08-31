package com.example.embviewer
import androidx.lifecycle.viewmodel.compose.viewModel


import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.embviewer.ui.screens.MainScreen
import com.example.embviewer.ui.viewmodel.ConverterViewModel


@Composable
fun EmbViewerApp() {
    val vm: ConverterViewModel = viewModel()
    MainScreen(viewModel = vm)
}