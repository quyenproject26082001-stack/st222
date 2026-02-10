package poster.maker.core.helper

import android.content.Context
import android.media.SoundPool

object SoundHelper {
    private val soundPool = SoundPool.Builder().setMaxStreams(5).build()
    private val soundMap = mutableMapOf<Int, Int>()

    private val streamMap = mutableMapOf<Int, Int>()         // resId -> streamId (last played)

    fun isSoundNotNull(resId: Int) : Boolean {
        return soundMap[resId] != null
    }

    fun loadSound(context: Context, resId: Int) {
        if (soundMap[resId] == null) {
            soundMap[resId] = soundPool.load(context, resId, 1)
        }
    }

    fun stop(resId: Int) {
        streamMap[resId]?.let { streamId ->
            soundPool.stop(streamId)
            streamMap.remove(resId)
        }
    }

    fun stopAll() {
        streamMap.values.forEach { soundPool.stop(it) }
        streamMap.clear()
    }
    fun playSound(resId: Int) {
        soundMap[resId]?.let { id ->
            val streamId = soundPool.play(id, 1f, 1f, 0, 0, 1f)
            streamMap[resId] = streamId
        }
    }

    fun release() {
        soundPool.release()
    }
}