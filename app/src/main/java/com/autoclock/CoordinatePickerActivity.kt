package com.autoclock

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.autoclock.databinding.ActivityCoordinatePickerBinding

class CoordinatePickerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TARGET = "com.autoclock.EXTRA_TARGET"
        const val TARGET_OPEN_APP = "open_app"
        const val TARGET_CLOCK_BUTTON = "clock_button"
        const val TARGET_AFTER_CLOCK = "after_clock"

        private const val REQUEST_PICK_SCREENSHOT = 1001
        private const val MAX_PREVIEW_IMAGE_SIZE = 2_048
    }

    private lateinit var binding: ActivityCoordinatePickerBinding
    private lateinit var prefs: Prefs
    private lateinit var target: String
    private var imageWidth = 0
    private var imageHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityCoordinatePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)
        target = intent.getStringExtra(EXTRA_TARGET) ?: TARGET_OPEN_APP

        setupTexts()
        setupButtons()
        setupScreenshotTouch()
    }

    override fun onDestroy() {
        binding.ivScreenshot.setImageDrawable(null)
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PICK_SCREENSHOT || resultCode != Activity.RESULT_OK) {
            return
        }

        val uri = data?.data
        if (uri == null) {
            toast("未选择截图")
            return
        }

        loadScreenshot(uri)
    }

    private fun setupTexts() {
        val title = when (target) {
            TARGET_CLOCK_BUTTON -> "点选任务按钮位置"
            TARGET_AFTER_CLOCK  -> "点选任务完成后点击的位置"
            else                -> "点选桌面快捷方式位置"
        }
        val help = when (target) {
            TARGET_CLOCK_BUTTON -> "请选择目标 App任务页面截图，然后点击截图中的任务按钮。"
            TARGET_AFTER_CLOCK  -> "请选择手机桌面截图，然后点击截图中任务完成后要点击的图标（如后续 App）。"
            else                -> "请选择手机桌面截图，然后点击截图中的目标 App快捷方式。"
        }
        binding.tvPickerTitle.text = title
        binding.tvPickerHelp.text = help
    }

    private fun setupButtons() {
        binding.btnChooseScreenshot.setOnClickListener { chooseScreenshot() }
        binding.btnCancelPicker.setOnClickListener { finish() }
    }

    private fun setupScreenshotTouch() {
        binding.ivScreenshot.setOnTouchListener { _, event ->
            if (event.action != MotionEvent.ACTION_UP) {
                return@setOnTouchListener true
            }

            val coordinate = CoordinateMapper.mapFitCenterTapToRatio(
                touchX = event.x,
                touchY = event.y,
                viewWidth = binding.ivScreenshot.width,
                viewHeight = binding.ivScreenshot.height,
                imageWidth = imageWidth,
                imageHeight = imageHeight
            )
            if (coordinate == null) {
                toast("请点击截图图片范围内的位置")
                return@setOnTouchListener true
            }

            saveCoordinate(coordinate)
            true
        }
    }

    private fun chooseScreenshot() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, REQUEST_PICK_SCREENSHOT)
    }

    private fun loadScreenshot(uri: Uri) {
        try {
            val mimeType = contentResolver.getType(uri)
            if (mimeType?.startsWith("image/") == false) {
                toast("请选择图片文件")
                return
            }

            val bounds = decodeImageBounds(uri)
            if (bounds == null) {
                toast("无法读取这张截图，请重新选择")
                return
            }

            val bitmap = decodePreviewBitmap(uri, bounds.outWidth, bounds.outHeight)
            if (bitmap == null) {
                toast("无法读取这张截图，请重新选择")
                return
            }

            imageWidth = bounds.outWidth
            imageHeight = bounds.outHeight
            binding.ivScreenshot.setImageBitmap(bitmap)
            binding.tvSelectedCoordinate.text = "截图已载入，请点击目标位置"
        } catch (e: Exception) {
            toast("读取截图失败，请重新选择")
        } catch (e: OutOfMemoryError) {
            toast("截图过大，请选择普通手机截图")
        }
    }

    private fun decodeImageBounds(uri: Uri): BitmapFactory.Options? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return null
        }
        return options
    }

    private fun decodePreviewBitmap(uri: Uri, width: Int, height: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(width, height)
        }
        return contentResolver.openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > MAX_PREVIEW_IMAGE_SIZE || height / sampleSize > MAX_PREVIEW_IMAGE_SIZE) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun saveCoordinate(coordinate: CoordinateRatio) {
        when (target) {
            TARGET_CLOCK_BUTTON -> {
                prefs.clockBtnX = coordinate.xRatio
                prefs.clockBtnY = coordinate.yRatio
            }
            TARGET_AFTER_CLOCK -> {
                prefs.afterClockX = coordinate.xRatio
                prefs.afterClockY = coordinate.yRatio
            }
            else -> {
                prefs.openAppX = coordinate.xRatio
                prefs.openAppY = coordinate.yRatio
            }
        }

        val xPercent = formatPercent(coordinate.xRatio)
        val yPercent = formatPercent(coordinate.yRatio)
        binding.tvSelectedCoordinate.text = "已选择：X ${xPercent}%，Y ${yPercent}%"
        toast("坐标已保存：X ${xPercent}%，Y ${yPercent}%")
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun formatPercent(ratio: Float): String {
        val percent = ratio * 100f
        return String.format("%.1f", percent).trimEnd('0').trimEnd('.')
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
