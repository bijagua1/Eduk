package com.eduk.app.cloud

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Path

data class EdukCloudErrorBody(val error: EdukCloudError? = null)
data class EdukCloudError(val code: String, val message: String)

class EdukCloudException(
    val statusCode: Int,
    val errorCode: String?,
    override val message: String
) : IllegalStateException(message)

object EdukCloudConfig {
    // Published production HTTPS endpoint for Eduk Family Cloud.
    const val BASE_URL = "https://edukcloud-cwj69xod.manus.space/"
}

data class ParentRegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
    val country: String,
    val language: String
)

data class ParentLoginRequest(val email: String, val password: String)
data class ParentFamily(val id: String, val email: String, val displayName: String)
data class ParentSessionResponse(val family: ParentFamily, val token: String, val expiresAt: String? = null)
data class TokenRefreshResponse(val token: String, val expiresAt: String? = null)

data class CreateChildRequest(
    val displayName: String,
    val username: String,
    val pin: String,
    val gradeLevel: Int,
    val dailyTimeLimitMinutes: Int
)

data class ChildCreatedResponse(val child: CloudChild)
data class CloudChild(
    val id: String,
    val displayName: String,
    val username: String,
    val gradeLevel: Int,
    val dailyTimeLimitMinutes: Int,
    val timeAvailableMinutes: Int = 0,
    val isBlockingEnabled: Boolean = true,
    val accuracyPercent: Int = 0,
    val questionsAnswered: Int = 0,
    val isDeviceLinked: Boolean = false,
    val linkedDeviceLabel: String? = null
)

data class DashboardResponse(val children: List<CloudChild>)
data class PairingCodeResponse(val childId: String, val code: String, val expiresAt: String)
data class StudentPairRequest(
    val username: String,
    val pin: String,
    val deviceId: String,
    val pairingCode: String,
    val deviceLabel: String
)
data class StudentLoginRequest(val username: String, val pin: String, val deviceId: String)
data class StudentProfile(val id: String, val displayName: String, val username: String)
data class StudentSessionResponse(val token: String, val child: StudentProfile, val expiresAt: String? = null)
data class StudentStateChild(
    val id: String,
    val displayName: String,
    val gradeLevel: Int,
    val timeAvailableMinutes: Int,
    val isBlockingEnabled: Boolean,
    val accuracyPercent: Int
)
data class StudentStateResponse(val child: StudentStateChild)
data class TimeAdjustmentRequest(val deltaMinutes: Int)
data class BlockingRequest(val isBlockingEnabled: Boolean)
data class TimeAdjustmentResponse(val childId: String, val timeAvailableMinutes: Int)
data class BlockingResponse(val childId: String, val isBlockingEnabled: Boolean)
data class LearningEventRequest(val questionText: String, val subject: String, val wasCorrect: Boolean)
data class LearningEventResponse(val minutesAwarded: Int, val timeAvailableMinutes: Int)
data class LearningHistoryEvent(
    val id: String,
    val questionText: String,
    val subject: String,
    val wasCorrect: Boolean,
    val minutesAwarded: Int,
    val answeredAt: String
)
data class LearningHistoryResponse(val events: List<LearningHistoryEvent>)
data class CloudControlPolicy(
    val childId: String,
    val revision: Int,
    val isBlockingEnabled: Boolean,
    val lockEntertainmentUntilLearning: Boolean,
    val dailyEarnedTimeCapMinutes: Int
)
data class CloudAppRule(
    val id: String,
    val packageName: String,
    val displayName: String? = null,
    val category: String? = null,
    val accessMode: String,
    val dailyLimitMinutes: Int? = null,
    val isEnabled: Boolean = true
)
data class CloudSchedule(
    val id: String,
    val name: String,
    val daysOfWeek: String,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val mode: String,
    val isEnabled: Boolean = true
)
data class CloudRewardRule(
    val id: String,
    val name: String,
    val subject: String? = null,
    val correctAnswerMinutes: Int,
    val completedChallengeMinutes: Int,
    val dailyMaxEarnedMinutes: Int,
    val minimumCorrectAnswers: Int,
    val isActive: Boolean = true
)
data class StudentPolicyResponse(
    val policy: CloudControlPolicy,
    val appRules: List<CloudAppRule>,
    val schedules: List<CloudSchedule>,
    val rewardRules: List<CloudRewardRule>
)
data class StudentChallengeQuestion(
    val id: String,
    val questionText: String,
    val choicesJson: String? = null,
    val subject: String,
    val topic: String,
    val difficulty: Int
)
data class ActiveStudentChallenge(
    val id: String,
    val title: String,
    val requiredCorrectAnswers: Int,
    val questions: List<StudentChallengeQuestion>
)
data class StudentChallengeResponse(
    val challenge: ActiveStudentChallenge? = null,
    val message: String? = null
)
data class ChallengeAttemptRequest(
    val challengeId: String,
    val questionId: String,
    val answer: String,
    val responseTimeSeconds: Int? = null
)
data class ChallengeAttemptResponse(
    val wasCorrect: Boolean,
    val minutesAwarded: Int,
    val timeAvailableMinutes: Int,
    val challengeCompleted: Boolean,
    val explanation: String? = null
)
data class SubjectLearningProgress(
    val subject: String,
    val attempts: Int,
    val correct: Int,
    val minutesEarned: Int,
    val accuracyPercent: Int
)
data class TopicLearningProgress(
    val subject: String,
    val topic: String,
    val attempts: Int,
    val correct: Int,
    val minutesEarned: Int,
    val accuracyPercent: Int
)
data class LearningProgressResponse(
    val totalAttempts: Int,
    val correctAttempts: Int,
    val accuracyPercent: Int,
    val minutesEarned: Int,
    val currentStreak: Int,
    val xp: Int,
    val completedChallenges: Int,
    val bySubject: List<SubjectLearningProgress>,
    val byTopic: List<TopicLearningProgress> = emptyList()
)
data class ParentQuestion(
    val id: String,
    val questionText: String,
    val choicesJson: String? = null,
    val correctAnswer: String,
    val explanation: String,
    val subject: String,
    val topic: String,
    val difficulty: Int,
    val reviewStatus: String,
    val validationStatus: String
)
data class ParentQuestionListResponse(val questions: List<ParentQuestion>)
data class ReviewQuestionRequest(val reviewStatus: String)
data class ParentPolicyResponse(
    val policy: CloudControlPolicy,
    val appRules: List<CloudAppRule>,
    val schedules: List<CloudSchedule>,
    val rewardRules: List<CloudRewardRule>
)
data class LearningPreferencesRequest(
    val subjects: List<String>,
    val difficulty: String,
    val goals: String? = null
)
data class LearningPreferencesResponse(
    val subjects: List<String>,
    val difficulty: String,
    val goals: String? = null
)
data class AppRuleRequest(
    val packageName: String,
    val displayName: String? = null,
    val category: String? = null,
    val accessMode: String,
    val dailyLimitMinutes: Int? = null
)
data class ScheduleRequest(
    val name: String,
    val daysOfWeek: List<Int>,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val mode: String
)
data class RewardRuleRequest(
    val name: String,
    val subject: String? = null,
    val correctAnswerMinutes: Int,
    val completedChallengeMinutes: Int = 0,
    val dailyMaxEarnedMinutes: Int,
    val minimumCorrectAnswers: Int = 1
)
data class StudyMaterialRequest(
    val sourceType: String,
    val displayName: String,
    val subject: String? = null,
    val imageBase64: String? = null,
    val imageMimeType: String? = null,
    val extractedText: String? = null,
    val questionCount: Int = 5
)
data class GeneratedCloudQuestion(
    val id: String,
    val questionText: String,
    val choicesJson: String?,
    val subject: String,
    val topic: String,
    val difficulty: Int,
    val reviewStatus: String,
    val validationStatus: String
)
data class StudyMaterialResponse(
    val sourceId: String,
    val processingStatus: String,
    val questions: List<GeneratedCloudQuestion>
)

interface EdukCloudService {
    @POST("api/mobile/v1/parents/register")
    suspend fun registerParent(@Body request: ParentRegisterRequest): Response<ParentSessionResponse>

    @POST("api/mobile/v1/parents/login")
    suspend fun loginParent(@Body request: ParentLoginRequest): Response<ParentSessionResponse>

    @POST("api/mobile/v1/parents/refresh")
    suspend fun refreshParentSession(@Header("Authorization") authorization: String): Response<TokenRefreshResponse>

    @GET("api/mobile/v1/parents/dashboard")
    suspend fun getDashboard(@Header("Authorization") authorization: String): Response<DashboardResponse>

    @POST("api/mobile/v1/children")
    suspend fun createChild(
        @Header("Authorization") authorization: String,
        @Body request: CreateChildRequest
    ): Response<ChildCreatedResponse>

    @POST("api/mobile/v1/children/{childId}/pairing-code")
    suspend fun createPairingCode(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String
    ): Response<PairingCodeResponse>

    @POST("api/mobile/v1/students/pair")
    suspend fun pairStudentDevice(@Body request: StudentPairRequest): Response<StudentSessionResponse>

    @POST("api/mobile/v1/students/login")
    suspend fun loginStudent(@Body request: StudentLoginRequest): Response<StudentSessionResponse>

    @POST("api/mobile/v1/students/refresh")
    suspend fun refreshStudentSession(@Header("Authorization") authorization: String): Response<TokenRefreshResponse>

    @GET("api/mobile/v1/students/state")
    suspend fun getStudentState(@Header("Authorization") authorization: String): Response<StudentStateResponse>

    @GET("api/mobile/v1/students/policy")
    suspend fun getStudentPolicy(@Header("Authorization") authorization: String): Response<StudentPolicyResponse>

    @GET("api/mobile/v1/students/challenge")
    suspend fun getStudentChallenge(@Header("Authorization") authorization: String): Response<StudentChallengeResponse>

    @GET("api/mobile/v1/students/learning-progress")
    suspend fun getStudentLearningProgress(@Header("Authorization") authorization: String): Response<LearningProgressResponse>

    @POST("api/mobile/v1/students/challenge-attempts")
    suspend fun submitChallengeAttempt(
        @Header("Authorization") authorization: String,
        @Body request: ChallengeAttemptRequest
    ): Response<ChallengeAttemptResponse>

    @GET("api/mobile/v1/children/{childId}/questions")
    suspend fun getParentQuestions(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String
    ): Response<ParentQuestionListResponse>

    @GET("api/mobile/v1/children/{childId}/learning-analytics")
    suspend fun getParentLearningProgress(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String
    ): Response<LearningProgressResponse>

    @PATCH("api/mobile/v1/children/{childId}/questions/{questionId}/review")
    suspend fun reviewParentQuestion(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String,
        @Path("questionId") questionId: String,
        @Body request: ReviewQuestionRequest
    ): Response<Any>

    @GET("api/mobile/v1/children/{childId}/policy")
    suspend fun getParentPolicy(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String
    ): Response<ParentPolicyResponse>

    @GET("api/mobile/v1/children/{childId}/learning-preferences")
    suspend fun getLearningPreferences(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String
    ): Response<LearningPreferencesResponse>

    @PUT("api/mobile/v1/children/{childId}/learning-preferences")
    suspend fun saveLearningPreferences(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String,
        @Body request: LearningPreferencesRequest
    ): Response<LearningPreferencesResponse>

    @POST("api/mobile/v1/children/{childId}/app-rules")
    suspend fun saveAppRule(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String,
        @Body request: AppRuleRequest
    ): Response<Any>

    @POST("api/mobile/v1/children/{childId}/schedules")
    suspend fun createSchedule(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String,
        @Body request: ScheduleRequest
    ): Response<Any>

    @POST("api/mobile/v1/children/{childId}/reward-rules")
    suspend fun createRewardRule(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String,
        @Body request: RewardRuleRequest
    ): Response<Any>

    @POST("api/mobile/v1/children/{childId}/study-materials")
    suspend fun submitStudyMaterial(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String,
        @Body request: StudyMaterialRequest
    ): Response<StudyMaterialResponse>

    @PATCH("api/mobile/v1/children/{childId}/time")
    suspend fun updateTime(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String,
        @Body request: TimeAdjustmentRequest
    ): Response<TimeAdjustmentResponse>

    @PATCH("api/mobile/v1/children/{childId}/blocking")
    suspend fun updateBlocking(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String,
        @Body request: BlockingRequest
    ): Response<BlockingResponse>

    @GET("api/mobile/v1/children/{childId}/history")
    suspend fun getLearningHistory(
        @Header("Authorization") authorization: String,
        @Path("childId") childId: String
    ): Response<LearningHistoryResponse>

    @POST("api/mobile/v1/students/learning-events")
    suspend fun recordLearningEvent(
        @Header("Authorization") authorization: String,
        @Body request: LearningEventRequest
    ): Response<LearningEventResponse>
}

object EdukCloudRepository {
    private val service: EdukCloudService by lazy {
        Retrofit.Builder()
            .baseUrl(EdukCloudConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EdukCloudService::class.java)
    }

    private fun bearer(token: String) = "Bearer $token"

    private suspend fun <T> body(response: Response<T>): T {
        if (response.isSuccessful && response.body() != null) return response.body()!!
        val rawError = response.errorBody()?.string().orEmpty()
        val parsed = runCatching {
            com.google.gson.Gson().fromJson(rawError, EdukCloudErrorBody::class.java)
        }.getOrNull()?.error
        throw EdukCloudException(
            statusCode = response.code(),
            errorCode = parsed?.code,
            message = parsed?.message ?: "Eduk Family Cloud returned an unexpected error."
        )
    }

    suspend fun registerParent(request: ParentRegisterRequest) = body(service.registerParent(request))
    suspend fun loginParent(request: ParentLoginRequest) = body(service.loginParent(request))
    suspend fun refreshParentSession(parentToken: String) = body(service.refreshParentSession(bearer(parentToken)))
    suspend fun getDashboard(parentToken: String) = body(service.getDashboard(bearer(parentToken)))
    suspend fun createChild(parentToken: String, request: CreateChildRequest) = body(service.createChild(bearer(parentToken), request))
    suspend fun createPairingCode(parentToken: String, childId: String) = body(service.createPairingCode(bearer(parentToken), childId))
    suspend fun pairStudentDevice(request: StudentPairRequest) = body(service.pairStudentDevice(request))
    suspend fun loginStudent(request: StudentLoginRequest) = body(service.loginStudent(request))
    suspend fun refreshStudentSession(studentToken: String) = body(service.refreshStudentSession(bearer(studentToken)))
    suspend fun getStudentState(studentToken: String) = body(service.getStudentState(bearer(studentToken)))
    suspend fun getStudentPolicy(studentToken: String) = body(service.getStudentPolicy(bearer(studentToken)))
    suspend fun getStudentChallenge(studentToken: String) = body(service.getStudentChallenge(bearer(studentToken)))
    suspend fun getStudentLearningProgress(studentToken: String) = body(service.getStudentLearningProgress(bearer(studentToken)))
    suspend fun submitChallengeAttempt(studentToken: String, request: ChallengeAttemptRequest) =
        body(service.submitChallengeAttempt(bearer(studentToken), request))
    suspend fun getParentQuestions(parentToken: String, childId: String) = body(service.getParentQuestions(bearer(parentToken), childId))
    suspend fun getParentLearningProgress(parentToken: String, childId: String) = body(service.getParentLearningProgress(bearer(parentToken), childId))
    suspend fun reviewParentQuestion(parentToken: String, childId: String, questionId: String, reviewStatus: String) =
        body(service.reviewParentQuestion(bearer(parentToken), childId, questionId, ReviewQuestionRequest(reviewStatus)))
    suspend fun getParentPolicy(parentToken: String, childId: String) = body(service.getParentPolicy(bearer(parentToken), childId))
    suspend fun getLearningPreferences(parentToken: String, childId: String) = body(service.getLearningPreferences(bearer(parentToken), childId))
    suspend fun saveLearningPreferences(parentToken: String, childId: String, request: LearningPreferencesRequest) =
        body(service.saveLearningPreferences(bearer(parentToken), childId, request))
    suspend fun saveAppRule(parentToken: String, childId: String, request: AppRuleRequest) = body(service.saveAppRule(bearer(parentToken), childId, request))
    suspend fun createSchedule(parentToken: String, childId: String, request: ScheduleRequest) = body(service.createSchedule(bearer(parentToken), childId, request))
    suspend fun createRewardRule(parentToken: String, childId: String, request: RewardRuleRequest) = body(service.createRewardRule(bearer(parentToken), childId, request))
    suspend fun submitStudyMaterial(studentToken: String, childId: String, request: StudyMaterialRequest) =
        body(service.submitStudyMaterial(bearer(studentToken), childId, request))
    suspend fun updateTime(parentToken: String, childId: String, deltaMinutes: Int) = body(service.updateTime(bearer(parentToken), childId, TimeAdjustmentRequest(deltaMinutes)))
    suspend fun updateBlocking(parentToken: String, childId: String, isBlockingEnabled: Boolean) = body(service.updateBlocking(bearer(parentToken), childId, BlockingRequest(isBlockingEnabled)))
    suspend fun getLearningHistory(parentToken: String, childId: String) = body(service.getLearningHistory(bearer(parentToken), childId))
    suspend fun recordLearningEvent(studentToken: String, request: LearningEventRequest) = body(service.recordLearningEvent(bearer(studentToken), request))
}
