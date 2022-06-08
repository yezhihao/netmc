package io.github.yezhihao.netmc.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.function.BiFunction;

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public interface Client extends Closeable {

    void send(byte[] bytes) throws IOException;

    static Client[] UDP(String host, int port, int size) {
        InetAddress target = newInetAddress(host);
        Client[] result = new Client[size];
        for (int i = 0; i < size; i++) {
            result[i] = UDP.apply(target, port);
        }
        return result;
    }

    static Client[] TCP(String host, int port, int size) {
        InetAddress target = newInetAddress(host);
        Client[] result = new Client[size];
        for (int i = 0; i < size; i++) {
            result[i] = TCP.apply(target, port);
        }
        return result;
    }

    static InetAddress newInetAddress(String host) {
        try {
            return InetAddress.getByName(host);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static Socket newSocket(InetAddress target, int port) {
        try {
            return new Socket(target, port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static DatagramSocket newDatagramSocket() {
        try {
            return new DatagramSocket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    BiFunction<InetAddress, Integer, Client> UDP = (target, port) -> new Client() {
        private final DatagramSocket client = newDatagramSocket();

        @Override
        public void send(byte[] bytes) throws IOException {
            client.send(new DatagramPacket(bytes, bytes.length, target, port));
        }

        @Override
        public void close() {
            client.close();
        }
    };

    BiFunction<InetAddress, Integer, Client> TCP = (target, port) -> new Client() {
        private final Socket client = newSocket(target, port);

        @Override
        public void send(byte[] bytes) throws IOException {
            OutputStream os = client.getOutputStream();
            os.write(bytes);
            os.flush();
        }

        @Override
        public void close() {
            try {
                client.close();
            } catch (IOException ignored) {
            }
        }
    };
}