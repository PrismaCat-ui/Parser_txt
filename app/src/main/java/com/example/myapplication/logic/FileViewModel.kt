package com.example.myapplication.logic

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface FileUiState {
    data object Idle : FileUiState
    data object Loading : FileUiState
    data class Success(val text: String, val fileName: String) : FileUiState
    data class Error(val message: String) : FileUiState
}

class FileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<FileUiState>(FileUiState.Idle)
    val uiState: StateFlow<FileUiState> = _uiState

    fun loadFile(context: Context, uri: Uri) {
        _uiState.value = FileUiState.Loading
        viewModelScope.launch {
            try {
                val fileName = getFileName(context, uri)
                val text = withContext(Dispatchers.IO) {
                    FileParser.readFile(context, uri, fileName)
                }
                _uiState.value = FileUiState.Success(text, fileName)
            } catch (e: Exception) {
                _uiState.value = FileUiState.Error(e.message ?: "Неизвестная ошибка при чтении файла")
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "Неизвестный файл.txt"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}