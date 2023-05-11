package Server;

import java.io.*;
import java.net.*;
import java.util.*;

/*
server端
 */


public class ChatServer {
    private ServerSocket ss;
    private boolean flag = false;
    private Map<String, Client> clients = new HashMap<>();

    public static void main(String args[]) {
        new ChatServer().start();
    }

    public void start() {
        try {
            ss = new ServerSocket(8888);
            flag = true;
            System.out.println("Server啟動...");
        } catch (BindException e) {
            System.out.println("使用中!!!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            
            while (flag) {
                
                Socket socket = ss.accept();
                Client c = new Client(socket);
                System.out.println("一個Client 已連接...");
                new Thread(c).start();
            }
        } catch (IOException e) {
            System.out.println("Client 已關閉...");
        } finally {
            try {
                if (ss != null)
                    ss.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    private class Client implements Runnable {
        private Socket s;
        private String userName = "";
        private DataInputStream input = null;
        private DataOutputStream output = null;
        private boolean connected = false;
        private BufferedOutputStream fout = null;
        public Client(Socket s) {
            this.s = s;
            try {
                input = new DataInputStream(s.getInputStream());
                output = new DataOutputStream(s.getOutputStream());
                connected = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        @Override
        public void run() {
            try {
                while (connected) {
                    String msg[] = input.readUTF().split("#");
                    switch (msg[0]) {
                        case "LOGIN":
                            String userName = msg[1];
                            if (clients.containsKey(userName)) {
                                output.writeUTF("FAIL");
                                System.out.println("拒絕一個重複連接...");
                                closeConnect();
                            } else {
                                output.writeUTF("SUCCESS");
                                clients.put(userName, this);
                               
                                StringBuffer allUsers = new StringBuffer();
                                allUsers.append("ALLUSERS#");
                                for (String user : clients.keySet())
                                    allUsers.append(user + "#");
                                output.writeUTF(allUsers.toString());
                               
                                String newLogin = "LOGIN#" + userName;
                                sendMsg(userName, newLogin);
                                this.userName = userName;
                            }
                            break;
                        case "LOGOUT":
                            clients.remove(this.userName);
                            String logoutMsg = "LOGOUT#" + this.userName;
                            sendMsg(this.userName, logoutMsg);
                            System.out.println("用户" + this.userName + "已下線...");
                            closeConnect();
                            break;
                        case "SENDONE":
                            Client c = clients.get(msg[1]);
                            String msgToOne="";
                            if (c != null) {
                                msgToOne="SENDONE#" + this.userName + "#" + msg[2];
                                c.output.writeUTF(msgToOne);
                                c.output.flush();
                            }
                            break;
                        case "SENDALL":
                            String msgToAll = "";
                            msgToAll = "SENDALL#" + this.userName + "#" + msg[1];
                            sendMsg(this.userName, msgToAll);
                            break;

                    }

                }
            } catch (IOException e) {
                System.out.println("Client closed...");
                connected = false;
            } finally {

                try {
                    if (input != null)
                        input.close();
                    if (output != null)
                        output.close();
                    if (fout != null)
                        fout.close();
                    if (s != null)
                        s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        public void closeConnect() {
            connected = false;
            try {
                if (input != null)
                    input.close();
                if (output != null)
                    output.close();
                if (s != null)
                    s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendMsg(String fromUser, String msg) {
            String tempUser = "";
            try {
                for (String toUser : clients.keySet()) {
                    if (!toUser.equals(fromUser)) {
                        tempUser = toUser;
                        DataOutputStream out = clients.get(toUser).output;
                        out.writeUTF(msg);
                        out.flush();
                    }
                }
            } catch (IOException e) {
                System.out.println("用户" + tempUser + "已經離線！！！");
            }
        }
    }
}


