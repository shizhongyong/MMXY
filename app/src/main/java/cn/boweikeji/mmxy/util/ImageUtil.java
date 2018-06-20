package cn.boweikeji.mmxy.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by shizy on 2018/6/5.
 */

public class ImageUtil {

	private static final String DOCUMENT_EXTERNAL_STORAGE = "com.android.externalstorage.documents";
	private static final String DOCUMENT_DOWNLOADS = "com.android.providers.downloads.documents";
	private static final String DOCUMENT_MEDIA = "com.android.providers.media.documents";
	private static final String DOCUMENT_GOOGLE_PHOTOS = "com.google.android.apps.photos.content";

	/**
	 * 缩略图
	 *
	 * @param path    图片路径
	 * @param targetW 宽度
	 * @param targetH 高度
	 * @return
	 */
	public static Bitmap getThumbnail(String path, int targetW, int targetH) {
		// Get the dimensions of the bitmap
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		int scaleFactor = 1;
		if (targetW != 0 && targetH != 0) {
			bmOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(path, bmOptions);
			int photoW = bmOptions.outWidth;
			int photoH = bmOptions.outHeight;

			// Determine how much to scale down the image
			scaleFactor = Math.min(photoW / targetW, photoH / targetH);
		}

		// Decode the image file into a Bitmap
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		return BitmapFactory.decodeFile(path, bmOptions);
	}

	/**
	 * 压缩图片
	 *
	 * @param imgPath
	 * @param target
	 * @param targetW
	 * @param targetH
	 * @param quality
	 * @return
	 */
	public static boolean compressImage(String imgPath, File target, int targetW, int targetH, int quality) {
		if (imgPath == null || target == null) {
			return false;
		}

		Bitmap bitmap = getThumbnail(imgPath, targetW, targetH);
		if (bitmap != null) {
			if (quality <= 0 || quality > 100) {
				quality = 100;
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
			try {
				FileOutputStream fos = new FileOutputStream(target);
				fos.write(baos.toByteArray());
				fos.flush();
				fos.close();
				baos.close();

				bitmap.recycle();

				return true;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * 获取图片真实路径
	 *
	 * @param context
	 * @param uri
	 * @return
	 */
	public static String getImagePath(final Context context, final Uri uri) {

		final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

		// DocumentProvider
		if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
			// ExternalStorageProvider
			if (isExternalStorageDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				if ("primary".equalsIgnoreCase(type)) {
					return Environment.getExternalStorageDirectory() + "/" + split[1];
				}

			}
			// DownloadsProvider
			else if (isDownloadsDocument(uri)) {
				final String id = DocumentsContract.getDocumentId(uri);
				final Uri contentUri = ContentUris.withAppendedId(
						Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

				return getDataColumn(context, contentUri, null, null);
			}
			// MediaProvider
			else if (isMediaDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				Uri contentUri = null;
				if ("image".equals(type)) {
					contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				} else if ("video".equals(type)) {
					contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				} else if ("audio".equals(type)) {
					contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				}

				final String selection = "_id=?";
				final String[] selectionArgs = new String[]{
						split[1]
				};

				return getDataColumn(context, contentUri, selection, selectionArgs);
			}
		}
		// MediaStore (and general)
		else if ("content".equalsIgnoreCase(uri.getScheme())) {
			// Return the remote address
			if (isGooglePhotosUri(uri)) {
				return uri.getLastPathSegment();
			}

			return getDataColumn(context, uri, null, null);
		}
		// File
		else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}

	public static boolean isExternalStorageDocument(Uri uri) {
		return DOCUMENT_EXTERNAL_STORAGE.equals(uri.getAuthority());
	}

	public static boolean isDownloadsDocument(Uri uri) {
		return DOCUMENT_DOWNLOADS.equals(uri.getAuthority());
	}

	public static boolean isMediaDocument(Uri uri) {
		return DOCUMENT_MEDIA.equals(uri.getAuthority());
	}

	public static boolean isGooglePhotosUri(Uri uri) {
		return DOCUMENT_GOOGLE_PHOTOS.equals(uri.getAuthority());
	}

	public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = {column};

		try {
			cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
			if (cursor != null && cursor.moveToFirst()) {
				final int index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(index);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return null;
	}

}
