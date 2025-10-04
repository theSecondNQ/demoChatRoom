package com.nqserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    public static final Map<Socket,String> onLineSockets = new HashMap<>(); //用Map集合存储各客户端管道与其用户名
    public static void main(String[] args) throws Exception { //服务端主线程
        System.out.println("启动服务端...");
        try {
            ServerSocket serverSocket=new ServerSocket(Constant.PORT);// 注册端口
            new ServerChatConsole(); //启动服务器聊天终端
            while (true){ //主线程负责接收客户端连接请求
                System.out.println("等待客户端连接...");
                Socket socket=serverSocket.accept(); //调用accpet方法，获取客户端Socket对象
                new ServerReaderThread(socket).start(); //将接收到的客户端交由子线程处理，以实现接收多个客户端信息
                System.out.println("一个客户端连接成功...");
            }
        } catch (IOException e) {
            e.printStackTrace(); //输出错误报告
        }

    }

    public static void msgSyncSave(String msg) throws IOException { //消息漫游消息存储方法
        String filePath = getFilePath(); //获取消息存储路径
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) { //启动BufferedWriter保存聊天记录
            writer.write(msg); //保存消息
        }

    }

    public static String getFilePath(){ //获取消息存储路径方法
        String filePath1 = ("message.txt"); //获取消息存储文件路径
        return filePath1; //返回路径
    }

}