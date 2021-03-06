package cordova.plugin.posprintermanager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;

import java.io.FileOutputStream;
import java.lang.Exception;
import java.io.File;
import java.util.HashMap;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.epson.epos2.Epos2Exception;
import com.epson.epos2.discovery.DeviceInfo;
import com.epson.epos2.discovery.Discovery;
import com.epson.epos2.discovery.DiscoveryListener;
import com.epson.epos2.discovery.FilterOption;

import com.epson.epos2.printer.Printer;
import com.epson.epos2.printer.PrinterStatusInfo;
import com.epson.epos2.printer.ReceiveListener;
import com.epson.epos2.Epos2CallbackCode;

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

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = { Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };

    private ArrayList<HashMap<String, String>> mPrinterList = null;
    private FilterOption mFilterOption = null;
    private Printer mPrinter = null;
    private CallbackContext callbackContext = null;

    private String AppCacheDir = null;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        // your init code here
        AppCacheDir = cordova.getActivity().getExternalCacheDir().toString();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        this.callbackContext = callbackContext;

        if (action.equals("buildImage")) {
            final JSONArray printContent = args.optJSONArray(0);
            final JSONArray printCanvas = args.optJSONArray(1);
            final String filename = args.optString(2);
            String fileDir = args.optString(3);
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    buildImage(printContent, printCanvas, filename, fileDir);
                }
            });
            return true;
        }

        if (action.equals("search")) {
            final int millSeconds = args.optInt(0, 10 * 1000);
            Log.i("millSeconds", "time:" + millSeconds);
            final String vendor = args.optString(1);
            final String type = args.optString(2);

            initSearchPrinter(millSeconds, vendor, type);

            return true;
        }

        if (action.equals("print")) {
            final String vendor = args.optString(0);
            final JSONArray printData = args.optJSONArray(1);
            final JSONArray printCanvas = args.optJSONArray(2);
            final JSONArray pulse = args.optJSONArray(3);
            final int model = args.optInt(4);
            final int lang = args.optInt(5);
            final String address = args.optString(6);
            initPrint(vendor, printData, printCanvas, pulse, model, lang, address);

            return true;
        }

        return false;
    }

    private void initSearchPrinter(final int millSeconds, final String vendor, final String type) {
        if (vendor.equals("EPSON")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    EpsonPrinter epsonPrinter = new EpsonPrinter(cordova.getActivity(), callbackContext);
                    epsonPrinter.search(millSeconds, cordova.getActivity());
                }
            });
        } else if (vendor.equals("STAR")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    StarPrinter starPrinter = new StarPrinter();
                    starPrinter.initialize(cordova, webView);
                    starPrinter.portDiscovery(type, cordova.getActivity(), callbackContext);
                }
            });
        } else {
            this.callbackContext.error("no");
        }

    }

    private void initPrint(final String vendor, final JSONArray printData, final JSONArray printCanvas,
            final JSONArray pulse, final int model, final int lang, final String address) {
        if (vendor.equals("EPSON")) {

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    Bitmap printRaw = buildPrintRaw(printData, printCanvas);
                    EpsonPrinter epsonPrinter = new EpsonPrinter(cordova.getActivity(), callbackContext);
                    epsonPrinter.print(printRaw, pulse, model, lang, address, cordova.getActivity(), callbackContext);
                    printRaw = null;

                }
            });
        } else if (vendor.equals("STAR")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    Bitmap printRaw = buildPrintRaw(printData, printCanvas);
                    StarPrinter starPrinter = new StarPrinter();
                    starPrinter.PrintText(address, printRaw, cordova.getActivity(), callbackContext);
                    printRaw = null;
                }
            });
        } else {
            this.callbackContext.error("no");
        }
    }

    private Bitmap buildPrintRaw(final JSONArray printContent, final JSONArray printCanvas) {
        try {
            ReceiptBuilderExt receiptBuilder = new ReceiptBuilderExt(cordova.getActivity(), printCanvas);
            Bitmap printRaw = receiptBuilder.build(printContent);
            return printRaw;
        } catch (Exception e) {
            Log.e("TestError: ", Log.getStackTraceString(e));
            this.callbackContext.error(Log.getStackTraceString(e));
            this.showToast("Image build failed");
            return null;
        }
    }

    private void buildImage(final JSONArray printContent, final JSONArray printCanvas, final String filename,
            String fileDir) {

        this.verifyStoragePermissions(cordova.getActivity());

        try {
            Bitmap printRaw = buildPrintRaw(printContent, printCanvas);
            final int bitmapWidth = printRaw.getWidth();
            final int bitmapHeight = printRaw.getHeight();
            // check dirs
            String path = fileDir != null ? AppCacheDir + '/' + fileDir : AppCacheDir;

            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // save Bitmap to file
            File file = new File(path, filename);
            FileOutputStream fOut = new FileOutputStream(file);
            printRaw.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.close();
            // this.showToast("Image built");

            final String contentProvider = "file://";
            final String uri = contentProvider + file.getAbsolutePath();
            JSONObject item = new JSONObject();

            item.put("uri", uri);
            item.put("base64URL", "data:image/jpeg;base64," + ImageUtil.convert(printRaw));
            item.put("paperWidth", bitmapWidth);
            item.put("paperHeight", bitmapHeight);

            JSONObject event = new JSONObject();
            final String eventName = "CANVAS_BUILT";
            event.put("name", eventName);
            event.put("data", item);
            callbackContext.success(event);
        } catch (Exception e) {
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
     * If the app does not has permission then the user will be prompted to grant
     * permissions
     *
     * @param activity
     */
    private static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }
    }

}
