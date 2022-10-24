package painting.drawing.nft

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback


const val REWARDED_AD_UNIT_ID = "ca-app-pub-7754594115862131/1574911988"

class AdMobActivity(context: Context) {

    private var mIsLoading = false
    var mRewardedAd: RewardedAd? = null
    private var TAG = "AdMobActivity"
    private var _liveData = MutableLiveData<Boolean>()
    val liveData: MutableLiveData<Boolean> = _liveData

    private var _liveData2 = MutableLiveData<Boolean>()
    val liveData2: MutableLiveData<Boolean> = _liveData2

    private var _liveData3 = MutableLiveData<Boolean>()
    val liveData3: MutableLiveData<Boolean> = _liveData3

    var adLoaded = false
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
                        Log.d(TAG, loadAdError.message)
                        mRewardedAd = null
                        mIsLoading = false
                        adLoaded = false
                        _liveData3.postValue(adLoaded)
                        loadRewardedAd(context)
                    }

                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        mRewardedAd = rewardedAd
                        Log.d(TAG, "onAdLoaded")
                        mIsLoading = false
                        _liveData.postValue(mIsLoading)
                        adLoaded = true
                        _liveData3.postValue(adLoaded)
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
                    loadRewardedAd(context) //??
                }
            }
        }
    }

    fun rewardItem(context: Context) {
        if (mRewardedAd != null) {
            mRewardedAd?.show(
                context as Activity
            ) {
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