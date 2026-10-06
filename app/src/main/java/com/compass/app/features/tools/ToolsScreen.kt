// SPDX-License-Identifier: GPL-3.0-or-later

package com.compass.app.features.tools

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.compass.app.R
import com.compass.app.features.tools.level.LevelScreen
import com.compass.app.features.tools.ruler.RulerScreen
import com.compass.app.ui.components.LocalBottomBarInset

/**
 * The tools that share the Tools tab. Long-pressing the tab in the bottom bar switches between
 * them, and the tab shows the name and icon of the one on screen.
 */
enum class Tool(@param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
    LEVEL(R.string.level, R.drawable.ic_level),
    RULER(R.string.ruler, R.drawable.ic_ruler);

    fun next(): Tool = entries[(ordinal + 1) % entries.size]
}

@Composable
fun ToolsScreen(tool: Tool, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(bottom = LocalBottomBarInset.current)
    ) {
        when (tool) {
            Tool.LEVEL -> LevelScreen(modifier = Modifier.weight(1f))
            Tool.RULER -> RulerScreen(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp)
            )
        }
    }
}
