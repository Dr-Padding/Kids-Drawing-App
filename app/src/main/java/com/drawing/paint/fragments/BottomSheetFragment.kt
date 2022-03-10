package com.drawing.paint.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.drawing.paint.R
import com.drawing.paint.adapters.ViewPagerAdapter
import com.drawing.paint.databinding.BottomSheetFragmentBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class BottomSheetFragment: BottomSheetDialogFragment() {

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
            R.drawable.img1,
            R.drawable.img2,
            R.drawable.img3
        )

        val adapter = ViewPagerAdapter(gifsList)
        binding!!.pager.adapter = adapter

    }


    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

}