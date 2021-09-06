package org.jetbrains.skiko.redrawer

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import org.jetbrains.skiko.FrameDispatcher
import org.jetbrains.skiko.FrameLimiter
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaLayerProperties

internal class SoftwareRedrawer(
    private val layer: SkiaLayer,
    private val properties: SkiaLayerProperties
) : Redrawer {
    private val frameJob = Job()
    private val frameLimiter = FrameLimiter(CoroutineScope(Dispatchers.IO + frameJob), layer.backedLayer)

    private val frameDispatcher = FrameDispatcher(Dispatchers.Swing) {
        if (properties.isVsyncEnabled && properties.isVsyncFramelimitFallbackEnabled) {
            frameLimiter.awaitNextFrame()
        }

        layer.update(System.nanoTime())
        if (layer.prepareDrawContext()) {
            layer.draw()
        }
    }

    override fun dispose() {
        frameDispatcher.cancel()
        runBlocking {
            frameJob.cancelAndJoin()
        }
    }

    override fun needRedraw() {
        frameDispatcher.scheduleFrame()
    }

    override suspend fun awaitRedraw(): Boolean {
        return frameDispatcher.awaitFrame()
    }

    override fun redrawImmediately() {
        layer.update(System.nanoTime())
        if (layer.prepareDrawContext()) {
            layer.draw()
        }
    }
}