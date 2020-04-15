import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;


public abstract class SocketForModbusTCP
{
    private String ip;
    private int port;

    private Socket mSocket;
    private SocketAddress mSocketAddress;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private boolean _isConnected = false;

    public SocketForModbusTCP(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void connect() {

        try {
            this.mSocket = new Socket();
            this.mSocket.setKeepAlive(true);
            this.mSocketAddress = new InetSocketAddress(ip, port);
            this.mSocket.connect( mSocketAddress, 3000 );// 设置连接超时时间为3秒

            this.mOutputStream = mSocket.getOutputStream();
            this.mInputStream = mSocket.getInputStream();

            this.mReadThread = new ReadThread();
            this.mReadThread.start();
            this._isConnected = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (this.mReadThread != null) {
            this.mReadThread.interrupt();
        }
        if (this.mSocket != null) {
            try {
                this.mSocket.close();
                this.mSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this._isConnected = false;
    }

    public boolean isConnected() {
        return this._isConnected;
    }

    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                try {
                    if (SocketForModbusTCP.this.mInputStream == null) {
                        return;
                    }
                    int available = SocketForModbusTCP.this.mInputStream.available();
                    if (available > 0) {
                        byte[] buffer = new byte[available];
                        int size = SocketForModbusTCP.this.mInputStream.read(buffer);
                        if (size > 0) {
                            SocketForModbusTCP.this.onDataReceived(buffer, size);
                        }
                    } else {
                        SystemClock.sleep(50);
                    }


                } catch (Throwable e) {
                    Log.e("error", e.getMessage());
                    return;
                }
            }
        }
    }

    private void send(byte[] bOutArray) {
        try {
            this.mOutputStream.write(bOutArray);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendHex(String sHex) {
        byte[] bOutArray = ByteUtil.HexToByteArr(sHex);//转成16进制
        send(bOutArray);
    }

    protected abstract void onDataReceived(byte[] readBuffer, int size);
}
