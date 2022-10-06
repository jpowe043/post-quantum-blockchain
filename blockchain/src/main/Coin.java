package main;

import main.Models.*;

import javafx.application.Application;
import javafx.stage.Stage;
import main.Threads.MiningThread;
import main.Threads.PeerClient;
import main.Threads.PeerServer;
import main.Threads.UI;

import javax.xml.transform.Result;
import java.security.*;
import java.sql.*;
import java.time.LocalDateTime;

public class Coin extends Application{
    public static void main(String[] args) throws Exception {
//        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        //Displays the UI
        new UI().start(primaryStage);
        //Starts the client
        new PeerClient().start();
        //Starts server on port 6000
        new PeerServer(6000).start();
        //Blockchain verification and consensus ****
        new MiningThread().start();
    }

    @Override
    public void init(){
        try{
            //Creates your wallet if there isn't one already and generates a KeyPair  ****

            //We will create it in a separate db for better security and ease of portability
            Connection walletConnection = DriverManager.getConnection("jdbc:sqlite:/home/polyphery/Desktop/Fall2022/Honours/post-quantum-blockchain/blockchain/db/wallet.db");

            Statement walletStatement = walletConnection.createStatement();

            //Creates a wallet db if it doesn't exist, or can't find it
            walletStatement.executeUpdate("CREATE TABLE IF NOT EXISTS WALLET ( " +
                                                "PRIVATE_KEY BLOB NOT NULL UNIQUE, " +
                                                "PUBLIC_KEY BLOB NOT NULL UNIQUE, " +
                                                "PRIMARY KEY (PRIVATE_KEY, PUBLIC_KEY)");

            ResultSet resultSet = walletStatement.executeQuery(" SELECT * FROM WALLET");

            //Checks if the ruleSet is at the next (currently empty) position, so it can create a new wallet entry
            if(!resultSet.next()){
                Wallet newWallet = new Wallet();
                byte[] pubBlob = newWallet.getPublicKey().getEncoded();
                byte[] prvBlob = newWallet.getPrivateKey().getEncoded();

                //Inserts the new data to the wallet db
                PreparedStatement preparedStatement = walletConnection.prepareStatement("INSERT INTO WALLET(PRIVATE_KEY, PUBLIC_KEY) VALUES(?,?)");
                preparedStatement.setBytes(1,prvBlob);
                preparedStatement.setBytes(2,pubBlob);
                preparedStatement.executeUpdate();
            }

            resultSet.close();
            walletStatement.close();
            walletConnection.close();
            WalletData.getInstance().loadWallet();

            Connection blockchainConnection = DriverManager.getConnection("jdbc:sqlite:/home/polyphery/Desktop/Fall2022/Honours/post-quantum-blockchain/blockchain/db/blockchain.db");
            Statement blockchainStatement = blockchainConnection.createStatement();

            //Creates a blockchain db if it doesn't exist, or can't find it
            blockchainStatement.executeUpdate("CREATE TABLE IF NOT EXISTS BLOCKCHAIN ( " +
                                                    " ID INTEGER NOT NULL UNIQUE, " +
                                                    " PREVIOUS_HASH BLOB UNIQUE, " +
                                                    " CURRENT_HASH BLOB UNIQUE, " +
                                                    " LEDGER_ID INTEGER NOT NULL UNIQUE, " +
                                                    " CREATED_ON TEXT, " +
                                                    " CREATED_BY BLOB, " +
                                                    " MINING_POINTS TEXT, " +
                                                    " LUCK NUMERIC, " +
                                                    " PRIMARY KEY(ID AUTOINCREMENT) )");

            ResultSet resultSetBlockchain = blockchainStatement.executeQuery("SELECT * FROM BLOCKCHAIN");
            Transaction initBlockRewardTransaction = null;

            //Check if blockchain entry exists and if not, then creates one
            if(!resultSetBlockchain.next()){
                Block firstBlock = new Block();
                firstBlock.setMinedBy(WalletData.getInstance().getWallet().getPublicKey().getEncoded());
                firstBlock.setTimeStamp(LocalDateTime.now().toString());

                //helper class
                Signature signing = Signature.getInstance("SHA256withDSA");
                signing.initSign(WalletData.getInstance().getWallet.getPrivate());
                signing.update(firstBlock.toString().getBytes());

                firstBlock.setCurrHash(signing.sign());

                //Sends new data to blockchain db to be saved
                PreparedStatement preparedStatement = blockchainConnection.prepareStatement("INSERT INTO BLOCKCHAIN(" +
                        "PREVIOUS_HASH, CURRENT_HASH, LEDGER_ID, CREATED_ON, CREATED_BY, MINING_POINTS, LUCK) " +
                        "VALUES(?,?,?,?,?,?,?)");
                preparedStatement.setBytes(1, firstBlock.getPrevHash());
                preparedStatement.setBytes(2, firstBlock.getCurrHash());
                preparedStatement.setInt(3, firstBlock.getLedgerId());
                preparedStatement.setString(4, firstBlock.getTimeStamp());
                preparedStatement.setBytes(5,WalletData.getInstance().getWallet().getPublicKey().getEncoded());
                preparedStatement.setInt(6,firstBlock.getMiningPoints());
                preparedStatement.setDouble(7,firstBlock.getLuck());
                preparedStatement.executeUpdate();

                Signature transactionSignature = Signature.getInstance("SHA256withDSA");
                initBlockRewardTransaction = new Transaction(WalletData.getInstance().getWallet, WalletData.getInstance().getWallet().getPublicKey().getEncoded(), 100, 1, transactionSignature);

            }

            resultSetBlockchain.close();

            //Checks if transaction table exists, if not creates it
            blockchainStatement.executeUpdate("CREATE TABLE IF NOT EXISTS TRANSACTIONS ( " +
                    " ID INTEGER NOT NULL UNIQUE, " +
                    " \"FROM\" BLOCK, " +
                    "\"TO\" BLOB, " +
                    "LEDGER_ID INTEGER, " +
                    "VALUE INTEGER, " +
                    "SIGNATURE BLOB UNIQUE, " +
                    "CREATED_ON TEXT, " +
                    "PRIMARY KEY(ID AUTOINCREMENT))");

            //Checks if we have initial reward transaction created
            if(initBlockRewardTransaction != null){
                //Sends reward transaction to database and updates application state
                BlockchainData.getInstance().addTransaction(initBlockRewardTransaction,true);
                BlockchainData.getInstance().addTransactionState(initBlockRewardTransaction);
            }
            //Closes connections to db
            blockchainStatement.close();
            blockchainConnection.close();

        } catch (SQLException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e){
            System.out.println("db failed: " + e.getMessage());
        } catch (GeneralSecurityException e){
            e.printStackTrace();
        }
        BlockchainData.getInstance().loadBlockchain();
    }


}
