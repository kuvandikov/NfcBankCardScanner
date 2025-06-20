package io.github.kuvandikov.scan_card_nfc.utils;

import android.nfc.tech.IsoDep;
import android.util.Log;


import java.io.IOException;

import io.github.kuvandikov.scan_card_nfc.enums.SwEnum;
import io.github.kuvandikov.scan_card_nfc.exception.CommunicationException;
import io.github.kuvandikov.scan_card_nfc.parser.IProvider;

public class Provider implements IProvider {


    private static final String TAG = Provider.class.getName();

    private StringBuffer log = new StringBuffer();

    private IsoDep mTagCom;

    public void setmTagCom(final IsoDep mTagCom) {
        this.mTagCom = mTagCom;
    }


    public StringBuffer getLog() {
        return log;
    }

    @Override
    public byte[] transceive(byte[] pCommand) throws CommunicationException {
        log.append("=================<br/>");
        log.append("<font color='green'><b>send:</b> " + BytesUtils.bytesToString(pCommand)).append("</font><br/>");

        byte[] response = null;
        try {
            // send command to emv card
            response = mTagCom.transceive(pCommand);
        } catch (IOException e) {
            throw new CommunicationException(e.getMessage());
        }

        log.append("<font color='blue'><b>resp:</b> " + BytesUtils.bytesToString(response)).append("</font><br/>");
        Log.d(TAG, "resp: " + BytesUtils.bytesToString(response));
        try {
            Log.d(TAG, "resp: " + TlvUtil.prettyPrintAPDUResponse(response));
            SwEnum val = SwEnum.getSW(response);
            if (val != null) {
                Log.d(TAG, "resp: " + val.getDetail());
            }
            log.append("<pre>").append(TlvUtil.prettyPrintAPDUResponse(response).replace("\n", "<br/>").replace(" ", "&nbsp;"))
                    .append("</pre><br/>");
        } catch (Exception e) {
        }

        return response;
    }
}
