package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        enableEdgeToEdge()

        setContent {

            MyApplicationTheme {
                LoginScreen()
                // call db and functions
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(
                                this@MainActivity,
                                    Allexam::class.java
                                )
                                startActivity(intent) },
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) { Text("Visa prov") }

                        Button(
                            onClick = {
                                val intent = Intent(
                                    this@MainActivity,
                                    StudyScreen::class.java
                                )
                                startActivity(intent) },
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) { Text("Plugga") }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen() {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    Column {

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") }
        )

        Button(
            onClick = {

                scope.launch {

                    try {

                        SupabaseManager.client.auth.signUpWith(
                            io.github.jan.supabase.gotrue.providers.builtin.Email
                        ) {

                            this.email = email
                            this.password = password
                        }

                    } catch(e: Exception) {
                        println(e.message)
                    }
                }
            }
        ) {
            Text("Create Account")
        }
    }
}

