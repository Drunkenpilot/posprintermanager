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
import android.app.Activity;
import android.content.DialogInterface;
import android.app.ProgressDialog;
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
    private ArrayList<HashMap<String, String>> mPrinterList = null;
	private FilterOption mFilterOption = null;
	private CallbackContext callbackContext = null;
	private Printer  mPrinter = null;

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

            return true;
        }

        return false;
    }

    private void initSearchPrinter(final int millSeconds, final String vendor, final String type) {
        if(vendor.equals("EPSON")) {    
            // EpsonPrinter epsonPrinter = new EpsonPrinter(cordova.getActivity(), callbackContext);
            cordova.getThreadPool().execute(new Runnable() {
				public void run() {
                    mPrinterList = new ArrayList<HashMap<String, String>>();
					mFilterOption = new FilterOption();
					mFilterOption.setDeviceType(Discovery.TYPE_PRINTER);
					mFilterOption.setEpsonFilter(Discovery.FILTER_NAME);
					mFilterOption.setPortType(Discovery.PORTTYPE_ALL);
					try {
						onPreExecute();
						Discovery.start(cordova.getActivity(), mFilterOption, mDiscoveryListener);
						Thread.sleep(millSeconds);
					} catch (Epos2Exception e) {
						Log.i("测试", "e:" + e.getErrorStatus());
						onPostExecute();
						ShowMsg.showException(e, "start", cordova.getActivity());
						//EpsonPrinter.this.callbackContext.error("e:" + e.getErrorStatus());
					} catch (InterruptedException e) {
						Log.i("测试", "InterruptedException: " + e.getMessage());
					} finally {
						stopDiscovery();
					}

                }
            });
        } else if (vendor.equals("STAR")) {

        } else {
            this.callbackContext.error("no");
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



	@Override
	public void onDestroy() {
		Log.i("停止搜索", "停止1");
		super.onDestroy();

		stopDiscovery();
		mFilterOption = null;
	}

	private void stopDiscovery() {
		while (true) {
			try {
				Discovery.stop();
				break;
			} catch (Epos2Exception e) {
				if (e.getErrorStatus() != Epos2Exception.ERR_PROCESSING) {
					break;
				}
			}
		}

		JSONArray jsonArray = new JSONArray();
		for (HashMap<String, String> one : mPrinterList) {
			JSONObject jsonObject = new JSONObject();

			try {
				jsonObject.put("PrinterName", one.get("PrinterName"));
				jsonObject.put("Target", one.get("Target"));
			} catch (JSONException e) {
				onPostExecute();
			}

			jsonArray.put(jsonObject);

		}
		callbackContext.success(jsonArray);
		onPostExecute();
	}

	private ProgressDialog progressDialog;   // class variable

	private void showProgressDialog(final String title, final String message)
	{
		cordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				progressDialog = new ProgressDialog(cordova.getActivity());

				progressDialog.setTitle(title); //title

				progressDialog.setMessage(message); // message

				progressDialog.setCancelable(false);

				progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						stopDiscovery();
						dialog.dismiss();
					}
				});

				progressDialog.show();
			}
		});
	}

	protected void onPreExecute()
	{
		showProgressDialog("Searching Printers","Please wait...");
	}

	protected void onPostExecute()
	{
		if(progressDialog != null && progressDialog.isShowing())
		{
			progressDialog.dismiss();
		}
	}

	private DiscoveryListener mDiscoveryListener = new DiscoveryListener() {
		@Override
		public void onDiscovery(final DeviceInfo deviceInfo) {
			Log.i("测试", "测试5");
			HashMap<String, String> item = new HashMap<String, String>();
			item.put("PrinterName", deviceInfo.getDeviceName());
			item.put("Target", deviceInfo.getTarget());
			Log.i("测试", "PrinterName: " + deviceInfo.getDeviceName() + "; " + "Target: " + deviceInfo.getTarget());

			mPrinterList.add(item);
			for (HashMap<String, String> one : mPrinterList) {
				Log.i("测试", "mPrinterList: " + one.get("PrinterName") + " ~ " + one.get("Target"));
			}
			Log.i("测试", "测试6");
			this.showToast(deviceInfo);
			// mPrinterListAdapter.notifyDataSetChanged();
			// return item;
			Log.i("测试", "测试7");
			Log.i("测试", "测试8");
		}

		public void showToast(final DeviceInfo deviceInfo) {
			cordova.getActivity().runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(cordova.getActivity(), "PrinterName: " + deviceInfo.getDeviceName(),
					Toast.LENGTH_SHORT).show();
					Toast.makeText(cordova.getActivity(), "Target: " + deviceInfo.getTarget(), Toast.LENGTH_SHORT)
					.show();
				}
			});
		}

	};

    
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
