package cordova.plugin.posprintermanager;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import java.io.FileOutputStream;
// import java.io.OutputStream;
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
import android.content.Context;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
    private static String[] PERMISSIONS_STORAGE = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private ArrayList<HashMap<String, String>> mPrinterList = null;
    private FilterOption mFilterOption = null;
    private Printer  mPrinter = null;
    private Context mContext = null;
    private CallbackContext callbackContext = null;


    private static final String AppExternalDataDir = "/BetaResto/";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        this.callbackContext = callbackContext;
        mContext = this;

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
			Log.i("millSeconds", "time:" + millSeconds);
            final String vendor = args.optString(1);
            final String type = args.optString(2);

            initSearchPrinter(millSeconds,vendor, type);

            return true;
        }

        if(action.equals("print")) {
            final String vendor = args.optString(0);
            final JSONArray printData = args.optJSONArray(1);
            final JSONArray printCanvas = args.optJSONArray(2);
            final JSONArray pulse = args.optJSONArray(3);
            final int model = args.optInt(4);
            final int lang = args.optInt(5);
            final String address = args.optString(6);
            initPrint(vendor, printData, printCanvas, pulse, model, lang, address);
        }

        return false;
    }

    private void initSearchPrinter(final int millSeconds, final String vendor, final String type) {
        if(vendor.equals("EPSON")) {
			cordova.getThreadPool().execute(new Runnable() {
				public void run() {
					EpsonPrinter epsonPrinter = new EpsonPrinter(cordova.getActivity(), callbackContext);
					epsonPrinter.search(millSeconds, cordova.getActivity());
				}
			});
        } else if (vendor.equals("STAR")) {

        } else {
            this.callbackContext.error("no");
        }


    }

    private void initPrint(final String vendor,  final JSONArray printData, final JSONArray printCanvas, final JSONArray pulse, final int model, final int lang, final String address) {
        if(vendor.equals("EPSON")) {

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
//                    Bitmap printRaw = buildPrintRaw(printData, printCanvas);
//                    EpsonPrinter epsonPrinter = new EpsonPrinter(cordova.getActivity(), callbackContext);
//                    epsonPrinter.print(printRaw,pulse, model, lang, address);
                    runPrintReceiptSequence(printData, model,lang,address, printCanvas);
                }
            });
        } else if (vendor.equals("STAR")) {

        } else {
            this.callbackContext.error("no");
        }
    }

    private Bitmap buildPrintRaw (final JSONArray printContent, final JSONArray printCanvas) {
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


    /** Epson printer test code **/

    private boolean runPrintReceiptSequence(final JSONArray printContent, final int printerSeries, final int lang, final String printTarget, final JSONArray printCanvas) {
        if (!initializeObject(printerSeries, lang)) {
            return false;
        }

        if (!createReceiptData(printContent, printCanvas)) {
            finalizeObject();
            return false;
        }

        if (!printData(printTarget)) {
            finalizeObject();
            return false;
        }

        return true;
    }

    private boolean initializeObject(final int printerSeries, final int lang) {
        try {
            mPrinter = new Printer(printerSeries,lang,this);
        }
        catch (Exception e) {
//            this.callbackContext.error("e:" + ((Epos2Exception) e).getErrorStatus());
            ShowMsg.showException(e, "Printer", cordova.getActivity());
            return false;
        }

        mPrinter.setReceiveEventListener(this );

        return true;
    }

    private boolean printData(final String printTarget) {
        if (mPrinter == null) {
            return false;
        }

        if (!connectPrinter(printTarget)) {
            return false;
        }

        PrinterStatusInfo status = mPrinter.getStatus();

        // dispPrinterWarnings(status);

        if (!isPrintable(status)) {
           this.callbackContext.error("e:" + makeErrorMessage(status));
            ShowMsg.showMsg(makeErrorMessage(status), cordova.getActivity());
            try {
                mPrinter.disconnect();
            }
            catch (Exception ex) {
                // Do nothing
            }
            return false;
        }

        try {
            mPrinter.sendData(Printer.PARAM_DEFAULT);
        }
        catch (Exception e) {
            ShowMsg.showException(e, "sendData", cordova.getActivity());
            try {
                mPrinter.disconnect();
            }
            catch (Exception ex) {
                // Do nothing
            }
            return false;
        }

        return true;
    }

    private boolean createReceiptData(final JSONArray printContent, final JSONArray printCanvas) {
        if (mPrinter == null) {
            return false;
        }
        String method = "";
        // 		Line mode
        StringBuilder textData = new StringBuilder();

        try {


            //			Generate main content
                method = "addTextAlign";
                mPrinter.addTextAlign(Printer.ALIGN_CENTER);
                ReceiptBuilderExt receiptBuilder = new ReceiptBuilderExt(cordova.getActivity(), printCanvas);
                Bitmap testImg = receiptBuilder.build(printContent);

                method = "addImage";
                mPrinter.addImage(testImg, 0, 0,
                        testImg.getWidth(),
                        testImg.getHeight(),
                        Printer.COLOR_1,
                        Printer.MODE_MONO,
                        Printer.HALFTONE_DITHER,
                        Printer.PARAM_DEFAULT,
                        Printer.COMPRESS_AUTO);

                method = "addFeedLine";
                mPrinter.addFeedLine(1);



            method = "addCut";
            mPrinter.addCut(Printer.CUT_FEED);


        }
        catch (Exception e) {
            ShowMsg.showException(e, method, cordova.getActivity());
            return false;
        }

        textData = null;

        return true;
    }


    private void finalizeObject() {
        if (mPrinter == null) {
            return;
        }

        mPrinter.clearCommandBuffer();

        mPrinter.setReceiveEventListener(null);

        mPrinter = null;
    }


    private boolean connectPrinter(final String printTarget) {
        boolean isBeginTransaction = false;

        if (mPrinter == null) {
            return false;
        }

        try {
            mPrinter.connect(printTarget, Printer.PARAM_DEFAULT);
        }
        catch (Exception e) {
            this.callbackContext.error("e:" + ((Epos2Exception) e).getErrorStatus());
            ShowMsg.showException(e, "connect", cordova.getActivity());
            return false;
        }

        try {
            mPrinter.beginTransaction();
            isBeginTransaction = true;
        }
        catch (Exception e) {
            this.callbackContext.error("e:" + ((Epos2Exception) e).getErrorStatus());
            ShowMsg.showException(e, "beginTransaction", cordova.getActivity());
        }

        if (isBeginTransaction == false) {
            try {
                mPrinter.disconnect();
            }
            catch (Epos2Exception e) {
                // Do nothing
                return false;
            }
        }

        return true;
    }


    private void disconnectPrinter() {
        if (mPrinter == null) {
            return;
        }

        try {
            mPrinter.endTransaction();
        }
        catch (final Exception e) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    ShowMsg.showException(e, "endTransaction", cordova.getActivity());
                }
            });
        }

        try {
            // Log.i("停止打印","停止打印1");
            mPrinter.disconnect();
            // Log.i("停止打印","停止打印2");
        }
        catch (final Exception e) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    ShowMsg.showException(e, "disconnect", cordova.getActivity());
                }
            });
        }

        finalizeObject();
    }



    private boolean isPrintable(PrinterStatusInfo status) {
        if (status == null) {
            return false;
        }

        if (status.getConnection() == Printer.FALSE) {
            return false;
        }
        else if (status.getOnline() == Printer.FALSE) {
            return false;
        }
        else {
            ;//print available
        }

        return true;
    }


    private String makeErrorMessage(PrinterStatusInfo status) {
        String msg = "";
        if (status.getOnline() == Printer.FALSE) {
            msg += cordova.getActivity().getString(R.string.handlingmsg_err_offline);
        }
        if (status.getConnection() == Printer.FALSE) {
            msg += cordova.getActivity().getString(R.string.handlingmsg_err_no_response);
        }
        if (status.getCoverOpen() == Printer.TRUE) {
            msg += cordova.getActivity().getString(R.string.handlingmsg_err_cover_open);
        }
        if (status.getPaper() == Printer.PAPER_EMPTY) {
            msg += cordova.getActivity().getString(R.string.handlingmsg_err_receipt_end);
        }
        if (status.getPaperFeed() == Printer.TRUE || status.getPanelSwitch() == Printer.SWITCH_ON) {
            msg += cordova.getActivity().getString(R.string.handlingmsg_err_paper_feed);
        }
        if (status.getErrorStatus() == Printer.MECHANICAL_ERR || status.getErrorStatus() == Printer.AUTOCUTTER_ERR) {
            msg += cordova.getActivity().getString(R.string.handlingmsg_err_autocutter);
            msg += cordova.getActivity().getString(R.string.handlingmsg_err_need_recover);
        }
        if (status.getErrorStatus() == Printer.UNRECOVER_ERR) {
            msg += cordova.getActivity().getString(R.string.handlingmsg_err_unrecover);
        }
        if (status.getErrorStatus() == Printer.AUTORECOVER_ERR) {
            if (status.getAutoRecoverError() == Printer.HEAD_OVERHEAT) {
                msg += cordova.getActivity().getString(R.string.handlingmsg_err_overheat);
                msg += cordova.getActivity().getString(R.string.handlingmsg_err_head);
            }
            if (status.getAutoRecoverError() == Printer.MOTOR_OVERHEAT) {
                msg += cordova.getActivity().getString(R.string.handlingmsg_err_overheat);
                msg += cordova.getActivity().getString(R.string.handlingmsg_err_motor);
            }
            if (status.getAutoRecoverError() == Printer.BATTERY_OVERHEAT) {
                msg += cordova.getActivity().getString(R.string.handlingmsg_err_overheat);
                msg += cordova.getActivity().getString(R.string.handlingmsg_err_battery);
            }
            if (status.getAutoRecoverError() == Printer.WRONG_PAPER) {
                msg += cordova.getActivity().getString(R.string.handlingmsg_err_wrong_paper);
            }
        }
        if (status.getBatteryLevel() == Printer.BATTERY_LEVEL_0) {
            msg += cordova.getActivity().getString(R.string.handlingmsg_err_battery_real_end);
        }

        return msg;
    }

//     @Override
    public void onPtrReceive(final Printer printerObj, final int code, final PrinterStatusInfo status, final String printJobId) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public synchronized void run() {
                posprintermanager.this.callbackContext.success();
                // ShowMsg.showResult(code, makeErrorMessage(status), cordova.getActivity());
                Toast.makeText(cordova.getActivity(), "Result: " + getCodeText(code), Toast.LENGTH_SHORT)
                        .show();
                // dispPrinterWarnings(status);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        disconnectPrinter();
                    }
                }).start();
            }
        });
    }


    private static String getCodeText(int state) {
        String return_text = "";
        switch (state) {
            case Epos2CallbackCode.CODE_SUCCESS:
                return_text = "PRINT_SUCCESS";
                break;
            case Epos2CallbackCode.CODE_PRINTING:
                return_text = "PRINTING";
                break;
            case Epos2CallbackCode.CODE_ERR_AUTORECOVER:
                return_text = "ERR_AUTORECOVER";
                break;
            case Epos2CallbackCode.CODE_ERR_COVER_OPEN:
                return_text = "ERR_COVER_OPEN";
                break;
            case Epos2CallbackCode.CODE_ERR_CUTTER:
                return_text = "ERR_CUTTER";
                break;
            case Epos2CallbackCode.CODE_ERR_MECHANICAL:
                return_text = "ERR_MECHANICAL";
                break;
            case Epos2CallbackCode.CODE_ERR_EMPTY:
                return_text = "ERR_EMPTY";
                break;
            case Epos2CallbackCode.CODE_ERR_UNRECOVERABLE:
                return_text = "ERR_UNRECOVERABLE";
                break;
            case Epos2CallbackCode.CODE_ERR_FAILURE:
                return_text = "ERR_FAILURE";
                break;
            case Epos2CallbackCode.CODE_ERR_NOT_FOUND:
                return_text = "ERR_NOT_FOUND";
                break;
            case Epos2CallbackCode.CODE_ERR_SYSTEM:
                return_text = "ERR_SYSTEM";
                break;
            case Epos2CallbackCode.CODE_ERR_PORT:
                return_text = "ERR_PORT";
                break;
            case Epos2CallbackCode.CODE_ERR_TIMEOUT:
                return_text = "ERR_TIMEOUT";
                break;
            case Epos2CallbackCode.CODE_ERR_JOB_NOT_FOUND:
                return_text = "ERR_JOB_NOT_FOUND";
                break;
            case Epos2CallbackCode.CODE_ERR_SPOOLER:
                return_text = "ERR_SPOOLER";
                break;
            case Epos2CallbackCode.CODE_ERR_BATTERY_LOW:
                return_text = "ERR_BATTERY_LOW";
                break;
            default:
                return_text = String.format("%d", state);
                break;
        }
        return return_text;
    }



}
