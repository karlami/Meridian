package com.arubanetworks.meridiansamples.utils

import android.graphics.*

import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource

class CropCircleTransformation @JvmOverloads constructor(private val mBitmapPool: BitmapPool, private val borderWidth: Int = 0) : Transformation<Bitmap> {
    private var shadowWidth: Int = 0

    init {
        if (this.borderWidth > 0) {
            this.shadowWidth = borderWidth // fixed value
        }
    }

    override fun transform(resource: Resource<Bitmap>, outWidth: Int, outHeight: Int): Resource<Bitmap> {
        val source = resource.get()
        val originalSize = Math.min(source.width, source.height)

        val width = (source.width - borderWidth * 2 - shadowWidth * 2 - originalSize) / 2
        val height = (source.height - borderWidth * 2 - shadowWidth * 2 - originalSize) / 2

        val size = originalSize + borderWidth * 2 + shadowWidth * 2
        var bitmap: Bitmap? = mBitmapPool.get(size, size, Bitmap.Config.ARGB_8888)
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        }

        val canvas = Canvas(bitmap!!)
        val paint = Paint()
        val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        if (width != 0 || height != 0) {
            val matrix = Matrix()
            matrix.setTranslate((-width).toFloat(), (-height).toFloat())
            shader.setLocalMatrix(matrix)
        }
        paint.shader = shader
        paint.isAntiAlias = true

        val r = originalSize / 2f
        val rWithBorder = (size / 2).toFloat()

        if (borderWidth > 0) {
            val paintBorder = Paint()
            paintBorder.isAntiAlias = true
            paintBorder.color = Color.WHITE
            paintBorder.setShadowLayer(shadowWidth.toFloat(), 0.0f, 0.0f, Color.DKGRAY)

            canvas.drawCircle(rWithBorder, rWithBorder, rWithBorder - shadowWidth / 2, paintBorder)
        }
        canvas.drawCircle(rWithBorder, rWithBorder, r, paint)

        return BitmapResource.obtain(bitmap, mBitmapPool)
    }

    override fun getId(): String {
        return "CropCircleTransformation()"
    }
}