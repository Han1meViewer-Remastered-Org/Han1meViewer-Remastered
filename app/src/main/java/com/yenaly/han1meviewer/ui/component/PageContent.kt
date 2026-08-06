package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yenaly.han1meviewer.logic.state.PageState
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.content.ErrorContent
import com.yenaly.han1meviewer.ui.component.content.LoadingContent

@Composable
fun PageContent(
    isLoading: Boolean,
    isError: Boolean,
    isEmpty: Boolean,
    modifier: Modifier = Modifier,
    loadingMessage: String = "",
    errorMessage: String = "",
    onRetry: () -> Unit = {},
    loading: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (loadingMessage.isBlank()) LoadingContent() else LoadingContent(message = loadingMessage)
        }
    },
    error: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ErrorContent(message = errorMessage, onRetry = onRetry)
        }
    },
    empty: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyContent(hint = "")
        }
    },
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        when {
            isError -> error()
            isLoading -> loading()
            isEmpty -> empty()
            else -> content()
        }
    }
}

val <T> PageState<T>.isFirstPageLoading: Boolean
    get() = this is PageState.Loading

val <T> PageState<T>.isFirstPageError: Boolean
    get() = this is PageState.Error && cachedInfo == null

val <T> PageState<T>.isFirstPageEmpty: Boolean
    get() = this is PageState.Empty
