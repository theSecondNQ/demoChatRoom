package com.nqchatroom.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataOutputStream;
import java.net.Socket;

public class ChatUILogin extends JFrame {
    private JTextField usernameField; //定义用户名输入框
    private Socket socket; //定义客户端系统的通信管道

    public ChatUILogin() { //登录界面
        setTitle("聊天室登录"); //显示标题
        setSize(300, 200); //设置界面大小
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //设置默认关闭操作
        setResizable(false); //设置不可更改界面大小

        setLayout(new FlowLayout()); //设置布局管理器

        //添加用户名标签和文本框
        JLabel usernameLabel = new JLabel("用户名:"); //创建"用户名:"输入框左侧提示
        usernameField = new JTextField(20); //创建文本输入框
        add(usernameLabel); //添加提示
        add(usernameField); //添加文本输入框

        //添加登录按钮
        JButton loginButton = new JButton("登录"); //创建登录按钮
        loginButton.addActionListener(new ActionListener() { //创建动作监听器
            @Override //重写父类方法，增强可读性和可维护性
            public void actionPerformed(ActionEvent e) { //监听按钮按下
                String username = usernameField.getText(); //获取登录用户名
                if (!username.isEmpty()) { //检查用户名是否为空
                    try { //登录
                        login(username); //登录
                        new ChatUIChat(username, socket); //启动聊天窗口
                        dispose(); //关闭登录窗口
                    } catch (Exception ex) {
                        ex.printStackTrace(); //输出错误报告
                    }
                } else {
                    nameEmpty(); //提醒用户用户名为空
                }
            }
        });
        add(loginButton); //添加登录按钮

        setLocationRelativeTo(null); //显示居中
        setVisible(true); //显示登录界面
    }

    public void nameEmpty() { //提醒用户用户名为空
        JOptionPane.showMessageDialog(this, "请输入昵称!"); //弹窗提醒用户名为空
    }

    public void login(String username) throws Exception { //登录
        socket = new Socket(Constant.SERVER_IP, Constant.SERVER_PORT); //创建socket管道请求连接服务器
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); //创建数据输出流
        dos.writeInt(1); //告知服务端为登录消息
        dos.writeUTF(username); //告知服务端用户名
        dos.writeInt(9); //告知服务端更新好友在线状态
        dos.writeUTF(username); //告知服务端用户名
        dos.flush(); //刷新数据避免滞留缓存区
    }
}
