package com.livescore.football.livescores.footballscores.ui.onboarding


import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.base.AbsBaseActivity
import com.livescore.football.livescores.footballscores.databinding.ActivityIntroSlideshowBinding
import com.livescore.football.livescores.footballscores.ui.onboarding.fragment.*
import com.livescore.football.livescores.footballscores.utils.SystemConfiguration
import com.livescore.football.livescores.footballscores.utils.SystemUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IntroSlideshowActivity : AbsBaseActivity() {

    @javax.inject.Inject
    lateinit var limitManager: com.livescore.football.livescores.footballscores.data.local.RequestLimitManager

    private lateinit var binding: ActivityIntroSlideshowBinding

    override fun bind() {
        SystemUtil.setLocale(this)
        SystemConfiguration.setStatusBarColor(
            this,
            R.color.transparent,
            SystemConfiguration.IconColor.ICON_LIGHT
        )

        binding = ActivityIntroSlideshowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                }
            }
        )

        val fragmentList = ArrayList<Fragment>()

        fragmentList.add(FragmentIntro1())
        fragmentList.add(FragmentIntro2())
        fragmentList.add(FragmentIntro2ads())
        fragmentList.add(FragmentIntro3())
        fragmentList.add(FragmentIntro4())

        val adapter = ViewIntroAdapter(
            this,
            fragmentList,
            supportFragmentManager,
            lifecycle
        )

        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 2

        binding.viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    if (position == 2 && ::limitManager.isInitialized && limitManager.isPremium()) {
                        binding.viewPager.setCurrentItem(3, false)
                    }
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}