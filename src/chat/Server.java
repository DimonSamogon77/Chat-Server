package chat;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<String, Connection>();

    public static void main(String[] args) throws IOException {
        int port = ConsoleHelper.readInt();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            ConsoleHelper.writeMessage("Server started");
            while (true) {
                Socket socket = serverSocket.accept();
                new Handler(socket).start();
            }
        } catch (Exception e) {
            ConsoleHelper.writeMessage("Error");
        }
    }

    public static void sendBroadcastMessage(Message message){
        try{
            for(Map.Entry<String, Connection> map : connectionMap.entrySet()){
                map.getValue().send(message);
            }
        }catch (IOException e){
            ConsoleHelper.writeMessage("Сообщение не доставлено");
        }
    }


    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            String userName = null;
            try {
                ConsoleHelper.writeMessage("Connection established with "+ socket.getRemoteSocketAddress());
                Connection connection = new Connection(socket);
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                notifyUsers(connection, userName);
                serverMainLoop(connection, userName);

            }catch (IOException | ClassNotFoundException e){
                ConsoleHelper.writeMessage("Error");
            }
            if(userName != null){
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
            }



        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));

                Message message = connection.receive();
                if (message.getType() != MessageType.USER_NAME) {
                    ConsoleHelper.writeMessage("Получено сообщение от " + socket.getRemoteSocketAddress() + ". Тип сообщения не соответствует протоколу.");
                    continue;
                }

                String userName = message.getData();

                if (userName.isEmpty()) {
                    ConsoleHelper.writeMessage("Попытка подключения к серверу с пустым именем от " + socket.getRemoteSocketAddress());
                    continue;
                }

                if (connectionMap.containsKey(userName)) {
                    ConsoleHelper.writeMessage("Попытка подключения к серверу с уже используемым именем от " + socket.getRemoteSocketAddress());
                    continue;
                }
                connectionMap.put(userName, connection);

                connection.send(new Message(MessageType.NAME_ACCEPTED));
                return userName;
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException{
            for(Map.Entry<String, Connection> map : connectionMap.entrySet()){
                if(!map.getKey().equals(userName)){
                    connection.send(new Message(MessageType.USER_ADDED, map.getKey()));
                    ConsoleHelper.writeMessage("Пользователь с именем "+map.getKey()+" был добавлен");
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException{
            while(true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {
                    Message sendAll = new Message(MessageType.TEXT, userName+": "+message.getData());
                    sendBroadcastMessage(sendAll);
                } else{
                    ConsoleHelper.writeMessage("Error");
                    continue;
                }
            }
        }
    }
}

