package com.ootd.today.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ootd.today.R
import com.ootd.today.ui.common.*

@Composable
fun OotdBottomBar(currentRoute: String?, onTabSelected: (BottomDestination) -> Unit, onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Surface(
        modifier = modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=18.dp, vertical=10.dp).shadow(10.dp,shape, ambientColor=StudioInk.copy(alpha=.1f), spotColor=StudioInk.copy(alpha=.1f)),
        shape=shape, color=MaterialTheme.colorScheme.surface
    ) {
        Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            BottomDestination.entries.forEach { dest ->
                if(dest.isCenterAction) {
                    Box(Modifier.weight(1f),contentAlignment=Alignment.Center) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(StudioInk).clickable(onClick=onAddClick),contentAlignment=Alignment.Center) {
                            StudioIcon(StudioSymbol.ADD,color=StudioLime,description=stringResource(R.string.nav_add_item_desc))
                        }
                    }
                } else {
                    val selected = currentRoute == dest.route
                    val color by animateColorAsState(if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,label="navigation")
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).clickable { onTabSelected(dest) }.padding(vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        StudioIcon(when(dest) { BottomDestination.TODAY -> StudioSymbol.TODAY; BottomDestination.WARDROBE -> StudioSymbol.WARDROBE; BottomDestination.CALENDAR -> StudioSymbol.CALENDAR; else -> StudioSymbol.PERSON },color=color)
                        Text(stringResource(dest.labelRes),style=MaterialTheme.typography.labelSmall,color=color,fontWeight=if(selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}
