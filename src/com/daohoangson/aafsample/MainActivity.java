package com.daohoangson.aafsample;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.daohoangson.aafsample.osgi.BundleWrapper;
import com.daohoangson.aafsample.osgi.OSGiService;

public class MainActivity extends ListActivity {

	public static final String TAG = MainActivity.class.getName();

	private TheAdapter mAdapter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAdapter = new TheAdapter();

		ListView listView = getListView();
		listView.setAdapter(mAdapter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// when the activity is destroyed
		// it's important to stop the osgi framework
		stop();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// register receiver for osgi intents
		registerReceiver(mOSGiBroadcastReceiver, new IntentFilter(
				OSGiService.INTENT_FRAMEWORK_CHANGED_ACTION));
		registerReceiver(mOSGiBroadcastReceiver, new IntentFilter(
				OSGiService.INTENT_BUNDLE_CHANGED_ACTION));
		registerReceiver(mOSGiBroadcastReceiver, new IntentFilter(
				OSGiService.INTENT_SERVICE_CHANGED_ACTION));
		registerReceiver(mOSGiBroadcastReceiver, new IntentFilter(
				OSGiService.INTENT_BUNDLES_LIST_ACTION));
	}

	@Override
	protected void onPause() {
		super.onPause();

		// unregister receivers
		unregisterReceiver(mOSGiBroadcastReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.main_menu, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_start:
			start();
			break;
		case R.id.menu_stop:
			stop();
			break;
		case R.id.menu_install_log:
			installBundle(R.raw.log);
			installBundle(R.raw.interval_logging);
			break;
		case R.id.menu_install_cxf:
			installBundle(R.raw.cxf);
			break;
		case R.id.menu_install_cxf_consumer:
			installBundle(R.raw.cxf_api);
			installBundle(R.raw.cxf_consumer);
			break;
		}

		return false;
	}

	private void sendCommand(String cmd) {
		Intent intent = new Intent(this, OSGiService.class);
		intent.putExtra(OSGiService.INTENT_PRIMARY_DATA_COMMAND, cmd);
		startService(intent);
	}

	private void installBundle(int resId) {
		Intent installIntent = new Intent(MainActivity.this, OSGiService.class);
		installIntent.putExtra(OSGiService.INTENT_PRIMARY_DATA_COMMAND,
				OSGiService.INTENT_PRIMARY_DATA_COMMAND_INSTALL_BUNDLE);
		installIntent
				.putExtra(
						OSGiService.INTENT_PRIMARY_DATA_COMMAND_INSTALL_BUNDLE_RESOURCE_ID,
						resId);
		startService(installIntent);
	}

	private void start() {
		sendCommand(OSGiService.INTENT_PRIMARY_DATA_COMMAND_START);
	}

	private void stop() {
		sendCommand(OSGiService.INTENT_PRIMARY_DATA_COMMAND_STOP);
	}

	private void onBundleChanged(int type, BundleWrapper bundle) {
		Log.i(TAG, String.format("onBundleChanged: #%d %s (%s)", bundle.mId,
				bundle.mBundleName, bundle.mBundleVersion));

		sendCommand(OSGiService.INTENT_PRIMARY_DATA_COMMAND_GET_BUNDLES);
	}

	private void onServiceChanged(int type, BundleWrapper bundle) {
		Log.i(TAG, String.format("onServiceChanged: #%d %s (%s)", bundle.mId,
				bundle.mBundleName, bundle.mBundleVersion));
	}

	private void onBundlesList(Parcelable[] bundles) {
		mAdapter.clear();
		for (Parcelable bundle : bundles) {
			mAdapter.add(bundle);
		}

		mAdapter.notifyDataSetInvalidated();
	}

	private BroadcastReceiver mOSGiBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (OSGiService.INTENT_FRAMEWORK_CHANGED_ACTION.equals(action)) {
				// TODO: ?
			} else if (OSGiService.INTENT_BUNDLE_CHANGED_ACTION.equals(action)) {
				int type = intent.getIntExtra(
						OSGiService.INTENT_BUNDLE_CHANGED_DATA_TYPE, 0);
				BundleWrapper bundle = intent
						.getParcelableExtra(OSGiService.INTENT_BUNDLE_CHANGED_DATA_BUNDLE);
				onBundleChanged(type, bundle);
			} else if (OSGiService.INTENT_SERVICE_CHANGED_ACTION.equals(action)) {
				int type = intent.getIntExtra(
						OSGiService.INTENT_SERVICE_CHANGED_DATA_TYPE, 0);
				BundleWrapper bundle = intent
						.getParcelableExtra(OSGiService.INTENT_SERVICE_CHANGED_DATA_BUNDLE);
				onServiceChanged(type, bundle);
			} else if (OSGiService.INTENT_BUNDLES_LIST_ACTION.equals(action)) {
				Parcelable[] bundles = intent
						.getParcelableArrayExtra(OSGiService.INTENT_BUNDLES_LIST_DATA_BUNDLES);
				onBundlesList(bundles);
			}
		}

	};

	class TheAdapter extends ArrayAdapter<Object> {

		public TheAdapter() {
			super(MainActivity.this, android.R.layout.simple_list_item_1);
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public int getItemViewType(int position) {
			Object item = getItem(position);

			if (item instanceof BundleWrapper) {
				return 0;
			}

			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			ViewHolder viewHolder = null;

			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				switch (getItemViewType(position)) {
				default:
					row = inflater.inflate(R.layout.row_bundle, null);
					viewHolder = new BundleViewHolder(row);
					break;
				}

				row.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) row.getTag();
			}

			viewHolder.populateFrom(getItem(position));

			return row;
		}
	}

	interface ViewHolder {
		public void populateFrom(Object object);
	}

	class BundleViewHolder implements ViewHolder {
		TextView mTxtBundleName;
		TextView mTxtBundleVersion;
		TextView mTxtState;

		public BundleViewHolder(View view) {
			mTxtBundleName = (TextView) view.findViewById(R.id.txtBundleName);
			mTxtBundleVersion = (TextView) view
					.findViewById(R.id.txtBundleVersion);
			mTxtState = (TextView) view.findViewById(R.id.txtState);
		}

		@Override
		public void populateFrom(Object object) {
			BundleWrapper bundle = (BundleWrapper) object;

			mTxtBundleName.setText(bundle.mBundleName);
			mTxtBundleVersion.setText(bundle.mBundleVersion);
			mTxtState.setText(String.valueOf(bundle.mState));
		}
	}
}
