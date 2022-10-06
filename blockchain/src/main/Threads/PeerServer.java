package main.Threads;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.

public class PeerServer extends Thread{

    private ServerSocket serverSocket;
    public PeerServer(Integer socketPort) throws IOException{
        serverSocket = new ServerSocket(socketPort);
    }

    @Override
    public void run(){
        while(true){
            try{
                new PeerRequestThread(serverSocket.accept()).start();

            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

}
