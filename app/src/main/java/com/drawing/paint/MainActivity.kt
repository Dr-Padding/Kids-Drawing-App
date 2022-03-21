package com.drawing.paint


import android.Manifest
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drawing.paint.Constants.STORAGE_PERMISSION_CODE
import com.drawing.paint.Constants.STORAGE_REQUEST_CODE
import com.drawing.paint.Constants.UPLOAD_PERMISSION
import com.drawing.paint.Constants.tag
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.drawing.paint.adapters.Adapter
import com.drawing.paint.databinding.*
import com.drawing.paint.fragments.BottomSheetFragment
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
const val TAG = "MainActivity"

//Bismillahi-r-Rahmani-r-Rahim
class MainActivity : AppCompatActivity(), Adapter.MyOnClickListener {

    lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    lateinit var cameraSelector: CameraSelector
    private lateinit var preview: Preview
    lateinit var cameraProvider: ProcessCameraProvider
    var mCameraLaunched = false
    private val bottomSheetFragment = BottomSheetFragment()
    private var mRewardedAd: RewardedAd? = null
    private lateinit var adRequest: AdRequest
    private var mRewardedAdShowed = false




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MobileAds.initialize(this@MainActivity) {}
        adRequest = AdRequest.Builder().build()
        binding.avTopBanner.loadAd(adRequest)

        loadRewardedAd()


        val toolsList = mutableListOf(
            Tools(R.drawable.ic_camera_sign),
            Tools(R.drawable.ic_upload_sign),
            Tools(R.drawable.ic_brush),
            Tools(R.drawable.ic_palette),
            Tools(R.drawable.ic_undo),
            Tools(R.drawable.ic_redo),
            Tools(R.drawable.ic_eraser),
            Tools(R.drawable.ic_bin),
            Tools(R.drawable.ic_play),
            Tools(R.drawable.ic_save),
            Tools(R.drawable.ic_share)
        )

        val adapter = Adapter(toolsList, this@MainActivity)
        binding.rvToolsMenu.adapter = adapter
        binding.rvToolsMenu.layoutManager =
            LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        binding.drawingView.setSizeForBrush(10.toFloat())

        binding.btnTakePhoto.visibility = View.INVISIBLE

        binding.viewFinder.visibility = View.INVISIBLE

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnTakePhoto.setOnClickListener {
            binding.btnTakePhoto.visibility = View.INVISIBLE
            showProgressBar()
                takePhoto()
            cameraProvider.unbind(preview)
            mCameraLaunched = false
            binding.drawingView.visibility = View.VISIBLE
        }
    }

    /*   internal fun getBitmapFromView(view: View): Bitmap {
           val returnedBitmap = Bitmap.createBitmap(view.width,
           view.height, Bitmap.Config.ARGB_8888)
           val canvas = Canvas(returnedBitmap)

           view.draw(canvas)
           return returnedBitmap
       }*/


    private fun uploadLaunch() {
        //check if permission is granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            //run our code to get the image from the gallery
            val pickPhotoIntent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(pickPhotoIntent, STORAGE_REQUEST_CODE)
        } else {
            //Request the permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    UPLOAD_PERMISSION.toString()
                )
            ) {
                Toast.makeText(
                    this,
                    "Requires permission to add background",
                    Toast.LENGTH_SHORT
                ).show()
            }
            ActivityCompat.requestPermissions(
                this,
                UPLOAD_PERMISSION, STORAGE_PERMISSION_CODE
            )
        }

        if(mCameraLaunched) {
            cameraProvider.unbind(preview)
            cameraExecutor.shutdownNow()
            binding.viewFinder.visibility = View.INVISIBLE
            binding.btnTakePhoto.visibility = View.INVISIBLE
            binding.drawingView.visibility = View.VISIBLE
            mCameraLaunched = false
        }
    }


    private fun cameraLaunch() {
        if (cameraPermissionGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                Constants.CAMERA_PERMISSION,
                Constants.REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun onClickBin() {
        if (binding.ivBackground.drawable != null) {
            val dialogClickListener =
                DialogInterface.OnClickListener { _, which ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            binding.ivBackground.setImageURI(null)
                        }
                        DialogInterface.BUTTON_NEGATIVE -> {
                        }
                    }
                }

            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setMessage("Do you want to delete the background?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show()
        } else {
            Toast.makeText(this, "No background", Toast.LENGTH_SHORT).show()
        }
    }


    private fun brushSizeChooseDialog() {

        val brushDialog = Dialog(this)
        brushDialog.setTitle(R.string.brush_size)

        val dialogBinding: DialogBrushSizeBinding = DialogBrushSizeBinding.inflate(layoutInflater)
        brushDialog.setContentView(dialogBinding.root)

        val smallBtn = dialogBinding.ibSmallBrush
        smallBtn.setOnClickListener {
            binding.drawingView.onClickEraser(false)
            binding.drawingView.setSizeForBrush(4.toFloat())
            brushDialog.dismiss()
        }

        val mediumBtn = dialogBinding.ibMediumBrush
        mediumBtn.setOnClickListener {
            binding.drawingView.onClickEraser(false)
            binding.drawingView.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val largeBtn = dialogBinding.ibLargeBrush
        largeBtn.setOnClickListener {
            binding.drawingView.onClickEraser(false)
            binding.drawingView.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }


    private fun eraserSizeChooseDialog() {

        val brushDialog = Dialog(this)
        brushDialog.setTitle(R.string.eraser_size)

        val dialogBinding: DialogEraserSizeBinding = DialogEraserSizeBinding.inflate(layoutInflater)
        brushDialog.setContentView(dialogBinding.root)


        val smallBtn = dialogBinding.ibSmallBrush
        smallBtn.setOnClickListener {
            binding.drawingView.onClickEraser(true)
            binding.drawingView.setSizeForBrush(4.toFloat())
            brushDialog.dismiss()
        }

        val mediumBtn = dialogBinding.ibMediumBrush
        mediumBtn.setOnClickListener {
            binding.drawingView.onClickEraser(true)
            binding.drawingView.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val largeBtn = dialogBinding.ibLargeBrush
        largeBtn.setOnClickListener {
            binding.drawingView.onClickEraser(true)
            binding.drawingView.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()

        val clearAllBtn = dialogBinding.btnClearAll
        clearAllBtn.setOnClickListener {

                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForBrush(999999.toFloat())
            brushDialog.dismiss()
        }
    }


    private fun colorPicker() {
        binding.drawingView.onClickEraser(false)

        val colorPickerDialog = Dialog(this)
        colorPickerDialog.setTitle(R.string.choose_the_color)

        val dialogBindingRewarded: DialogColorPickerRewardedBinding = DialogColorPickerRewardedBinding.inflate(layoutInflater)

        val dialogBinding: DialogColorPickerBinding =
            DialogColorPickerBinding.inflate(layoutInflater)

        if(!mRewardedAdShowed) {
            colorPickerDialog.setContentView(dialogBinding.root)
        }else{
            colorPickerDialog.setContentView(dialogBindingRewarded.root)
        }

        val white = dialogBinding.ibWhite
        white.setOnClickListener {
            val colorTag = white.tag.toString()
            binding.drawingView.setColor(colorTag) //set color to brush
            colorPickerDialog.dismiss()
        }

        val black = dialogBinding.ibBlack
        black.setOnClickListener {
            val colorTag = black.tag.toString()
            binding.drawingView.setColor(colorTag)
            colorPickerDialog.dismiss()
        }

        val green = dialogBinding.ibGreen
        green.setOnClickListener {
            val colorTag = green.tag.toString()
            binding.drawingView.setColor(colorTag)
            colorPickerDialog.dismiss()
        }

        val getMoreColors = dialogBinding.btnMoreColors
        getMoreColors.setOnClickListener {

            if (mRewardedAd != null) {
                mRewardedAd?.show(this, OnUserEarnedRewardListener() {

                    colorPickerDialog.setContentView(dialogBindingRewarded.root)

                    mRewardedAdShowed = true



//                    fun onUserEarnedReward(rewardItem: RewardItem) {
//                        var rewardAmount = rewardItem.getReward()
//                        var rewardType = rewardItem.getType()
//                        Log.d(TAG, "User earned the reward.")
//                    }
                })
            } else {
                Log.d(TAG, "The rewarded ad wasn't ready yet.")
            }


        }
        colorPickerDialog.show()
    }


    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let { mFile ->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }


    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                Constants.FILE_NAME_FORMAT,
                Locale.getDefault()
            ).format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOption, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    binding.ivBackground.setImageURI(null)
                    binding.ivBackground.setImageURI(savedUri)
                    //val msg = "Photo saved"
                    cameraExecutor.shutdownNow() // ?????????
                    binding.viewFinder.visibility = View.INVISIBLE
                    //Toast.makeText(this@MainActivity, "$msg $savedUri", Toast.LENGTH_LONG).show()
                    hideProgressBar()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(Constants.TAG, "onError: ${exception.message}", exception)
                }

            }
        )
    }

    private fun hideProgressBar(){
        binding.ProgressBar.visibility = View.INVISIBLE
    }

    private fun showProgressBar(){
        binding.ProgressBar.visibility = View.VISIBLE
    }


    private fun cameraPermissionGranted(): Boolean =
        Constants.CAMERA_PERMISSION.all {
            ContextCompat.checkSelfPermission(
                baseContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Permission granted, now you can read the storage!",
                    Toast.LENGTH_SHORT
                ).show()
                val pickPhotoIntent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
            } else {
                Toast.makeText(
                    this,
                    "Oops, you just denied permission for the storage! You can also allow it from settings!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            if (cameraPermissionGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Oops, you just denied permission for the storage! You can also allow it from settings!",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({

            cameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder().build().also { mPreview ->
                mPreview.setSurfaceProvider(
                    binding.viewFinder.surfaceProvider
                )
            }

            binding.btnTakePhoto.visibility = View.VISIBLE

            binding.viewFinder.visibility = View.VISIBLE

            binding.drawingView.visibility = View.INVISIBLE

            imageCapture = ImageCapture.Builder().build()

            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA


            try {

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (e: Exception) {
                Log.d(Constants.TAG, "Start Camera Fail:", e)
            }

        }, ContextCompat.getMainExecutor(this))

        mCameraLaunched = true
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == STORAGE_REQUEST_CODE) {
                try {
                    if (data!!.data != null) {
                        binding.ivBackground.visibility = View.VISIBLE
                        binding.ivBackground.setImageURI(data.data)
                    } else {
                        Toast.makeText(
                            this,
                            "Oops! Error in parsing the image or its corrupted!", Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }
    }


    override fun onClick(position: Int) {
        when (position) {
            0 -> {
                cameraLaunch()
            }
            1 -> {
                uploadLaunch()
            }
            2 -> {
                brushSizeChooseDialog()
            }
            3 -> {
                colorPicker()
            }
            4 -> {
                binding.drawingView.onClickUndo()
            }
            5 -> {
                binding.drawingView.onClickRedo()
            }
            6 -> {
                eraserSizeChooseDialog()
            }
            7 -> {
                onClickBin()
            }
            8 -> {
                bottomSheetFragment.show(supportFragmentManager, tag)
            }
            9 -> {

            }
        }
    }

    private fun loadRewardedAd() {
        RewardedAd.load(this, AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.message)
                mRewardedAd = null
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                Log.d(TAG, "Ad was loaded.")
                mRewardedAd = rewardedAd
                setFullScreenContentCallbackForRewardedAd()
            }
        })
    }


    private fun setFullScreenContentCallbackForRewardedAd() {
        if (mRewardedAd != null) {
            mRewardedAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    Log.d(TAG, "Ad was shown.")
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                    // Called when ad fails to show.
                    Log.d(TAG, "Ad failed to show.")
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    Log.d(TAG, "Ad was dismissed.")
                    mRewardedAd = null
                }
            }
        }
    }




    override fun onBackPressed() {
        if (mCameraLaunched){
            cameraProvider.unbind(preview)
            cameraExecutor.shutdownNow()
            binding.viewFinder.visibility = View.INVISIBLE
            binding.btnTakePhoto.visibility = View.INVISIBLE
            binding.drawingView.visibility = View.VISIBLE
            mCameraLaunched = false
        }else{
            super.onBackPressed()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}


