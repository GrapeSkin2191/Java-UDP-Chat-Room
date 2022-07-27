package io.github.grapeskin2191.client;

import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.DatagramPacket;

import static io.github.grapeskin2191.client.MainClient.*;

public class ChatFrame extends JFrame{
    private static JTextArea chat_ta;
    private static JTextField name_tf;
    private static JTextArea input_ta;

    public ChatFrame(String title) {
        super(title);

        double screenWidth = Toolkit.getDefaultToolkit().getScreenSize().getWidth();
        double screenHeight = Toolkit.getDefaultToolkit().getScreenSize().getHeight();
        int width = 1000;
        int height = 700;

        GridBagLayout gridBag = new GridBagLayout();
        JPanel panel = new JPanel(gridBag);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 10;

        chat_ta = new JTextArea();
        chat_ta.setLineWrap(true);
        chat_ta.setWrapStyleWord(true);
        chat_ta.setEditable(false);
        chat_ta.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        JScrollPane sp = new JScrollPane(chat_ta);
        sp.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
        c.weighty = 9;
        c.insets = new Insets(10, 10, 0, 10);
        gridBag.addLayoutComponent(sp, c);
        panel.add(sp);

        // 底部面板
        GridBagLayout bottomGridBag = new GridBagLayout();
        JPanel bottomPanel = new JPanel(bottomGridBag);
        GridBagConstraints bc = new GridBagConstraints();
        bc.fill = GridBagConstraints.BOTH;

        // 名称输入框
        name_tf = new JTextField(10);
        name_tf.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        name_tf.setText(String.format("用户%04d", (int) (Math.random() * 10000)));
        bc.weightx = 1;
        gridBag.addLayoutComponent(name_tf, bc);
        bottomPanel.add(name_tf);

        // 消息输入框
        input_ta = new JTextArea(3, 50);
        input_ta.setLineWrap(true);
        input_ta.setWrapStyleWord(true);
        input_ta.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        JScrollPane input_sp = new JScrollPane(input_ta);
        bc.gridx = 1;
        bc.weightx = 8;
        bc.insets = new Insets(0, 10, 0, 10);
        gridBag.addLayoutComponent(input_sp, bc);
        bottomPanel.add(input_sp);

        // 发送按钮
        JButton send_btn = new JButton("发送");
        send_btn.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        bc.gridx = 9;
        bc.weightx = 1;
        bc.insets = new Insets(0, 0, 0, 0);
        gridBag.addLayoutComponent(send_btn, bc);
        bottomPanel.add(send_btn);

        bottomPanel.setBorder(BorderFactory.createEtchedBorder());
        c.gridy = 9;
        c.weighty = 1;
        c.insets = new Insets(10, 10, 10, 10);
        gridBag.addLayoutComponent(bottomPanel, c);
        panel.add(bottomPanel);

        setContentPane(panel);
        setSize(width, height);
        setResizable(false);
        // 窗口居中
        setLocation((int) (screenWidth - width) / 2, (int) (screenHeight - height) / 2);

        try {
            if (new File("udpclient.png").exists()) {
                setIconImage(Toolkit.getDefaultToolkit().createImage("udpclient.png"));
            } else {
                logger.warning("未找到图标文件");
            }
        } catch (Exception e) {
            logger.warning("图标加载失败。错误信息：" + e);
        }

        setVisible(true);
        input_ta.grabFocus();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!chat_ta.getText().equals("")) {
                    logger.info("保存聊天中...");
                    try (FileWriter fos = new FileWriter("chat_client.txt");
                         BufferedWriter bos = new BufferedWriter(fos)){
                        bos.write(chat_ta.getText());
                    } catch (Exception ex) {
                        logger.warning("保存聊天失败。错误信息：" + ex);
                    }
                }

                logger.info("结束程序中...");
                try {
                    byte[] msg = "bye".getBytes();
                    DatagramPacket packet = new DatagramPacket(msg, msg.length, serverAddress);
                    socket.send(packet);
                    System.exit(0);
                } catch (Exception ex) {
                    logger.warning("结束时发生错误。错误信息：" + ex);
                }
            }
        });

        // 点击发送按钮
        send_btn.addActionListener(ChatFrame::onSendBtnClick);
    }

    private static void onSendBtnClick(ActionEvent event) {
        String user_name = name_tf.getText();
        String msg = input_ta.getText();
        input_ta.grabFocus();
        if (!user_name.equals("") && !msg.equals("")) {
            input_ta.setText("");
            Thread sendThread = new Thread(() -> {
                try {
                    JSONObject msgJSON = new JSONObject();
                    msgJSON.put("user_name", user_name);
                    msgJSON.put("msg", msg);

                    byte[] msgByte = msgJSON.toString().getBytes();
                    DatagramPacket packet = new DatagramPacket(msgByte, msgByte.length, serverAddress);
                    socket.send(packet);
                } catch (Exception e) {
                    logger.warning("客户端发生错误。错误信息：" + e);
                }
            });
            sendThread.start();
        }
    }

    static void updateChat(String new_msg) {
        JSONObject msgJSON = new JSONObject(new_msg);
        String msg = String.format("[%s]%s: %s%n", msgJSON.getString("time"),
                msgJSON.getString("user_name"), msgJSON.getString("msg"));
        chat_ta.setText(chat_ta.getText() + msg);
    }
}
