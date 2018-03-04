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

public class EpsonPrinter extends CordovaPlugin implements ReceiveListener {

	private Activity activity;

    private ArrayList<HashMap<String, String>> mPrinterList = null;
	private FilterOption mFilterOption = null;
	private CallbackContext callbackContext = null;
	private Printer  mPrinter = null;


    public EpsonPrinter(Activity context, CallbackContext callbackContext) {
        activity = context;
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

    public void print(final Bitmap printRaw, final JSONArray addPulse, final int printerSeries, final int lang, final String printTarget, Activity activity) {
					//			printRaw Bitmap create by ReceiptBuilderExt
					//			addPulse cash drawer  [ (0 no , 1 yes), (0-1  2pin, 5pin) , (time 0-4  100ms-500ms) ]
					//          addSound  Buzzer //TODO
					//			printerSeries  example { "model": "TM-T20, TM-T20II, TM-T20II-i", "value": "6" }
					//			lang ANK model 0, Simplified Chinese model 1, etc
					//          printTarget USB:/dev/////  BT: // TCP:192.168.1.101
		new Thread(new Runnable() {
			@Override
			public void run() {
				runPrintReceiptSequence(printRaw, addPulse, printerSeries, lang, printTarget);
			}
		}).run();

	}



	private boolean runPrintReceiptSequence(final Bitmap printRaw, final JSONArray addPulse, final int printerSeries, final int lang, final String printTarget) {
		if (!initializeObject(printerSeries, lang)) {
			return false;
		}

		if (!createReceiptData(printRaw, addPulse)) {
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
			mPrinter = new Printer(printerSeries,lang, activity.getApplicationContext());
		}
		catch (Exception e) {
//			EpsonPrinter.this.callbackContext.error("e:" + ((Epos2Exception) e).getErrorStatus());
			ShowMsg.showException(e, "Printer", activity);
			return false;
		}

		mPrinter.setReceiveEventListener(this);

		Log.i("调试","调试+");
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
//			EpsonPrinter.this.callbackContext.error("e:" + makeErrorMessage(status));
			ShowMsg.showMsg(makeErrorMessage(status), activity);
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
			mPrinter.disconnect();
		}
		catch (Exception e) {
			ShowMsg.showException(e, "sendData", activity);
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



	private boolean createReceiptData(final Bitmap printRaw, final JSONArray addPulse) {
		if (mPrinter == null) {
			return false;
		}
		String method = "";
		// 		Line mode
		StringBuilder textData = new StringBuilder();

		try {

			//			Generate main content
//			try {
				method = "addTextAlign";
				mPrinter.addTextAlign(Printer.ALIGN_LEFT);
//				textData.append("THE STORE 123 (555) 555 – 5555\n");
//				textData.append("STORE DIRECTOR – John Smith\n");
//				textData.append("\n");
//				textData.append("7/01/07 16:58 6153 05 0191 134\n");
//				textData.append("ST# 21 OP# 001 TE# 01 TR# 747\n");
//				textData.append("------------------------------\n");
//				method = "addText";
//				mPrinter.addText(textData.toString());
//				textData.delete(0, textData.length());
				method = "addImage";
				mPrinter.addImage(
						printRaw, 0, 0,
						printRaw.getWidth(),
						printRaw.getHeight(),
						Printer.COLOR_1,
						Printer.MODE_MONO,
						Printer.HALFTONE_DITHER,
						Printer.PARAM_DEFAULT,
						Printer.COMPRESS_AUTO
				);

				method = "addFeedLine";
				mPrinter.addFeedLine(1);

//  TODO add QR Code
//				method = "addTextAlign";
//				mPrinter.addTextAlign(Printer.ALIGN_CENTER);
//				//			QR code
//				method = "addSymbol";
//				mPrinter.addSymbol(this.activity.getResources().getString(R.string.url), Printer.SYMBOL_QRCODE_MODEL_2, Printer.LEVEL_L, 3, 3, 3);
//
//				method = "addText";
//				mPrinter.addText(this.activity.getResources().getString(R.string.website));
//
//				method = "addFeedLine";
//				mPrinter.addFeedLine(1);
//				//			code bar
//				//		  final int barcodeWidth = 2;
//				//		  final int barcodeHeight = 100;
//				//			method = "addBarcode";
//				//			mPrinter.addBarcode("01209457",
//				//			Printer.BARCODE_CODE39,
//				//			Printer.HRI_BELOW,
//				//			Printer.FONT_A,
//				//			barcodeWidth,
//				//			barcodeHeight);


				method = "addCut";
				mPrinter.addCut(Printer.CUT_FEED);

//			final int pulse = addPulse.optInt(0);
//
//			if(pulse == 1){
//
//				final int drawer = addPulse.optInt(1);
//				final int drawerPin;
//				final int signal = addPulse.optInt(2);
//				final int signalTime;
//				switch (drawer) {
//					case 0:  drawerPin = Printer.DRAWER_2PIN;
//						break;
//					case 1:  drawerPin = Printer.DRAWER_5PIN;
//						break;
//					default: drawerPin = Printer.PARAM_DEFAULT;
//						break;
//				}
//
//				switch (signal) {
//					case 0:  signalTime = Printer.PULSE_100;
//						break;
//					case 1:  signalTime = Printer.PULSE_200;
//						break;
//					case 2:  signalTime = Printer.PULSE_300;
//						break;
//					case 3:  signalTime = Printer.PULSE_400;
//						break;
//					case 4:  signalTime = Printer.PULSE_500;
//						break;
//					default: signalTime = Printer.PARAM_DEFAULT;
//						break;
//				}
//
//				method = "addPulse";
//				mPrinter.addPulse(drawerPin, signalTime);
//			}

//			} catch (JSONException e){
//
//			}
		}
		catch (Exception e) {
			ShowMsg.showException(e, method, activity);
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
		showToast("Data cleared");
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
//			EpsonPrinter.this.callbackContext.error("e:" + ((Epos2Exception) e).getErrorStatus());
			ShowMsg.showException(e, "connect", activity);
			return false;
		}

		try {
			mPrinter.beginTransaction();
			isBeginTransaction = true;
		}
		catch (Exception e) {
//			EpsonPrinter.this.callbackContext.error("e:" + ((Epos2Exception) e).getErrorStatus());
			ShowMsg.showException(e, "beginTransaction", activity);
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
		Log.i("调试","调试****");
		if (mPrinter == null) {
			Log.i("调试","调试5*");
			return;
		}

		try {
			mPrinter.endTransaction();
		}
		catch (final Exception e) {
			EpsonPrinter.this.callbackContext.error("e:" + ((Epos2Exception) e).getErrorStatus());
			activity.runOnUiThread(new Runnable() {
				@Override
				public synchronized void run() {
					ShowMsg.showException(e, "endTransaction", activity);
				}
			});
		}

		try {
			 Log.i("停止打印","停止打印1");
			mPrinter.disconnect();
			 Log.i("停止打印","停止打印2");
			showToast("disconnected");
		}
		catch (final Exception e) {
			EpsonPrinter.this.callbackContext.error("e:" + ((Epos2Exception) e).getErrorStatus());
			activity.runOnUiThread(new Runnable() {
				@Override
				public synchronized void run() {
					ShowMsg.showException(e, "disconnect", activity);
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
			msg += activity.getString(R.string.handlingmsg_err_offline);
		}
		if (status.getConnection() == Printer.FALSE) {
			msg += activity.getString(R.string.handlingmsg_err_no_response);
		}
		if (status.getCoverOpen() == Printer.TRUE) {
			msg += activity.getString(R.string.handlingmsg_err_cover_open);
		}
		if (status.getPaper() == Printer.PAPER_EMPTY) {
			msg += activity.getString(R.string.handlingmsg_err_receipt_end);
		}
		if (status.getPaperFeed() == Printer.TRUE || status.getPanelSwitch() == Printer.SWITCH_ON) {
			msg += activity.getString(R.string.handlingmsg_err_paper_feed);
		}
		if (status.getErrorStatus() == Printer.MECHANICAL_ERR || status.getErrorStatus() == Printer.AUTOCUTTER_ERR) {
			msg += activity.getString(R.string.handlingmsg_err_autocutter);
			msg += activity.getString(R.string.handlingmsg_err_need_recover);
		}
		if (status.getErrorStatus() == Printer.UNRECOVER_ERR) {
			msg += activity.getString(R.string.handlingmsg_err_unrecover);
		}
		if (status.getErrorStatus() == Printer.AUTORECOVER_ERR) {
			if (status.getAutoRecoverError() == Printer.HEAD_OVERHEAT) {
				msg += activity.getString(R.string.handlingmsg_err_overheat);
				msg += activity.getString(R.string.handlingmsg_err_head);
			}
			if (status.getAutoRecoverError() == Printer.MOTOR_OVERHEAT) {
				msg += activity.getString(R.string.handlingmsg_err_overheat);
				msg += activity.getString(R.string.handlingmsg_err_motor);
			}
			if (status.getAutoRecoverError() == Printer.BATTERY_OVERHEAT) {
				msg += activity.getString(R.string.handlingmsg_err_overheat);
				msg += activity.getString(R.string.handlingmsg_err_battery);
			}
			if (status.getAutoRecoverError() == Printer.WRONG_PAPER) {
				msg += activity.getString(R.string.handlingmsg_err_wrong_paper);
			}
		}
		if (status.getBatteryLevel() == Printer.BATTERY_LEVEL_0) {
			msg += activity.getString(R.string.handlingmsg_err_battery_real_end);
		}

		return msg;
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

	@Override
	public void onPtrReceive(final Printer printerObj, final int code, final PrinterStatusInfo status, final String printJobId) {
		Log.i("调试","调试*");

				Log.i("调试","调试**");
				callbackContext.success();
//				ShowMsg.showResult(code, makeErrorMessage(status), activity);
				showToast(getCodeText(code));
				new Thread(new Runnable() {
					@Override
					public void run() {
						Log.i("调试","调试***");
						disconnectPrinter();
					}
				}).start();


	}

	private void showToast(final String message) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public synchronized void run() {
				Toast.makeText(activity, "Message:  " + message,
						Toast.LENGTH_SHORT).show();
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