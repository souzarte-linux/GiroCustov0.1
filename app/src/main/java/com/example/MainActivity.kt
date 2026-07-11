package com.example

import android.os.Bundle
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.GiroCustoMainApp
import com.example.ui.GiroCustoViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Configure osmdroid userAgent and base path to comply with OSM policies
    org.osmdroid.config.Configuration.getInstance().userAgentValue = packageName
    org.osmdroid.config.Configuration.getInstance().osmdroidBasePath = filesDir
    org.osmdroid.config.Configuration.getInstance().osmdroidTileCache = File(cacheDir, "osmdroid")

    enableEdgeToEdge()
    setContent {
      val viewModel: GiroCustoViewModel = viewModel()
      GiroCustoMainApp(viewModel = viewModel)
    }
  }
}
