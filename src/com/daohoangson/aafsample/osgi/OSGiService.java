package com.daohoangson.aafsample.osgi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.launch.Framework;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class OSGiService extends Service implements BundleListener,
		ServiceListener {

	public static final String TAG = OSGiService.class.getName();

	// constants for the primary start service intent
	// because this service doesn't support binding, all command
	// must be sent via a invent (using Context.startService)
	public static final String INTENT_PRIMARY_DATA_COMMAND = "cmd";
	private static final String INTENT_PRIMARY_DATA_COMMAND_PREPARE = "prepare";
	public static final String INTENT_PRIMARY_DATA_COMMAND_START = "start";
	public static final String INTENT_PRIMARY_DATA_COMMAND_STOP = "stop";
	public static final String INTENT_PRIMARY_DATA_COMMAND_INSTALL_BUNDLE = "installBundle";
	public static final String INTENT_PRIMARY_DATA_COMMAND_INSTALL_BUNDLE_RESOURCE_ID = "resourceId";
	public static final String INTENT_PRIMARY_DATA_COMMAND_GET_BUNDLES = "getBundles";

	// constants for the broadcasting intent "framework changed"
	public static final String INTENT_FRAMEWORK_CHANGED_ACTION = "com.daohoangson.aafsample.FRAMEWORK_CHANGED";
	public static final String INTENT_FRAMEWORK_CHANGED_DATA_TYPE = "type";
	public static final int INTENT_FRAMEWORK_CHANGED_DATA_TYPE_PREPARED = 0;
	public static final int INTENT_FRAMEWORK_CHANGED_DATA_TYPE_STARTED = 1;
	public static final int INTENT_FRAMEWORK_CHANGED_DATA_TYPE_STOPPED = 2;

	// constants for the broadcasting intent "bundle changed"
	public static final String INTENT_BUNDLE_CHANGED_ACTION = "com.daohoangson.aafsample.BUNDLE_CHANGED";
	public static final String INTENT_BUNDLE_CHANGED_DATA_TYPE = "type";
	public static final String INTENT_BUNDLE_CHANGED_DATA_BUNDLE = "bundle";

	// constants for the broadcasting intent "service changed"
	public static final String INTENT_SERVICE_CHANGED_ACTION = "com.daohoangson.aafsample.SERVICE_CHANGED";
	public static final String INTENT_SERVICE_CHANGED_DATA_TYPE = "type";
	public static final String INTENT_SERVICE_CHANGED_DATA_BUNDLE = "bundle";

	// constants for the broadcasting intent "bundles list"
	public static final String INTENT_BUNDLES_LIST_ACTION = "com.daohoangson.aafsample.BUNDLES_LIST";
	public static final String INTENT_BUNDLES_LIST_DATA_BUNDLES = "bundles";

	protected int mId;
	protected File mCacheDir = null;
	protected Framework mFramework = null;

	private static int sCounter = 0;
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;

	@Override
	public void onCreate() {
		super.onCreate();

		HandlerThread thread = new HandlerThread(OSGiService.class.getName());
		thread.start();
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);

		mId = sCounter++;

		Log.i(TAG, String.format("%d.onCreate()", mId));

		// automatically prepare upon creation
		Message msg = mServiceHandler.obtainMessage();
		android.os.Bundle bundle = new android.os.Bundle();
		bundle.putString(INTENT_PRIMARY_DATA_COMMAND,
				INTENT_PRIMARY_DATA_COMMAND_PREPARE);
		msg.setData(bundle);
		mServiceHandler.sendMessage(msg);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		Log.i(TAG, String.format("%d.onDestroy()", mId));

		try {
			stopFramework();
		} catch (IllegalStateException ise) {
			// ignore
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		android.os.Bundle bundle = intent.getExtras();

		if (bundle != null && !bundle.isEmpty() && mServiceHandler != null) {
			Message msg = mServiceHandler.obtainMessage();
			msg.setData(bundle);
			mServiceHandler.sendMessage(msg);
		}

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// no bounding support, return null
		return null;
	}

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			android.os.Bundle bundle = msg.getData();
			String cmd = bundle.getString(INTENT_PRIMARY_DATA_COMMAND);

			if (INTENT_PRIMARY_DATA_COMMAND_PREPARE.equals(cmd)) {
				prepareFramework();
			} else if (INTENT_PRIMARY_DATA_COMMAND_START.equals(cmd)) {
				startFramework();
			} else if (INTENT_PRIMARY_DATA_COMMAND_STOP.equals(cmd)) {
				// switch to call stopSelf instead
				// onDestroy will call stopFramework later
				// stopFramework();
				stopSelf();
			} else if (INTENT_PRIMARY_DATA_COMMAND_INSTALL_BUNDLE.equals(cmd)) {
				int resId = bundle.getInt(
						INTENT_PRIMARY_DATA_COMMAND_INSTALL_BUNDLE_RESOURCE_ID,
						0);
				if (resId != 0) {
					installBundle(resId);
				}
			} else if (INTENT_PRIMARY_DATA_COMMAND_GET_BUNDLES.equals(cmd)) {
				getBundles();
			}
		}
	}

	private void prepareFramework() {
		try {
			// TODO: constant for "osgi-cache-%d"?
			File tempFile = File.createTempFile(
					String.format("osgi-cache-%d", mId), null);
			File tempDir = new File(tempFile.getAbsolutePath() + ".d");
			tempFile.delete();
			if (tempDir.isDirectory() || tempDir.mkdirs()) {
				mCacheDir = tempDir;

				Log.i(TAG, String.format("%d.prepare() | Cache directory: %s",
						mId, mCacheDir.getAbsolutePath()));
			} else {
				Log.e(TAG, String.format(
						"%d.prepare() | Unable to create cache directory %s",
						mId, tempDir.getAbsolutePath()));
			}
		} catch (IOException e) {
			Log.e(TAG,
					String.format("%d.prepare() | IO Exception: %s", mId,
							e.getMessage()));
			e.printStackTrace();
		}

		if (mCacheDir != null) {
			HashMap<Object, Object> config = new HashMap<Object, Object>();
			config.put(FelixConstants.LOG_LEVEL_PROP, "1");
			config.put(BundleCache.CACHE_ROOTDIR_PROP, ".");
			config.put(FelixConstants.FRAMEWORK_STORAGE,
					mCacheDir.getAbsolutePath());
			config.put(FelixConstants.FRAMEWORK_STORAGE_CLEAN,
					FelixConstants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
			
			config.put(FelixConstants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
					"javax.xml,javax.xml.datatype,javax.xml.namespace,javax.xml.parsers"
					+ ",javax.xml.transform,javax.xml.transform.dom"
					+ ",javax.xml.transform.sax,javax.xml.transform.stream"
					+ ",javax.xml.validation,javax.xml.xpath"
					+ ",org.w3c,org.w3c.dom,org.w3c.dom.ls"
					+ ",org.xml.sax,org.xml.sax.ext,org.xml.sax.helpers");
					//+ ",org.apache.commons.logging,org.apache.commons.logging.impl");
			
			config.put(FelixConstants.IMPLICIT_BOOT_DELEGATION_PROP, "false");
			config.put("org.osgi.service.http.port", "22222");

			/*
			LogActivator logActivator = new LogActivator();
			List<BundleActivator> activators = new ArrayList<BundleActivator>();
			activators.add(logActivator);
			config.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, activators);
			*/

			mFramework = new Felix(config);

			Log.i(TAG, String.format("%d.prepare() | Prepared", mId));

			Intent intent = new Intent(INTENT_FRAMEWORK_CHANGED_ACTION);
			intent.putExtra(INTENT_FRAMEWORK_CHANGED_DATA_TYPE,
					INTENT_FRAMEWORK_CHANGED_DATA_TYPE_PREPARED);
			sendBroadcast(intent);
		}
	}

	private void startFramework() {
		if (mFramework != null) {
			try {
				mFramework.init();
				
				BundleContext bc = mFramework.getBundleContext();
				bc.addBundleListener(this);
				bc.addServiceListener(this);
				
				mFramework.start();

				Log.i(TAG, String.format("%d.start() | Started", mId));

				Intent intent = new Intent(INTENT_FRAMEWORK_CHANGED_ACTION);
				intent.putExtra(INTENT_FRAMEWORK_CHANGED_DATA_TYPE,
						INTENT_FRAMEWORK_CHANGED_DATA_TYPE_STARTED);
				sendBroadcast(intent);
			} catch (BundleException e) {
				Log.e(TAG, String.format("%d.start() | Bundle Exception: %s",
						mId, e.getMessage()));
				e.printStackTrace();
			}
		} else {
			Log.e(TAG, String.format("%d.start() | mFramework is null", mId));
			throw new IllegalStateException(
					"OSGi Framework could start without being prepared");
		}
	}

	private void stopFramework() {
		if (mFramework != null) {
			try {
				mFramework.stop();
				mFramework = null;

				// TODO: empty cache directory before deleting
				// mCacheDir.delete();

				Log.i(TAG, String.format("%d.stop() | Stopped", mId));

				Intent intent = new Intent(INTENT_FRAMEWORK_CHANGED_ACTION);
				intent.putExtra(INTENT_FRAMEWORK_CHANGED_DATA_TYPE,
						INTENT_FRAMEWORK_CHANGED_DATA_TYPE_STOPPED);
				sendBroadcast(intent);

				// terminate the service
				stopSelf();
			} catch (BundleException e) {
				Log.e(TAG, String.format("%d.stop() | Bundle Exception: %s",
						mId, e.getMessage()));
				e.printStackTrace();
			}
		} else {
			Log.e(TAG, String.format("%d.stop() | mFramework is null", mId));
			throw new IllegalStateException(
					"OSGi Framework could stop without being prepared (and started)");
		}
	}

	private void getBundles() {
		if (mFramework != null) {
			BundleContext bc = mFramework.getBundleContext();
			
			if (bc != null) {
				Bundle[] bundles = bc.getBundles();
				BundleWrapper[] bws = new BundleWrapper[bundles.length];

				for (int i = 0; i < bundles.length; i++) {
					bws[i] = new BundleWrapper(bundles[i]);
				}

				Log.i(TAG, String.format("%d.getBundles() | Count: %d", mId,
						bundles.length));

				Intent intent = new Intent(INTENT_BUNDLES_LIST_ACTION);
				intent.putExtra(INTENT_BUNDLES_LIST_DATA_BUNDLES, bws);
				sendBroadcast(intent);
			} else {
				Log.e(TAG, String.format("%d.getBundles() | BundleContext is null", mId));
			}
		} else {
			Log.e(TAG, String.format("%d.getBundles() | mFramework is null", mId));
		}
	}

	private void installBundle(int resId) {
		if (mFramework != null) {
			InputStream is = getResources().openRawResource(resId);
			try {
				// TODO: constant for "/non/existent/bundle/%d"?
				Bundle bundle = mFramework.getBundleContext().installBundle(
						String.format("/non/existent/bundle/%d", resId), is);
				bundle.start();

				Log.i(TAG, String.format("%d.installBundle(c, %d) | Installed",
						mId, resId));
			} catch (BundleException e) {
				Log.e(TAG, String.format(
						"%d.installBundle(c, %d) | Bundle Exception: %s", mId,
						resId, e.getMessage()));
				e.printStackTrace();
			}
		} else {
			Log.e(TAG, String.format(
					"%d.installBundle(c, %d) | mFramework is null", mId, resId));
		}
	}

	@Override
	public void bundleChanged(BundleEvent e) {
		Log.i(TAG, String.format("%d.bundleChanged()", mId));

		Intent intent = new Intent(INTENT_BUNDLE_CHANGED_ACTION);
		Bundle bundle = e.getBundle();
		BundleWrapper bw = new BundleWrapper(bundle);

		intent.putExtra(INTENT_BUNDLE_CHANGED_DATA_TYPE, e.getType());
		intent.putExtra(INTENT_BUNDLE_CHANGED_DATA_BUNDLE, bw);

		sendBroadcast(intent);
	}

	@Override
	public void serviceChanged(ServiceEvent e) {
		Object service = mFramework.getBundleContext().getService(e.getServiceReference());
		if (service != null) {
			Log.i(TAG, String.format("%d.serviceChanged() | Service: %s", mId, service.getClass().getName()));
		} else {
			Log.e(TAG, String.format("%d.serviceChanged() | Service is null", mId));
		}

		Intent intent = new Intent(INTENT_SERVICE_CHANGED_ACTION);
		Bundle bundle = e.getServiceReference().getBundle();
		BundleWrapper bw = new BundleWrapper(bundle);

		intent.putExtra(INTENT_SERVICE_CHANGED_DATA_TYPE, e.getType());
		intent.putExtra(INTENT_SERVICE_CHANGED_DATA_BUNDLE, bw);

		sendBroadcast(intent);
	}
}
