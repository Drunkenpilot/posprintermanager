package cordova.plugin.posprintermanager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.danielfelgar.drawreceiptlib.ReceiptBuilder;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Base64;
import android.util.Log;

public class ReceiptBuilderExt {
	private ReceiptBuilder builder;

	// attribute: Typeface(string), Align(LEFT,CENTER,RIGHT;),
	// TextSize(float)
	private Activity activity;

	/**
	 *
	 * <pre>
	 *  element:
	 *  	Text(string,[boolean]),Image(Bitmap),BlankSpace(int),Paragraph,Line([int]);
	 *  	Typeface(string), Align(LEFT,CENTER,RIGHT), TextSize(float)
	
	 * {
	 * 	name: element, required
	 * 	value: string / int,
	 * 	newLine: boolean, (Text)
	 * }
	 *
	 * </pre>
	 */

	public ReceiptBuilderExt(Activity activity, JSONArray printCanvas) {
		Log.i("打印数据", "打印数据1");

		int width = printCanvas.optInt(0);
		int marginTop = printCanvas.optInt(1);
		int marginRight = printCanvas.optInt(2);
		int marginBottom = printCanvas.optInt(3);
		int marginLeft = printCanvas.optInt(4);

		Log.d("width", "Width = " + width);
		this.activity = activity;
		builder = new ReceiptBuilder(width);
		builder.setColor(Color.BLACK);
		builder.setMarginBottom(marginBottom).setMarginLeft(marginLeft).setMarginRight(marginRight)
				.setMarginTop(marginTop);
	}

	public Bitmap build(JSONArray printContent) throws JSONException {
		if (printContent == null || printContent.length() == 0) {
			Log.i("printContent", "printContent is null");
			return null;
		}

		for (int i = 0; i < printContent.length(); i++) {
			JSONArray one = printContent.getJSONArray(i);
			line(one);
		}

		return builder.build();
	}

	private void line(JSONArray oneLine) throws JSONException {
		for (int i = 0; i < oneLine.length(); i++) {
			JSONObject elem = oneLine.getJSONObject(i);
			Log.i("elem.toString()", elem.toString());

			if (!elem.has("name")) {
				continue;
			}

			String name = elem.getString("name");
			if (name == null || name.length() == 0) {
				continue;
			}
			if (name.equals("Text")) {
				buildText(elem);
			} else if (name.equals("Image")) {
				buildImage(elem);
			} else if (name.equals("BlankSpace")) {
				buildBlankSpace(elem);
			} else if (name.equals("Paragraph")) {
				buildParagraph(elem);
			} else if (name.equals("Line")) {
				buildLine(elem);
			} else if (name.equals("Typeface")) {
				buildTypeface(elem);
			} else if (name.equals("Align")) {
				buildAlign(elem);
			} else if (name.equals("TextSize")) {
				buildTextSize(elem);
			} else if (name.equals("BgColor")) {
				setBackgroudColor(elem);
			} else {
				continue;
			}
		}

	}

	private void setBackgroudColor(JSONObject elem) {
		int value = elem.optInt("value");
		builder.setBackgroudColor(value);
	}

	private void buildTextSize(JSONObject elem) {
		Double value = elem.optDouble("value");
		builder.setTextSize(value.floatValue());
	}

	private void buildAlign(JSONObject elem) {
		String value = elem.optString("value");
		builder.setAlign(Paint.Align.valueOf(value));
	}

	private void buildTypeface(JSONObject elem) {
		String value = elem.optString("value");
		builder.setTypeface(activity, value);
	}

	private void buildLine(JSONObject elem) {
		if (elem.isNull("value")) {
			builder.addLine();
		} else {
			int value = elem.optInt("value");
			builder.addLine(value);
		}
	}

	private void buildParagraph(JSONObject elem) {
		builder.addParagraph();
	}

	private void buildBlankSpace(JSONObject elem) {
		int value = elem.optInt("value");
		builder.addBlankSpace(value);
	}

	private void buildImage(JSONObject elem) {
		final String encodedBase64Url = elem.optString("value");
		Bitmap bitmap = ImageUtil.convert(encodedBase64Url);
		builder.addImage(bitmap);
	}

	private void buildText(JSONObject elem) {
		String value = elem.optString("value");
		if (elem.isNull("newLine")) {
			builder.addText(value);
		} else {
			boolean newLine = elem.optBoolean("newLine");
			Log.i("newLine", "newLine = " + newLine);
			builder.addText(value, newLine);
		}

	}
}
