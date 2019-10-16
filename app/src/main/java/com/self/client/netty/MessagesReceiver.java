package com.self.client.netty;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public abstract class MessagesReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String intentType = intent.getAction();

        if (intentType == null) {
            throw new NullPointerException("MessagesReceiver intent action is null!");
        }

        int msgType = intent.getIntExtra(MessageType.MSG_TYPE, 0);
        int msgId = intent.getIntExtra(MessageType.MSG_ID, 0);
        String msgContent = intent.getStringExtra(MessageType.MSG_CONTENT);

        if (intentType.equals(MessageReceiveManager.NEW_MESSAGE_RECEIVER_BROADCAST)) {

            receiveNewMessage(msgType, msgId, msgContent);

        } else if (intentType.equals(MessageReceiveManager.MESSAGE_SEND_SUCCESS_BACK_BROADCAST)) {

            receiverMessageSendSuccessBack(msgType, msgId, msgContent);
        }
    }

    protected abstract void receiveNewMessage(int msgType, int msgId, String msgContent);
    protected abstract void receiverMessageSendSuccessBack(int msgType, int msgId, String msgContent);
}
