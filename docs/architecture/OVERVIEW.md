# Technical Architecture Overview

**Eduk** follows the principles of **Clean Architecture** combined with the **MVVM (Model-View-ViewModel)** pattern to ensure scalability, testability, and a "Magnifique" user experience.

## Layered Architecture

### 1. Presentation Layer (UI)
- **Framework**: Jetpack Compose (Material 3).
- **Components**: 
    - `MainActivity`: Entry point and navigation host.
    - `QuestionScreen`: The student-facing gatekeeper.
    - `ParentDashboard`: Management and analytics for the account manager.
- **State Management**: ViewModels use `StateFlow` to provide a reactive UI state.

### 2. Domain Layer (Use Cases)
- Contains the business logic of the application.
- **Key Use Cases**:
    - `EvaluateAnswerUseCase`: Checks if the student's response is correct.
    - `UnlockAppsUseCase`: Interfaces with the Monitoring Service to grant time.
    - `GenerateAIQuestionsUseCase`: Orchestrates the AI Vision workflow.

### 3. Data Layer (Repository & Sources)
- **Room Database**: Local persistence for questions, profiles, and stats.
- **AI Question Engine**: Bridge to OpenAI/Gemini Vision APIs.
- **Parent Reporting**: Secure networking to sync data with the manager's remote account.

## App Blocking Logic
Eduk uses a hybrid approach for legitimate app management:
1.  **Accessibility Service**: Monitors foreground app changes.
2.  **Device Policy Manager**: Provides the infrastructure for a "Device Owner" mode, allowing more strict enforcement in future versions.
3.  **UsageStatsManager**: Tracks time spent in restricted applications to ensure accuracy in reporting.

## AI Vision Pipeline
Refer to `docs/ai_vision/IMPLEMENTATION.md` for the detailed flow of how images are converted into academic questions.
