package com.google.firebase.codelab.friendlychat;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;

/**
 * Created by hubbler-sudesh on 15/02/18.
 */

public class MyFCMClass extends FirebaseMessagingService{

    @Override
    public void onMessageSent(String s) {
        super.onMessageSent(s);
    }

    @Override
    public void onSendError(String s, Exception e) {
        super.onSendError(s, e);
    }
}
