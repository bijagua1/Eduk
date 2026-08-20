package com.eduk.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eduk.app.ui.theme.EdukTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val triggerQuestion = intent.getBooleanExtra("TRIGGER_QUESTION", false)

        setContent {
            EdukTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EdukApp(startDestination = if (triggerQuestion) "question" else "onboarding")
                }
            }
        }
    }
}

@Composable
fun EdukApp(startDestination: String) {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") { 
            OnboardingScreen(onComplete = { navController.navigate("parent_dashboard") }) 
        }
        composable("parent_dashboard") { 
            ParentDashboard() 
        }
        composable("question") { 
            QuestionScreen() 
        }
    }
}
