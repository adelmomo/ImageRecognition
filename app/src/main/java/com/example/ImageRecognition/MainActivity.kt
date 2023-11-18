package com.example.ImageRecognition

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.ImageProxy
import com.example.ImageRecognition.databinding.ActivityMainBinding
import java.nio.ByteBuffer

typealias AnalyzerListener = (image: Bitmap) -> Unit

class MainActivity : AppCompatActivity() {
    private class ImageAnalyzer(private val listener: AnalyzerListener) : ImageAnalysis.Analyzer {
        private lateinit var  bitmap:Bitmap
        override fun analyze(image: ImageProxy) {
            bitmap = Bitmap.createBitmap(
                image.width,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            image.use { bitmap.copyPixelsFromBuffer(image.planes[0].buffer) }

            //val bitmap: Bitmap? = BitmapFactory.decodeByteArray(data, 0, data.size)
            listener(bitmap)

            image.close()
        }
    }
    private lateinit var viewBinding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        viewBinding.result.textSize=20f
        viewBinding.result.gravity = android.view.Gravity.CENTER
        ImageRecognizer.classifierInit(assets)
        ImageRecognizer.createLabels(this)
        // Request camera permissions
        if (allPermissionsGranted()) {
            viewBinding.videoCaptureButton.visibility= View.INVISIBLE
            viewBinding.cameraCloseButton.visibility= View.VISIBLE
            startCamera()
        } else {
            viewBinding.videoCaptureButton.visibility= View.VISIBLE
            viewBinding.cameraCloseButton.visibility= View.INVISIBLE
            requestPermissions()
        }
        viewBinding.cameraCloseButton.setOnClickListener {
            cameraProvider.unbindAll()
            viewBinding.videoCaptureButton.visibility= View.VISIBLE
            viewBinding.cameraCloseButton.visibility= View.INVISIBLE

        }
        viewBinding.videoCaptureButton.setOnClickListener {
            startCamera()
            viewBinding.videoCaptureButton.visibility= View.INVISIBLE
            viewBinding.cameraCloseButton.visibility= View.VISIBLE

        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    private lateinit var cameraProvider:ProcessCameraProvider
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalyzer { image ->
                        var scores=ImageRecognizer.classify(image)
                        var maxScore=0.0f
                        var label=0
                        for(i in scores.indices){
                            if(scores[i]>maxScore){
                                maxScore=scores[i]
                                label=i
                            }
                        }

                        viewBinding.result.setText("%.2f".format(maxScore*100)+"% "+ImageRecognizer.labels[label])

                    })
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all { it
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}