package com.arkdev.a3dmodelapp.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arkdev.a3dmodelapp.data.model.ModelData
import com.arkdev.a3dmodelapp.databinding.ActivityModelViewerBinding
import com.arkdev.a3dmodelapp.repo.ModelRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ModelViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityModelViewerBinding
    private lateinit var repository: ModelRepository
    private var currentModel: ModelData? = null

    companion object {
        private const val TAG = "ModelViewerActivity"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModelViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        repository = ModelRepository(this)

        setupWebView()
        setupButtons()

        val modelId = intent.getIntExtra("MODEL_ID", -1)
        if (modelId != -1) {
            loadModel(modelId)
        } else {
            Toast.makeText(this, "Invalid model ID", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }

            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(msg: ConsoleMessage?): Boolean {
                    msg?.let { Log.d(TAG, "WebView: ${it.message()}") }
                    return true
                }
            }
        }
    }

    private fun loadModel(modelId: Int) {
        binding.progressBar.visibility = View.VISIBLE
        binding.webView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Loading model ID: $modelId")

                currentModel = withContext(Dispatchers.IO) {
                    repository.getModelById(modelId)
                }

                if (currentModel == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ModelViewerActivity,
                            "Model not found",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    return@launch
                }

                Log.d(TAG, "Model: ${currentModel?.name}")
                Log.d(TAG, "Path: ${currentModel?.filePath}")


                val file = File(currentModel!!.filePath)
                if (!file.exists()) {
                    Log.e(TAG, "File not found!")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ModelViewerActivity,
                            "File not found",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@launch
                }

                Log.d(TAG, "File exists, size: ${file.length()} bytes")

                // Convert file to Base64
                val base64Data = withContext(Dispatchers.IO) {
                    val bytes = file.readBytes()
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                }

                Log.d(TAG, "File converted to Base64, length: ${base64Data.length}")

                withContext(Dispatchers.Main) {
                    load3DModel(base64Data)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading model", e)
                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE

                    MaterialAlertDialogBuilder(this@ModelViewerActivity)
                        .setTitle("Error")
                        .setMessage("Failed to load model:\n\n${e.message}")
                        .setPositiveButton("OK") { _, _ -> finish() }
                        .show()
                }
            }
        }
    }



    private fun load3DModel(base64Data: String) {
        try {
            Log.d(TAG, "Loading 3D model into WebView")

            // Create data URL from Base64
            val dataUrl = "data:model/gltf-binary;base64,$base64Data"

            val html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                    <title>3D Model Viewer</title>
                    
                    <script type="module" src="https://ajax.googleapis.com/ajax/libs/model-viewer/3.4.0/model-viewer.min.js"></script>
                    
                    <style>
                        * {
                            margin: 0;
                            padding: 0;
                            box-sizing: border-box;
                        }
                        
                        body {
                            width: 100vw;
                            height: 100vh;
                            overflow: hidden;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        }
                        
                        model-viewer {
                            width: 100%;
                            height: 100%;
                            background: transparent;
                        }
                        
                        model-viewer::part(default-progress-bar) {
                            background-color: #6750A4;
                            height: 4px;
                        }
                        
                        .loading {
                            position: absolute;
                            top: 50%;
                            left: 50%;
                            transform: translate(-50%, -50%);
                            color: white;
                            font-family: Arial, sans-serif;
                            font-size: 16px;
                            text-align: center;
                            z-index: 1000;
                        }
                        
                        .spinner {
                            border: 3px solid rgba(255,255,255,0.3);
                            border-top: 3px solid white;
                            border-radius: 50%;
                            width: 40px;
                            height: 40px;
                            animation: spin 1s linear infinite;
                            margin: 0 auto 10px;
                        }
                        
                        @keyframes spin {
                            0% { transform: rotate(0deg); }
                            100% { transform: rotate(360deg); }
                        }
                    </style>
                </head>
                <body>
                    <div class="loading" id="loading">
                        <div class="spinner"></div>
                        <div>Loading 3D Model...</div>
                    </div>
                    
                    <model-viewer 
                        id="modelViewer"
                        src="${dataUrl}"
                        alt="3D Model"
                        camera-controls
                        touch-action="pan-y"
                        auto-rotate
                        auto-rotate-delay="1000"
                        rotation-per-second="30deg"
                        shadow-intensity="1"
                        shadow-softness="0.8"
                        exposure="1.0"
                        camera-orbit="0deg 75deg 105%"
                        field-of-view="45deg"
                        loading="eager">
                    </model-viewer>
                    
                    <script>
                        const modelViewer = document.getElementById('modelViewer');
                        const loading = document.getElementById('loading');
                        
                        modelViewer.addEventListener('load', () => {
                            console.log('✅ Model loaded successfully');
                            loading.style.display = 'none';
                        });
                        
                        modelViewer.addEventListener('error', (event) => {
                            console.error('❌ Model loading error:', event);
                            loading.innerHTML = '<div style="color:#ff5252;">Failed to load model</div>';
                        });
                        
                        modelViewer.addEventListener('progress', (event) => {
                            const progress = Math.round(event.detail.totalProgress * 100);
                            console.log('Loading progress: ' + progress + '%');
                        });
                        
                        console.log('Model viewer initialized with data URL');
                    </script>
                </body>
                </html>
            """.trimIndent()

            binding.webView.loadDataWithBaseURL(
                "https://example.com/",
                html,
                "text/html",
                "UTF-8",
                null
            )

            binding.progressBar.visibility = View.GONE
            binding.webView.visibility = View.VISIBLE

            Toast.makeText(this, "Drag to rotate • Pinch to zoom", Toast.LENGTH_SHORT).show()

            Log.d(TAG, "WebView content loaded with Base64 data")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading 3D model", e)
            binding.progressBar.visibility = View.GONE

            MaterialAlertDialogBuilder(this)
                .setTitle("Error")
                .setMessage("Failed to display model:\n\n${e.message}")
                .setPositiveButton("OK") { _, _ -> finish() }
                .show()
        }
    }

    private fun setupButtons() {
        binding.btnResetCamera.setOnClickListener {
            binding.webView.evaluateJavascript(
                """
                const viewer = document.getElementById('modelViewer');
                if (viewer) {
                    viewer.cameraOrbit = '0deg 75deg 105%';
                    viewer.fieldOfView = '45deg';
                }
                """.trimIndent(),
                null
            )
            Toast.makeText(this, "Camera reset", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }


    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
    }
}