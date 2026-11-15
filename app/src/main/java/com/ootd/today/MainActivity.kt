package com.ootd.today

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ootd.today.ui.navigation.OotdNavHost
import com.ootd.today.ui.theme.OotdTodayTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 唯一的 Activity。
 *
 * 全部界面由 Compose 承载，[OotdNavHost] 负责页面路由与底部导航，
 * 因此这里只需套一层主题与背景 Surface。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OotdTodayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OotdNavHost()
                }
            }
        }
    }
}
