package services.joystick

import com.google.gson.annotations.SerializedName

data class Command(
    @SerializedName("aileron") val Aileron: Float,
    @SerializedName("elevator") val Elevator: Float,
    @SerializedName("throttle") val Throttle: Float,
    @SerializedName("rudder") val Rudder: Float
)