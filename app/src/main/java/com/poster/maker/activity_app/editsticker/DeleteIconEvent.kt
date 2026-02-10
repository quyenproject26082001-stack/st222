package poster.maker.activity_app.editsticker

import android.view.MotionEvent

class DeleteIconEvent : StickerIconEvent {
    override fun onActionDown(stickerView: StickerView, event: MotionEvent) {
        // No action needed on down
    }

    override fun onActionMove(stickerView: StickerView, event: MotionEvent) {
        // No action needed on move
    }

    override fun onActionUp(stickerView: StickerView, event: MotionEvent) {
        stickerView.removeSticker()
    }
}
