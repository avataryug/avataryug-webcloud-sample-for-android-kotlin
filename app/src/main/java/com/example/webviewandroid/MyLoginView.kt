package com.example.webviewandroid

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import android.widget.Button

class MyLoginView : AppCompatActivity() {
    private lateinit var button1: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_login_view)

        var btn1:Button = findViewById(R.id.button);
        var btn2:Button = findViewById(R.id.button2);

        btn1.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("ACTION", "LoginWithServer")
            startActivity(intent)
        }
        btn2.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("ACTION", "LoginWithCustom")
            startActivity(intent)
        }
    }
}