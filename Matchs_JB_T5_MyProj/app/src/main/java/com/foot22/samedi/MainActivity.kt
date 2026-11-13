package com.foot22.samedi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.foot22.samedi.ui.navigation.Samedi22NavShell
import com.foot22.samedi.ui.theme.Samedi22Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Samedi22Theme {
                Samedi22NavShell()
            }
        }
    }
}
