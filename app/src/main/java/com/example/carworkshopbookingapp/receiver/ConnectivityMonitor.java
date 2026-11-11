package com.example.carworkshopbookingapp.receiver;

import android.content.*;
import android.net.*;
import com.example.carworkshopbookingapp.service.SyncCoordinator;

/** NetworkCallback emits package-only NETWORK_RESTORED/NETWORK_CHANGED; this private receiver requests sync through the background-safe coordinator. */
public final class ConnectivityMonitor extends BroadcastReceiver {
    public static final String ACTION_NETWORK_RESTORED="com.example.carworkshopbookingapp.ACTION_NETWORK_RESTORED";
    public static final String ACTION_NETWORK_CHANGED="com.example.carworkshopbookingapp.ACTION_NETWORK_CHANGED";
    private static ConnectivityManager.NetworkCallback callback;
    public static void start(Context context) {
        if (callback!=null) return;
        Context app=context.getApplicationContext();
        callback=new ConnectivityManager.NetworkCallback() {
            private boolean online;
            @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                boolean next=capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                if (next==online) return;
                online=next;
                app.sendBroadcast(new Intent(next?ACTION_NETWORK_RESTORED:ACTION_NETWORK_CHANGED).setPackage(app.getPackageName()));
            }
            @Override public void onLost(Network network) {online=false;app.sendBroadcast(new Intent(ACTION_NETWORK_CHANGED).setPackage(app.getPackageName()));}
        };
        app.getSystemService(ConnectivityManager.class).registerDefaultNetworkCallback(callback);
    }
    public static void stop(Context context) {
        if (callback!=null) {context.getSystemService(ConnectivityManager.class).unregisterNetworkCallback(callback);callback=null;}
    }
    @Override public void onReceive(Context context,Intent intent) {
        if (ACTION_NETWORK_RESTORED.equals(intent.getAction())) SyncCoordinator.networkRestored(context);
    }
}
