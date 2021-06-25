package com.example.wearablenotification.main

import android.annotation.SuppressLint
import android.graphics.*
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * 検出結果を表示する透過surfaceView
 */
@SuppressLint("ViewConstructor")
class OverlaySurfaceView(surfaceView: SurfaceView) :
    SurfaceView(surfaceView.context), SurfaceHolder.Callback {

    companion object{
        const val TAG = "OverlaySurfaceView"

        const val TEXT_SIZE = 50f
        const val TEXT_DETECTING = "信号機を探しています・・・"
        const val TEXT_TRAFFICLIGHT = "信号"
        const val TEXT_REDLIGHT = "赤信号"
        const val TEXT_GREENLIGHT = "青信号"
    }

    init {
        surfaceView.holder.addCallback(this)
        surfaceView.setZOrderOnTop(true)
    }

    private var surfaceHolder = surfaceView.holder
    private val paint = Paint()

    override fun surfaceCreated(holder: SurfaceHolder) {
        // surfaceViewを透過させる
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }

    /**
     * surfaceViewのクリア
     */
    fun clear(){
        // surfaceHolder経由でキャンバス取得(画面がactiveでない時にもdrawされてしまいexception発生の可能性があるのでnullableにして以下扱ってます)
        val canvas: Canvas? = surfaceHolder.lockCanvas()
        // 前に描画していたものをクリア
        canvas?.drawColor(0, PorterDuff.Mode.CLEAR)

        surfaceHolder.unlockCanvasAndPost(canvas ?: return)
    }

    /**
     * surfaceViewに物体検出結果を表示
     */
    fun draw(
        roi: RectF,
        detectedObjectList: List<DetectionObject>,
        trafficLightColor : Int,
        imageProxySize: Size,
        resultViewSize: Size
    ) {

        // surfaceHolder経由でキャンバス取得(画面がactiveでない時にもdrawされてしまいexception発生の可能性があるのでnullableにして以下扱ってます)
        val canvas: Canvas? = surfaceHolder.lockCanvas()
        // 前に描画していたものをクリア
        canvas?.drawColor(0, PorterDuff.Mode.CLEAR)

        // ImageProxy座標　-> ResultView座標への変換値
        val imageProxyToResultViewX = resultViewSize.width.toFloat() / imageProxySize.width
        val imageProxyToResultViewY = resultViewSize.height.toFloat() / imageProxySize.height

        Log.d(TAG, "image to result x ratio : $imageProxyToResultViewX")
        Log.d(TAG, "image to result y ratio : $imageProxyToResultViewY")

        // ImageProxy座標　-> ResultView座標への変換
        val rvRoi = Rect(
            (roi.left * imageProxyToResultViewX).toInt(),
            (roi.top * imageProxyToResultViewY).toInt(),
            (roi.right * imageProxyToResultViewX).toInt(),
            (roi.bottom * imageProxyToResultViewY).toInt()
        )

        // ROIを白い矩形で囲う
        paint.apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 7f
            isAntiAlias = false
        }
        canvas?.drawRect(
            Rect(rvRoi.left, rvRoi.top, rvRoi.right, rvRoi.bottom),
            paint
        )

        // ROIテキストの矩形
        paint.style = Paint.Style.FILL
        canvas?.drawRect(
            RectF(rvRoi.left - paint.strokeWidth,
                rvRoi.top - TEXT_SIZE - 5f,
                rvRoi.left + paint.textSize * TEXT_DETECTING.length,
                rvRoi.top.toFloat()),
            paint
        )
        // ROIのテキストを表示
        paint.apply {
            color = Color.GRAY
            isAntiAlias = true
            textSize = TEXT_SIZE
        }
        canvas?.drawText(
            TEXT_DETECTING,
            rvRoi.left.toFloat(),
            rvRoi.top - 5f,
            paint
        )

        if(detectedObjectList.isNotEmpty()) {

            // ラベル名を更新
            detectedObjectList[0].label = if (trafficLightColor == 1) {
                TEXT_REDLIGHT
            } else if (trafficLightColor == 2) {
                TEXT_GREENLIGHT
            } else {
                TEXT_TRAFFICLIGHT
            }

            val textDisplayed = detectedObjectList[0].label + " " + "%,.2f".format(detectedObjectList[0].score * 100) + "%"

            // roiBitmap座標　-> ResultView座標への変換
            detectedObjectList[0].boundingBox = RectF(
                rvRoi.left + detectedObjectList[0].boundingBox.left * imageProxyToResultViewX,
                rvRoi.top + detectedObjectList[0].boundingBox.top * imageProxyToResultViewY,
                rvRoi.left + detectedObjectList[0].boundingBox.right * imageProxyToResultViewX,
                rvRoi.top + detectedObjectList[0].boundingBox.bottom * imageProxyToResultViewY
            )

            // バウンディングボックスの表示
            paint.apply {
                color = if (trafficLightColor == 1) {
                    Color.RED
                } else if (trafficLightColor == 2) {
                    Color.GREEN
                } else {
                    Color.GRAY
                }

                style = Paint.Style.STROKE
                strokeWidth = 7f
                isAntiAlias = false
            }
            canvas?.drawRect(detectedObjectList[0].boundingBox, paint)

            // ラベルとスコアの矩形
            paint.style = Paint.Style.FILL
            canvas?.drawRect(
                RectF(detectedObjectList[0].boundingBox.left,
                    detectedObjectList[0].boundingBox.top - TEXT_SIZE - 5f,
                    detectedObjectList[0].boundingBox.left + TEXT_SIZE/1.5f * textDisplayed.length,
                    detectedObjectList[0].boundingBox.top),
                paint
            )
            // ラベルとスコアの表示
            paint.apply {
                color = Color.WHITE
                isAntiAlias = true
                textSize = TEXT_SIZE
            }
            canvas?.drawText(
                textDisplayed,
                detectedObjectList[0].boundingBox.left,
                detectedObjectList[0].boundingBox.top - 5f,
                paint
            )

        }

        surfaceHolder.unlockCanvasAndPost(canvas ?: return)
    }
}