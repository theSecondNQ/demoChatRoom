package com.nqserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ServerReaderThread extends Thread { //服务端监听子线程
    private Socket socket; //socket变量

    public ServerReaderThread(Socket socket) {
        this.socket = socket; //赋值socket变量
    }

    @Override
    public void run() {
        try {
            //与客户端声明协议发送消息：1、登录消息（含昵称）2、群聊消息 3、好友请求 4、同意好友请求 5、拒绝好友请求
            // 6、图片发送 7、文件发送 8、消息同步 9、好友在线状态同步
            //用socket管道接收客户端发送的消息类型编号
            DataInputStream dis = new DataInputStream(socket.getInputStream()); //定义数据输入流，从客户端获取数据
            while (true) {
                int type = dis.readInt(); //获取消息类型编号
                switch (type) {
                    case 1: //登录消息
                        String username = dis.readUTF();//读取用户名
                        Server.onLineSockets.put(socket, username);//存储客户端管道和用户名于Map集合
                        updateClientOnLineUsersList(); //更新在线人数列表
                        break;
                    case 2: //群聊消息
                        String msg = dis.readUTF(); //读取用户发送消息
                        sendMsgToAll(msg); //发送信息给所有用户
                        break;
                    case 3: //好友请求
                        String requestUsername = dis.readUTF(); //读取接受方用户名
                        String sendRequestUsername = dis.readUTF(); //读取发送方用户名
                        sendRequestFriendServer(requestUsername, sendRequestUsername); //发送好友请求
                        break;
                    case 4: //同意好友请求
                        String senderName = dis.readUTF(); //读取发送方用户名
                        String username1 = dis.readUTF(); //读取同意方用户名
                        responseFriendRequest(senderName, username1); //发送同意好友请求信息
                        break;
                    case 5: //拒绝好友请求
                        String RsenderName = dis.readUTF(); //读取发送方用户名
                        String Rusername1 = dis.readUTF(); //读取拒绝方用户名
                        rejectResponseFriendRequest(RsenderName, Rusername1); //发送拒绝好友请求信息
                        break;
                    case 6: //发送图片
                        int imageDataLength = dis.readInt(); //读取图片数据长度
                        byte[] imageData = new byte[imageDataLength]; //定义新字节数组
                        dis.readFully(imageData); //读取图片数据
                        sendImage(imageData); //发送图片
                        break;
                    case 7: //发送文件
                        String fileName = dis.readUTF(); //读取文件名
                        int fileDataLength = dis.readInt(); //读取文件数据长度
                        byte[] fileData = new byte[fileDataLength]; //定义新字节数组
                        dis.readFully(fileData); //读取文件数据
                        sendFile(fileData, fileName); //发送文件
                        break;
                    case 8: //消息漫游同步
                        String username2 = dis.readUTF(); //读取上线用户名
                        msgSync(username2); //将消息同步给新上线用户
                        break;
                    case 9: //更新好友在线状态
                        String onlineUsername = dis.readUTF(); //读取上线用户名
                        onlineFriend(onlineUsername); //更新好友在线状态
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("客户端下线:" + socket.getInetAddress().getHostAddress()); //输出客户端下线信息
            String usernameRemove = Server.onLineSockets.get(socket); //获取下线客户端用户名
            Server.onLineSockets.remove(socket); //将下线的客户端socket从集合中移除
            updateClientOnLineUsersList(); //更新在线人数列表
            removeFriend(usernameRemove); //更新好友离线状态
        }
    }

    private void onlineFriend(String onlineUsername) { //更新好友在线状态
        Collection<String> onLineUsers = Server.onLineSockets.values(); //获取所有在线用户昵称
        for (Socket socket : Server.onLineSockets.keySet()){ //获取集合中管道
            try { //将新上线用户名发送给所有在线用户
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); //创建数据输出流
                dos.writeInt(10); //告诉客户端接下来为好友在线状态更新消息
                dos.writeUTF(onlineUsername); //发送新上线用户名
                dos.flush(); //刷新避免数据滞留缓存区
            } catch (Exception e){
                e.printStackTrace(); //输出错误报告
            }
        }
    }

    private void removeFriend(String usernameRemove) { //更新好友离线状态
        Collection<String> onLineUsers = Server.onLineSockets.values(); //获取所有在线用户昵称
        for (Socket socket : Server.onLineSockets.keySet()){ //获取集合中管道
            try { //把下线用户名发送给所有在线用户
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); //创建数据输出流
                dos.writeInt(9); //告诉客户端接下来为更新好友离线状态消息
                dos.writeUTF(usernameRemove); //发送下线用户名
                dos.flush(); //刷新避免数据滞留缓存区
            } catch (Exception e){
                e.printStackTrace(); //输出错误报告
            }
        }
    }

    private void msgSync(String username) throws IOException { //消息漫游同步
        String filePath = Server.getFilePath(); //获取消息文件保存路径
        Path path = Paths.get(filePath); //定义路径变量并赋值
        String msgSync = ""; //定义同步消息变量
        if (Files.exists(path)) { //判断文件是否存在
            msgSync = new String(Files.readAllBytes(path)); //读取保存消息
        } else {
            msgSync = "无消息记录"; //设定同步消息变量为“无消息变量”
        }
        String[] onLineUsers = Server.onLineSockets.values().toArray(new String[0]); //获取所有在线用户名称
        int i = 0; //定义变量i，初始化为0
        for(Socket socket : Server.onLineSockets.keySet()) { //获取所有在线用户管道
            if (Objects.equals(onLineUsers[i], username)) { //判断新上线用户与当前遍历用户名称是否匹配
                try { //发送消息记录
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); //创建数据输出流
                    dos.writeInt(8); //告诉客户端为消息同步信息
                    dos.writeUTF(msgSync); //发送消息记录
                    dos.flush(); //刷新数据避免滞留缓存区
                } catch (Exception e) {
                    e.printStackTrace(); //输出错误报告
                }
            }
            i+=1; //变量i自增
        }
    }




    private void sendImage(byte[] imageData) throws IOException { //发送图片
        StringBuilder sb = new StringBuilder(); //拼装消息用
        String name = Server.onLineSockets.get(socket); //获取用户名
        LocalDateTime now = LocalDateTime.now(); //获取时间
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss EEE"); //时间格式
        String nowStr = dtf.format(now); //将时间按格式拼装
        String msgResult = String.valueOf(sb.append(name).append(" ").append(nowStr).append("\r\n")); //拼装消息
        String msgSync = (msgResult + "发送了一张图片\n"); //记录消息
        Server.msgSyncSave(msgSync); //存储消息
        for(Socket socket : Server.onLineSockets.keySet()){ //获取所有在线用户管道
            try {
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); //创建数据输出流
                dos.writeInt(6); //告诉客户端为图片发送信息
                dos.writeUTF(msgResult); //发送文本消息
                dos.writeInt(imageData.length); //告诉客户端图片数据长度
                dos.write(imageData); //发送图片数据
                dos.flush(); //刷新数据避免滞留缓存区
            } catch (IOException e) {
                e.printStackTrace(); //输出错误报告
            }
        }
    }

    private void sendFile(byte[] fileData,String fileName) throws IOException { //发送文件
        StringBuilder sb = new StringBuilder(); //拼装消息用
        String name = Server.onLineSockets.get(socket); //获取用户名
        LocalDateTime now = LocalDateTime.now(); //获取时间
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss EEE"); //时间格式
        String nowStr = dtf.format(now); //将时间按格式拼装
        String msgResult = String.valueOf(sb.append(name).append(" ").append(nowStr).append("\r\n")); //拼装消息
        String msgSync = (msgResult + "发送了一个文件\n"); //记录消息
        Server.msgSyncSave(msgSync); //存储消息
        for(Socket socket : Server.onLineSockets.keySet()){ //获取所有在线用户管道
            try {
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); //创建数据输出流
                dos.writeInt(7); //告诉客户端为发送文件消息
                dos.writeUTF(msgResult); //发送文本消息
                dos.writeInt(fileData.length); //告诉客户端文件数据长度
                dos.write(fileData); //发送文件数据
                dos.writeUTF(fileName); //发送文件名
                dos.flush(); //刷新数据避免滞留缓存区
            } catch (IOException e) {
                e.printStackTrace(); //输出错误报告
            }
        }
    }

    private void rejectResponseFriendRequest(String rsenderName, String rusername1) { //拒绝好友请求
        String[] onLineUsers = Server.onLineSockets.values().toArray(new String[0]); //获取所有在线用户信息
        int i = 0; //定义变量i,初始化为0
        for(Socket socket : Server.onLineSockets.keySet()) { //获取所有在线用户管道
            if (Objects.equals(onLineUsers[i], rsenderName)) { //判断请求方用户名与当前遍历用户名称是否匹配
                try { //发送拒绝信息
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); //创建数据输出流
                    dos.writeInt(5); //告知客户端为好友拒绝信息
                    dos.writeUTF(rusername1); //发送拒绝方用户名
                    dos.flush(); //刷新数据避免滞留缓存区
                } catch (Exception e) {
                    e.printStackTrace(); //输出错误报告
                }
            }
            i+=1; //变量i自增
        }
    }

    private void responseFriendRequest(String senderName,String username) { //同意好友请求
        String[] onLineUsers = Server.onLineSockets.values().toArray(new String[0]); //获取在线用户名称
        int i = 0; //定义变量i，初始化为0
        for(Socket socket : Server.onLineSockets.keySet()){ //获取在线用户管道
            if(Objects.equals(onLineUsers[i], senderName)){ //判断请求方用户名与当前遍历用户名称是否匹配
                try { //发送同意信息
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); //创建数据输出流
                    dos.writeInt(4); //告知客户端为好友同意信息
                    dos.writeUTF(username); //发送同意方用户名
                    dos.flush(); //刷新数据避免滞留缓存区
                } catch (Exception e){
                    e.printStackTrace(); //输出错误报告
                }
            }
            i+=1; //变量i自增
        }
    }

    private void sendRequestFriendServer(String requestUsername,String sendRequestUsername) { //发送好友请求
        String[] onLineUsers = Server.onLineSockets.values().toArray(new String[0]); //获取在线用户名称
        int i = 0; //定义变量i,初始化为0
        for (Socket socket : Server.onLineSockets.keySet()){ //获取在线用户管道
            if(Objects.equals(onLineUsers[i], requestUsername)){ //判断接收方用户名与当前遍历用户名称是否匹配
                try { //发送好友请求信息
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); //创建数据输出流
                    dos.writeInt(3); //告诉客户端为好友请求信息
                    dos.writeUTF(sendRequestUsername); //发送请求方用户名
                    dos.flush(); //刷新数据避免滞留缓存区
                } catch (Exception e){
                    e.printStackTrace(); //输出错误报告
                }
            }
            i+=1; //变量i自增
        }
    }

    private void sendMsgToAll(String msg) throws IOException { //给所有用户推送消息
        StringBuilder sb = new StringBuilder(); //拼装消息用
        String name = Server.onLineSockets.get(socket); //获取用户名
        LocalDateTime now = LocalDateTime.now(); //获取时间
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss EEE"); //时间格式
        String nowStr = dtf.format(now); //将时间按格式拼装
        String msgResult = sb.append(name).append(" ").append(nowStr).append("\r\n")
                .append(msg).append("\r\n").toString(); //拼装消息
        Server.msgSyncSave(msgResult); //存储消息
        for (Socket socket : Server.onLineSockets.keySet()){ //获取集合中管道
            try { //把消息发至客户端
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); //创建数据输出流
                dos.writeInt(2); //告诉客户端接下来为群聊消息
                dos.writeUTF(msgResult); //发送消息
                dos.flush(); //刷新数据避免滞留缓存区
            } catch (Exception e){
                e.printStackTrace(); //输出错误报告
            }
        }
    }

    private void updateClientOnLineUsersList() { //更新在线人数列表
        Collection<String> onLineUsers = Server.onLineSockets.values(); //获取所有在线用户昵称
        for (Socket socket : Server.onLineSockets.keySet()){ //获取集合中管道
            try { //把集合中的名称发至客户端
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); //创建数据输出流
                dos.writeInt(1); //告诉客户端接下来为在线人数列表消息
                dos.writeInt(onLineUsers.size()); //告诉客户端将发用户名称数量
                for (String onLineUser : onLineUsers){ //遍历所有在线用户
                    dos.writeUTF(onLineUser); //发送当前遍历用户名
                }
                dos.flush(); //刷新避免数据滞留缓存区
            } catch (Exception e){
                e.printStackTrace(); //输出错误报告
            }
        }
    }

}
