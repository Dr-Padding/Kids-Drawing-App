package com.drawing.paint


import android.Manifest
import android.app.Dialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drawing.paint.Constants.DOWNLOAD_PERMISSION_CODE
import com.drawing.paint.Constants.SHARE_PERMISSION_CODE
import com.drawing.paint.Constants.STORAGE_PERMISSION_CODE
import com.drawing.paint.Constants.UPLOAD_PERMISSION
import com.drawing.paint.Constants.tag
import com.drawing.paint.adapters.Adapter
import com.drawing.paint.databinding.*
import com.drawing.paint.fragments.BottomSheetFragment
import com.drawing.paint.fragments.PrivacyPolicyBottomSheetFragment
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.testing.FakeReviewManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


//Bismillahi-r-Rahmani-r-Rahim
const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
const val TAG = "MainActivity"


class MainActivity : AppCompatActivity(), Adapter.MyOnClickListener {

    lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    lateinit var cameraSelector: CameraSelector
    private lateinit var preview: Preview
    private lateinit var cameraProvider: ProcessCameraProvider
    var mCameraLaunched = false
    var isFragmentShown = false
    private var mRewardedAd: RewardedAd? = null
    private var mInterstitialAd: InterstitialAd? = null
    private var mAdShowedForBrush = false
    private var mAdShowedForColors = false
    private var mAdShowedForEraser = false
    private var mAdShowedForMaxSize = false
    private var mIsLoading = false
    private var mAdIsLoading = false
    lateinit var manager: ReviewManager
    private lateinit var reviewInfo: ReviewInfo
    var liveData = MutableLiveData<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this@MainActivity) {}
        loadRewardedAd()
        loadInterstitialAd()
        checkConnection()
        binding = ActivityMainBinding.inflate(layoutInflater)
        installSplashScreen()
        setContentView(binding.root)

        val adRequestBanner = AdRequest.Builder().build()
        binding.avTopBanner.loadAd(adRequestBanner)

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
            Tools(R.drawable.ic_share),
            Tools(R.drawable.ic_star),
            Tools(R.drawable.ic_dr)
        )

        val adapter = Adapter(toolsList, this@MainActivity)
        binding.apply {
            rvToolsMenu.adapter = adapter
            rvToolsMenu.layoutManager =
                LinearLayoutManager(this@MainActivity, RecyclerView.HORIZONTAL, false)
            drawingView.setSizeForBrush(10.toFloat())
            btnTakePhoto.visibility = View.INVISIBLE
        }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (mRewardedAd == null && !mIsLoading) {
            loadRewardedAd()
            mIsLoading = true
        }

        if (!mAdIsLoading && mInterstitialAd == null) {
            loadInterstitialAd()
            mAdIsLoading = true
        }

        val sharedPreferences = getSharedPreferences("sharedPref", MODE_PRIVATE)
        val convertedString = sharedPreferences.getString("lastDrawing", null)
        val revertedBitmap = convertedString?.let {
            binding.drawingView.revertBase64toBitmap(
                it
            )
        }
        val emptyBitmap = revertedBitmap?.width?.let {
            Bitmap.createBitmap(
                it,
                revertedBitmap.height,
                revertedBitmap.config
            )
        }

        if (revertedBitmap != null) {
            if (!revertedBitmap.sameAs(emptyBitmap)) {
                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setMessage("Want to restore your last drawing?")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { _, _ ->
                        binding.drawingView.restoreLastDrawing(revertedBitmap)
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        // Dismiss the dialog
                        dialog.dismiss()
                    }
                val alert = builder.create()
                alert.show()
            }
        }

        activateReviewInfo()




    }

    override fun onStart() {
        loadRewardedAd()
        loadInterstitialAd()
        super.onStart()
        if (!isConnected()) {
            Toast.makeText(
                this@MainActivity,
                "To access more colors, brushes and erasers, connect to the Internet!",
                Toast.LENGTH_LONG
            )
                .show()
        }
    }

    override fun onRestart() {
        super.onRestart()
        loadRewardedAd()
        loadInterstitialAd()
    }

    override fun onPause() {
        super.onPause()
        val sharedPref: SharedPreferences = getSharedPreferences("sharedPref", MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPref.edit()
        val drawingView: DrawingView =
            findViewById(R.id.drawing_view)
        val bitmapToString = getBitmapFromDrawingView(drawingView)
        val convertedString = convertBitmapToBase64(bitmapToString)
        editor.apply {
            if (convertedString != null) {
                putString("lastDrawing", convertedString)
            }
            apply()
        }
    }

    private fun convertBitmapToBase64(bm: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

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
            resultLauncher.launch(pickPhotoIntent)
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
            binding.apply {
                viewFinder.visibility = View.INVISIBLE
                btnTakePhoto.visibility = View.INVISIBLE
                drawingView.visibility = View.VISIBLE
                ivBackground.visibility = View.VISIBLE
            }
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
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setMessage("Do you want to delete the background?")
                .setCancelable(false)
                .setPositiveButton("Yes") { _, _ ->
                    binding.ivBackground.setImageURI(null)
                }
                .setNegativeButton("No") { dialog, _ ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
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

        brushDialog.setContentView(dialogBindingWithMoreSizesBtn.root)

        if (!mAdShowedForBrush) {
            liveData.observe(this@MainActivity) {
                if (it == false) {
                    dialogBindingWithMoreSizesBtn.btnMoreSizes.visibility = View.VISIBLE
                    dialogBindingWithMoreSizesBtn.progressBar.visibility = View.INVISIBLE
                } else {
                    dialogBindingWithMoreSizesBtn.btnMoreSizes.visibility = View.INVISIBLE
                    dialogBindingWithMoreSizesBtn.progressBar.visibility = View.VISIBLE
                }
            }
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

        val dialogBindingEraserWithButtons: DialogEraserSizeBinding =
            DialogEraserSizeBinding.inflate(layoutInflater)

        val dialogBindingEraserWithMoreSizesOnly: DialogEraserSizeWithMoreSizesBinding =
            DialogEraserSizeWithMoreSizesBinding.inflate(layoutInflater)

        val dialogBindingEraserWithMaxSizesOnly: DialogEraserSizeWithMaxSizeBinding =
            DialogEraserSizeWithMaxSizeBinding.inflate(layoutInflater)

        val dialogBindingEraserFull: DialogEraserSizeFullBinding =
            DialogEraserSizeFullBinding.inflate(layoutInflater)

        brushDialog.setContentView(dialogBindingEraserWithButtons.root)

        if (!mAdShowedForEraser && !mAdShowedForMaxSize) {

            liveData.observe(this@MainActivity) {
                if (it == false) {
                    dialogBindingEraserWithButtons.btnMoreSizes.visibility = View.VISIBLE
                    dialogBindingEraserWithButtons.btnClearAll.visibility = View.VISIBLE
                    dialogBindingEraserWithButtons.progressBar.visibility = View.INVISIBLE
                } else {
                    dialogBindingEraserWithButtons.btnMoreSizes.visibility = View.INVISIBLE
                    dialogBindingEraserWithButtons.btnClearAll.visibility = View.INVISIBLE
                    dialogBindingEraserWithButtons.progressBar.visibility = View.VISIBLE
                }
            }
        } else if (!mAdShowedForEraser && mAdShowedForMaxSize) {
            brushDialog.setContentView(dialogBindingEraserWithMaxSizesOnly.root)
        } else if (mAdShowedForEraser && !mAdShowedForMaxSize) {
            brushDialog.setContentView(dialogBindingEraserWithMoreSizesOnly.root)
        } else {
            brushDialog.setContentView(dialogBindingEraserFull.root)
        }

        dialogBindingEraserWithButtons.apply {

            btnMoreSizes.setOnClickListener {
                showRewardedVideo()
                if (mRewardedAd != null) {
                    mRewardedAd?.show(
                        this@MainActivity,
                        OnUserEarnedRewardListener() {
                            if (!mAdShowedForMaxSize && !mAdShowedForEraser) {
                                brushDialog.setContentView(dialogBindingEraserWithMoreSizesOnly.root)
                                Log.d("TAG", "User earned the reward.")
                                mAdShowedForEraser = true
                            }

//                    fun onUserEarnedReward(rewardItem: RewardItem) {
//                        var rewardAmount = rewardItem.amount
//                        //addCoins(rewardAmount)
//                    }
                        }
                    )
                }
            }

            btnClearAll.setOnClickListener {
                showRewardedVideo()
                if (mRewardedAd != null) {
                    mRewardedAd?.show(
                        this@MainActivity,
                        OnUserEarnedRewardListener() {
                            if (!mAdShowedForMaxSize && !mAdShowedForEraser) {
                                brushDialog.setContentView(dialogBindingEraserWithMaxSizesOnly.root)
                                Log.d("TAG", "User earned the reward.")
                                mAdShowedForMaxSize = true
                            }

//                    fun onUserEarnedReward(rewardItem: RewardItem) {
//                        var rewardAmount = rewardItem.amount
//                        //addCoins(rewardAmount)
//                    }
                        }
                    )
                }
            }
        }

        dialogBindingEraserWithMoreSizesOnly.apply {
            btnClearAll.setOnClickListener {
                showRewardedVideo()
                if (mRewardedAd != null) {
                    mRewardedAd?.show(
                        this@MainActivity,
                        OnUserEarnedRewardListener() {
                            brushDialog.setContentView(dialogBindingEraserFull.root)
                            Log.d("TAG", "User earned the reward.")
                            mAdShowedForMaxSize = true


//                    fun onUserEarnedReward(rewardItem: RewardItem) {
//                        var rewardAmount = rewardItem.amount
//                        //addCoins(rewardAmount)
//                    }
                        }
                    )
                }
            }
        }

        dialogBindingEraserWithMaxSizesOnly.apply {
            btnMoreSizes.setOnClickListener {
                showRewardedVideo()
                if (mRewardedAd != null) {
                    mRewardedAd?.show(
                        this@MainActivity,
                        OnUserEarnedRewardListener() {
                            brushDialog.setContentView(dialogBindingEraserFull.root)
                            Log.d("TAG", "User earned the reward.")
                            mAdShowedForEraser = true


//                    fun onUserEarnedReward(rewardItem: RewardItem) {
//                        var rewardAmount = rewardItem.amount
//                        //addCoins(rewardAmount)
//                    }
                        }
                    )
                }
            }
        }

        dialogBindingEraserWithButtons.apply {
            ibSmallBrush.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(4.toFloat())
                brushDialog.dismiss()
            }


            ibMediumBrush.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(10.toFloat())
                brushDialog.dismiss()
            }


            ibLargeBrush.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(20.toFloat())
                brushDialog.dismiss()
            }
        }

        dialogBindingEraserWithMoreSizesOnly.apply {
            ib2f.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(2.toFloat())
                brushDialog.dismiss()
            }


            ib6f.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(6.toFloat())
                brushDialog.dismiss()
            }


            ib10f.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(10.toFloat())
                brushDialog.dismiss()
            }


            ib12f.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(12.toFloat())
                brushDialog.dismiss()
            }


            ib16f.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(16.toFloat())
                brushDialog.dismiss()
            }


            ib20f.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(20.toFloat())
                brushDialog.dismiss()
            }


            ib22f.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(22.toFloat())
                brushDialog.dismiss()
            }


            ib26f.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(26.toFloat())
                brushDialog.dismiss()
            }


            ib30f.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(30.toFloat())
                brushDialog.dismiss()
            }

        }


        dialogBindingEraserWithMaxSizesOnly.apply {
            ibSmallBrush.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(4.toFloat())
                brushDialog.dismiss()
            }


            ibMediumBrush.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(10.toFloat())
                brushDialog.dismiss()
            }


            ibLargeBrush.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(20.toFloat())
                brushDialog.dismiss()
            }


            btnClearAll.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(999999.toFloat())
                brushDialog.dismiss()
            }
        }

        dialogBindingEraserFull.apply {
            ib2f.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(2.toFloat())
                brushDialog.dismiss()
            }


            ib6f.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(6.toFloat())
                brushDialog.dismiss()
            }


            ib10f.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(10.toFloat())
                brushDialog.dismiss()
            }


            ib12f.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(12.toFloat())
                brushDialog.dismiss()
            }


            ib16f.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(16.toFloat())
                brushDialog.dismiss()
            }


            ib20f.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(20.toFloat())
                brushDialog.dismiss()
            }


            ib22f.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(22.toFloat())
                brushDialog.dismiss()
            }


            ib26f.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(26.toFloat())
                brushDialog.dismiss()
            }


            ib30f.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(30.toFloat())
                brushDialog.dismiss()
            }

            btnClearAll.setOnClickListener {
                binding.drawingView.onClickEraser(true)
                binding.drawingView.setSizeForEraser(999999.toFloat())
                brushDialog.dismiss()
            }

        }

        brushDialog.show()
    }


    private fun colorPicker() {
        val colorPickerDialog = Dialog(this@MainActivity)
        colorPickerDialog.setTitle(R.string.choose_the_color)

        val dialogBindingRewarded: DialogColorPickerRewardedBinding =
            DialogColorPickerRewardedBinding.inflate(layoutInflater)

        val dialogBindingWithMoreColorsBtn: DialogColorPickerBinding =
            DialogColorPickerBinding.inflate(layoutInflater)

        colorPickerDialog.setContentView(dialogBindingWithMoreColorsBtn.root)

        if (!mAdShowedForColors) {

            liveData.observe(this@MainActivity) {
                if (it == false) {
                    dialogBindingWithMoreColorsBtn.btnMoreColors.visibility = View.VISIBLE
                    dialogBindingWithMoreColorsBtn.progressBar.visibility = View.INVISIBLE
                } else {
                    dialogBindingWithMoreColorsBtn.btnMoreColors.visibility = View.INVISIBLE
                    dialogBindingWithMoreColorsBtn.progressBar.visibility = View.VISIBLE
                }
            }
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
                    binding.ivBackground.setImageURI(savedUri)
                    cameraExecutor.shutdownNow() // ?????????
                    binding.viewFinder.visibility = View.INVISIBLE
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(Constants.TAG, "onError: ${exception.message}", exception)
                }
            }
        )
    }

    private fun cameraPermissionGranted(): Boolean =
        Constants.CAMERA_PERMISSION.all {
            ContextCompat.checkSelfPermission(
                baseContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun isReadStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return result == PackageManager.PERMISSION_GRANTED
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                uploadLaunch()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Oops, you denied permission for the storage! Please allow it from settings!",
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
                    "Oops, you denied permission for the storage! Please allow it from settings!",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }

        if (requestCode == DOWNLOAD_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showInterstitial()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Oops, you denied permission for the storage! Please allow it from settings!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        if (requestCode == SHARE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                lifecycleScope.launch {
                    val flDrawingView: FrameLayout =
                        findViewById(R.id.flBackgroundAndDrawingViewContainer)
                    saveBitmapFileToShare(getBitmapFromView(flDrawingView))
                }
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Oops, you denied permission for the storage! Please allow it from settings!",
                    Toast.LENGTH_LONG
                ).show()
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

        binding.btnTakePhoto.setOnClickListener {
            takePhoto()
            binding.btnTakePhoto.visibility = View.INVISIBLE
            cameraProvider.unbind(preview)
            binding.drawingView.visibility = View.VISIBLE
        }
        mCameraLaunched = true
    }


    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                if (result!!.data != null) {
                    binding.ivBackground.visibility = View.VISIBLE
                    binding.ivBackground.setImageURI(result.data?.data)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


    override fun onClick(position: Int) {
        when (position) {
            0 -> {
                cameraLaunch()
                loadRewardedAd()
                loadInterstitialAd()
            }
            1 -> {
                uploadLaunch()
                loadRewardedAd()
                loadInterstitialAd()
            }
            2 -> {
                loadRewardedAd()
                loadInterstitialAd()
                brushSizeChooseDialog()
            }
            3 -> {
                loadRewardedAd()
                loadInterstitialAd()
                colorPicker()
            }
            4 -> {
                binding.drawingView.onClickUndo()
                loadRewardedAd()
                loadInterstitialAd()
            }
            5 -> {
                binding.drawingView.onClickRedo()
                loadRewardedAd()
                loadInterstitialAd()
            }
            6 -> {
                loadRewardedAd()
                loadInterstitialAd()
                eraserSizeChooseDialog()
            }
            7 -> {
                onClickBin()
                loadRewardedAd()
                loadInterstitialAd()
            }
            8 -> {
                val bottomSheetFragment = BottomSheetFragment()
                if(!isFragmentShown) {
                    bottomSheetFragment.show(supportFragmentManager, tag)
                }
                loadRewardedAd()
                loadInterstitialAd()
            }
            9 -> {
                loadRewardedAd()
                loadInterstitialAd()
                if (isReadStorageAllowed()) {
                    showInterstitial()
                } else {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        UPLOAD_PERMISSION, DOWNLOAD_PERMISSION_CODE
                    )
                }
            }
            10 -> {
                loadRewardedAd()
                loadInterstitialAd()
                if (isReadStorageAllowed()) {
                    lifecycleScope.launch {
                        val flDrawingView: FrameLayout =
                            findViewById(R.id.flBackgroundAndDrawingViewContainer)
                        saveBitmapFileToShare(getBitmapFromView(flDrawingView))
                    }
                } else {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        UPLOAD_PERMISSION, SHARE_PERMISSION_CODE
                    )
                }
            }
            11 -> {
                startReviewFlow()
            }
            12 -> {
                val privacyPolicyBottomSheetFragment = PrivacyPolicyBottomSheetFragment()
                if(!isFragmentShown) {
                    privacyPolicyBottomSheetFragment.show(supportFragmentManager, tag)
                }
            }
        }
    }

    private fun loadRewardedAd() {
        if (mRewardedAd == null) {
            val adRequest = AdRequest.Builder().build()

            RewardedAd.load(
                this@MainActivity, REWARDED_AD_UNIT_ID, adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d(TAG, adError.message)
                        mIsLoading = false
                        mRewardedAd = null
                        loadRewardedAd()
                    }

                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        Log.d(TAG, "Ad was loaded.")
                        mRewardedAd = rewardedAd
                        mIsLoading = false
                        liveData.postValue(mIsLoading)
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
                    // Don't forget to set the ad reference to null so you don't show the ad a second time.
                    mRewardedAd = null
                    loadRewardedAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "Ad failed to show.")
                    // Don't forget to set the ad reference to null so you don't show the ad a second time.
                    mRewardedAd = null
                    loadRewardedAd() //???????????
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.")
                    // Called when ad is shown.
                    mRewardedAd = null //???????????
                    loadRewardedAd()   //???????????
                }
            }
        }
    }

    private fun loadInterstitialAd() {
        if (mInterstitialAd == null) {
            val adRequest = AdRequest.Builder().build()

            InterstitialAd.load(
                this, INTERSTITIAL_AD_UNIT_ID, adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d(TAG, adError.message)
                        mInterstitialAd = null
                        mAdIsLoading = false
                        loadInterstitialAd()
                    }

                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        Log.d(TAG, "Ad was loaded.")
                        mInterstitialAd = interstitialAd
                        mAdIsLoading = false
                    }
                }
            )
        }
    }


    private fun showInterstitial() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad was dismissed.")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    mInterstitialAd = null
                    loadInterstitialAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "Ad failed to show.")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    mInterstitialAd = null
                    loadInterstitialAd() //?????????
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.")
                    // Called when ad is showed.

                    lifecycleScope.launch {
                        val flDrawingView: FrameLayout =
                            findViewById(R.id.flBackgroundAndDrawingViewContainer)
                        saveBitmapFile(getBitmapFromView(flDrawingView))
                    }

                    mInterstitialAd = null //?????????
                    loadInterstitialAd() //?????????
                }
            }
            mInterstitialAd?.show(this)
        } else {
            if (isReadStorageAllowed()) {
                lifecycleScope.launch {
                    val flDrawingView: FrameLayout =
                        findViewById(R.id.flBackgroundAndDrawingViewContainer)
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }
            loadInterstitialAd() //?????????
        }
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(
            view.width,
            view.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private fun getBitmapFromDrawingView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(
            view.width,
            view.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(returnedBitmap)
        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
                    val f = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                            .toString()
                                + File.separator + "HandyPaints_" + System.currentTimeMillis() / 1000 + ".png"
                    )

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread {
                        if (result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File successfully saved: $result",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong while saving the file.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private suspend fun saveBitmapFileToShare(mBitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
                    val f = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                            .toString()
                                + File.separator + "HandyPaints_" + System.currentTimeMillis() / 1000 + ".png"
                    )

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread {
                        if (result.isNotEmpty()) {
                            shareImage(result)
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong while sharing the file.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun shareImage(result: String) {
        MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null) { path, uri ->
            val shareIntent = Intent()
            shareIntent.apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/png"
                startActivity(Intent.createChooser(shareIntent, "Share"))
            }
        }
    }

    private fun checkConnection() {
        val connectivity = CheckConnectivity(application)
        connectivity.observe(this@MainActivity) { isConnected ->
            if (isConnected) {
                loadRewardedAd()
                loadInterstitialAd()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "To access more colors, brushes and erasers, connect to the Internet!",
                    Toast.LENGTH_LONG
                )
                    .show()
            }
        }
    }

    private fun isConnected(): Boolean {
        var connected = false
        try {
            val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val nInfo = cm.activeNetwork
            connected = nInfo != null
            return connected
        } catch (e: Exception) {
            Log.e("Connectivity Exception", e.message!!)
        }
        return connected
    }

    private fun activateReviewInfo() {
//        manager = ReviewManagerFactory.create(this@MainActivity)
        manager = FakeReviewManager(this@MainActivity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // We got the ReviewInfo object
                reviewInfo = task.result

                Log.d("aaaaaaaaaaaaaaaaa", reviewInfo.toString())

            } else {
                // There was some problem, log or handle the error code.
//                @ReviewErrorCode val reviewErrorCode = (task.exception as TaskException).errorCode
            }
        }
    }

    private fun startReviewFlow() {
        val flow = manager.launchReviewFlow(this@MainActivity, reviewInfo)
        flow.addOnCompleteListener { _ ->
            // The flow has finished. The API does not indicate whether the user
            // reviewed or not, or even whether the review dialog was shown. Thus, no
            // matter the result, we continue our app flow.

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





