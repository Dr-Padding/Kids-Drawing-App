package painting.drawing.nft.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import painting.drawing.nft.MainActivity
import com.drawing.paint.R
import painting.drawing.nft.adapters.ViewPagerAdapter
import com.drawing.paint.databinding.BottomSheetFragmentBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class BottomSheetFragment : BottomSheetDialogFragment() {

    private var binding: BottomSheetFragmentBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = BottomSheetFragmentBinding.bind(view)


        var gifsList = mutableListOf(
            R.drawable.gif_1,
            R.drawable.gif_2,
            R.drawable.gif_3,
            R.drawable.gif_4,
            R.drawable.gif_5,
            R.drawable.gif_6,
            R.drawable.gif_7,
            R.drawable.gif_8,
            R.drawable.gif_9,
            R.drawable.gif_10
        )

        val adapter = ViewPagerAdapter(gifsList)
        binding!!.apply {
            pager.adapter = adapter
            pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    adapter.notifyItemChanged(position)
                }
            })
        }
    }

    override fun onStart() {
        super.onStart()
        (activity as MainActivity).isFragmentShown = true
    }

    override fun onPause() {
        super.onPause()
        (activity as MainActivity).isFragmentShown = false
    }


    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

}