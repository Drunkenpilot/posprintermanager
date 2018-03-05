package cordova.plugin.posprintermanager;

import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.widget.Toast;


import com.starmicronics.stario.PortInfo;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.stario.StarPrinterStatus;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import javax.security.auth.callback.Callback;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cordova.plugin.posprintermanager.RasterDocument.RasPageEndMode;
import cordova.plugin.posprintermanager.RasterDocument.RasSpeed;
import cordova.plugin.posprintermanager.RasterDocument.RasTopMargin;

public class StarPrinter extends CordovaPlugin  {
  // private CallbackContext callbackContext = null;
  private CallbackContext _callbackContext = null;
  String strInterface;

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    // your init code here
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (action.equals("checkStatus")) {
        String portName = args.getString(0);
        String portSettings = getPortSettingsOption(portName);
        this.checkStatus(portName, portSettings, callbackContext);
        return true;
    }else if (action.equals("portDiscovery")) {
      final String port = args.getString(0);
      final  CallbackContext _callbackContext = callbackContext;
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          portDiscovery(port, _callbackContext);
        }
      });
      return true;
    }else if (action.equals("printReceipt")) {


//      final  String portName = args.getString(0);
//      final  String portSettings = getPortSettingsOption(portName);
//      Log.d("portSettings","PortSettings : "+ portSettings);
//      // final  String receipt = args.getString(1);
//      final JSONArray receipt = args.optJSONArray(1);
//
//      final  CallbackContext _callbackContext = callbackContext;
//      cordova.getThreadPool().execute(new Runnable() {
//        public void run() {
//          PrintText(portName, portSettings, receipt, _callbackContext);
//        }
//      });
      return true;
    }
    return false;

  }


  public enum RasterCommand {
    Standard, Graphics
  };

  public void PrintText(String portName, String portSettings, JSONArray receipt, CallbackContext callbackContext) {


    Log.d("portName","portName = "+portName);
    try {
//      ReceiptBuilderExt receiptBuilder = new ReceiptBuilderExt(cordova.getActivity(),[545,0,0,0,15]);
      Bitmap testImg = null;//receiptBuilder.build(receipt);
      //
      int paperWidth = 576;

      boolean compressionEnable = false;
      boolean pageModeEnable = false;

      //
      if (portSettings.toUpperCase(Locale.US).contains("PORTABLE") && portSettings.toUpperCase(Locale.US).contains("ESCPOS")) {
        // MiniPrinterFunctions.PrintBitmapImage(this, portName, portSettings, getResources(), source, paperWidth, compressionEnable, pageModeEnable);
      } else {
        RasterCommand rasterType = RasterCommand.Standard;
        if (portSettings.toUpperCase(Locale.US).contains("PORTABLE")) {
          rasterType = RasterCommand.Graphics;
        }
        PrintBitmap(cordova.getActivity().getApplicationContext(), cordova.getActivity(), portName, portSettings, testImg, paperWidth, compressionEnable, rasterType, callbackContext);
      }
    }catch(JSONException e){
      e.printStackTrace();
//      callbackContext.error(e.getMessage());
    }catch (IllegalArgumentException e) {
      postMessage("Failure", "Size is too large.");
      callbackContext.error("Failure: Size is too large.");
    } catch (OutOfMemoryError e) {
      postMessage("Failure", "Size is too large.");
      callbackContext.error("Failure: Size is too large.");
    }

  }

  public static void PrintBitmap(Context context, Activity activity, String portName, String portSettings, Bitmap source, int maxWidth, boolean compressionEnable, RasterCommand rasterType, CallbackContext callbackContext) {
    try {
      ArrayList<byte[]> commands = new ArrayList<byte[]>();

      RasterDocument rasterDoc = new RasterDocument(RasSpeed.Medium, RasPageEndMode.FeedAndFullCut, RasPageEndMode.FeedAndFullCut, RasTopMargin.Standard, 0, 0, 0);
      StarBitmap starbitmap = new StarBitmap(source, false, maxWidth);

      if (rasterType == RasterCommand.Standard) {
        commands.add(rasterDoc.BeginDocumentCommandData());

        commands.add(starbitmap.getImageRasterDataForPrinting_Standard(compressionEnable));

        commands.add(rasterDoc.EndDocumentCommandData());
      } else {
        commands.add(starbitmap.getImageRasterDataForPrinting_graphic(compressionEnable));
        commands.add(new byte[] { 0x1b, 0x64, 0x02 }); // Feed to cutter position
      }

      sendCommand(context, activity, portName, portSettings, commands, callbackContext);
    } catch (OutOfMemoryError e) {
      callbackContext.error(e.getMessage());
      throw e;

    }
  }

  private static byte[] convertFromListByteArrayTobyteArray(List<byte[]> ByteArray) {
    int dataLength = 0;
    for (int i = 0; i < ByteArray.size(); i++) {
      dataLength += ByteArray.get(i).length;
    }

    int distPosition = 0;
    byte[] byteArray = new byte[dataLength];
    for (int i = 0; i < ByteArray.size(); i++) {
      System.arraycopy(ByteArray.get(i), 0, byteArray, distPosition, ByteArray.get(i).length);
      distPosition += ByteArray.get(i).length;
    }

    return byteArray;
  }


  private static void sendCommand( final Context context, final Activity activity, final String portName,final String portSettings, final ArrayList<byte[]> byteList, CallbackContext callbackContext) {

    StarIOPort port = null;
    try {
      /*
      * using StarIOPort3.1.jar (support USB Port) Android OS Version: upper 2.2
      */
      Log.d("portName","portName = "+portName);
      Log.d("portSettings","portSettings = "+portSettings);
      port = StarIOPort.getPort(portName, portSettings, 10000, context);
      /*
      * using StarIOPort.jar Android OS Version: under 2.1 port = StarIOPort.getPort(portName, portSettings, 10000);
      */
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        callbackContext.error(e.getMessage());
      }

      /*
      * Using Begin / End Checked Block method When sending large amounts of raster data,
      * adjust the value in the timeout in the "StarIOPort.getPort" in order to prevent
      * "timeout" of the "endCheckedBlock method" while a printing.
      *
      * If receipt print is success but timeout error occurs(Show message which is "There
      * was no response of the printer within the timeout period." ), need to change value
      * of timeout more longer in "StarIOPort.getPort" method.
      * (e.g.) 10000 -> 30000
      */
      StarPrinterStatus status = port.beginCheckedBlock();

      if (true == status.offline) {
        throw new StarIOPortException("A printer is offline");
      }

      byte[] commandToSendToPrinter = convertFromListByteArrayTobyteArray(byteList);
      port.writePort(commandToSendToPrinter, 0, commandToSendToPrinter.length);

      port.setEndCheckedBlockTimeoutMillis(30000);// Change the timeout time of endCheckedBlock method.

      status = port.endCheckedBlock();

      if (status.coverOpen == true) {
        throw new StarIOPortException("Printer cover is open");
      } else if (status.receiptPaperEmpty == true) {
        throw new StarIOPortException("Receipt paper is empty");
      } else if (status.offline == true) {
        throw new StarIOPortException("Printer is offline");
      }
    } catch (StarIOPortException e) {
      showToast(e.getMessage(), activity);
      callbackContext.error(e.getMessage());
    } finally {
      callbackContext.success();
      if (port != null) {
        try {
          StarIOPort.releasePort(port);
        } catch (StarIOPortException e) {
        }
      }
      showToast("Print success", activity);
    }

  }

  protected void postMessage(String titleText, String messageText) {

    AlertDialog.Builder dialog = new AlertDialog.Builder(cordova.getActivity());
    dialog.setNegativeButton("Ok", null);
    AlertDialog alert = dialog.create();
    alert.setTitle(titleText);
    alert.setMessage(messageText);
    alert.setCancelable(false);
    alert.show();
  }

  public static void showToast(final String msg, final Activity context ) {
    context.runOnUiThread(new Runnable() {
      public void run() {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT)
        .show();
      }
    });
  }

  public void portDiscovery(String strInterface, CallbackContext callbackContext) {

    JSONArray result = new JSONArray();
    try {

      if (strInterface.equals("LAN")) {
        result = getPortDiscovery("LAN");
      } else if (strInterface.equals("Bluetooth")) {
        result = getPortDiscovery("Bluetooth");
      } else if (strInterface.equals("USB")) {
        result = getPortDiscovery("USB");
      } else {
        result = getPortDiscovery("All");
      }

    } catch (StarIOPortException exception) {
      callbackContext.error(exception.getMessage());

    } catch (JSONException e) {

    } finally {

      Log.d("Discovered ports", result.toString());
      callbackContext.success(result);
    }
  }


  private JSONArray getPortDiscovery(String interfaceName) throws StarIOPortException, JSONException {
    List<PortInfo> BTPortList;
    List<PortInfo> TCPPortList;
    List<PortInfo> USBPortList;

    final Context context = this.cordova.getActivity();
    final ArrayList<PortInfo> arrayDiscovery = new ArrayList<PortInfo>();

    JSONArray arrayPorts = new JSONArray();


    if (interfaceName.equals("Bluetooth") || interfaceName.equals("All")) {
      BTPortList = StarIOPort.searchPrinter("BT:");

      for (PortInfo portInfo : BTPortList) {
        arrayDiscovery.add(portInfo);
      }
    }
    if (interfaceName.equals("LAN") || interfaceName.equals("All")) {
      TCPPortList = StarIOPort.searchPrinter("TCP:");

      for (PortInfo portInfo : TCPPortList) {
        arrayDiscovery.add(portInfo);
      }
    }
    if (interfaceName.equals("USB") || interfaceName.equals("All")) {
      USBPortList = StarIOPort.searchPrinter("USB:", context);

      for (PortInfo portInfo : USBPortList) {
        arrayDiscovery.add(portInfo);
      }
    }

    for (PortInfo discovery : arrayDiscovery) {
      String portName;

      JSONObject port = new JSONObject();
      port.put("name", discovery.getPortName());

      if (!discovery.getMacAddress().equals("")) {

        port.put("macAddress", discovery.getMacAddress());

        if (!discovery.getModelName().equals("")) {
          port.put("modelName", discovery.getModelName());
        }
      } else if (interfaceName.equals("USB") || interfaceName.equals("All")) {
        if (!discovery.getModelName().equals("")) {
          port.put("modelName", discovery.getModelName());
        }
        if (!discovery.getUSBSerialNumber().equals(" SN:")) {
          port.put("USBSerialNumber", discovery.getUSBSerialNumber());
        }
      }

      arrayPorts.put(port);
    }

    return arrayPorts;
  }




  private String getPortSettingsOption(String portName) {
    String portSettings = "";

    if (portName.toUpperCase(Locale.US).startsWith("TCP:")) {
      portSettings += ""; // retry to yes
    } else if (portName.toUpperCase(Locale.US).startsWith("BT:")) {
      portSettings += ";p"; // or ";p"
      portSettings += ";l"; // standard
    }

    return portSettings;
  }

  public void checkStatus(String portName, String portSettings, CallbackContext callbackContext) {

      final Context context = this.cordova.getActivity();
      final CallbackContext _callbackContext = callbackContext;

      final String _portName = portName;
      final String _portSettings = portSettings;

      cordova.getThreadPool()
              .execute(new Runnable() {
                  public void run() {

                      StarIOPort port = null;
                      try {

                          port = StarIOPort.getPort(_portName, _portSettings, 10000, context);

                          // A sleep is used to get time for the socket to completely open
                          try {
                              Thread.sleep(500);
                          } catch (InterruptedException e) {
                          }

                          StarPrinterStatus status;
                          status = port.retreiveStatus();

                          JSONObject json = new JSONObject();
                          try {
                              json.put("offline", status.offline);
                              json.put("coverOpen", status.coverOpen);
                              json.put("cutterError", status.cutterError);
                              json.put("receiptPaperEmpty", status.receiptPaperEmpty);
                          } catch (JSONException ex) {

                          } finally {
                              _callbackContext.success(json);
                          }


                      } catch (StarIOPortException e) {
                          _callbackContext.error("Failed to connect to printer :" + e.getMessage());
                      } finally {

                          if (port != null) {
                              try {
                                  StarIOPort.releasePort(port);
                              } catch (StarIOPortException e) {
                                  _callbackContext.error("Failed to connect to printer" + e.getMessage());
                              }
                          }

                      }


                  }
              });
  }





}
