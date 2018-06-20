package cn.boweikeji.mmxy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
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

import cn.boweikeji.mmxy.util.ImageUtil;

/**
 * Created by shizy on 2018/6/4.
 * 为JavaScript提供接口，用来调起Native方法
 */

public class JsInterface {

	public static final String NAME = "Native";

	private static final int RC_CAPTURE = 0x00F1;
	private static final int RC_ALBUM = 0x00F2;

	public interface CallBack {
		void invokeJsMethod(String method, String data);
	}

	private WeakReference<Activity> mActivityRef;
	private Handler mThreadHandler;
	private CallBack mCallBack;

	// 扫码回调方法
	private String mScanCallBack;
	private String mCaptureCallBack;
	private String mAlbumCallBack;
	private String mCurrentPhotoPath;

	public JsInterface(Activity activity, CallBack callBack) {
		mActivityRef = new WeakReference<>(activity);
		mCallBack = callBack;
		HandlerThread thread = new HandlerThread(getClass().getSimpleName());
		thread.start();
		mThreadHandler = new Handler(thread.getLooper());
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == IntentIntegrator.REQUEST_CODE) {
			handleScanResult(requestCode, resultCode, data);
		} else {
			switch (requestCode) {
				case RC_CAPTURE:
					handleCaptureResult(resultCode, data);
					break;
				case RC_ALBUM:
					handleAlbumResult(resultCode, data);
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
		if (mCallBack == null) {
			return;
		}

		IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
		if (result != null && result.getContents() != null) {
			mCallBack.invokeJsMethod(mScanCallBack, result.getContents());
		} else {
			mCallBack.invokeJsMethod(mScanCallBack, null);
		}
	}

	private void handleCaptureResult(int resultCode, Intent data) {
		if (mCallBack == null) {
			return;
		}

		if (resultCode == Activity.RESULT_OK) {
//			Uri uri = Uri.fromFile(new File(mCurrentPhotoPath));
			mCallBack.invokeJsMethod(mCaptureCallBack, mCurrentPhotoPath);
		} else {
			mCallBack.invokeJsMethod(mCaptureCallBack, null);
		}
	}

	private void handleAlbumResult(int resultCode, Intent data) {
		if (mCallBack == null) {
			return;
		}

		final Activity activity = mActivityRef.get();
		if (activity == null) {
			return;
		}

		Uri uri = (data == null || resultCode != Activity.RESULT_OK) ? null : data.getData();
		mCallBack.invokeJsMethod(mAlbumCallBack, uri == null ? null : ImageUtil.getImagePath(activity, uri));
	}

	private void sendCompressImageResult(final File target, final boolean success, final String callback) {
		final Activity activity = mActivityRef.get();
		if (activity == null) {
			return;
		}

		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (mCallBack != null) {
					mCallBack.invokeJsMethod(callback, success ? target.getAbsolutePath() : null);
				}
			}
		});
	}

	private void startScan(Activity activity) {
		IntentIntegrator integrator = new IntentIntegrator(activity);
		integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
		integrator.setPrompt(activity.getString(R.string.msg_scan));
		integrator.setCameraId(0);  // Use a specific camera of the device
		integrator.setBeepEnabled(true);
		integrator.setBarcodeImageEnabled(true);
		integrator.initiateScan();
	}

	private void startCapture(Activity activity) {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (intent.resolveActivity(activity.getPackageManager()) != null) {
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

	private void startAlbum(Activity activity) {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("image/*");

		if (intent.resolveActivity(activity.getPackageManager()) != null) {
			activity.startActivityForResult(intent, RC_ALBUM);
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
		image.deleteOnExit();
		return image;
	}

	/**
	 * 扫码
	 *
	 * @param callback 回调函数名称
	 */
	@JavascriptInterface
	public void scan(String callback) {
		final Activity activity = mActivityRef.get();
		if (activity == null) {
			return;
		}
		mScanCallBack = callback;

		startScan(activity);
	}

	/**
	 * 拍照
	 *
	 * @param callback 回调函数名称
	 */
	@JavascriptInterface
	public void capture(String callback) {
		final Activity activity = mActivityRef.get();
		if (activity == null) {
			return;
		}
		mCaptureCallBack = callback;

		startCapture(activity);
	}

	/**
	 * 打开相册，选择照片
	 *
	 * @param callback 回调函数名称
	 */
	@JavascriptInterface
	public void album(String callback) {
		final Activity activity = mActivityRef.get();
		if (activity == null) {
			return;
		}
		mAlbumCallBack = callback;

		startAlbum(activity);
	}

	/**
	 * 压缩图片
	 *
	 * @param path     图片路径
	 * @param targetW  压缩后的高度
	 * @param targetH  压缩后的宽度
	 * @param quality  压缩后的质量[0-100]
	 * @param callback 回调函数名称
	 */
	@JavascriptInterface
	public void compressImage(final String path, final int targetW, final int targetH,
							  final int quality, final String callback) {
		final Activity activity = mActivityRef.get();
		if (activity == null) {
			return;
		}

		mThreadHandler.post(new Runnable() {
			@Override
			public void run() {
				File target = null;
				boolean success = false;
				try {
					target = createImageFile(activity);
					success = ImageUtil.compressImage(path, target, targetW, targetH, quality);
				} catch (IOException e) {
					e.printStackTrace();
				}
				sendCompressImageResult(target, success, callback);
			}
		});
	}
}
