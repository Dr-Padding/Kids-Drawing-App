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
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drawing.paint.Constants.STORAGE_PERMISSION_CODE
import com.drawing.paint.Constants.STORAGE_REQUEST_CODE
import com.drawing.paint.Constants.UPLOAD_PERMISSION
import com.drawing.paint.Constants.tag
import com.drawing.paint.adapters.Adapter
import com.drawing.paint.databinding.*
import com.drawing.paint.fragments.BottomSheetFragment
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


//Bismillahi-r-Rahmani-r-Rahim
const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
const val TAG = "MainActivity"


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
    private var mAdShowedForBrush = false
    private var mAdShowedForColors = false
    private var mAdShowedForEraser = false
    private var mIsLoading = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MobileAds.initialize(this@MainActivity) {}
        val adRequestBanner = AdRequest.Builder().build()
        binding.avTopBanner.loadAd(adRequestBanner)

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

        if (mRewardedAd == null && !mIsLoading) {
            loadRewardedAd()
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
                this@MainActivity,
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
                    this@MainActivity,
                    UPLOAD_PERMISSION.toString()
                )
            ) {
                Toast.makeText(
                    this@MainActivity,
                    "Requires permission to add background",
                    Toast.LENGTH_SHORT
                ).show()
            }
            ActivityCompat.requestPermissions(
                this@MainActivity,
                UPLOAD_PERMISSION, STORAGE_PERMISSION_CODE
            )
        }

        if (mCameraLaunched) {
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
                this@MainActivity,
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
            Toast.makeText(this@MainActivity, "No background", Toast.LENGTH_SHORT).show()
        }
    }


    private fun brushSizeChooseDialog() {

        val brushDialog = Dialog(this@MainActivity)
        brushDialog.setTitle(R.string.brush_size)

        val dialogBindingBrushesRewarded: DialogBrushSizeRewardedBinding =
            DialogBrushSizeRewardedBinding.inflate(layoutInflater)

        val dialogBindingWithMoreSizesBtn: DialogBrushSizeBinding =
            DialogBrushSizeBinding.inflate(layoutInflater)

        val dialogBindingBrushesInitial: DialogBrushSizeWithoutMoreSizesBtnBinding =
            DialogBrushSizeWithoutMoreSizesBtnBinding.inflate(layoutInflater)
        brushDialog.setContentView(dialogBindingBrushesInitial.root)


        if (mRewardedAd != null) {
            brushDialog.setContentView(dialogBindingWithMoreSizesBtn.root)
        } else {
            brushDialog.setContentView(dialogBindingBrushesInitial.root)
        }

        if (!mAdShowedForBrush) {
            brushDialog.setContentView(dialogBindingWithMoreSizesBtn.root)
        } else {
            brushDialog.setContentView(dialogBindingBrushesRewarded.root)
        }

        val getMoreSizes = dialogBindingWithMoreSizesBtn.btnMoreSizes
        getMoreSizes.setOnClickListener {
                showRewardedVideo()
                if (mRewardedAd != null) {
                    mRewardedAd?.show(
                        this@MainActivity,
                        OnUserEarnedRewardListener() {
                            brushDialog.setContentView(dialogBindingBrushesRewarded.root)
                            Log.d("TAG", "User earned the reward.")
                            mAdShowedForBrush = true

//                    fun onUserEarnedReward(rewardItem: RewardItem) {
//                        var rewardAmount = rewardItem.amount
//                        //addCoins(rewardAmount)
//                    }
                        }
                    )
                }
        }

        dialogBindingBrushesInitial.apply {
            ibSmallBrush.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(4.toFloat())
                brushDialog.dismiss()
            }

            ibMediumBrush.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(10.toFloat())
                brushDialog.dismiss()
            }

            ibLargeBrush.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(20.toFloat())
                brushDialog.dismiss()
            }
        }

        dialogBindingWithMoreSizesBtn.apply {
            ibSmallBrush.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(4.toFloat())
                brushDialog.dismiss()
            }

            ibMediumBrush.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(10.toFloat())
                brushDialog.dismiss()
            }

            ibLargeBrush.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(20.toFloat())
                brushDialog.dismiss()
            }
        }

        dialogBindingBrushesRewarded.apply {

            ib2f.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(2.toFloat())
                brushDialog.dismiss()
            }

            ib4f.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(4.toFloat())
                brushDialog.dismiss()
            }

            ib6f.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(6.toFloat())
                brushDialog.dismiss()
            }

            ib8f.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(8.toFloat())
                brushDialog.dismiss()
            }

            ib10f.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(10.toFloat())
                brushDialog.dismiss()
            }

            ib12f.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(12.toFloat())
                brushDialog.dismiss()
            }

            ib14f.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(14.toFloat())
                brushDialog.dismiss()
            }

            ib16f.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(16.toFloat())
                brushDialog.dismiss()
            }

            ib18f.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(18.toFloat())
                brushDialog.dismiss()
            }

            ib20f.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(20.toFloat())
                brushDialog.dismiss()
            }

            ib22f.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(22.toFloat())
                brushDialog.dismiss()
            }

            ib24f.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(24.toFloat())
                brushDialog.dismiss()
            }

            ib26f.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(26.toFloat())
                brushDialog.dismiss()
            }

            ib28f.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(28.toFloat())
                brushDialog.dismiss()
            }

            ib30f.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                binding.drawingView.setSizeForBrush(30.toFloat())
                brushDialog.dismiss()
            }
        }
        brushDialog.show()
    }


    private fun eraserSizeChooseDialog() {

        val brushDialog = Dialog(this@MainActivity)
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
        val colorPickerDialog = Dialog(this@MainActivity)
        colorPickerDialog.setTitle(R.string.choose_the_color)

        val dialogBindingRewarded: DialogColorPickerRewardedBinding =
            DialogColorPickerRewardedBinding.inflate(layoutInflater)

        val dialogBindingWithMoreColorsBtn: DialogColorPickerBinding =
            DialogColorPickerBinding.inflate(layoutInflater)

        val dialogBindingInitial: DialogColorPickerWithoutMoreColorsBtnBinding =
            DialogColorPickerWithoutMoreColorsBtnBinding.inflate(layoutInflater)
        colorPickerDialog.setContentView(dialogBindingInitial.root)

        if (mRewardedAd != null) {
            colorPickerDialog.setContentView(dialogBindingWithMoreColorsBtn.root)
        } else {
            colorPickerDialog.setContentView(dialogBindingInitial.root)
        }

        if (!mAdShowedForColors) {
            colorPickerDialog.setContentView(dialogBindingWithMoreColorsBtn.root)
        } else {
            colorPickerDialog.setContentView(dialogBindingRewarded.root)
        }

        val getMoreColors = dialogBindingWithMoreColorsBtn.btnMoreColors
        getMoreColors.setOnClickListener {
                showRewardedVideo()
                if (mRewardedAd != null) {
                    mRewardedAd?.show(
                        this@MainActivity,
                        OnUserEarnedRewardListener() {
                            colorPickerDialog.setContentView(dialogBindingRewarded.root)
                            Log.d("TAG", "User earned the reward.")
                            mAdShowedForColors = true

//                    fun onUserEarnedReward(rewardItem: RewardItem) {
//                        var rewardAmount = rewardItem.amount
//                        //addCoins(rewardAmount)
//                    }
                        }
                    )
                }
        }

        dialogBindingInitial.apply {
            ibWhite.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibBlack.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibGreen.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }
        }

        dialogBindingWithMoreColorsBtn.apply {
            ibWhite.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibBlack.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibGreen.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }
        }

        dialogBindingRewarded.apply {
            ibWhite.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibAntiqueWhite.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibLemonChiffon.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibBlack.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibSteelGrey.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibGreyGoose.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibDarkGreen.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibGreen.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibGreenYellow.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibGold.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibYellow.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibLightYellow.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibRed.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibOrange.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibHotPink.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibBrown.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibSalmon.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibPeach.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibBlue.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibSkyBlue.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibCyan.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibDarkViolet.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibViolet.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
            }

            ibMagenta.setOnClickListener {
                binding.drawingView.onClickEraser(false)
                val colorTag = it.tag.toString()
                binding.drawingView.setColor(colorTag) //set color to brush
                colorPickerDialog.dismiss()
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
            outputOption, ContextCompat.getMainExecutor(this@MainActivity),
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

    private fun hideProgressBar() {
        binding.ProgressBar.visibility = View.INVISIBLE
    }

    private fun showProgressBar() {
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
                    this@MainActivity,
                    "Permission granted, now you can read the storage!",
                    Toast.LENGTH_SHORT
                ).show()
                val pickPhotoIntent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
            } else {
                Toast.makeText(
                    this@MainActivity,
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
                    this@MainActivity,
                    "Oops, you just denied permission for the storage! You can also allow it from settings!",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@MainActivity)
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
                    this@MainActivity, cameraSelector, preview, imageCapture
                )

            } catch (e: Exception) {
                Log.d(Constants.TAG, "Start Camera Fail:", e)
            }

        }, ContextCompat.getMainExecutor(this@MainActivity))

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
                            this@MainActivity,
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
        if (mRewardedAd == null) {
            mIsLoading = true
            var adRequest = AdRequest.Builder().build()

            RewardedAd.load(
                this@MainActivity, AD_UNIT_ID, adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d(TAG, adError?.message)
                        mIsLoading = false
                        mRewardedAd = null
                    }

                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        Log.d(TAG, "Ad was loaded.")
                        mRewardedAd = rewardedAd
                        mIsLoading = false
                    }
                }
            )
        }
    }


    private fun showRewardedVideo() {
        if (mRewardedAd != null) {
            mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad was dismissed.")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    mRewardedAd = null
                    loadRewardedAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                    Log.d(TAG, "Ad failed to show.")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    mRewardedAd = null
                    loadRewardedAd() //???????????
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.")
                    // Called when ad is shown.
                }
            }
        }
    }

    override fun onBackPressed() {
        if (mCameraLaunched) {
            cameraProvider.unbind(preview)
            cameraExecutor.shutdownNow()
            binding.viewFinder.visibility = View.INVISIBLE
            binding.btnTakePhoto.visibility = View.INVISIBLE
            binding.drawingView.visibility = View.VISIBLE
            mCameraLaunched = false
        } else {
            super.onBackPressed()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        mRewardedAd = null
    }

}


