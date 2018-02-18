package com.google.firebase.codelab.friendlychat;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by hubbler-hp on 14/2/18.
 */

public class PersistantEnabler extends Application{

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
