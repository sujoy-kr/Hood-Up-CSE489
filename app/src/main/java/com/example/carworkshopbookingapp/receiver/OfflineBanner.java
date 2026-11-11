package com.example.carworkshopbookingapp.receiver;

import android.content.*;
import android.view.View;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.carworkshopbookingapp.util.NetworkState;

/** Dynamically handles app network-state broadcasts and updates an Activity-owned offline banner. */
public final class OfflineBanner extends BroadcastReceiver {
    private final TextView banner;
    public OfflineBanner(TextView banner) {this.banner=banner;}
    public void start(Context context) {
        IntentFilter filter=new IntentFilter(ConnectivityMonitor.ACTION_NETWORK_CHANGED);filter.addAction(ConnectivityMonitor.ACTION_NETWORK_RESTORED);
        ContextCompat.registerReceiver(context,this,filter,ContextCompat.RECEIVER_NOT_EXPORTED);update(context);
    }
    @Override public void onReceive(Context context,Intent intent) {update(context);}
    private void update(Context context) {banner.setVisibility(NetworkState.online(context)?View.GONE:View.VISIBLE);}
}
