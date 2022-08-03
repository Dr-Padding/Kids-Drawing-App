package com.drawing.paint

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback


const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

class AdMobActivity(context: Context) {

    private var mIsLoading = false
    var mRewardedAd: RewardedAd? = null
    private var TAG = "AdMobActivity"
    private var _liveData = MutableLiveData<Boolean>()
    val liveData : MutableLiveData<Boolean> = _liveData

    private var _liveData2 = MutableLiveData<Boolean>()
    val liveData2 : MutableLiveData<Boolean> = _liveData2

    var showed = false


    fun loadRewardedAd(context: Context) {
        if (mRewardedAd == null) {
            mIsLoading = true
            val adRequest: AdRequest = AdRequest.Builder().build()
            RewardedAd.load(
                context.applicationContext,
                REWARDED_AD_UNIT_ID,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Handle the error.
                        Log.d(TAG, loadAdError.message)
                        mRewardedAd = null
                        mIsLoading = false
                        Toast.makeText(context.applicationContext, "onAdFailedToLoad", Toast.LENGTH_SHORT)
                            .show()
                        loadRewardedAd(context)
                    }

                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        mRewardedAd = rewardedAd
                        Log.d(TAG, "onAdLoaded")
                        mIsLoading = false
                        Toast.makeText(context.applicationContext, "onAdLoaded", Toast.LENGTH_SHORT).show()
                        _liveData.postValue(mIsLoading)
                    }
                })
        }
    }


    fun showRewardedVideo(context: Context) {
        if (mRewardedAd != null) {
            mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Don't forget to set the ad reference to null so you don't show the ad a second time.
                    mRewardedAd = null
                    loadRewardedAd(context)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Don't forget to set the ad reference to null so you don't show the ad a second time.
                    mRewardedAd = null
                    loadRewardedAd(context) //??
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    mRewardedAd = null //??
                    loadRewardedAd(context)   //??
                }
            }
        }
    }

    fun rewardItem(context: Context) {
        if (mRewardedAd != null) {
            mRewardedAd?.show(context as Activity) {
                showed = true
                _liveData.postValue(showed)
            }
        } else {
            showed = false
            _liveData.postValue(showed)
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
        }
    }

    fun rewardItem2(context: Context) {
        if (mRewardedAd != null) {
            mRewardedAd?.show(context as Activity) {
                showed = true
                _liveData2.postValue(showed)
            }
        } else {
            showed = false
            _liveData2.postValue(showed)
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
        }
    }



}