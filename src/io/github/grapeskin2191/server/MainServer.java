package io.github.grapeskin2191.server;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;

public class MainServer {

    /**
     * 获取服务端的日志器
     * @return 配置完成的日志器
     */
    private static Logger getLogger() {
        Logger logger;

        try {
            System.setProperty("java.util.logging.config.file", "server_logging.properties");
            logger = Logger.getLogger("udpserver");
        } catch (Exception e) {
            try {
                SimpleFormatter simpleFormatter = new SimpleFormatter();

                FileHandler fileHandler = new FileHandler("udpserver.log");
                fileHandler.setFormatter(simpleFormatter);

                logger = Logger.getLogger("udpserver");

                for (Handler handler : logger.getHandlers()) {
                    logger.removeHandler(handler);
                }
                logger.addHandler(fileHandler);

                logger.warning("读取日志配置失败，已使用默认配置。错误信息：" + e);
            } catch (Exception exception) {
                logger = Logger.getLogger("udpserver");

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
        try (FileReader fis = new FileReader("udpserver.json");
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

            return new DatagramSocket(new InetSocketAddress(InetAddress.getByName(HOST), PORT));
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
        Logger logger = getLogger();
        logger.info("服务器启动...");
        DatagramSocket socket = socketInit(logger);

        Thread chatThread = new Thread(() -> {
            List<byte[]> data = new ArrayList<>();
            List<InetSocketAddress> clientAddresses = new ArrayList<>();
            InetSocketAddress thisAddr;

            while (true) {
                thisAddr = null;
                try {
                    data.clear();

                    byte[] receivedMsg = new byte[10240];
                    DatagramPacket packet = new DatagramPacket(receivedMsg, receivedMsg.length);
                    socket.receive(packet);

                    InetSocketAddress address = new InetSocketAddress(packet.getAddress(), packet.getPort());
                    if (!clientAddresses.contains(address)) {
                        clientAddresses.add(address);
                        logger.config(clientAddresses.toString());
                    }

                    String decodedMsg = new String(receivedMsg, 0, packet.getLength());

                    if (decodedMsg.equals("test")) {
                        socket.send(new DatagramPacket(receivedMsg, receivedMsg.length, address));
                    } else if (decodedMsg.equals("bye")) {
                        logger.info(address + "离开了");
                        clientAddresses.remove(address);
                        socket.send(new DatagramPacket(receivedMsg, receivedMsg.length, address));
                    } else {
                        JSONObject msgJSON = new JSONObject(decodedMsg);

                        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        msgJSON.put("time", time);
                        data.add(msgJSON.toString().getBytes());

                        String msg = String.format("[%s]%s: %s", time,
                                msgJSON.getString("user_name"), msgJSON.getString("msg"));
                        logger.info(msg);
                    }

                    for (byte[] d : data) {
                        for (InetSocketAddress addr : clientAddresses) {
                            thisAddr = addr;
                            socket.send(new DatagramPacket(d, d.length, addr));
                        }
                    }
                } catch (PortUnreachableException e) {
                    clientAddresses.remove(thisAddr);
                } catch (Exception e) {
                    logger.warning("服务端发生错误。错误信息：" + e);
                }
            }
        });

        try {
            chatThread.start();
            BufferedReader keyboardIn = new BufferedReader(new InputStreamReader(System.in));
            logger.info("随时输入回车以结束程序");
            keyboardIn.readLine();
            logger.info("结束程序中");
        } catch (Exception e) {
            logger.severe("服务端发生错误，将退出程序。错误信息：" + e);
        }
        socket.close();
        System.exit(0);
    }
}
