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
    // Verified HTTPS endpoint for the current Eduk Family Cloud instance.
    const val BASE_URL = "https://3000-i9ngt61dg0xvs3rgnu1x1-2fc2b4ae.us4.manus.computer/"
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
data class ParentSessionResponse(val family: ParentFamily, val token: String)

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
data class StudentSessionResponse(val token: String, val child: StudentProfile)
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

interface EdukCloudService {
    @POST("api/mobile/v1/parents/register")
    suspend fun registerParent(@Body request: ParentRegisterRequest): Response<ParentSessionResponse>

    @POST("api/mobile/v1/parents/login")
    suspend fun loginParent(@Body request: ParentLoginRequest): Response<ParentSessionResponse>

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

    @GET("api/mobile/v1/students/state")
    suspend fun getStudentState(@Header("Authorization") authorization: String): Response<StudentStateResponse>

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
    suspend fun getDashboard(parentToken: String) = body(service.getDashboard(bearer(parentToken)))
    suspend fun createChild(parentToken: String, request: CreateChildRequest) = body(service.createChild(bearer(parentToken), request))
    suspend fun createPairingCode(parentToken: String, childId: String) = body(service.createPairingCode(bearer(parentToken), childId))
    suspend fun pairStudentDevice(request: StudentPairRequest) = body(service.pairStudentDevice(request))
    suspend fun loginStudent(request: StudentLoginRequest) = body(service.loginStudent(request))
    suspend fun getStudentState(studentToken: String) = body(service.getStudentState(bearer(studentToken)))
    suspend fun updateTime(parentToken: String, childId: String, deltaMinutes: Int) = body(service.updateTime(bearer(parentToken), childId, TimeAdjustmentRequest(deltaMinutes)))
    suspend fun updateBlocking(parentToken: String, childId: String, isBlockingEnabled: Boolean) = body(service.updateBlocking(bearer(parentToken), childId, BlockingRequest(isBlockingEnabled)))
    suspend fun getLearningHistory(parentToken: String, childId: String) = body(service.getLearningHistory(bearer(parentToken), childId))
    suspend fun recordLearningEvent(studentToken: String, request: LearningEventRequest) = body(service.recordLearningEvent(bearer(studentToken), request))
}
