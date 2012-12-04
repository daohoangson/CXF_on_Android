package com.daohoangson.aafsample.osgi;

import org.osgi.framework.Bundle;

import android.os.Parcel;
import android.os.Parcelable;

public class BundleWrapper implements Parcelable {

	public long mId;
	public String mBundleName;
	public String mBundleVersion;
	public int mState;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(mId);
		dest.writeString(mBundleName);
		dest.writeString(mBundleVersion);
		dest.writeInt(mState);
	}
	
	public BundleWrapper(Bundle bundle) {
		mId = bundle.getBundleId();
		mBundleName = bundle.getSymbolicName();
		mBundleVersion = bundle.getVersion().toString();
		mState = bundle.getState();
	}
	
	private BundleWrapper(Parcel in) {
		mId = in.readLong();
		mBundleName = in.readString();
		mBundleVersion = in.readString();
		mState = in.readInt();
	}

	public static final Parcelable.Creator<BundleWrapper> CREATOR = new Parcelable.Creator<BundleWrapper>() {
		public BundleWrapper createFromParcel(Parcel in) {
			return new BundleWrapper(in);
		}

		public BundleWrapper[] newArray(int size) {
			return new BundleWrapper[size];
		}
	};
}
