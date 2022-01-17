package com.example.smarket

import Product
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.android.synthetic.main.activity_barcode_scanner.*
import java.io.File
import java.util.concurrent.ExecutorService

typealias BarCodeListener = (barcode: String) -> Unit
private class BarCodeAnalyzer(private val listener: BarCodeListener) : ImageAnalysis.Analyzer {

    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8)
        .build()
    val scanner = BarcodeScanning.getClient(options)

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null)
        {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->

                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        listener(rawValue)
                    }
                    imageProxy.close()
                }
                .addOnFailureListener {
                    imageProxy.close()
                }
        }
        else
        {
            imageProxy.close()
        }
    }
}

class BarcodeScannerActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var barcodeCountMap = mutableMapOf<String,Int>()
    private val minimumBarcodeCount = 5
    private var waitingForResponse = false
    private lateinit var chosenBarcode: String
    private var chosenItemId: String? = null
    private var chosenItemName: String? = null
    private var db = FirebaseFirestore.getInstance()

    private fun scanConfirmed()
    {
        barcodeCountMap.clear()
        waitingForResponse = false

        AddItemActivity.addedItemsAdapter.addItem(Product(chosenItemName.toString(), 420.00, chosenItemId.toString(),"kurac"))
//        val intent = Intent(this, QuantitySelectorActivity::class.java)
//        intent.putExtra("itemName", chosenItemName)
//        intent.putExtra("itemId", chosenItemId)
//        startActivity(intent)
    }
    private fun scanDeclined()
    {
        barcodeCountMap.clear()
        waitingForResponse = false
        clearScanPrompt()
    }
    private fun promptScan()
    {
        findViewById<ConstraintLayout>(R.id.barcodeScannerItemFoundWrapper).visibility = View.VISIBLE
        findViewById<ConstraintLayout>(R.id.barcodeScannerButtonWrapper).visibility = View.VISIBLE
    }
    private fun clearScanPrompt()
    {
        findViewById<ConstraintLayout>(R.id.barcodeScannerItemFoundWrapper).visibility = View.GONE
        findViewById<ConstraintLayout>(R.id.barcodeScannerButtonWrapper).visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scanner)

        val addedItemsRecycler = findViewById<RecyclerView>(R.id.addedItemsRecyclerBarcode)
        addedItemsRecycler.adapter = AddItemActivity.addedItemsAdapter
        addedItemsRecycler.layoutManager = LinearLayoutManager(this)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Setup confirm and decline scan buttons
        findViewById<Button>(R.id.barcodeScannerAcceptButton).setOnClickListener {
            scanConfirmed()
        }
        findViewById<Button>(R.id.barcodeScannerDeclineButton).setOnClickListener {
            scanDeclined()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarCodeAnalyzer { barcode ->
                        Log.d(TAG, "Found barcode: $barcode")
                        onFoundBarcode(barcode)
                    })
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun showFoundItem()
    {
        db.collection("Products").whereEqualTo("barcode",chosenBarcode).get().addOnSuccessListener { documents->
            if (!documents.isEmpty)
            {
                chosenItemName= documents.first().data["name"]?.toString()
                chosenItemId = documents.first().data["id"]?.toString()
                findViewById<TextView>(R.id.barcodeScannerFoundItemText).text = chosenItemName
            }
        }
    }

    private fun onFoundBarcode(barcode: String)
    {
        promptScan()
        val oldValue = barcodeCountMap[barcode] ?: 0
        barcodeCountMap[barcode] = oldValue+1
        if (barcodeCountMap[barcode]!! >= minimumBarcodeCount && !waitingForResponse)
        {
            waitingForResponse = true
            chosenBarcode = barcode
            barcodeCountMap.clear()
            showFoundItem()
            promptScan()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "BarcodeScannerActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}