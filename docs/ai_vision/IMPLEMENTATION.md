# AI Vision Implementation: Book-to-Question Engine

The "Book Scan" feature is the core differentiator of Eduk. This document outlines the technical flow for converting a picture of a textbook into interactive questions.

## Technical Workflow

1.  **Image Capture**: The student or parent captures a high-resolution photo or short video of the study material using the Android Camera2 API.
2.  **Pre-processing**: The app performs local cropping, perspective correction, and noise reduction.
3.  **Cloud Analysis (Multi-modal AI)**:
    -   The image is sent to an AI Vision API (e.g., GPT-4o or Gemini 1.5 Pro).
    -   **Prompt Engineering**: The system uses a specialized prompt to extract key concepts, definitions, and facts.
4.  **Question Generation**:
    -   The AI generates 5-10 multiple-choice questions based on the extracted text.
    -   Each question includes an explanation for the correct answer.
5.  **Local Storage**: Questions are saved to the Room database for offline use during the "Unlock" phase.

## Data Schema for AI Questions

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | UUID | Unique identifier for the question |
| `source_material` | String | Reference to the book/page name |
| `content` | String | The actual question text |
| `options` | List<String> | Multiple choice options |
| `correct_index` | Integer | Index of the correct answer |
| `explanation` | String | Brief educational context for the answer |
| `difficulty` | Enum | Auto-assigned based on text complexity |

## AI Agent Integration
AI Agents (like Copilot) should refer to `com.eduk.app.ai.QuestionGenerator` for the implementation of the API calls. We recommend using a repository pattern to swap between different AI providers (OpenAI, Google, or local TFLite models).
