package com.daohoangson.aafsample.osgi;
/*
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import android.util.Log;

public class LogActivator implements BundleActivator {

	public final static String TAG = LogActivator.class.getName();
	protected LogReaderServiceTracker mLogReaderServiceTracker = null;

	@Override
	public void start(BundleContext context) throws Exception {
		Log.d(TAG, "start()");

		mLogReaderServiceTracker = new LogReaderServiceTracker(context);
		mLogReaderServiceTracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		Log.d(TAG, "stop()");
		
		mLogReaderServiceTracker.close();
	}

	@SuppressWarnings("rawtypes")
	class LogReaderServiceTracker extends ServiceTracker {

		public LogReaderServiceTracker(BundleContext context) {
			super(context, LogReaderService.class.getName(), null);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object addingService(ServiceReference ref) {
			Log.d(TAG, "addingService()");
			
			Object service = context.getService(ref);
			if (service != null && service instanceof LogReaderService) {
				LogListener logListener = new LogListener() {

					@Override
					public void logged(LogEntry entry) {
						switch (entry.getLevel()) {
						case LogService.LOG_ERROR:
							Log.e(TAG, entry.getMessage());
							break;
						case LogService.LOG_WARNING:
							Log.w(TAG, entry.getMessage());
							break;
						case LogService.LOG_INFO:
							Log.i(TAG, entry.getMessage());
							break;
						case LogService.LOG_DEBUG:
							Log.d(TAG, entry.getMessage());
							break;
						}
					}
				};

				((LogReaderService) service).addLogListener(logListener);
			}

			return service;
		}

	}
}
*/
