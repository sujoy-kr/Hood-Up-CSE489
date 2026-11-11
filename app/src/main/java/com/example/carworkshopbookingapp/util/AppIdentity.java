package com.example.carworkshopbookingapp.util;

import android.content.Context;
import android.content.pm.*;
import android.os.Build;
import java.security.MessageDigest;
import java.util.Locale;

public final class AppIdentity {
    private AppIdentity() {}
    @SuppressWarnings("deprecation")
    public static String signingSha1(Context context) throws Exception {
        PackageManager pm=context.getPackageManager();
        Signature[] signatures;
        if(Build.VERSION.SDK_INT>=28){
            PackageInfo info=pm.getPackageInfo(context.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);
            signatures=info.signingInfo.getApkContentsSigners();
        }else signatures=pm.getPackageInfo(context.getPackageName(),PackageManager.GET_SIGNATURES).signatures;
        if(signatures==null||signatures.length==0)throw new IllegalStateException("Signing certificate unavailable");
        byte[] digest=MessageDigest.getInstance("SHA-1").digest(signatures[0].toByteArray());
        StringBuilder text=new StringBuilder();for(byte b:digest)text.append(String.format(Locale.ROOT,"%02X",b&255));
        return text.toString();
    }
}
