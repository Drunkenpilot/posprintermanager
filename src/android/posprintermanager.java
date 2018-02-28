package cordova.plugin.posprintermanager;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import java.io.FileOutputStream;
// import java.io.OutputStream;
import java.lang.Exception;
import java.io.File;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.betaresto.terminal.R;
import android.app.Activity;
import android.support.v4.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.Manifest;
import android.graphics.Bitmap;
import android.os.Environment;
import android.widget.Toast;
import android.util.Log;
/**
 * This class echoes a string called from JavaScript.
 */
public class posprintermanager extends CordovaPlugin {
    // declarations
    private CallbackContext callbackContext = null;
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final String AppExternalDataDir = "/BetaResto/";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        this.callbackContext = callbackContext;

        if(action.equals("buildImage")) {
            final JSONArray printContent = args.optJSONArray(0);
            final int printTemplate = args.optInt(1);
            final JSONArray printCanvas = args.optJSONArray(2);
            cordova.getThreadPool().execute(new Runnable() {
				public void run() {
                    buildImage(printContent, printTemplate, printCanvas);
                }
            });
            return true;
        }

        if(action.equals("search")) {
            final int millSeconds = args.optInt(0, 10 * 1000);
            final String vendor = args.optString(1);
            final String type = args.optString(2);
            initSearchPrinter(millSeconds,vendor, type);
            });
            return true;
        }

        return false;
    }

    private void initSearchPrinter(final int millSeconds, final String vendor, final String type) {
        if(vendor.equals("EPSON")) {    
            EpsonPrinter epsonPrinter = new EpsonPrinter(cordova.getActivity(), callbackContext);
            epsonPrinter.search(millSeconds,cordova.getActivity());
        } else if (vendor.equals("STAR")) {

        } else {
            this.callbackContext.error();
        }


    }


    private void buildImage(final JSONArray printContent, final int printTemplate, final JSONArray printCanvas) {

        this.verifyStoragePermissions(cordova.getActivity());

        try{
        ReceiptBuilderExt receiptBuilder = new ReceiptBuilderExt(cordova.getActivity(), printCanvas);
        Bitmap testImg = receiptBuilder.build(printContent);
        //save Bitmap to file

        String path = Environment.getExternalStorageDirectory().toString();
        String filename = "test.jpg";    
        File file = new File(path + AppExternalDataDir , filename);
        FileOutputStream fOut = new FileOutputStream(file);
        testImg.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
        fOut.close();    
        this.showToast("Image built");
        callbackContext.success(path  + AppExternalDataDir + filename);
        }  catch (Exception e) {
            Log.e("TestError: ", Log.getStackTraceString(e));
            this.callbackContext.error(Log.getStackTraceString(e));
             this.showToast("Image build failed");
        }
        
    }

	private void showToast(final String message) {
		cordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(cordova.getActivity(), "Message: " + message, Toast.LENGTH_SHORT).show();
			}
		});
    }
    
    /**
 * Checks if the app has permission to write to device storage
 *
 * If the app does not has permission then the user will be prompted to grant permissions
 *
 * @param activity
 */
    private static void verifyStoragePermissions(Activity activity) { 
    // Check if we have write permission
    int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

    if (permission != PackageManager.PERMISSION_GRANTED) {
        // We don't have permission so prompt the user
        ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
        );
    }
}
    
}
