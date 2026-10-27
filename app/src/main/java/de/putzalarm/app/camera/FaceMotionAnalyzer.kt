package de.putzalarm.app.camera

import android.graphics.PointF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

/**
 * Was die Kamera gerade sieht. Koordinaten sind normiert (0..1) im aufrechten, NICHT gespiegelten Bild.
 * [motion] ist 0 bei Stillstand und 1 bei kräftigem Schrubben rund um den Mund.
 */
data class FaceReading(
    val hasFace: Boolean = false,
    val mouthX: Float = 0.5f,
    val mouthY: Float = 0.62f,
    val faceW: Float = 0f,
    val faceH: Float = 0f,
    val motion: Float = 0f,
    val imageW: Int = 640,
    val imageH: Int = 480,
    val timeMs: Long = 0L,
)

/**
 * Wertet jedes Kamerabild nur im Speicher aus und verwirft es sofort:
 * ML Kit findet Gesicht und Mund, ein einfacher Bildvergleich misst die Bewegung um den Mund.
 */
class FaceMotionAnalyzer : ImageAnalysis.Analyzer {
    val latest = AtomicReference(FaceReading())

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setMinFaceSize(0.15f)
            .build(),
    )

    private val gx = 28
    private val gy = 18
    private var prev: IntArray? = null
    private var motion = 0f
    private var lastFaceMs = 0L
    private var mx = 0.5f
    private var my = 0.62f
    private var fw = 0f
    private var fh = 0f

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val media = image.image
        if (media == null) {
            image.close()
            return
        }
        val rot = image.imageInfo.rotationDegrees
        val upW = if (rot % 180 == 0) image.width else image.height
        val upH = if (rot % 180 == 0) image.height else image.width
        val now = System.currentTimeMillis()
        val hasFace = now - lastFaceMs < 700

        // Bereich um den Mund (oder Bildmitte unten, falls kein Gesicht da ist)
        val rw = if (hasFace) (fw * 1.2f).coerceIn(0.15f, 0.8f) else 0.5f
        val rh = if (hasFace) (fh * 0.8f).coerceIn(0.15f, 0.8f) else 0.55f
        val cx = if (hasFace) mx else 0.5f
        val cy = if (hasFace) my else 0.65f
        val raw = measureMotion(image, rot, upW, upH, cx - rw / 2, cy - rh / 2, rw, rh)
        val level = ((raw - 3.5f) / 9f).coerceIn(0f, 1f)
        motion = motion * 0.55f + level * 0.45f
        latest.set(FaceReading(hasFace, mx, my, fw, fh, motion, upW, upH, now))

        detector.process(InputImage.fromMediaImage(media, rot))
            .addOnSuccessListener { faces ->
                val f = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: return@addOnSuccessListener
                val b = f.boundingBox
                val pts = listOf(FaceLandmark.MOUTH_BOTTOM, FaceLandmark.MOUTH_LEFT, FaceLandmark.MOUTH_RIGHT)
                    .mapNotNull { f.getLandmark(it)?.position }
                val m = if (pts.isNotEmpty()) PointF(pts.map { it.x }.average().toFloat(), pts.map { it.y }.average().toFloat())
                else PointF(b.exactCenterX(), b.top + b.height() * 0.75f)
                val k = if (System.currentTimeMillis() - lastFaceMs > 1000) 1f else 0.5f
                mx += (m.x / upW - mx) * k
                my += (m.y / upH - my) * k
                fw += (b.width().toFloat() / upW - fw) * k
                fh += (b.height().toFloat() / upH - fh) * k
                lastFaceMs = System.currentTimeMillis()
            }
            .addOnCompleteListener { image.close() }
    }

    /** Mittlerer Helligkeitsunterschied zum letzten Bild auf einem groben Raster im Bereich. */
    private fun measureMotion(image: ImageProxy, rot: Int, upW: Int, upH: Int, left: Float, top: Float, w: Float, h: Float): Float {
        val plane = image.planes[0]
        val buf = plane.buffer
        val rs = plane.rowStride
        val ps = plane.pixelStride
        val rawW = image.width
        val rawH = image.height
        val cur = IntArray(gx * gy)
        for (j in 0 until gy) for (i in 0 until gx) {
            val ux = ((left + (i + 0.5f) / gx * w).coerceIn(0f, 0.999f) * upW).toInt()
            val uy = ((top + (j + 0.5f) / gy * h).coerceIn(0f, 0.999f) * upH).toInt()
            val (rx, ry) = when (rot) {
                90 -> uy to (rawH - 1 - ux)
                180 -> (rawW - 1 - ux) to (rawH - 1 - uy)
                270 -> (rawW - 1 - uy) to ux
                else -> ux to uy
            }
            var sum = 0
            for ((dx, dy) in Neighbours) {
                val x = (rx + dx).coerceIn(0, rawW - 1)
                val y = (ry + dy).coerceIn(0, rawH - 1)
                val idx = y * rs + x * ps
                if (idx < buf.limit()) sum += buf.get(idx).toInt() and 0xFF
            }
            cur[j * gx + i] = sum / Neighbours.size
        }
        val p = prev
        prev = cur
        if (p == null) return 0f
        var d = 0L
        for (n in cur.indices) d += abs(cur[n] - p[n])
        return d.toFloat() / cur.size
    }

    fun close() = detector.close()

    private companion object {
        val Neighbours = listOf(0 to 0, 2 to 0, 0 to 2, 2 to 2)
    }
}
