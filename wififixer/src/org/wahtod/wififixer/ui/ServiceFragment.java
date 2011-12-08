/*Copyright [2010-2011] [David Van de Ven]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

package org.wahtod.wififixer.ui;

import org.wahtod.wififixer.R;
import org.wahtod.wififixer.prefs.PrefUtil;
import org.wahtod.wififixer.prefs.PrefConstants.Pref;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

public class ServiceFragment extends Fragment {
    public static final String REFRESH_ACTION = "org.wahtod.wififixer.ui.ServiceFragment.REFRESH";
    private TextView version;
    private ImageButton servicebutton;
    private ImageButton wifibutton;

    private Handler handler = new Handler() {
	@Override
	public void handleMessage(Message message) {
	    if (getActivity() == null)
		return;
	    int state = -1;
	    if (message.getData().isEmpty())
		setIcon();
	    else
		state = message.getData().getInt(WifiManager.EXTRA_WIFI_STATE);
	    if (state == WifiManager.WIFI_STATE_DISABLED
		    || state == WifiManager.WIFI_STATE_ENABLED)
		setIcon();
	}
    };
    private BroadcastReceiver wifireceiver = new BroadcastReceiver() {
	public void onReceive(final Context context, final Intent intent) {

	    /*
	     * Dispatch intent commands to handler
	     */
	    Message message = handler.obtainMessage();
	    Bundle data = new Bundle();
	    if (intent.getExtras() != null) {
		data.putString(PrefUtil.INTENT_ACTION, intent.getAction());
		data.putAll(intent.getExtras());
	    }
	    message.setData(data);
	    handler.sendMessage(message);
	}
    };

    @Override
    public void onPause() {
	unregisterReceiver();
	super.onPause();
    }

    private void unregisterReceiver() {
	getContext().unregisterReceiver(wifireceiver);
    }

    private void registerReceiver() {
	IntentFilter filter = new IntentFilter(
		WifiManager.WIFI_STATE_CHANGED_ACTION);
	filter.addAction(REFRESH_ACTION);
	getContext().registerReceiver(wifireceiver, filter);
    }

    @Override
    public void onResume() {
	super.onResume();
	registerReceiver();
	setIcon();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
	    Bundle savedInstanceState) {
	View v = inflater.inflate(R.layout.service, null);
	// Set layout version code
	version = (TextView) v.findViewById(R.id.version);
	servicebutton = (ImageButton) v.findViewById(R.id.ImageButton01);
	wifibutton = (ImageButton) v.findViewById(R.id.ImageButton02);
	setText();
	return v;
    }

    private Context getContext() {
	return getActivity().getApplicationContext();
    }

    private void setIcon() {

	servicebutton.setClickable(true);
	servicebutton.setFocusable(false);
	servicebutton.setFocusableInTouchMode(false);

	wifibutton.setClickable(true);
	wifibutton.setFocusable(false);
	wifibutton.setFocusableInTouchMode(false);

	/*
	 * Draw icon
	 */

	if (PrefUtil.readBoolean(getContext(), Pref.DISABLE_KEY.key())) {
	    servicebutton.setBackgroundResource(R.drawable.service_inactive);
	} else {
	    servicebutton.setBackgroundResource(R.drawable.service_active);
	}

	if (!WifiFixerActivity.getIsWifiOn(getContext())) {
	    wifibutton.setBackgroundResource(R.drawable.service_inactive);
	} else {
	    wifibutton.setBackgroundResource(R.drawable.service_active);
	}

    }

    void setText() {
	PackageManager pm = getContext().getPackageManager();
	String vers = "";
	try {
	    /*
	     * Get PackageInfo object
	     */
	    PackageInfo pi = pm
		    .getPackageInfo(getContext().getPackageName(), 0);
	    /*
	     * get version code string
	     */
	    vers = pi.versionName;
	} catch (NameNotFoundException e) {
	    /*
	     * shouldn't ever be not found
	     */
	    e.printStackTrace();
	}

	version.setText(vers.toCharArray(), 0, vers.length());
    }

}