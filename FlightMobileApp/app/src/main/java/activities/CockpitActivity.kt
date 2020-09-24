package activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.flightmobileapp.R
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_cockpit.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import services.joystick.Command
import services.joystick.JoystickListener
import services.rest.ApiService
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import kotlin.math.abs

class CockpitActivity : AppCompatActivity(), JoystickListener,
    SeekBar.OnSeekBarChangeListener {
    private var ioRouting: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private var aileron: Float = 0F
    private var elevator: Float = 0F
    private var rudderVal: Float = 0f
    private var throttleVal: Float = 0f
    private lateinit var screenshotApi: ApiService
    private lateinit var commandApi: ApiService

    @Volatile
    private var toastOn: Boolean = false

    @Volatile
    private var routingOn: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cockpit)
        rudder.setOnSeekBarChangeListener(this)
        throttle.setOnSeekBarChangeListener(this)
        // initialize REST api service
       initApis()
    }

    private fun initApis() {
        try {
            screenshotApi = Retrofit.Builder()
                .baseUrl(intent.getStringExtra("url")!!)
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder()
                            .setLenient()
                            .create()
                    )
                )
                .build().create(ApiService::class.java)
            commandApi = Retrofit.Builder()
                .baseUrl(intent.getStringExtra("url")!!)
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder()
                            .setLenient()
                            .create()
                    )
                )
                .build().create(ApiService::class.java)
        } catch (e: Exception) {
            tryToToast("Unable to initialize api - bad format")
        }
    }

    // on start - open a screenshot requests loop, and set 'toastOn' bool to false
    override fun onStart() {
        super.onStart()
        if (!routingOn)
            openScreenshotRouting()
        toastOn = false
    }

    // on resume - open a screenshot requests loop, and set 'toastOn' bool to false
    override fun onResume() {
        super.onResume()
        if (!routingOn)
            openScreenshotRouting()
    }

    // on pause - close a screenshot requests loop, and set 'toastOn' bool to true
    // (to unable screenshot request)
    override fun onPause() {
        super.onPause()
        if (routingOn)
            routingOn = false
        toastOn = true
    }

    // on destroy - close a screenshot requests loop, and set 'toastOn' bool to true
    // (to unable screenshot request)
    override fun onDestroy() {
        super.onDestroy()
        if (routingOn)
            routingOn = false
        toastOn = true
    }

    // try to pop up a toast message
    private fun tryToToast(msg: String) {
        if (!toastOn) {
            toastOn = true
            CoroutineScope(Default).launch {
                // after 3 sec, turn of 'toastOn'
                delay(3000)
                toastOn = false
            }
            runOnUiThread {
                Toast.makeText(
                    this@CockpitActivity,
                    msg,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ask the server for a screenshot every 1 sec
    private fun openScreenshotRouting() {
        routingOn = true
        ioRouting.launch {
            while (routingOn) {
                tryGetScreenshot()
                delay(1000)
            }
        }
    }

    // send a screenshot request to the server
    private fun tryGetScreenshot() {
        screenshotApi.getScreenshot().enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    // if success - set the screenshot into imageView
                    val inputstream = response.body()?.byteStream()
                    val bitmap = BitmapFactory.decodeStream(inputstream)
                    setImg(bitmap)
                } else {
                    // if failed - toast a fail message
                    tryToToast(
                        "server: " + getErrorBody(response) + "\ntry to reconnect"
                    )
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                tryToToast("failed to reach server\ntry to reconnect")
            }
        })
    }

    // set the screenshot into imageView
    private fun setImg(bitmap: Bitmap?) {
        runOnUiThread {
            imageView.setImageBitmap(bitmap)
        }
    }

    /*
    The joystick notify when there was a change with his values.
    and the function choose to save the changes and update the simulator
    if the changes were bigger the 1 percent from the range of the joystick
    values.
     */
    override fun onJoystickMoved(
        xPosition: Float, yPosition:
        Float, range: Float
    ) {
        val changeOnAileron = abs(xPosition - aileron) / range * 100
        val changeOnElevator = abs(yPosition - elevator) / range * 100
        var str = "aileron: " + String.format("%.2f", xPosition)
        aileronContent.text = str
        str = "elevator: " + String.format("%.2f", yPosition)
        elevatorContent.text = str
        var sendCommand = false
        //if the changes was bigger then 1 % save the new values
        // and update the simulator.
        if (changeOnAileron > 1) {
            aileron = xPosition
            sendCommand = true
        }
        if (changeOnElevator > 1) {
            elevator = yPosition
            sendCommand = true
        }
        if (sendCommand) {
            ioRouting.launch {
                sendCommandToSimulator()
            }
        }
    }

    //separate the event to two different events one for each seekBar.
    override fun onProgressChanged(
        seekBar: SeekBar?,
        progress: Int, fromUser: Boolean
    ) {
        if (seekBar == rudder) {
            setRudderVal(seekBar!!.progress.toFloat())
        } else {
            setThrottleVal(seekBar!!.progress.toFloat())
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {

    }


    /*
     calculate the rudder and if there was a change bigger then
     1 percent the save the new value and update the simulator.
     */
    private fun setRudderVal(value: Float) {
        val currentRudderValue: Float = if (value == 100F) {
            0F
        } else {
            (value - 100) / 100
        }
        val changeOnRudder = abs(currentRudderValue - rudderVal) / 2 * 100
        if (changeOnRudder > 1) {
            rudderVal = currentRudderValue
            ioRouting.launch {
                sendCommandToSimulator()
            }
        }
        val str = "rudder: $rudderVal"
        rudderContent.text = str
    }

    /*
     calculate the throttle and if there was a change bigger then
     1 percent the save the new value and update the simulator.
     */
    private fun setThrottleVal(value: Float) {
        throttleVal = value / 100
        val str = "throttle: $throttleVal"
        throttleContent.text = str
        ioRouting.launch {
            sendCommandToSimulator()
        }
    }

    // send a post request to the server with the controllers values
    private fun sendCommandToSimulator() {
        val command = Command(aileron, elevator, throttleVal, rudderVal)
        commandApi.postCommand(command).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (!response.isSuccessful) {
                    tryToToast("server: " + getErrorBody(response) + "\ntry to reconnect")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                tryToToast("failed to reach server\ntry to reconnect")
            }
        })
    }


    // get the error message from server response
    private fun getErrorBody(response: Response<ResponseBody>): String {
        val reader: BufferedReader?
        val sb = StringBuilder()
        try {
            reader = BufferedReader(InputStreamReader(response.errorBody()!!.byteStream()))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return sb.toString()
    }
}