package com.Carewell.view.ecg12;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.print.PrintAttributes;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PdfUtil {

    /**
     * 生成pdf
     */
    public static void saveBitmapForPdf(Bitmap bitmap, String outfilePath, boolean isVertical) {
        PdfDocument doc = new PdfDocument();
        int pageWidth;
        int pageHeight;

        if(isVertical){
            pageWidth = PrintAttributes.MediaSize.ISO_A4.getWidthMils() * 72 / 1000;
        }else{
            pageWidth = PrintAttributes.MediaSize.ISO_A4.getHeightMils() * 72 / 1000;
        }

        float scale = (float) pageWidth / (float) bitmap.getWidth();
        pageHeight = (int) (bitmap.getHeight() * scale);

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        File file = new File(outfilePath);
        FileOutputStream outputStream = null;
        try {
            PdfDocument.PageInfo newPage = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = doc.startPage(newPage);
            Canvas canvas = page.getCanvas();
            canvas.drawBitmap(bitmap, matrix, paint);
            doc.finishPage(page);
            bitmap.recycle();
            outputStream = new FileOutputStream(file);
            doc.writeTo(outputStream);
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            doc.close();
        }
    }
}
