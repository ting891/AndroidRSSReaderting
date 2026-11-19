package com.ting.rssreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ting.rssreader.ui.detail.ArticleDetailScreen
import com.ting.rssreader.ui.detail.DetailViewModel
import com.ting.rssreader.ui.feed.FeedListScreen
import com.ting.rssreader.ui.feed.FeedListViewModel
import com.ting.rssreader.ui.sources.SourcesScreen
import com.ting.rssreader.ui.sources.SourcesViewModel
import com.ting.rssreader.ui.theme.RssReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RssReaderTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "list") {
                    composable("list") {
                        val listViewModel: FeedListViewModel = viewModel()
                        FeedListScreen(
                            viewModel = listViewModel,
                            onOpenArticle = { id -> navController.navigate("detail/$id") },
                            onManageSources = { navController.navigate("sources") }
                        )
                    }
                    composable(
                        route = "detail/{articleId}",
                        arguments = listOf(navArgument("articleId") { type = NavType.LongType })
                    ) {
                        val detailViewModel: DetailViewModel = viewModel()
                        ArticleDetailScreen(
                            viewModel = detailViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("sources") {
                        val sourcesViewModel: SourcesViewModel = viewModel()
                        SourcesScreen(
                            viewModel = sourcesViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
