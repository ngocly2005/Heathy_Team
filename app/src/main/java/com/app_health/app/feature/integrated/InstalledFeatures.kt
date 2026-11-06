package com.app_health.app.feature.integrated

import androidx.compose.runtime.Composable

data class AppFeature(val route: String, val title: String,
    val load: suspend (AppViewModel, RequestContext) -> Unit = { _, _ -> },
    val screen: @Composable (AppViewModel, (String) -> Unit) -> Unit)

// Each task registers its screen here when that checkpoint is implemented.
fun installedFeatures(): List<AppFeature> = listOf(homeFeature(), profileFeature(), photoFeature(), foodFeature(), macroFeature(), activityFeature(), waterFeature(), mealsFeature(), weightFeature(), chatFeature(), diaryFeature(), calendarFeature())
