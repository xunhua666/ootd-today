package com.ootd.today.ui.wardrobe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ootd.today.data.model.*
import com.ootd.today.domain.WardrobeSort
import com.ootd.today.ui.common.*
import com.ootd.today.ui.theme.PillShape

@Composable
fun WardrobeScreen(viewModel: WardrobeViewModel, onItemClick: (Long) -> Unit, modifier: Modifier = Modifier, onAddItem: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    var sortMenu by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    LaunchedEffect(state.query, state.selectedCategory, state.sort, state.idleOnly) {
        gridState.scrollToItem(0)
    }
    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal=22.dp)) {
            EditorialHeader("THE WARDROBE", "我的衣橱", "把喜欢的衣服，穿进日常。") {
                Box(Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center) { StudioIcon(StudioSymbol.WARDROBE,color=MaterialTheme.colorScheme.primary) }
            }
            if (state.isDemo) {
                DemoWardrobeBanner(onStart = { viewModel.startPersonalWardrobe(onAddItem) })
                Spacer(Modifier.height(14.dp))
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                WardrobeStat(state.allCount,if(state.isDemo) "件体验单品" else "件在用单品")
                Box(Modifier.width(1.dp).height(38.dp).background(MaterialTheme.colorScheme.outlineVariant))
                WardrobeStat(state.idleCount,"件待唤醒")
                Box(Modifier.width(1.dp).height(38.dp).background(MaterialTheme.colorScheme.outlineVariant))
                WardrobeStat(state.items.size,"件筛选结果")
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value=state.query, onValueChange=viewModel::search, modifier=Modifier.fillMaxWidth(),
                placeholder={ Text("搜索名称、品牌、颜色",style=MaterialTheme.typography.bodyMedium) },
                leadingIcon={ StudioIcon(StudioSymbol.SEARCH,color=MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon=if(state.query.isNotEmpty()) { { IconButton(onClick={ viewModel.search("") }) { StudioIcon(StudioSymbol.CLOSE,description="清空搜索") } } } else null,
                singleLine=true,shape=RoundedCornerShape(18.dp),textStyle=MaterialTheme.typography.bodyMedium,
                keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={focus.clearFocus()}),
                colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=MaterialTheme.colorScheme.outlineVariant,unfocusedContainerColor=MaterialTheme.colorScheme.surface,focusedContainerColor=MaterialTheme.colorScheme.surface)
            )
            Spacer(Modifier.height(8.dp))
        }
        LazyRow(contentPadding=PaddingValues(horizontal=22.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            item { CategoryChip("全部",state.selectedCategory==null) { viewModel.selectCategory(null) } }
            items(ClothingCategory.entries.size) { index ->
                val cat=ClothingCategory.entries[index]
                CategoryChip(cat.label,state.selectedCategory==cat) { viewModel.selectCategory(cat) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            FilterChip(selected=state.idleOnly,onClick=viewModel::toggleIdle,label={Text("唤醒闲置 · 30 天未穿",fontSize=11.sp)},shape=PillShape,
                leadingIcon=if(state.idleOnly) { { StudioIcon(StudioSymbol.CHECK,Modifier.size(15.dp)) } } else null)
            Box {
                TextButton(onClick={sortMenu=true},contentPadding=PaddingValues(horizontal=6.dp)) {
                    StudioIcon(StudioSymbol.SORT,Modifier.size(17.dp),color=MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp));Text(state.sort.label,style=MaterialTheme.typography.labelSmall)
                }
                DropdownMenu(expanded=sortMenu,onDismissRequest={sortMenu=false}) {
                    WardrobeSort.entries.forEach { sort -> DropdownMenuItem(text={Text(sort.label)},onClick={viewModel.sortBy(sort);sortMenu=false},trailingIcon=if(state.sort==sort) { { StudioIcon(StudioSymbol.CHECK) } } else null) }
                }
            }
        }
        if(state.isLoading) {
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { CircularProgressIndicator() }
        } else if(state.items.isEmpty()) {
            Column(Modifier.fillMaxWidth().weight(1f).padding(bottom=110.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                StudioIcon(StudioSymbol.WARDROBE,Modifier.size(48.dp),color=MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text(if(state.allCount==0) "给衣橱留一点新灵感" else "没有找到符合条件的单品",style=MaterialTheme.typography.titleMedium)
                Text(if(state.allCount==0) "点击下方 +，录入你的第一件衣服" else "试试其他关键词，或清除筛选条件",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=8.dp))
                if(state.allCount>0) TextButton(onClick=viewModel::resetFilters) {Text("清除筛选")}
            }
        } else {
            LazyVerticalGrid(state=gridState,columns=GridCells.Adaptive(145.dp),modifier=Modifier.weight(1f),contentPadding=PaddingValues(start=22.dp,end=22.dp,bottom=124.dp,top=4.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                items(state.items,key={it.id}) { item -> WardrobeCard(item) { focus.clearFocus();onItemClick(item.id) } }
            }
        }
    }
}

@Composable private fun WardrobeStat(value:Int,label:String) {
    Column(horizontalAlignment=Alignment.CenterHorizontally) {
        Text(value.toString().padStart(2,'0'),fontSize=23.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary)
        Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun CategoryChip(label:String,selected:Boolean,onClick:()->Unit) {
    FilterChip(selected=selected,onClick=onClick,label={Text(label,style=MaterialTheme.typography.labelMedium)},shape=PillShape,
        colors=FilterChipDefaults.filterChipColors(selectedContainerColor=MaterialTheme.colorScheme.primary,selectedLabelColor=MaterialTheme.colorScheme.onPrimary),border=FilterChipDefaults.filterChipBorder(enabled=true,selected=selected,borderColor=MaterialTheme.colorScheme.outlineVariant))
}

@Composable private fun WardrobeCard(item:WardrobeItem,onClick:()->Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick=onClick).padding(9.dp)) {
        Box {
            ItemSwatch(item,Modifier.fillMaxWidth().aspectRatio(.92f),shape=RoundedCornerShape(14.dp),showBorder=false)
            Text(item.category.label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(8.dp).clip(PillShape).background(MaterialTheme.colorScheme.surface.copy(alpha=.9f)).padding(horizontal=8.dp,vertical=4.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(item.name,style=MaterialTheme.typography.titleSmall,maxLines=1,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurface)
        Row(Modifier.fillMaxWidth().padding(top=7.dp,bottom=5.dp),verticalAlignment=Alignment.CenterVertically) {
            ColorDot(item.colorArgb,size=9.dp)
            Spacer(Modifier.width(5.dp))
            Text(item.colorName,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
            Text("穿过 ${item.wearCount} 次",fontSize=10.sp,color=MaterialTheme.colorScheme.primary)
        }
    }
}
