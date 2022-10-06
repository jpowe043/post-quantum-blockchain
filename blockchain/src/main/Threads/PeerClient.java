package main.Threads;

import main.Models.Block;
import main.ServiceData.BlockchainData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PeerClient extends Thread{

    private Queue<Integer> queue = new ConcurrentLinkedQueue<>();

    //Port numbers of peers we want to connect to.
    public PeerClient(){
        this.queue.add(6001);
        this.queue.add(6002);
    }

    @Override
    public void run(){
        while(true){
            //Instantiate socket with local ip and call first port number
            //********** CHANGE FOR PRESENTATION **************
            try (Socket socket = new Socket("192.168.0.21",queue.peek())) {
                System.out.println("Sending blockchain object on port: " + queue.peek());

                //Move instantiated port number to bottom of queue
                queue.add(queue.poll());
                //If no response is heard from peer for 5 seconds (peer-side delay is 2 seconds) means they disconnected and the socket will time out
                //and then be added to the bottom of the queue to be checked again later.
                socket.setSoTimeout(5000);

                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());

                //retrieves blockchain
                LinkedList<Block> blockchain = BlockchainData.getInstance().getCurrentBlockchain();
                //sends blockchain to peer to be checked and compared by peer with their local blockchain
                objectOutputStream.writeObject(blockchain);

                //Peer will send back "winning" blockchain according to their consensus method
                LinkedList<Block> returnedBlockchain = (LinkedList<Block>) objectInputStream.readObject();
                System.out.println(" RETURNED BC LedgerID = " + returnedBlockchain.getLast().getLedgerId() +
                        "Size = " + returnedBlockchain.getLast().getTransactionLedger().size());
                //We can't fully trust peer (in case of tampering) so we conduct our own consensus check on the received data.
                //Constantly spreading and checking the blockchain to peers, while stopping false data, will ensure the blockchain is correct and tamper-free
                BlockchainData.getInstance().getBlockchainConsensus(returnedBlockchain);
                //Ensures a 2-second breaks between sending and receiving data to save computer power
                Thread.sleep(2000);

            }catch(SocketTimeoutException e){
                //Socket times out after 5 seconds and is added to bottom of peer so that we don't get stuck on a single peer
                System.out.println("The socket timed out!");
                queue.add(queue.poll());
            }catch(IOException e){
                //Error connecting
                System.out.println("Client Error: " + e.getMessage() + " -- Error on port: " + queue.peek());
                queue.add(queue.poll());
            }catch(InterruptedException | ClassNotFoundException e){
                e.printStackTrace();
                queue.add(queue.poll());
            }
        }
    }

}
