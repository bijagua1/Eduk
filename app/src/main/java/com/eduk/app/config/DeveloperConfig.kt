package com.eduk.app.config

object DeveloperConfig {
    // Set to true to bypass paywall and authentication during development
    var isDeveloperMode = true
    
    // Default subscription state
    var isSubscribed = false
        get() = if (isDeveloperMode) true else field
}
