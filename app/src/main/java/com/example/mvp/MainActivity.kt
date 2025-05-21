package com.example.mvp

import android.os.Bundle
import android.util.Log
import com.parse.ParseObject
import com.parse.ParseException
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.mvp.ui.theme.MVpTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // --- PARSE CONNECTION TEST ---
        val testObj = ParseObject("ConnectionTest")
        testObj.put("message", "Parse is up and running!")
        testObj.saveInBackground { e: ParseException? ->
            if (e != null) Log.e("MainActivity", "Parse save failed: ${e.localizedMessage}")
            else Log.d("MainActivity", "Parse connection successful – object saved.")
        }
        enableEdgeToEdge()
        setContent {
            MVpTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MVpTheme {
        Greeting("Android")
    }
}