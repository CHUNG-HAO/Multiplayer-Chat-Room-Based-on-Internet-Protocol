package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;


public class ChatClient extends JFrame {
    private JTextArea sendArea, contentArea;
    JPanel p1, p11, p12, p2, p21, p22;
    JComboBox<String> user1, user2;
    private RecvThread clientThread = null;
    
    public void start() {
        Container con = getContentPane();
        con.setLayout(new BorderLayout());
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width / 2;
        int screenHeight = screenSize.height / 2;
        int height = getHeight();
        int width = getWidth();
        setSize(350, 400);
        setLocation((screenWidth - width) / 2, (screenHeight - height) / 2);
        sendArea = new JTextArea(3, 10);
        sendArea.setBorder(BorderFactory.createLineBorder(Color.BLUE, 1));
        sendArea.setLineWrap(true);
        sendArea.setWrapStyleWord(true);
        sendArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if ('\n' == e.getKeyCode())
                    if (clientThread != null) {
                        clientThread.sendMsg();
                    }
            }
        });
        contentArea = new JTextArea(6, 10);
        contentArea.setBorder(BorderFactory.createLineBorder(Color.BLUE, 1));
        
        p1 = new JPanel();
        p1.setLayout(new BorderLayout());
        p11 = new JPanel();
        p11.setLayout(new GridLayout(1, 2));
        JLabel l1 = new JLabel("選擇你的身份：");
        user1 = new JComboBox<>();
        user1.addItem("-----選擇-----");
        user1.addItem("鍾弘浩");
        user1.addItem("411077033");
        user1.addItem("學生");
        user1.addItem("軟工二");

        user1.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {

                if (user1.getSelectedIndex() == 0) {
                    return;
                }
                clientThread = new RecvThread((String) user1.getSelectedItem());
                new Thread(clientThread).start();
            }
        });
        p11.add(l1);
        p11.add(user1);
        p12 = new JPanel();
        p12.setLayout(new GridLayout(1, 1));
        p12.add(new JScrollPane(contentArea));
        p1.add(p11, BorderLayout.NORTH);
        p1.add(p12, BorderLayout.SOUTH);
        
        p2 = new JPanel();
        p2.setLayout(new BorderLayout());
        p21 = new JPanel();
        p21.setLayout(new GridLayout(2, 2));
        user2 = new JComboBox<>();
        user2.addItem("所有用戶");
        JLabel l2 = new JLabel("選擇要發送的用戶");
        p21.add(l2);
        p21.add(user2);
        p22 = new JPanel();
        p22.setLayout(new GridLayout(1, 1));
        p22.add(new JScrollPane(sendArea));
        p2.add(p21, BorderLayout.NORTH);
        p2.add(p22, BorderLayout.SOUTH);
        con.add(p1, BorderLayout.NORTH);
        con.add(p2, BorderLayout.SOUTH);
        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (clientThread != null)
                    clientThread.exit();
                System.exit(0);
            }
        });
    }

    private class RecvThread implements Runnable {
        private Socket s = null;
        private DataInputStream in = null;
        private DataOutputStream out = null;
        private String userName;
        private boolean isLogin = false;
        StringBuilder msg = new StringBuilder();

        public RecvThread(String userName) {
            this.userName = userName;
        }

        
        public void login() {
            try {
                s = new Socket("127.0.0.1", 8888); 
                in = new DataInputStream(s.getInputStream());
                out = new DataOutputStream(s.getOutputStream());
                String sendMsg = "LOGIN#" + userName;
                out.writeUTF(sendMsg);
                out.flush();
                
                String recv = in.readUTF();
                if (recv.equals("FAIL")) {
                    showMsg(userName + "已經登陸！！！");
                    user1.setEnabled(true);
                    exit();
                    return;
                } else if (recv.equals("SUCCESS")) {
                    showMsg("登陸成功！！！");
                    user1.setEnabled(false);
                    isLogin = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void exit() {
            try {
                if (isLogin) {
                    out.writeUTF("LOGOUT");
                    out.flush();
                    isLogin = false;
                }
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
                if (s != null)
                    s.close();

            } catch (IOException e) {
                System.out.println("連接已關閉!!!");
            }
        }

        
        public void sendMsg() {
            
            if (!isLogin) {
                showMsg("請登錄！！！");
                return;
            }
            msg.setLength(0);
            String sendInfo = sendArea.getText().trim();
            String user = (String) user2.getSelectedItem();
            if(sendInfo.equals(""))
                sendInfo=" ";
            try {

                if (user.equals("所有用戶")) {
                    msg.append("SENDALL#");

                        msg.append(sendInfo);


                } else {
                    msg.append("SENDONE#");
                        msg.append(user+"#" + sendInfo);

                }
                out.writeUTF(msg.toString());
                showMsg("我：" + sendInfo);

                sendArea.setText("");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            login();
            try {
                while (isLogin) {
                    String msgs[] = in.readUTF().split("#");
                    switch (msgs[0]) {
                        case "LOGIN":
                            user2.addItem(msgs[1]);
                            break;
                        case "ALLUSERS":
                            for (int i = 1; i < msgs.length; i++) {
                                if (!"".equals(msgs[i]))
                                    user2.addItem(msgs[i]);
                            }
                            break;
                        case "SENDONE":

                                showMsg(msgs[1] + ":" + msgs[2]);
                            break;
                        case "SENDALL":

                                showMsg(msgs[1] + "對所有人說：" + msgs[2]);

                            break;
                        case "LOGOUT":
                            showMsg("用戶" + msgs[1] + "已下線！！！");
                            user2.removeItem(msgs[1]);
                            break;
                    }
                }
            } catch (SocketException e) {
                System.out.println(userName + "已退出...");
            } catch (IOException e) {
                isLogin = false;
                e.printStackTrace();
            }
        }
    }

    //顯示訊息
    public void showMsg(String msg) {
        contentArea.append(msg + "\n");
        contentArea.setCaretPosition(contentArea.getText().length());
    }


    public static void main(String args[]) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        new ChatClient().start();
    }
}

// 1.Server端啟動
// 2.Client端啟動 啟動一次為選擇一個身份
// 3.分別為Client1 、Client2 、Client3、Client4
// 啟動時 背景運作Server端之下，透過Client端進行聊天
// 該有的功能都有實現，也有”參考“老師的檔案
// 想法是如果Server自己也在背景跳出GUI怪怪的，因此做出Server在背景監聽
// 透過多個Client 連上本地端 達成可多人也可以雙人的聊天室．
// by 411077033 鍾弘浩