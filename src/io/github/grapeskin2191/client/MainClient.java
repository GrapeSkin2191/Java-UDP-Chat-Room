package io.github.grapeskin2191.client;

import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.*;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class MainClient {
    static InetSocketAddress serverAddress;
    static DatagramSocket socket;
    static Logger logger;

    /**
     * 获取服务端的日志器
     * @return 配置完成的日志器
     */
    private static Logger getLogger() {
        Logger logger;

        try {
            System.setProperty("java.util.logging.config.file", "client_logging.properties");
            logger = Logger.getLogger("udpclient");
        } catch (Exception e) {
            try {
                SimpleFormatter simpleFormatter = new SimpleFormatter();

                FileHandler fileHandler = new FileHandler("udpclient.log");
                fileHandler.setFormatter(simpleFormatter);

                logger = Logger.getLogger("udpclient");

                for (Handler handler : logger.getHandlers()) {
                    logger.removeHandler(handler);
                }
                logger.addHandler(fileHandler);

                logger.warning("读取日志配置失败，已使用默认配置。错误信息：" + e);
            } catch (Exception exception) {
                logger = Logger.getLogger("udpclient");

                logger.warning("打开日志文件失败，日志将只输出至控制。错误信息：" + exception);
            }
        }

        return logger;
    }

    /**
     * 初始化socket对象
     * @param logger 日志器，用于输出日志
     * @return 配置好的socket对象
     */
    private static DatagramSocket socketInit(Logger logger) {
        logger.info("加载socket配置中");
        try (FileReader fis = new FileReader("udpclient.json");
             BufferedReader bis = new BufferedReader(fis)){
            StringBuilder sb = new StringBuilder();
            String line = bis.readLine();
            while (line != null) {
                sb.append(line).append('\n');
                line = bis.readLine();
            }

            JSONObject jsonObject = new JSONObject(sb.toString()).getJSONObject("socket");
            String HOST = jsonObject.getString("Host");
            int PORT = jsonObject.getInt("Port");
            serverAddress = new InetSocketAddress(InetAddress.getByName(HOST), PORT);

            return new DatagramSocket();
        } catch (Exception e) {
            logger.severe("读取socket配置失败，请检查配置信息。错误信息：" + e);
            System.exit(0);
            return null;
        }
    }

    /**
     * 主方法
     * @param args 运行时给出的参数
     */
    public static void main(String[] args) {
        logger = getLogger();
        logger.info("客户端启动...");
        socket =  socketInit(logger);

        Thread receiveThread = new Thread(MainClient::receiveThread);

        logger.info("连接服务器中...");
        try {
            byte[] data = "test".getBytes();
            socket.send(new DatagramPacket(data, data.length, serverAddress));
            socket.setSoTimeout(5000);

            data = new byte[10240];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            socket.receive(packet);
            if (new String(data, 0, packet.getLength()).trim().equals("test")) {
                logger.info("连接服务器成功");
                socket.setSoTimeout(0);
            } else {
                failToConnect(null);
                return;
            }
        } catch (SocketTimeoutException e) {
            failToConnect(null);
            return;
        } catch (Exception e) {
            failToConnect(e);
            return;
        }

        receiveThread.start();
        new ChatFrame("UDP聊天室 ver1.0");
    }

    private static void failToConnect(Exception e) {
        logger.severe("连接服务器失败。" + (e == null ? "" : "错误信息：" + e));
        JLabel label = new JLabel("连接服务器失败");
        label.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        JOptionPane.showMessageDialog(null, label, "错误", JOptionPane.ERROR_MESSAGE);
        System.exit(0);
    }

    private static void receiveThread() {
        while (true) {
            try {
                byte[] data = new byte[10240];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                socket.receive(packet);
                String decodedData = new String(data, 0, packet.getLength());
                logger.config("收到消息：" + decodedData);
                if (decodedData.equals("bye")) {
                    break;
                }

                // 更新聊天框
                ChatFrame.updateChat(decodedData);

            } catch (Exception e) {
                logger.warning("客户端发生错误。错误信息：" + e);
            }
        }
        socket.close();
        System.exit(0);
    }
}
