package poster.maker.activity_app.editsticker

import android.view.MotionEvent

interface StickerIconEvent {
    fun onActionDown(stickerView: StickerView, event: MotionEvent)
    fun onActionMove(stickerView: StickerView, event: MotionEvent)
    fun onActionUp(stickerView: StickerView, event: MotionEvent)
}
