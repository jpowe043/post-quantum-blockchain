package main.Threads;

import main.Models.Block;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;

public class PeerRequestThread extends Thread{

    private Socket socket;

    public PeerRequestThread(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run(){
        try{
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());

            //Sets the received blockchain to a variable
            LinkedList<Block> receivedBlockchain = (LinkedList<Block>) objectInputStream.readObject();

            System.out.println("LedgerId = " + receivedBlockchain.getLast().getLedgerId() +
                    " Size = " + receivedBlockchain.getLast().getTransactionLedger().size());

            //Checks the blockchain data with the consensus alg
            objectOutputStream.writeObject(BlockchainData.getInstance().getBlockchainConsensus(receivedBlockchain));

        }catch(IOException | ClassNotFoundException e){
            e.printStackTrace();
        }
    }
}
