package cordova.plugin.posprintermanager;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

        // if (action.equals("coolMethod")) {
        //     String message = args.getString(0);
        //     this.coolMethod(message, callbackContext);
        //     return true;
        // }

        if(action.equals("buildImage")) {
            // final JSONArray printContent = args.optJSONArray(0);
            // final int printTemplate = args.optInt(1);
            this.buildImage(); //printContent, printTemplate
            return true;
        }
        return false;
    }

    private void buildImage() { // final JSONArray printContent, final int printTemplate

        this.showToast("Plugin ok");

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
