package com.kidtv.guard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent s = new Intent(context, GuardService.class);
        try {
            context.startService(s);
        } catch (Exception ignored) {
        }
    }
}
