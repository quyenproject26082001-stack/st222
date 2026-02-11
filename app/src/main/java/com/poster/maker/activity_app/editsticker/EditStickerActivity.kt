package poster.maker.activity_app.editsticker

import android.R.attr.bitmap
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.ocmaker.pixcel.maker.data.model.draw.Draw
import com.ocmaker.pixcel.maker.data.model.draw.DrawableDraw
import com.piratemaker.postermaker.listener.listenerdraw.OnDrawListener
import poster.maker.R
import poster.maker.dialog.YesNoDialog
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AlertDialog
import com.lvt.ads.util.Admob
import poster.maker.core.base.BaseActivity
import poster.maker.core.extensions.gone
import poster.maker.core.extensions.setOnSingleClick
import poster.maker.core.extensions.showInterAll
import poster.maker.core.extensions.visible
import poster.maker.core.helper.AssetHelper
import poster.maker.core.helper.BitmapHelper
import poster.maker.databinding.ActivityEditStickerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

class EditStickerActivity : BaseActivity<ActivityEditStickerBinding>() {


    private var touchedAnyDraw = false

    private var isEditingExisting: Boolean = false
    var currentDraw: Draw? = null

    var drawViewList: ArrayList<Draw> = arrayListOf()
    private lateinit var categoryAdapter: StickerCategoryAdapter

    private lateinit var stickerAdapter: StickerItemAdapter
    private var currentImagePath: String = ""
    private var currentCategoryId: Int = 1

    override fun setViewBinding(): ActivityEditStickerBinding {
        return ActivityEditStickerBinding.inflate(LayoutInflater.from(this))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initView() {
        // Get image path from intent
        currentImagePath = intent.getStringExtra("IMAGE_PATH") ?: ""

        isEditingExisting = intent.getBooleanExtra("IS_EDITING_EXISTING", false)
        // Load background image
        if (currentImagePath.isNotEmpty()) {
            Glide.with(this)
                .load(File(currentImagePath))
                .signature(com.bumptech.glide.signature.ObjectKey(File(currentImagePath).lastModified()))  // ✅ THÊM DÒNG NÀY
                .into(binding.imgBackground)
        }

        // Setup canvas touch to deselect stickers

        initDrawView()

        setupCategoryNavigation()
        setupStickerGrid()
        loadStickersForCategory(1)
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()

            tvCenter.text = getString(R.string.add_sticker)
            tvCenter.gone()

            btnActionBarRight.setImageResource(R.drawable.ic_save)
            btnActionBarRight.visible()
            btnActionBarReset.visible()
            btnActionBarRightText.gone()
            tvRightText.gone()
        }
    }

    override fun viewListener() {
        binding.actionBar.apply {
            btnActionBarLeft.setOnSingleClick {
                finish()
            }

            btnActionBarRight.setOnSingleClick {
                showInterAll {  saveAndReturn() }
            }
            btnActionBarReset.setOnSingleClick {
                showResetConfirmation()            }
        }
    }

    private fun setupCategoryNavigation() {
        val categories = AssetHelper.getAllStickerCategories().map { StickerCategory(it) }

        categoryAdapter = StickerCategoryAdapter(categories, 1) { categoryId ->
            currentCategoryId = categoryId
            loadStickersForCategory(categoryId)
        }

        // Use normal horizontal LinearLayoutManager
        val layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false
        )
        binding.rvCategories.layoutManager = layoutManager
        binding.rvCategories.adapter = categoryAdapter
    }

    private fun setupStickerGrid() {
        stickerAdapter = StickerItemAdapter { stickerPath ->
            addDrawable(stickerPath)
        }

        binding.rvStickers.layoutManager = GridLayoutManager(this, 5)
        binding.rvStickers.adapter = stickerAdapter
    }

    private fun loadStickersForCategory(categoryId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val stickers = AssetHelper.getStickersByCategory(this@EditStickerActivity, categoryId)
            withContext(Dispatchers.Main) {
                stickerAdapter.updateStickers(stickers)
            }
        }
    }


    private fun saveAndReturn() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmap = withContext(Dispatchers.Main) {
                    // Deselect all stickers before capturing to avoid showing handle boxes
                    binding.drawView.hideSelect()

                    // Small delay to ensure UI updates (non-blocking)
                    // Render entire canvas to bitmap (must be on Main thread)
                    BitmapHelper.createBimapFromView(binding.flCanvas)
                }

                val fileToSave = if(isEditingExisting &&currentImagePath.isNotEmpty())
                {File(currentImagePath)}
                else{
                    File(cacheDir,"temp_edited_${System.currentTimeMillis()}.jpg")
                }
                // Save to temp file (heavy I/O on background thread)
                FileOutputStream(fileToSave).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }

                // Check if any stickers were added
                val hasStickers = binding.drawView.getStickerCount() > 0

                withContext(Dispatchers.Main) {
                    val resultIntent = Intent().apply {
                        putExtra("EDITED_IMAGE_PATH", fileToSave.absolutePath)
                        putExtra("HAS_STICKERS", hasStickers)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }
    }

    private fun addDrawable(path: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val bitmapDefault =
                Glide.with(this@EditStickerActivity)
                    .load(path)
                    .override(400, 400)
                    .submit().get().toBitmap()
            Log.d("STICKER_TYPE", "==================")
            withContext(Dispatchers.Main) {
                binding.drawView.addDraw(loadDrawableEmoji(this@EditStickerActivity, bitmapDefault))
            }

            // ← CHECK TYPE
            val currentDraw = binding.drawView.getCurrentDraw()
            Log.d("STICKER_TYPE", "isText: ${currentDraw?.isText}")
            Log.d("STICKER_TYPE", "isCharacter: ${currentDraw?.isCharacter}")

            val values = FloatArray(9)
            currentDraw?.getMatrix()?.getValues(values)
            Log.d("STICKER_TYPE", "Initial scale: ${values[Matrix.MSCALE_X]}")

        }
    }


    private fun initDrawView() {
        binding.drawView.setOnTouchListener { _, ev ->
            if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
                Log.d("DEBUG", "DrawView touched!")  // ← THÊM LOG
                touchedAnyDraw = false

                // delay cực nhỏ để OnDrawListener có cơ hội set touchedAnyDraw=true nếu hit draw
                binding.drawView.post {
                    Log.d("DEBUG", "Post run, touchedAnyDraw=$touchedAnyDraw")
                    if (!touchedAnyDraw) {
                        binding.drawView.hideSelect()
                        Log.d("DEBUG", "Deselecting, childCount=${binding.drawView.childCount}")  // ← THÊM
                        // ==> CLICK OUTSIDE (vùng trống trên canvas)
                        currentDraw = null

                        // Ẩn handle/option (tuỳ lib của bạn: gọi hàm hide option nếu có)
                        // binding.drawView.hideOptionIcon()  // nếu thư viện có

                        // Nếu bạn có custom StickerView con trong drawView:
                        for (i in 0 until binding.drawView.childCount) {
                            val child = binding.drawView.getChildAt(i)
                            Log.d("DEBUG", "Child $i: ${child::class.simpleName}")  // ← THÊM
                            val stickerView = child as? StickerView
                            if(stickerView !=null)
                            {
                                Log.d("DEBUG", "Setting selected =false for sticker $i")
                                stickerView.setStickerSelected(false)
                            }
                            else{
                                Log.d("DEBUG", "Child $i is not a StickerView")
                            }

                            (binding.drawView.getChildAt(i) as? StickerView)?.setStickerSelected(
                                false
                            )
                        }
                    }
                }
            }
            false // trả false để drawView v
        }

        binding.drawView.apply {
            setConstrained(true)
            setLocked(false)
            setOnDrawListener(object : OnDrawListener {
                override fun onAddedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onAddedDraw")
                    updateCurrentCurrentDraw(draw)
                    addDrawView(draw)
                }

                override fun onClickedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onClickedDraw")

                }

                override fun onDeletedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onDeletedDraw")
                    deleteDrawView(draw)
                }

                override fun onDragFinishedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onDragFinishedDraw")
                }

                override fun onTouchedDownDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onTouchedDownDraw")
                    touchedAnyDraw = true
                    touchedAnyDraw =true
                    updateCurrentCurrentDraw(draw)

                    val values = FloatArray(9)
                    draw.getMatrix().getValues(values)
                    val scaleX = values[Matrix.MSCALE_X]
                    val scaleY = values[Matrix.MSCALE_Y]
                    Log.d("DEBUG", "Touch down - scaleX: $scaleX, scaleY: $scaleY")  // ← CHECK

                    touchedAnyDraw = true
                    updateCurrentCurrentDraw(draw)
                }

                override fun onZoomFinishedDraw(draw: Draw) {}

                override fun onFlippedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onFlippedDraw")

                }

                override fun onDoubleTappedDraw(draw: Draw) {}

                override fun onHideOptionIconDraw() {}

                override fun onUndoDeleteDraw(draw: List<Draw?>) {}

                override fun onUndoUpdateDraw(draw: List<Draw?>) {}

                override fun onUndoDeleteAll() {}

                override fun onRedoAll() {}

                override fun onReplaceDraw(draw: Draw) {}

                override fun onEditText(draw: DrawableDraw) {}

                override fun onReplace(draw: Draw) {}
            })
        }

    }

    fun updateCurrentCurrentDraw(draw: Draw) {
        currentDraw = draw
    }

    fun addDrawView(draw: Draw) {
        drawViewList.add(draw)
    }

    fun deleteDrawView(draw: Draw) {
        drawViewList.removeIf { it == draw }
    }

    fun loadDrawableEmoji(context: Context, bitmap: Bitmap): DrawableDraw {
        val drawable = bitmap.toDrawable(context.resources)
        val drawableEmoji =
            DrawableDraw(drawable, "${SimpleDateFormat("dd_MM_yyyy_hh_mm_ss").format(Date())}.png")
        return drawableEmoji
    }

    fun resetDraw() {
        drawViewList.clear()

    }

    private fun showResetConfirmation()
    {
        val dialog = YesNoDialog(
            context = this,
            title = R.string.reset,
            description = R.string.change_your_whole_design_are_you_sure
        )

        dialog.onYesClick = {
            showInterAll { resetToInitialState() }
            dialog.dismiss()
        }

        dialog.onNoClick = {
            dialog.dismiss()
        }
        dialog.show()

    }

    private fun resetToInitialState(){
        binding.drawView.removeAllDraw()
        drawViewList.clear()
        currentDraw =null
        if(currentImagePath.isNotEmpty()){
            Glide.with(this)
                .load(File(currentImagePath))
                .into(binding.imgBackground)
        }
    }

    override fun onRestart() {
        super.onRestart()
        initAds()
    }
    override fun initAds() {
        // Load native regular ad above back button and list
        // Load native collapsible ad at bottom
      //  Admob.getInstance().loadNativeCollapNotBanner(this, getString(R.string.native_collap_editFilter), binding.nativeCollapEditSticker)
    }
}
