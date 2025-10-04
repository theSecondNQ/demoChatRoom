package com.nqchatroom.ui;

import java.io.DataInputStream;
import java.net.Socket;

public class ClientReaderThread extends Thread{ //客户端监听子线程
    private Socket socket; //定义管道变量
    private DataInputStream dis; //定义文件输入流
    private ChatUIChat win; //定义聊天界面
    public ClientReaderThread(Socket socket,ChatUIChat win){ //接收主线程信息
        this.win = win; //引入聊天界面
        this.socket = socket; //读取管道
    }

    @Override //重写父类方法，增强可读性和可维护性
    public void run(){
        try {
            //与服务端声明协议发送消息：1、更新在线用户列表 2、群聊消息 3、好友请求 4、同意好友请求 5、拒绝好友请求
            // 6、图片接收 7、文件接收 8、消息同步 9、好友离线状态同步 10、好友在线状态同步
            //用socket管道接收客户端发送的消息类型编号
            dis = new DataInputStream(socket.getInputStream()); //创建数据输入流
            while (true) {
                int type = dis.readInt(); //获取消息类型编号
                switch (type) {
                    case 1:
                        updateClientOnLineUserList(); //更新在线用户列表
                        break;
                    case 2:
                        getMsgToWin(); //接收消息
                        break;
                    case 3:
                        getRequestToWin(); //接收好友请求
                        break;
                    case 4:
                        responseRequestToWin(); //接收好友请求同意信息
                        break;
                    case 5:
                        rejectResponseRequestToWin(); //接收好友请求拒绝信息
                        break;
                    case 6:
                        getImageToWin(); //接收图片
                        break;
                    case 7:
                        getFileToWin(); //接收文件
                        break;
                    case 8:
                        getSyncMsgToWin(); //消息漫游同步
                        break;
                    case 9:
                        setFriendOffline(); //更新好友离线状态
                        break;
                    case 10:
                        setFriendOnline(); //更新好友在线状态
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); //输出错误报告
        }
    }

    private void setFriendOnline() throws Exception { //更新好友在线状态
        String onlineFriend = dis.readUTF(); //读取上线好友用户名
        win.setFriendOnlineClient(onlineFriend); //在聊天界面更新好友在线状态
    }

    private void setFriendOffline() throws Exception { //更新好友离线状态
        String offlineFriend = dis.readUTF(); //读取离线好友用户名
        win.setFriendOfflineClient(offlineFriend); //在聊天界面更新好友离线状态
    }

    private void getSyncMsgToWin() throws Exception { //消息漫游同步
        String msgS = dis.readUTF(); //获取同步消息
        win.setMsgToWin(msgS); //在聊天界面同步消息
    }

    private void getFileToWin() throws Exception { //接收文件
        String Filefront = dis.readUTF(); //获取消息前缀
        int fileDataLength = dis.readInt(); //获取文件数据长度
        byte[] fileData = new byte[fileDataLength]; //定义字节数组
        dis.readFully(fileData); //获取文件数据
        String fileName = dis.readUTF(); //获取文件名
        win.setFileToWin(Filefront, fileData,fileName); //将文件传到客户端
    }

    private void getImageToWin() throws Exception { //接收图片
        String front = dis.readUTF(); //获取消息前缀
        int imageDataLength = dis.readInt(); //获取图片数据长度
        byte[] imageData = new byte[imageDataLength]; //定义字节数组
        dis.readFully(imageData); //获取图片数据
        win.setImageToWin(front,imageData); //将图片传到客户端
    }

    private void rejectResponseRequestToWin() throws Exception { //拒绝好友请求
        String Rusername = dis.readUTF(); //获取拒绝方用户名
        win.rejectSendRequestFeedback(Rusername); //将拒绝信息传到客户端
    }

    private void responseRequestToWin() throws Exception { //同意好友请求
        String username = dis.readUTF(); //获取同意方用户名
        win.sendRequestFeedback(username); //将同意信息传到客户端
    }

    private void getMsgToWin() throws Exception { //接收消息
        String msg = dis.readUTF(); //读取消息
        win.setMsgToWin(msg); //将消息传到客户端

    }

    private void updateClientOnLineUserList() throws Exception { //更新在线用户列表
        int count = dis.readInt(); //读取在线用户数
        String[] names = new String[count]; //定义字符串数组
        for(int i=0;i<count;i++){ //遍历
            String username = dis.readUTF(); //读取用户名
            names[i] = username; //赋值用户名到字符串数组
        }
        win.updateOnlineUsers(names,count); //将新列表传到客户端
    }

    private void getRequestToWin() throws Exception { //收到好友请求
        String sendRequestUsername = dis.readUTF(); //读取请求方用户名
        win.requestFriend(sendRequestUsername); //将请求信息传到客户端
    }
}
