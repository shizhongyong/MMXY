package cn.boweikeji.wuliu.webview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.webkit.JavascriptInterface;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by shizy on 2018/6/4.
 * 为JavaScript提供接口，用来调起Native方法
 */

public class JsInterface {

	public static final String NAME = "Native";

	private static final int RC_CAPTURE = 0x00F1;

	public interface CallBack {
		void invokeJsMethod(String method, String data);
	}

	private WeakReference<Activity> mActivityRef;
	private CallBack mCallBack;

	// 扫码回调方法
	private String mScanCallBack;
	private String mCaptureCallBack;
	private String mCurrentPhotoPath;

	public JsInterface(Activity activity, CallBack callBack) {
		mActivityRef = new WeakReference<>(activity);
		mCallBack = callBack;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == IntentIntegrator.REQUEST_CODE) {
			handleScanResult(requestCode, resultCode, data);
		} else {
			switch (requestCode) {
				case RC_CAPTURE:
					handleCaptureResult(resultCode, data);
					break;
			}
		}
	}

	/**
	 * 处理扫码结果
	 *
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 */
	private void handleScanResult(int requestCode, int resultCode, Intent data) {
		IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
		if (result != null && result.getContents() != null) {
			if (mCallBack != null) {
				mCallBack.invokeJsMethod(mScanCallBack, result.getContents());
			}
		}
	}

	private void handleCaptureResult(int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			Uri uri = Uri.fromFile(new File(mCurrentPhotoPath));
			mCallBack.invokeJsMethod(mCaptureCallBack, uri.toString());
		}
	}

	/**
	 * 扫码
	 *
	 * @param callback
	 */
	@JavascriptInterface
	public void scan(String callback) {
		final Activity activity = mActivityRef.get();
		if (activity == null) {
			return;
		}
		mScanCallBack = callback;

		IntentIntegrator integrator = new IntentIntegrator(activity);
		integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
		integrator.setPrompt(activity.getString(R.string.msg_scan));
		integrator.setCameraId(0);  // Use a specific camera of the device
		integrator.setBeepEnabled(true);
		integrator.setBarcodeImageEnabled(true);
		integrator.initiateScan();
	}

	/**
	 * 拍照
	 */
	@JavascriptInterface
	public void capture(String callback) {
		final Activity activity = mActivityRef.get();
		if (activity == null) {
			return;
		}
		mCaptureCallBack = callback;

		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (intent.resolveActivity(activity.getPackageManager()) != null) {
			activity.startActivityForResult(intent, RC_CAPTURE);

			File photoFile = null;
			try {
				photoFile = createImageFile(activity);
			} catch (IOException ex) {
				// Error occurred while creating the File
			}
			// Continue only if the File was successfully created
			if (photoFile != null) {
				Uri photoURI = FileProvider.getUriForFile(activity,
						"com.example.android.fileprovider",
						photoFile);
				intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
				activity.startActivityForResult(intent, RC_CAPTURE);
			}
		}
	}

	private File createImageFile(Context context) throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = "JPEG_" + timeStamp + "_";
		File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		File image = File.createTempFile(
				imageFileName,  /* prefix */
				".jpg",         /* suffix */
				storageDir      /* directory */
		);

		// Save a file: path for use with ACTION_VIEW intents
		mCurrentPhotoPath = image.getAbsolutePath();
		return image;
	}

}
