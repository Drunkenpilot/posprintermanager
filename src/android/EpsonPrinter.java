package cordova.plugin.posprintermanager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.ArrayList;

import com.betaresto.terminal.R;

import com.epson.epos2.Epos2Exception;
import com.epson.epos2.discovery.DeviceInfo;
import com.epson.epos2.discovery.Discovery;
import com.epson.epos2.discovery.DiscoveryListener;
import com.epson.epos2.discovery.FilterOption;

import com.epson.epos2.printer.Printer;
import com.epson.epos2.printer.PrinterStatusInfo;
import com.epson.epos2.printer.ReceiveListener;
import com.epson.epos2.Epos2CallbackCode;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;

public class EpsonPrinter {

	private Activity activity;

    private ArrayList<HashMap<String, String>> mPrinterList = null;
	private FilterOption mFilterOption = null;
	private CallbackContext callbackContext = null;
	private Printer  mPrinter = null;


    public EpsonPrinter(Activity activity, CallbackContext callbackContext) {
        this.activity = activity;
        this.callbackContext = callbackContext;
    }

    public void search(final int millSeconds, Activity activity) {

					mPrinterList = new ArrayList<HashMap<String, String>>();
					mFilterOption = new FilterOption();
					mFilterOption.setDeviceType(Discovery.TYPE_PRINTER);
					mFilterOption.setEpsonFilter(Discovery.FILTER_NAME);
					mFilterOption.setPortType(Discovery.PORTTYPE_ALL);
					try {
						onPreExecute();
						Discovery.start(activity, mFilterOption, mDiscoveryListener);
						Thread.sleep(millSeconds);
					} catch (Epos2Exception e) {
						Log.i("测试", "e:" + e.getErrorStatus());
						onPostExecute();
						ShowMsg.showException(e, "start", activity);
						//EpsonPrinter.this.callbackContext.error("e:" + e.getErrorStatus());
					} catch (InterruptedException e) {
						Log.i("测试", "InterruptedException: " + e.getMessage());
					} finally {
						stopDiscovery();
					}

    }


    private ProgressDialog progressDialog;   // class variable

	private void showProgressDialog(final String title, final String message)
	{
		activity.runOnUiThread(new Runnable() {
			public void run() {
				progressDialog = new ProgressDialog(activity);

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
    
    // @Override
	// public void onDestroy() {
	// 	Log.i("停止搜索", "停止1");
	// 	super.onDestroy();

	// 	stopDiscovery();
	// 	mFilterOption = null;
	// }

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
			activity.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(activity, "PrinterName: " + deviceInfo.getDeviceName(),
					Toast.LENGTH_SHORT).show();
					Toast.makeText(activity, "Target: " + deviceInfo.getTarget(), Toast.LENGTH_SHORT)
					.show();
				}
			});
		}

	};

}