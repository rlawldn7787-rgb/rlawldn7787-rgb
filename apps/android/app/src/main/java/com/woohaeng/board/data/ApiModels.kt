package com.woohaeng.board.data

import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.QueryMap
import retrofit2.http.Streaming

@JsonClass(generateAdapter = true)
data class LoginRequest(val username: String, val password: String)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: Int,
    val username: String,
    val name: String,
    val role: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(val token: String, val user: UserDto)

@JsonClass(generateAdapter = true)
data class MeResponse(val user: UserDto)

@JsonClass(generateAdapter = true)
data class RecordDto(
    val id: Int,
    val userId: Int,
    val workName: String,
    val workType: String,
    val location: String,
    val content: String,
    val workDate: String,
    val photoUrl: String,
    val photoThumbUrl: String?,
    val authorName: String,
    val authorUsername: String,
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class RecordsResponse(val records: List<RecordDto>)

@JsonClass(generateAdapter = true)
data class RecordResponse(val record: RecordDto)

@JsonClass(generateAdapter = true)
data class ErrorResponse(val error: String?)

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("auth/me")
    suspend fun me(@Header("Authorization") auth: String): MeResponse

    @GET("records")
    suspend fun records(
        @Header("Authorization") auth: String,
        @QueryMap params: Map<String, String>
    ): RecordsResponse

    @GET("records/{id}")
    suspend fun record(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): RecordResponse

    @Multipart
    @POST("records")
    suspend fun createRecord(
        @Header("Authorization") auth: String,
        @Part("workName") workName: RequestBody,
        @Part("workType") workType: RequestBody,
        @Part("location") location: RequestBody,
        @Part("content") content: RequestBody,
        @Part("workDate") workDate: RequestBody,
        @Part image: MultipartBody.Part
    ): RecordResponse

    @Streaming
    @GET("records/export.xlsx")
    suspend fun exportExcel(
        @Header("Authorization") auth: String,
        @QueryMap params: Map<String, String>
    ): ResponseBody
}
