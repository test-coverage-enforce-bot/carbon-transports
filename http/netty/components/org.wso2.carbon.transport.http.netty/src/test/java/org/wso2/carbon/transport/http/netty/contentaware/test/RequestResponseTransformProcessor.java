/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.transport.http.netty.contentaware.test;

import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.TransportSender;
import org.wso2.carbon.transport.http.netty.common.Constants;
import org.wso2.carbon.transport.http.netty.util.TestUtil;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Transform message in request and response path
 */
public class RequestResponseTransformProcessor implements CarbonMessageProcessor {

    private String responseValue;

    private TransportSender transportSender;

    public RequestResponseTransformProcessor(String responseValue) {
        this.responseValue = responseValue;
    }

    @Override
    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback) throws Exception {
        int length = carbonMessage.getFullMessageLength();
        List<ByteBuffer> byteBufferList = carbonMessage.getFullMessageBody();

        ByteBuffer byteBuff = ByteBuffer.allocate(length);
        byteBufferList.forEach(buf -> byteBuff.put(buf));
        String requestValue = new String(byteBuff.array());

        carbonMessage.setProperty(Constants.HOST, TestUtil.TEST_HOST);
        carbonMessage.setProperty(Constants.PORT, TestUtil.TEST_SERVER_PORT);

        if (responseValue != null) {
            byte[] array = responseValue.getBytes("UTF-8");
            ByteBuffer byteBuffer = ByteBuffer.allocate(array.length);
            byteBuffer.put(array);
            carbonMessage.setHeader(Constants.HTTP_CONTENT_LENGTH, String.valueOf(array.length));
            carbonMessage.addMessageBody(byteBuffer);
            carbonMessage.setEndOfMsgAdded(true);
            EngineCallBack engineCallBack = new EngineCallBack(requestValue, carbonCallback);
            transportSender.send(carbonMessage, engineCallBack);
        }

        return false;
    }

    @Override
    public void setTransportSender(TransportSender transportSender) {
        this.transportSender = transportSender;
    }

    @Override
    public String getId() {
        return null;
    }

    private class EngineCallBack implements CarbonCallback {

        String requestValue = "test";
        CarbonCallback requestCallBack;

        EngineCallBack(String requestValue, CarbonCallback carbonCallback) {
            this.requestValue = requestValue;
            requestCallBack = carbonCallback;
        }

        @Override
        public void done(CarbonMessage carbonMessage) {
            int length = carbonMessage.getFullMessageLength();
            List<ByteBuffer> byteBufferList = carbonMessage.getFullMessageBody();

            ByteBuffer byteBuffer = ByteBuffer.allocate(length);
            byteBufferList.forEach(buf -> byteBuffer.put(buf));
            String responseValue = new String(byteBuffer.array()) + ":" + requestValue;
            if (requestValue != null) {
                byte[] array = new byte[0];
                try {
                    array = responseValue.getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {

                }
                ByteBuffer byteBuff = ByteBuffer.allocate(array.length);
                byteBuff.put(array);
                carbonMessage.setHeader(Constants.HTTP_CONTENT_LENGTH, String.valueOf(array.length));
                carbonMessage.addMessageBody(byteBuff);
                carbonMessage.setEndOfMsgAdded(true);
                requestCallBack.done(carbonMessage);
            }

        }
    }
}
