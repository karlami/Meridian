package com.arubanetworks.meridiansamples.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;

public class CropCircleTransformation implements Transformation<Bitmap> {

    private BitmapPool mBitmapPool;
    private int borderWidth;
    private int shadowWidth;

    public CropCircleTransformation(BitmapPool pool) {
        this(pool, 0);
    }

    public CropCircleTransformation(BitmapPool pool, int borderWidth) {
        this.mBitmapPool = pool;
        this.borderWidth = borderWidth;
        if (this.borderWidth > 0) {
            this.shadowWidth = borderWidth; // fixed value
        }
    }

    @Override
    public Resource<Bitmap> transform(Resource<Bitmap> resource, int outWidth, int outHeight) {
        Bitmap source = resource.get();
        int originalSize = Math.min(source.getWidth(), source.getHeight());

        int width = (source.getWidth() - borderWidth * 2 - shadowWidth * 2 - originalSize) / 2;
        int height = (source.getHeight() - borderWidth * 2 - shadowWidth * 2 - originalSize) / 2;

        int size = originalSize + (borderWidth * 2) + (shadowWidth * 2);
        Bitmap bitmap = mBitmapPool.get(size, size, Bitmap.Config.ARGB_8888);
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        BitmapShader shader = new BitmapShader(source, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
        if (width != 0 || height != 0) {
            Matrix matrix = new Matrix();
            matrix.setTranslate(-width, -height);
            shader.setLocalMatrix(matrix);
        }
        paint.setShader(shader);
        paint.setAntiAlias(true);

        float r = originalSize / 2f;
        float rWithBorder = size / 2;

        if (borderWidth > 0) {
            Paint paintBorder = new Paint();
            paintBorder.setAntiAlias(true);
            paintBorder.setColor(Color.WHITE);
            paintBorder.setShadowLayer(shadowWidth, 0.0f, 0.0f, Color.DKGRAY);

            canvas.drawCircle(rWithBorder, rWithBorder, rWithBorder - shadowWidth / 2, paintBorder);
        }
        canvas.drawCircle(rWithBorder, rWithBorder, r, paint);

        return BitmapResource.obtain(bitmap, mBitmapPool);
    }

    @Override
    public String getId() {
        return "CropCircleTransformation()";
    }
}