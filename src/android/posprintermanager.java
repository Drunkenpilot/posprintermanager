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
import android.graphics.Bitmap;
import android.os.Environment;
import android.widget.Toast;
/**
 * This class echoes a string called from JavaScript.
 */
public class posprintermanager extends CordovaPlugin {
    // declarations
    private CallbackContext callbackContext = null;
    

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        this.callbackContext = callbackContext;

        if(action.equals("buildImage")) {
            final JSONArray printContent = args.optJSONArray(0);
            final int printTemplate = args.optInt(1);
            this.buildImage(printContent, printTemplate);
            return true;
        }
        return false;
    }

    private void buildImage(final JSONArray printContent, final int printTemplate) {

		ReceiptBuilderExt receiptBuilder = new ReceiptBuilderExt(cordova.getActivity());
        Bitmap testImg = receiptBuilder.build(printContent);
        //save Bitmap to file
        try{
        String path = Environment.getExternalStorageDirectory().toString();

        File file = new File(path, "test.jpg");
        FileOutputStream fOut = new FileOutputStream(file);
        testImg.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
        fOut.close();    
        }  catch (Exception e) {
            e.printStackTrack();
        }



        this.showToast("Image built");
        // callbackContext.success(testImg);

    }

    // private void coolMethod(String message, CallbackContext callbackContext) {
    //     if (message != null && message.length() > 0) {
    //         callbackContext.success(message);
    //     } else {
    //         callbackContext.error("Expected one non-empty string argument.");
    //     }
    // }

		private void showToast(final String message) {
			cordova.getActivity().runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(cordova.getActivity(), "Message: " + message, Toast.LENGTH_SHORT)
					.show();
				}
			});
		}
    
}
