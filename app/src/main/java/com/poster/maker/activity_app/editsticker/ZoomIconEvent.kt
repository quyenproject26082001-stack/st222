package poster.maker.activity_app.editsticker

import android.view.MotionEvent

class ZoomIconEvent : StickerIconEvent {
    override fun onActionDown(stickerView: StickerView, event: MotionEvent) {
        stickerView.onZoomAndRotateStart(event)
    }

    override fun onActionMove(stickerView: StickerView, event: MotionEvent) {
        stickerView.onZoomAndRotate(event)
    }

    override fun onActionUp(stickerView: StickerView, event: MotionEvent) {
        stickerView.onZoomAndRotateEnd()
    }
}
