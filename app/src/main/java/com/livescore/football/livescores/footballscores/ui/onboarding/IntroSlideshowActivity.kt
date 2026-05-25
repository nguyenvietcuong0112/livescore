package com.livescore.football.livescores.footballscores.ui.onboarding

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.databinding.ActivityIntroSlideshowBinding
import dagger.hilt.android.AndroidEntryPoint

data class IntroSlide(
    val title: String,
    val description: String,
    val imageResId: Int
)

@AndroidEntryPoint
class IntroSlideshowActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIntroSlideshowBinding
    private lateinit var slides: List<IntroSlide>
    private lateinit var adapter: IntroSlideshowAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroSlideshowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSlidesData()
        setupViewPager()
        setupListeners()
    }

    private fun setupSlidesData() {
        slides = listOf(
            IntroSlide(
                title = "TỈ SỐ & THỐNG KÊ TRỰC TIẾP",
                description = "Cập nhật diễn biến bóng đá nhanh nhất hành tinh với dữ liệu chi tiết từng giây, tỉ số trực tiếp & sơ đồ trực quan.",
                imageResId = R.drawable.intro_slide_1
            ),
            IntroSlide(
                title = "LỊCH THI ĐẤU & BẢNG XẾP HẠNG",
                description = "Theo dõi vị trí của các câu lạc bộ, bảng xếp hạng cập nhật tức thời và lịch thi đấu trọn vẹn mùa giải 2024-2026.",
                imageResId = R.drawable.intro_slide_2
            ),
            IntroSlide(
                title = "NHẮC NHỞ & BÁO THỨC TRẬN ĐẤU",
                description = "Nhận cảnh báo thông báo thông minh trước giờ bóng lăn 5 phút để bạn không bỏ lỡ bất kỳ khoảnh khắc ghi bàn nào.",
                imageResId = R.drawable.intro_slide_3
            ),
            IntroSlide(
                title = "CÚP THẾ GIỚI WORLD CUP 2026",
                description = "Đón đầu lễ hội bóng đá đỉnh cao với bảng đấu, tin tức & đếm ngược sự kiện tại Mỹ, Canada & Mexico.",
                imageResId = R.drawable.intro_slide_4
            )
        )
    }

    private fun setupViewPager() {
        adapter = IntroSlideshowAdapter(slides)
        binding.vpSlideshow.adapter = adapter

        // Register page change callbacks to update capsule dots indicators
        binding.vpSlideshow.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(position)
                updateButtonState(position)
            }
        })
    }

    private fun setupListeners() {
        binding.btnNextSlide.setOnClickListener {
            val current = binding.vpSlideshow.currentItem
            if (current < 3) {
                binding.vpSlideshow.currentItem = current + 1
            } else {
                // Launch IntroActivity (the 3 onboarding questions)
                val intent = Intent(this, IntroActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun updateIndicators(position: Int) {
        val activeWidth = dpToPx(32)
        val inactiveWidth = dpToPx(16)

        val activeColor = ContextCompat.getColor(this, R.color.neon_lime_green)
        val inactiveColor = ContextCompat.getColor(this, R.color.inactive_indicator_grey)

        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)

        dots.forEachIndexed { index, card ->
            val layoutParams = card.layoutParams
            if (index == position) {
                layoutParams.width = activeWidth
                card.setCardBackgroundColor(ColorStateList.valueOf(activeColor))
            } else {
                layoutParams.width = inactiveWidth
                card.setCardBackgroundColor(ColorStateList.valueOf(inactiveColor))
            }
            card.layoutParams = layoutParams
        }
    }

    private fun updateButtonState(position: Int) {
        if (position == 3) {
            binding.btnNextSlide.text = getString(R.string.intro_start)
            binding.btnNextSlide.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.neon_lime_green)
            )
            binding.btnNextSlide.setTextColor(ContextCompat.getColor(this, R.color.bg_dark))
            binding.btnNextSlide.strokeWidth = 0
        } else {
            binding.btnNextSlide.text = getString(R.string.intro_next)
            binding.btnNextSlide.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, android.R.color.transparent)
            )
            binding.btnNextSlide.setTextColor(ContextCompat.getColor(this, R.color.text_white))
            binding.btnNextSlide.strokeColor = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.neon_lime_green)
            )
            binding.btnNextSlide.strokeWidth = dpToPx(1f).toInt()
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}

// ==========================================
// ViewPager2 ListAdapter definition
// ==========================================

class IntroSlideshowAdapter(
    private val list: List<IntroSlide>
) : RecyclerView.Adapter<IntroSlideshowAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_intro_slide, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage = view.findViewById<ImageView>(R.id.ivSlideImage)
        val tvTitle = view.findViewById<TextView>(R.id.tvSlideTitle)
        val tvDesc = view.findViewById<TextView>(R.id.tvSlideDescription)

        fun bind(slide: IntroSlide) {
            tvTitle.text = slide.title
            tvDesc.text = slide.description
            ivImage.setImageResource(slide.imageResId)
        }
    }
}
