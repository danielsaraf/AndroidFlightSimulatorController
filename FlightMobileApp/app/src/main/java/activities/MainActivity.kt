package activities

import kotlinx.android.synthetic.main.activity_main.*
import services.rest.ApiService
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.room.Room
import com.example.flightmobileapp.R
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import services.database.AppDB
import services.database.UrlEntity

class MainActivity : AppCompatActivity() {
    private var btns = mutableListOf<Button>()
    private var views = mutableListOf<ConstraintLayout>()
    private lateinit var db: AppDB
    private var ioRouting: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var api: ApiService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // set views list, get Database and load 5 last buttons from it
        initViews()
        initDB()
        setBtnsFromDB(this)
        // clear all DB when click on the trash icon
        trashIcon.setOnClickListener {
            clearAll()
        }
        // connect button listener
        btnConnect.setOnClickListener {
            insertOrUpdateDBRecord(txtUrl.text.toString())
            // check if new url is already in the 5 last used urls, if it is, update its position
            val index = checkIfBtnExits(txtUrl.text.toString())
            if (index != -1) {
                getBtnToTop(index)
                tryGetScreenshot(txtUrl.text.toString())
                return@setOnClickListener
            }
            // if there is 5 btn, remove the LRU one
            if (btns.size == 5)
                btns.remove(btns[0])
            // add a new button with the current URL
            val button = createBtn(this, txtUrl.text.toString())
            btns.add(button)
            refreshButtons()
            //try to get screenshot from the new url
            tryGetScreenshot(txtUrl.text.toString())
        }
    }

    // set a new REST api with "url" URL
    private fun setApi(url: String) {
            val gson = GsonBuilder()
                .setLenient()
                .create()
            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
            api = retrofit.create(ApiService::class.java)
    }

    // change btn position to the top of the list
    private fun getBtnToTop(index: Int) {
        val btn = btns[index]
        btns.remove(btn)
        btns.add(btn)
        refreshButtons()
    }

    // initialize database
    private fun initDB() {
        db = Room.databaseBuilder(applicationContext, AppDB::class.java, "UrlDB").build()
    }

    // initialize views list to hold the 5 views where the buttons will be fit in
    private fun initViews() {
        views.add(findViewById(R.id.view1))
        views.add(findViewById(R.id.view2))
        views.add(findViewById(R.id.view3))
        views.add(findViewById(R.id.view4))
        views.add(findViewById(R.id.view5))
    }

    // create a new button with a text
    private fun createBtn(mainActivity: MainActivity, text: String): Button {
        val button = Button(mainActivity)
        button.layoutParams =
            ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, 155)
        button.text = text
        button.setOnClickListener {
            txtUrl.setText(button.text.toString())
        }
        button.setBackgroundColor(Color.BLACK)
        button.setTextColor(Color.WHITE)
        button.textSize = 22F
        button.gravity = Gravity.START
        return button
    }

    // clear all buttons from btns List and from DataBase
    private fun clearAll() {
        btns.clear()
        refreshButtons()
        ioRouting.launch {
            db.clearAllTables()
        }
    }

    // read 5 LRU URLs from data base and create 5 buttons according to them
    private fun setBtnsFromDB(context: MainActivity) {
        loadBtnsFromDB(context)
        refreshButtons()
    }


    // insert a new URL record to database, and if exists update its LRU time
    private fun insertOrUpdateDBRecord(url: String) {
        ioRouting.launch {
            val urlEntity = UrlEntity()
            urlEntity.url = url
            try {
                db.urlDAO().insertOrUpdateUrl(urlEntity)
            } catch (e: Exception) {
                Log.i("@DB", "failed to insert to db")
            }
        }
    }

    // get 5 LRU URLs and create 5 buttons according to it, add the buttons to 'btns' list
    private fun loadBtnsFromDB(
        context: MainActivity
    ) {
        ioRouting.launch {
            db.urlDAO().readTop5Urls().forEach {
                val button = createBtn(context, it.url)
                btns.add(button)
            }
            btns.reverse()
        }
    }

    // clear all 5 views, and reset buttons to them according to the order of the buttons in
    // 'btns' list
    private fun refreshButtons() {
        ioRouting.launch {
            withContext(Dispatchers.Main) {
                var i = 0
                while (i < views.size) {
                    views[i].removeAllViews()
                    i++
                }
                i = 0
                while (i < btns.size) {
                    views[4 - i].addView(btns[btns.size - 1 - i])
                    i++
                }
            }
        }
    }

    // check if there is a button who already set to 'newUrl' text, if there is return its position
    private fun checkIfBtnExits(newUrl: String): Int {
        for ((i, btn) in btns.withIndex()) {
            if (btn.text.toString() == newUrl)
                return i
        }
        return -1
    }

    // request a screenshot from url, if response is successful, move to cockpit activity
    // o.w show a toast message
    private fun tryGetScreenshot(url: String) {
        try {
            setApi(url)
        } catch (e: Exception) {
            showFailMsg(url)
            return
        }
        api.getScreenshot().enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    // server return status 200
                    intent = Intent(applicationContext, CockpitActivity::class.java)
                    intent.putExtra("url", url)
                    startActivity(intent)
                } else {
                    showFailMsg(url)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                showFailMsg(url)
            }
        })
    }

    // show 'Unable to connect' toast
    private fun showFailMsg(url: String) {
        Toast.makeText(
            this@MainActivity,
            "Unable to connect $url",
            Toast.LENGTH_LONG
        ).show()
    }
}
