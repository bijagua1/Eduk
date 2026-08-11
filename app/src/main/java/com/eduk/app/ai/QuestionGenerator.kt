package com.eduk.app.ai

import com.eduk.app.model.Question

/**
 * Interface for the AI-powered question generation system.
 * This is designed to be "Agent-Friendly" and supports both 
 * local mock data and remote Vision AI providers.
 */
interface QuestionGenerator {
    
    /**
     * Generates a list of questions from an image or video frame.
     * @param mediaPath Local path to the captured image/video
     * @param context Additional context (e.g., grade level, subject)
     */
    suspend fun generateFromMedia(mediaPath: String, context: GenerationContext): List<Question>
}

data class GenerationContext(
    val gradeLevel: Int,
    val targetSubject: String?,
    val preferredLanguage: String = "English"
)

/**
 * Mock implementation for initial MVP testing.
 */
class MockQuestionGenerator : QuestionGenerator {
    override suspend fun generateFromMedia(mediaPath: String, context: GenerationContext): List<Question> {
        // Simulate AI processing delay
        kotlinx.coroutines.delay(2000)
        
        return listOf(
            Question(
                subject = context.targetSubject ?: "General Science",
                gradeLevel = context.gradeLevel,
                difficulty = "Medium",
                questionText = "Based on the text in the image, what is the primary cause of photosynthesis?",
                options = listOf("Oxygen", "Sunlight", "Nitrogen", "Soil"),
                correctOptionIndex = 1,
                explanation = "Photosynthesis requires sunlight as the primary energy source.",
                sourceMaterial = "Book Scan"
            )
        )
    }
}
