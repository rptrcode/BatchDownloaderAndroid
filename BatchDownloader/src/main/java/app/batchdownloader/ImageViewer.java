package app.batchdownloader;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ImageViewer {
	private static final int IMAGE_MAX_SIZE = 550;

	public ImageViewer(Activity activity, String filepath, String filename) {
		File directory = new File(filepath);
		if (directory.exists()) {
			Dialog settingsDialog = new Dialog(activity);
			settingsDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
			ImageView image = new ImageView(activity);
			image.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			File imageFile = new File(directory.getPath() + "/" + filename);
			if (imageFile.exists()) {
				image.setImageBitmap(decodeFileLocal(imageFile));
				settingsDialog.setContentView(image);
				settingsDialog.show();
			}
		}
	}

	private Bitmap decodeFileLocal(File f) {
		Bitmap b = null;

		//Decode image size
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		BitmapFactory.decodeStream(fis, null, o);
		try {
			fis.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		int scale = 1;
		if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
			scale = (int) Math.pow(2, (int) Math.ceil(Math.log(IMAGE_MAX_SIZE /
					(double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
		}

		//Decode with inSampleSize
		BitmapFactory.Options o2 = new BitmapFactory.Options();
		o2.inSampleSize = scale;
		try {
			fis = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		b = BitmapFactory.decodeStream(fis, null, o2);
		try {
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return b;
	}
}
