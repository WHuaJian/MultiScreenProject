package com.whj.multiscreenproject.screenop;


import android.content.Context;
import android.util.Log;

import com.whj.multiscreenproject.SharePrefUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * udp广播屏幕帮助类
 *
 * @author William
 * @Github WHuaJian
 * Created at 2018/6/20 下午3:26
 */

public class UdpByteSendHelper {

    private DatagramSocket mSocket;
    private InetAddress mAddress; //地址
    private int mPort; //端口号
    private Context context;


    private volatile static UdpByteSendHelper instance = null;

    public static UdpByteSendHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (UdpByteSendHelper.class) {
                if (instance == null) {
                    instance = new UdpByteSendHelper(context);
                }
            }
        }

        return instance;
    }

    public UdpByteSendHelper(Context context) {
        try {
            this.mAddress = InetAddress.getByName(SharePrefUtil.getString(context,"ip",""));
            this.mPort = Integer.parseInt(SharePrefUtil.getString(context,"port",""));
            if (mSocket == null || mSocket.isClosed()) {
                mSocket = new DatagramSocket();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }

    }


    /**
     * 发送数据
     *
     * @param bytes
     */
    public void sendByte(byte[] bytes) {
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, mAddress, mPort);
        try {
            mSocket.setSendBufferSize(64 * 1024);
            mSocket.send(packet);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void close() {
        mSocket.close();
        mSocket = null;
        instance = null;
    }
}
