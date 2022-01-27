package com.example.smarket

import android.os.Bundle
import android.Manifest
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.android.synthetic.main.activity_barcode_scanner.*
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

class BarcodeScannerActivity : BaseActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var barcodeCountMap = mutableMapOf<String,Int>()
    private var ignoreBarcodesSet = mutableSetOf<String>()
    private val minimumBarcodeCount = 5
    private var waitingForResponse = false
    private lateinit var chosenBarcode: String
    private lateinit var chosenProduct: UserData.Product
    private var db = FirebaseFirestore.getInstance()
    private var addingItemToFridge: Boolean = true

    private fun scanConfirmed()
    {
        barcodeCountMap.clear()
        waitingForResponse = false
        clearScanPrompt()
        ignoreBarcodesSet.add(chosenBarcode)

        if (addingItemToFridge)
        {
            UserData.addNewFridgeItem("kom",chosenProduct,1)
        }
        else
        {
            val contextView = findViewById<View>(R.id.barcodeCameraViewFinder)
            UserData.removeFridgeItemByProduct(chosenProduct).addOnSuccessListener {
                val itemRemovedText = getString(R.string.removed_item_snackbar_text, chosenProduct.name.databaseValue)
                Snackbar.make(contextView, itemRemovedText, Snackbar.LENGTH_SHORT).show()
            }.addOnFailureListener {
                val itemRemovedText = getString(R.string.removed_item_fail_snackbar_text, chosenProduct.name.databaseValue)
                Snackbar.make(contextView, itemRemovedText, Snackbar.LENGTH_SHORT).show()
            }
        }
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
        val barcodeScannerPrompt = findViewById<TextView>(R.id.barcodeScannerPromptText)
        barcodeScannerPrompt.text = getString(R.string.prompt_scan_barcode)
        barcodeScannerPrompt.visibility = View.GONE
    }

    private fun promptFail()
    {
        findViewById<TextView>(R.id.barcodeScannerPromptText).text = getString(R.string.barcode_scanner_activity_fail_text)
    }

    private fun clearScanPrompt()
    {
        findViewById<ConstraintLayout>(R.id.barcodeScannerItemFoundWrapper).visibility = View.GONE
        findViewById<ConstraintLayout>(R.id.barcodeScannerButtonWrapper).visibility = View.GONE
        findViewById<TextView>(R.id.barcodeScannerPromptText).visibility = View.VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scanner)
        super.bindListenersToTopBar()
        super.setTitle(getString(R.string.barcode_scanner_activity_title))

        findViewById<FloatingActionButton>(R.id.finishBarcodeScanningFab).setOnClickListener {
            finish()
        }

        addingItemToFridge = intent.getBooleanExtra("adding", true)
        if (addingItemToFridge) {
            val addedItemsRecycler = findViewById<RecyclerView>(R.id.addedItemsRecyclerBarcode)
            addedItemsRecycler.adapter = AddItemActivity.addedItemsAdapter
            addedItemsRecycler.layoutManager = LinearLayoutManager(this)
        }
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
                    it.setSurfaceProvider(barcodeCameraViewFinder.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarCodeAnalyzer { barcode ->
                        Log.d(TAG, "Found barcode: $barcode")
                        if (!ignoreBarcodesSet.contains(barcode)) {
                            onFoundBarcode(barcode)
                        }
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
                val product = UserData.productFromDoc(documents.first())
                if (product != null) {
                    chosenProduct = product
                    findViewById<TextView>(R.id.barcodeScannerFoundItemText).text = chosenProduct.name.databaseValue
                    promptScan()
                }
            }
            else{
                promptFail()
                scanDeclined()
            }
        }
    }

    private fun onFoundBarcode(barcode: String)
    {
        val oldValue = barcodeCountMap[barcode] ?: 0
        barcodeCountMap[barcode] = oldValue+1
        if (barcodeCountMap[barcode]!! >= minimumBarcodeCount && !waitingForResponse)
        {
            waitingForResponse = true
            chosenBarcode = barcode
            barcodeCountMap.clear()
            showFoundItem()
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