package com.livescore.football.livescores.footballscores.ui.onboarding.fragment

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.base.AbsBaseFragment
import com.livescore.football.livescores.footballscores.databinding.FragmentIntro3Binding


class FragmentIntro3 : AbsBaseFragment<FragmentIntro3Binding?>() {
    override fun getLayout(): Int {
        return R.layout.fragment_intro3
    }

    override fun initView() {
        val viewPager = requireActivity().findViewById<ViewPager2>(R.id.viewPager)
        binding!!.txtNext.setOnClickListener(View.OnClickListener { view: View? ->
            viewPager.setCurrentItem(4)
        })
    }
}
