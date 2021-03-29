package chat.client;


import chat.Connection;
import chat.ConsoleHelper;
import chat.Message;
import chat.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client extends Thread{
    protected Connection connection;
    private volatile boolean clientConnected = false;

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    public class SocketThread extends Thread{
        protected void processIncomingMessage(String message){
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName){
            ConsoleHelper.writeMessage("Участник "+userName+" присоединился");
        }

        protected void informAboutDeletingNewUser(String username){
            ConsoleHelper.writeMessage("Участник "+username+" покинул чат");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected){
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this){
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException{
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.NAME_REQUEST) {
                    connection.send(new Message(MessageType.USER_NAME, getUserName()));
                }else if(message.getType() == MessageType.NAME_ACCEPTED){
                    notifyConnectionStatusChanged(true);
                    return;
                } else{
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException{
            while(true) {

                Message message = connection.receive();

                if(message.getType() == MessageType.TEXT){
                    processIncomingMessage(message.getData());
                }else if(message.getType() == MessageType.USER_ADDED){
                    informAboutAddingNewUser(message.getData());
                } else if(message.getType() == MessageType.USER_REMOVED){
                    informAboutDeletingNewUser(message.getData());
                } else{
                    throw new IOException("Unexpected MessageType");
                }
            }

        }

        @Override
        public void run() {
            try {
                connection = new Connection(new Socket(getServerAddress(), getServerPort()));
                clientHandshake();
                clientMainLoop();

            } catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }


        }
    }

    protected String getServerAddress(){
        ConsoleHelper.writeMessage("Введите адрес сервера");
        return ConsoleHelper.readString();
    }

    protected int getServerPort(){
        ConsoleHelper.writeMessage("Введите порт сервера");
        return ConsoleHelper.readInt();
    }

    protected String getUserName(){
        ConsoleHelper.writeMessage("Введите имя");
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole(){
        return true;
    }

    protected SocketThread getSocketThread(){
        return new SocketThread();
    }

    protected void sendTextMessage(String text){
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Ашыбка");
            this.clientConnected = false;
        }


    }

    @Override
    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        try{
            synchronized (this){
                wait();
            }
        } catch (InterruptedException e){
            ConsoleHelper.writeMessage("Ашыбка");
            return;
        }

        while(clientConnected){
            String text = ConsoleHelper.readString();
            if(text.equalsIgnoreCase("exit")){
                break;
            }
            if(shouldSendTextFromConsole()){
                sendTextMessage(text);
            }
        }

    }
}
