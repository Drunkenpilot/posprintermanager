package cordova.plugin.posprintermanager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.graphics.Matrix;
import java.io.ByteArrayOutputStream;

public class ImageUtil {
    public static Bitmap convert(String base64Str) throws IllegalArgumentException {
        byte[] decodedBytes = Base64.decode(base64Str.substring(base64Str.indexOf(",") + 1), Base64.DEFAULT);

        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    public static String convert(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
    }

    public static Bitmap resize(Bitmap bm, int newWidth, int newHeight , boolean filter) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = 0;
        float scaleHeight = 0;

        if(newWidth>0 && newHeight>0) {
            scaleWidth = ((float) newWidth) / width;
            scaleHeight = ((float) newHeight) / height;    
        } else if(newWidth > 0) {
            scaleWidth = ((float) newWidth) / width;
            scaleHeight = ((float) height * scaleWidth) / height;  
        } else if(newHeight > 0) {
            scaleHeight = ((float) newHeight) / height;    
            scaleWidth = ((float) width * scaleHeight) / width;
        } else {
            return bm;
        }

        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, filter);
        bm.recycle();
        return resizedBitmap;
    }

    public static Bitmap resizeScale(Bitmap bm, float scale, boolean filter) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float newWidth = width * scale;
        float newHeight = height * scale;

        float scaleWidth = newWidth / width;
        float scaleHeight = newHeight / height;

        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, filter);
        bm.recycle();
        return resizedBitmap;
    }

    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize, boolean filter) {

        float ratio = Math.min( maxImageSize / realImage.getWidth(),
                 maxImageSize / realImage.getHeight());
        
                 if (ratio >= 1.0) {
            return realImage;
        }

        int width = Math.round(ratio * realImage.getWidth());
        int height = Math.round(ratio * realImage.getHeight());
        realImage.recycle();
        return Bitmap.createScaledBitmap(realImage, width, height, filter);
    }

    public Bitmap scaleUp(Bitmap realImage, float minImageSize, boolean filter) {
        
        float ratio = Math.min( minImageSize / realImage.getWidth(),
        minImageSize / realImage.getHeight());
        
                 if (ratio <= 1.0) {
            return realImage;
        }

        int width = Math.round(ratio * realImage.getWidth());
        int height = Math.round(ratio * realImage.getHeight());
        realImage.recycle();
        return Bitmap.createScaledBitmap(realImage, width, height, filter);
    }

}
