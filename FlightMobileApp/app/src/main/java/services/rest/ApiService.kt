package services.rest

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import services.joystick.Command

interface ApiService {
    @GET("/screenshot")
    fun getScreenshot(): Call<ResponseBody>

    @POST("/api/command")
    fun postCommand(@Body command: Command): Call<ResponseBody>
}