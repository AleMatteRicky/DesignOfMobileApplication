package com.example.augmentedrealityglasses.weather.screen

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * Composable that allows to perform actions on scrolling down the content
 */
@Composable
fun SwipeDownRefresh(
    isRefreshing: Boolean,
    scrollDownEnabled: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            SwipeRefreshLayout(context).apply {
                val composeView = ComposeView(context).apply {
                    tag = "SwipeRefreshContent"
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setContent { content() }
                }
                addView(composeView)
                setOnRefreshListener { onRefresh() }
            }
        },
        update = { layout ->
            layout.setOnChildScrollUpCallback { _, _ -> scrollDownEnabled }
            layout.isRefreshing = isRefreshing
        }
    )
}