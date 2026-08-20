package com.eduk.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
                    EdukApp(startDestination = if (triggerQuestion) "question" else "localization")
                }
            }
        }
    }
}

@Composable
fun EdukApp(startDestination: String) {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = startDestination) {
        composable("localization") {
            LocalizationSelectionScreen(onComplete = { country, lang ->
                navController.navigate("role_selection")
            })
        }
        composable("role_selection") {
            RoleSelectionScreen(onRoleSelected = { role ->
                if (role == "parent") {
                    navController.navigate("auth")
                } else {
                    navController.navigate("onboarding/child")
                }
            })
        }
        composable("auth") {
            ProfessionalAuthScreen(onAuthSuccess = {
                navController.navigate("subscription")
            })
        }
        composable("subscription") {
            SubscriptionPaywallScreen(onSubscribed = {
                navController.navigate("onboarding/parent")
            })
        }
        composable("onboarding/{role}") { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "parent"
            OnboardingScreen(
                role = role,
                onComplete = {
                    if (role == "parent") {
                        navController.navigate("parent_dashboard")
                    } else {
                        navController.navigate("child_home")
                    }
                }
            )
        }
        composable("parent_dashboard") { 
            ParentDashboardScreen() 
        }
        composable("child_home") {
            ChildHomeScreen(onOpenScanner = { navController.navigate("scanner") })
        }
        composable("question") { 
            QuestionScreen(onCorrect = {
                navController.navigate("child_home") {
                    popUpTo("question") { inclusive = true }
                }
            }) 
        }
        composable("scanner") {
            BookScannerScreen(onScanComplete = { 
                navController.popBackStack()
            })
        }
    }
}
