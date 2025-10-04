package com.nqserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ServerChatConsole extends JFrame {
    private JTextArea chatArea; //定义消息展示区
    private JScrollPane scrollPane; //定义滚动面板
    private JTextArea messageField; //定义消息输入框
    private JButton sendButton; //定义发送按钮
    private JButton sendImageButton; //定义图片发送按钮
    private JButton sendFileButton; //定义文件发送按钮
    private JList<String> userList; //定义用户列表
    private JList<String> friendList; //定义好友列表
    private DefaultListModel<String> userListModel; //定义用户列表模型
    private DefaultListModel<String> friendListModel; //定义好友列表模型
    private String username; //定义用户名
    private Socket socket; //定义管道

    public ServerChatConsole() { //服务器聊天终端
        try {
            socket = new Socket(Constant.IP, Constant.PORT); //创建socket管道请求连接服务器
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); //创建数据输出流
            dos.writeInt(1); //告知服务端为登录消息
            dos.writeUTF("Server"); //告知服务端为服务器聊天终端
            dos.flush(); //刷新数据避免滞留缓存区
        } catch (Exception e){
            e.printStackTrace(); //输出错误报告
        }
        this.username = "Server"; //定义全局username变量为"Server"
        setTitle("服务器聊天终端"); //设置标题为“服务器聊天终端”
        new ServerChatConsoleReaderThread(socket, this).start(); //启动服务器聊天终端监听子线程
        sendMsgToServer("Server Started"); //发送服务器启动消息
        setSize(800, 600); //设置窗口大小
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //设置默认窗口关闭动作

        //创建消息展示区
        chatArea = new JTextArea(); //创建消息展示区
        chatArea.setEditable(false); //设置不可编辑
        chatArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14)); //设置展示字体
        chatArea.setBackground(Color.decode("#F8F8F8")); //设置背景色
        chatArea.setForeground(Color.BLACK); //设置前景色
        scrollPane = new JScrollPane(chatArea); //设置消息展示区为滚动面板
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY)); //设置边界线为灰色
        add(scrollPane, BorderLayout.CENTER); //设置消息展示区布局

        //创建消息输入框
        messageField = new JTextArea(); //创建消息输入框
        messageField.setLineWrap(true); //允许自动换行
        messageField.setWrapStyleWord(true); //单词换行
        messageField.setPreferredSize(new Dimension(400, 50)); //调整宽度和高度
        messageField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14)); //设置展示字体
        messageField.setBackground(Color.WHITE); //设置背景色
        messageField.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY)); //设置边界线为浅灰色

        //添加键盘监听器
        messageField.addKeyListener(new KeyListener() { //创建新键盘监听器
            @Override //重写父类方法，增强可读性和可维护性
            public void keyTyped(KeyEvent e) { //监听字符输入
            }

            @Override //重写父类方法，增强可读性和可维护性
            public void keyPressed(KeyEvent e) { //监听键盘敲下
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) { //判断是否只有Enter被按下
                    sendMessage(); //发送消息
                    e.consume(); //阻止默认操作
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isShiftDown()) { //判断Enter和Shift是否一同被按下
                    int caretPosition = messageField.getCaretPosition(); //跳转至文段末尾
                    messageField.insert("\n", caretPosition); //换行
                    e.consume(); //阻止默认操作
                }
            }

            @Override //重写父类方法，增强可读性和可维护性
            public void keyReleased(KeyEvent e) { //监听键盘释放
            }
        });

        //创建发送按钮
        sendButton = new JButton("发送"); //创建发送按钮
        sendButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14)); //设置“发送”字体格式
        sendButton.setBackground(Color.decode("#007BFF")); //设置按钮背景色
        sendButton.setForeground(Color.WHITE); //设置按钮前景色
        sendButton.setBorderPainted(false); //设置边框不被渲染
        sendButton.addActionListener(e -> sendMessage()); //设置动作监听器，当按钮被按下则发送消息

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0)); //增加水平间隔
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); //边框留白
        inputPanel.add(new JScrollPane(messageField), BorderLayout.CENTER); //设置按钮布局
        inputPanel.add(sendButton, BorderLayout.EAST); //按钮放在右侧
        add(inputPanel, BorderLayout.SOUTH); //设置按钮在界面中布局

        //创建发送图片按钮
        sendImageButton = new JButton("发送图片"); //创建发送图片按钮
        sendImageButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14)); //设置"发送图片"字体格式
        sendImageButton.setBackground(Color.decode("#28A745")); //设置背景色
        sendImageButton.setForeground(Color.WHITE); //设置前景色
        sendImageButton.setBorderPainted(false); //设置边框不渲染
        sendImageButton.addActionListener(e -> sendImage()); //设置动作监听器，当按钮被按下则发送图片

        //创建发送文件按钮
        sendFileButton = new JButton("发送文件"); //创建文件发送按钮
        sendFileButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14)); //设置"发送文件"字体格式
        sendFileButton.setBackground(Color.decode("#DC3545")); //设置背景色
        sendFileButton.setForeground(Color.WHITE); //设置前景色
        sendFileButton.setBorderPainted(false); //设置边框不渲染
        sendFileButton.addActionListener(e -> sendFile()); //设置动作监听器，当按钮被按下则发送文件

        inputPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0)); //增加水平间隔
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); //边框留白
        inputPanel.add(new JScrollPane(messageField)); //将按钮增加至界面
        inputPanel.add(sendButton); //增加消息发送按钮
        inputPanel.add(sendImageButton); //增加图片发送按钮
        inputPanel.add(sendFileButton); //增加文件发送按钮
        add(inputPanel, BorderLayout.SOUTH); //设置按钮在界面中的布局

        //创建在线用户列表
        userListModel = new DefaultListModel<>(); //创建在线用户列表模型
        userList = new JList<>(userListModel); //创建在线用户列表
        userList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14)); //设置展示字体
        userList.setBackground(Color.decode("#F8F8F8")); //设置背景色
        userList.setForeground(Color.BLACK); //设置前景色
        userList.setFixedCellWidth(100); //设置列表宽度
        userList.requestFocusInWindow(); //设置窗口内聚焦
        JScrollPane userScrollPane = new JScrollPane(userList); //将用户列表置于界面
        userScrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY)); //设置边界为灰色

        //创建好友列表
        friendListModel = new DefaultListModel<>(); //创建好友列表模型
        friendList = new JList<>(friendListModel); //创建好友列表
        friendList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14)); //设置展示字体
        friendList.setBackground(Color.decode("#F8F8F8")); //设置背景色
        friendList.setForeground(Color.BLACK); //设置前景色
        friendList.setFixedCellWidth(100); //设置列表宽度
        JScrollPane friendScrollPane = new JScrollPane(friendList); //将好友列表置于界面
        friendScrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY)); //设置边界为灰色

        //创建一个容器来包含用户列表和好友列表
        JPanel usersAndFriendsContainer = new JPanel(); //创建容器
        usersAndFriendsContainer.setLayout(new BoxLayout(usersAndFriendsContainer, BoxLayout.Y_AXIS)); //设置列表在容器内布局
        usersAndFriendsContainer.add(userScrollPane); //增加用户列表
        usersAndFriendsContainer.add(friendScrollPane); //增加好友列表

        //将用户列表容器添加到主面板上
        add(usersAndFriendsContainer, BorderLayout.EAST);

        //居中显示
        setLocationRelativeTo(null);

        //添加鼠标监听器以支持右键点击
        userList.addMouseListener(new MouseAdapter() {  //添加鼠标监听器
            @Override //重写父类方法，增强可读性和可维护性
            public void mousePressed(MouseEvent e) { //监听鼠标按下
                if (SwingUtilities.isRightMouseButton(e)) { //判断鼠标右键是否被按下
                    int index = userList.locationToIndex(e.getPoint()); //获取鼠标坐标点
                    if (index != -1 && index < userListModel.getSize()) { //确保索引有效并且小于模型大小
                        String selectedUser = userListModel.getElementAt(index); //获取所选用户索引
                        showPopupMenu(selectedUser, e); //展示下拉列表
                    }
                }
            }
        });

        setVisible(true); //显示聊天界面
    }

    private void sendMessage() { //发送消息
        String message = messageField.getText().trim(); //获取消息输入框内信息
        if (!message.isEmpty()) { //判断消息输入框内是否为空
            StringBuilder sb = new StringBuilder(); //拼装消息用
            String name = username; //获取用户名
            LocalDateTime now = LocalDateTime.now(); //获取时间
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss EEE"); //时间格式
            String nowStr = dtf.format(now); //将时间按格式拼装
            sb.append(name).append(" ").append(nowStr).append("\r\n")
                    .append(message).append("\r\n").toString(); //拼装消息
            messageField.setText(""); //清空输入框
            sendMsgToServer(message); //发送消息至服务端
        }
    }

    private void sendMsgToServer(String message) { //发送消息至服务端
        try { //发送消息至服务端
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); //创建数据输出流
            dos.writeInt(2); //告知服务端将要发送消息
            dos.writeUTF(message); //发送消息
            dos.flush(); //刷新数据避免滞留缓存区
        } catch (IOException e) {
            e.printStackTrace(); //输出错误报告
        }
    }

    private void showPopupMenu(String selectedUser, MouseEvent e) { //好友请求下拉列表
        JPopupMenu popupMenu = new JPopupMenu(); //创建下拉列表
        JMenuItem addFriendItem = new JMenuItem("添加好友"); //增加“添加好友”选项
        addFriendItem.addActionListener(new ActionListener() { //创建动作监听器
            @Override //重写父类方法，增强可读性和可维护性
            public void actionPerformed(ActionEvent e) { //监听按下动作
                try { //发送好友请求
                    addFriend(selectedUser); //发送好友请求至服务端
                } catch (Exception ex) {
                    ex.printStackTrace(); //输出错误报告
                }
            }
        });
        popupMenu.add(addFriendItem); //创建下拉列表

        if (popupMenu.getComponentCount() > 0) { //确保菜单中有至少一个条目
            popupMenu.show(userList, e.getX(), e.getY()); //显示下拉列表
        }
    }

    private void addFriend(String selectedUser) throws Exception { //好友请求
        if (!friendListModel.contains(selectedUser)) { //判断好友列表模型内是否存在该用户
            sendRequestFriend(selectedUser); //发送好友请求
        }

    }


    public void updateOnlineUsers(String[] onLineNames,int count) { //更新在线用户列表
        userList.setListData(onLineNames); //选中列表数据中所有在线用户名
        userListModel.removeAllElements(); //移除旧列表数据
        for(int i=0;i < count;i++){ //遍历
            userListModel.addElement(onLineNames[i]); //更新在线用户列表
        }
    }

    public void setMsgToWin(String msg) { //将接收消息展示到聊天框
        chatArea.append(msg); //展示消息
    }

    public void sendRequestFriend(String selectedUser) { //发送好友请求
        try { //发送好友请求
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); //创建数据输出流
            dos.writeInt(3); //告知服务端为好友请求信息
            dos.writeUTF(selectedUser); //告知服务端接收方用户名
            dos.writeUTF(username); //告知服务端请求方用户名
            dos.flush(); //刷新数据避免滞留缓存区
        } catch (Exception e){
            e.printStackTrace(); //输出错误报告
        }
    }

    public void requestFriend(String sendRequestUsername){ //展示好友请求弹窗
        SwingUtilities.invokeLater(() -> { //将弹窗置于屏幕最前
            showFriendRequestDialog(sendRequestUsername); //展示好友请求弹窗
        });
    }

    public void sendRequestFeedback(String username){ //好友请求同意
        friendListModel.addElement(username + " 在线"); //将同意方添加至好友列表
        chatArea.append(username+"接受了你的好友请求\n"); //告知用户请求已被同意
    }

    public void rejectSendRequestFeedback(String rusername){ //好友请求拒绝
        chatArea.append(rusername+"拒绝了你的好友请求\n"); //告知用户请求已被拒绝
    }

    public void showFriendRequestDialog(String senderName) { //好友请求弹窗
        Object[] options = {"接受", "拒绝"}; //创建一个选项面板，包含两个按钮：接受和拒绝
        String message = "您收到了来自 " + senderName + " 的好友请求。接受吗？"; //创建提示消息
        //显示带有选项的对话框
        int option = JOptionPane.showOptionDialog(
                null, //使用默认的父组件（null表示顶级窗口）
                message, //展示提示信息
                username+"的好友请求处理窗口", //设置对话框标题
                JOptionPane.YES_NO_OPTION, //设置选项类型为YES_NO_CANCEL_OPTION
                JOptionPane.QUESTION_MESSAGE, //设置对话框图标类型
                null, //设置自定义图标（null表示使用默认图标）
                options, //设置选项数组
                options[0] //设置默认选中的选项（接受）
        );

        //根据用户的响应处理结果
        if (option == JOptionPane.YES_OPTION) {
            acceptFriendRequest(senderName); //用户选择了接受，发送同意请求
        } else if (option == JOptionPane.NO_OPTION) {
            rejectFriendRequest(senderName); //用户选择了拒绝，发送拒绝请求
        }

    }

    private void acceptFriendRequest(String senderName) { //发送同意好友请求消息
        friendListModel.addElement(senderName + " 在线"); //在好友列表中添加请求方
        try { //向服务端发送同意好友请求消息
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); //创建数据输出流
            dos.writeInt(4); //告知服务端为好友请求同意消息
            dos.writeUTF(senderName); //发送请求方用户名
            dos.writeUTF(username); //发送同意方用户名
            dos.flush(); //刷新数据避免滞留缓存区
        } catch (Exception e) {
            e.printStackTrace(); //输出错误报告

        }
    }

    private void rejectFriendRequest(String senderName) { //发送拒绝好友请求消息
        try { //向服务端发送拒绝好友请求消息
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); //创建数据输出流
            dos.writeInt(5); //告知服务端为好友请求拒绝消息
            dos.writeUTF(senderName); //发送请求方用户名
            dos.writeUTF(username); //发送拒绝方用户名
            dos.flush(); //刷新数据避免滞留缓存区
        } catch (Exception e) {
            e.printStackTrace(); //输出错误报告

        }
    }

    private void sendImage() { //发送图片
        JFileChooser fileChooser = new JFileChooser(); //打开图片选择界面
        int result = fileChooser.showOpenDialog(this); //显示界面
        if (result == JFileChooser.APPROVE_OPTION) { //判断确定按钮是否被按下
            File selectedFile = fileChooser.getSelectedFile(); //获取所选图片
            if (selectedFile != null) { //判断所选图片是否为空
                try { //发送图片
                    sendImageToServer(selectedFile); //发送图片
                } catch (IOException e) {
                    e.printStackTrace(); //输出错误报告
                }
            }
        }
    }

    private void sendImageToServer(File file) throws IOException { //向服务端发送图片
        FileInputStream fis = new FileInputStream(file); //创建文件输入流
        byte[] imageData = new byte[(int) file.length()]; //定义字节数组
        fis.read(imageData); //读取图片数据
        fis.close(); //关闭文件输入流
        try { //向服务端发送图片
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); //创建数据输出流
            dos.writeInt(6); //告知服务端将要发送图片
            dos.writeInt(imageData.length); //发送图片数据长度
            dos.write(imageData); //发送图片数据
            dos.flush(); //刷新数据避免滞留缓存区
        } catch (IOException e) {
            e.printStackTrace(); //输出错误报告
        }
    }

    public void setImageToWin(String front,byte[] imageData){ //接收图片
        try { //接收图片
            File tempImageFile = File.createTempFile("chat_image_", ".png"); //创建临时文件
            FileOutputStream fos = new FileOutputStream(tempImageFile); //创建文件输出流
            fos.write(imageData); //将图片数据写入临时文件
            fos.close(); //关闭文件输出流
            chatArea.append(front); //显示消息前缀
            chatArea.append("发送了一张图片\n"); //显示图片发送消息
            ImageIcon imageIcon = new ImageIcon(tempImageFile.getAbsolutePath()); //创建ImageIcon并载入图片
            JFrame imageFrame = new JFrame("图片"); //创建图片弹窗
            imageFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); //设置默认关闭操作
            JLabel imageLabel = new JLabel(imageIcon); //设置图片标签
            imageFrame.getContentPane().add(imageLabel); //添加图片标签到帧中
            imageFrame.pack(); //设置帧大小适应图片尺寸
            imageFrame.setSize(Math.max(imageFrame.getWidth(), 800), Math.max(imageFrame.getHeight(), 600)); //显示帧
            imageFrame.setLocationRelativeTo(null); //居中显示
            imageFrame.setVisible(true); //显示弹窗
        } catch (IOException e) {
            e.printStackTrace(); //输出错误报告
        }
    }

    private void sendFile() { //发送文件
        JFileChooser fileChooser = new JFileChooser(); //创建文件选择窗口
        int result = fileChooser.showOpenDialog(this); //打开文件选择窗口
        if (result == JFileChooser.APPROVE_OPTION) { //判断确定按钮是否被按下
            File selectedFile = fileChooser.getSelectedFile(); //获取所选文件
            if (selectedFile != null) { //判断所选文件是否为空
                try { //发送文件
                    sendFileToServer(selectedFile); //发送文件
                } catch (Exception e) {
                    e.printStackTrace(); //发送错误报告
                }
            }
        }
    }

    private void sendFileToServer(File file) throws IOException { //发送文件到服务端
        FileInputStream fis = new FileInputStream(file); //创建文件输入流
        byte[] fileData = new byte[(int) file.length()]; //定义字节数组
        fis.read(fileData); //读取文件数据
        fis.close(); //关闭文件输入流
        try { //将文件发送到服务端
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); //创建数据输出流
            dos.writeInt(7); //告诉服务器将要发送文件
            dos.writeUTF(file.getName()); //发送文件名
            dos.writeInt(fileData.length); //发送文件数据长度
            dos.write(fileData); //发送文件数据
            dos.flush(); //刷新数据避免滞留缓存区
        } catch (IOException e) {
            e.printStackTrace(); //打印错误报告
        }
    }

    public void setFileToWin(String front,byte[] fileData,String fileName) throws IOException { //接收文件
        try { //接收文件
            File receivedFile = new File("received_" + fileName); //创建接收文件
            FileOutputStream fos = new FileOutputStream(receivedFile); //创建文件输出流
            fos.write(fileData); //写入文件数据
            fos.close(); //关闭文件输出流
            String filePath = receivedFile.getAbsolutePath(); //获取文件的绝对路径
            chatArea.append(front); //显示消息前缀
            chatArea.append("发送了一个文件\n"); //显示文件发送消息
            chatArea.append("文件保存于"+filePath+"\n"); //告知用户文件保存路径
            //提示用户是否要打开文件
            int result = JOptionPane.showConfirmDialog(this, "是否立即打开文件？", "文件已收到", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) { //判断用户是否要打开文件
                Desktop.getDesktop().open(receivedFile); //打开文件
            }
        } catch (IOException e) {
            e.printStackTrace(); //输出错误报告
        }
    }

    public void setFriendOfflineClient(String offlineFriend) { //更新好友离线状态
        for(int i = 0;i<friendListModel.getSize();i++){ //遍历好友列表
            //判断离线好友用户名与当前遍历好友用户名是否匹配
            if (Objects.equals(friendListModel.getElementAt(i), (offlineFriend + " 在线"))){
                friendListModel.removeElementAt(i); //移除好友在线状态
                friendListModel.addElement(offlineFriend + " 离线"); //更新好友离线状态
            }
        }
    }

    public void setFriendOnlineClient(String onlineFriend) { //更新好友在线状态
        for(int i = 0;i<friendListModel.getSize();i++){ //遍历好友列表
            //判断在线好友用户名与当前遍历好友用户名是否匹配
            if (Objects.equals(friendListModel.getElementAt(i), (onlineFriend + " 离线"))){
                friendListModel.removeElementAt(i); //移除好友离线状态
                friendListModel.addElement(onlineFriend + " 在线"); //更新好友在线状态
            }
        }
    }



}

