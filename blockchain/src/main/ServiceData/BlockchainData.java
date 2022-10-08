package main.ServiceData;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import main.Models.Block;
import main.Models.Transaction;
import main.Models.Wallet;
import sun.security.provider.DSAPublicKeyImpl;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

public class BlockchainData {

    //For display only
    private ObservableList<Transaction> newBlockTransactionsFX;
    //Represents current ledger of blockchain
    private ObservableList<Transaction> newBlockTransactions;

    private LinkedList<Block> currentBlockchain = new LinkedList<>();
    private Block latestBlock;
    //Helps for front end
    private boolean exit = false;
    private int miningPoints;
    private static final int TIMEOUT_INTERVAL = 65;
    private static final int MINING_INTERVAL = 60;

    //helper class
    private Signature signing = Signature.getInstance("SHA256withDSA");

    //Singleton class
    private static BlockchainData instance;

    static {
        try{
            instance = new BlockchainData();
        }catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }
    }

    public BlockchainData() throws NoSuchAlgorithmException{
        newBlockTransactions = FXCollections.observableArrayList();
        newBlockTransactionsFX = FXCollections.observableArrayList();
    }

    //Returns the singleton class instead of instantiating
    public static BlockchainData getInstance(){
        return instance;
    }

    //Creates comparator that will compare and sort by timestamps
    Comparator<Transaction> transactionComparator = Comparator.comparing(Transaction::getTimeStamp);

    public ObservableList<Transaction> getTransactionLedgerFX(){
        newBlockTransactionsFX.clear();
        newBlockTransactions.sort(transactionComparator);
        newBlockTransactionsFX.addAll(newBlockTransactions);
        return FXCollections.observableArrayList(newBlockTransactionsFX);
    }

    public String getWalletBalanceFX(){
        return getBalance(currentBlockchain, newBlockTransactions, WalletData.getInstance().getWallet().getPublicKey()).toString();
    }

    //Goes through a given blockchain and a current ledger with potential transactions for the new block and finds the balance for the given public address
    private Integer getBalance(LinkedList<Block> blockchain, ObservableList<Transaction> currentLedger, PublicKey walletAddress){
        Integer balance = 0;

        //Goes through blockchain and for each transaction we check whether the given address is sending or receiving funds
        for(Block block: blockchain){
            for(Transaction transaction : block.getTransactionLedger()){

                if(Arrays.equals(transaction.getFrom(), walletAddress.getEncoded())){
                    balance -= transaction.getValue();
                }
                if(Arrays.equals(transaction.getTo(), walletAddress.getEncoded())){
                    balance += transaction.getValue();
                }
            }
        }
        //Prevents double spending by subtracting any funds we are already trying to send
        //Double spending is when the sender tries to send his total funds multiple times
        //We don't check incoming funds here since we wait until they are put onto the blockchain to prevent invalid transactions
        for(Transaction transaction: currentLedger){
            if (Arrays.equals(transaction.getFrom(), walletAddress.getEncoded())) {
                balance -= transaction.getValue();
            }
        }
        return balance;
    }

    //Uses the isVerified methods found in Block and Transaction here and throws a GeneralSecurityException if they aren't verified
    private void verifyBlockchain(LinkedList<Block> currentBlockchain) throws GeneralSecurityException{
        for(Block block : currentBlockchain){
            if(!block.isVerified(signing)){
                throw new GeneralSecurityException("Block validation failed");
            }
            ArrayList<Transaction> transactions = block.getTransactionLedger();

            for(Transaction transaction : transactions){
                if (!transaction.isVerified(signing)) {
                    throw new GeneralSecurityException("Transaction validation failed");
                }
            }
        }
    }

    public void addTransactionState(Transaction transaction){
        newBlockTransactions.add(transaction);
        newBlockTransactions.sort(transactionComparator);
    }

    //Adds new transaction to the ledger and checks if it's a regular transaction, or is a miner reward
    public void addTransaction(Transaction transaction, boolean blockReward) throws GeneralSecurityException{

        try {
            //Checks if the sender's balance is less than what they are sending and stops the transaction
            //This is not the case if it is a reward though
            if (getBalance(currentBlockchain, newBlockTransactions, new DSAPublicKeyImpl(transaction.getFrom())) < transaction.getValue() && !blockReward) {
                throw new GeneralSecurityException("Not enough funds by sender to record transaction");
            } else {
                Connection connection = DriverManager.getConnection("jdbc:sqlite:/home/polyphery/Desktop/Fall2022/Honours/post-quantum-blockchain/blockchain/db/blockchain.db");

                PreparedStatement preparedStatement;
                preparedStatement = connection.prepareStatement("INSERT INTO TRANSACTIONS" +
                        "(\"FROM\", \"TO\", LEDGER_ID, VALUE, SIGNATURE, CREATED_ON) +" +
                        " VALUES (?,?,?,?,?,?) ");
                preparedStatement.setBytes(1, transaction.getFrom());
                preparedStatement.setBytes(2, transaction.getTo());
                preparedStatement.setInt(3, transaction.getLedgerId());
                preparedStatement.setInt(4, transaction.getLedgerId());
                preparedStatement.setBytes(5, transaction.getSignature());
                preparedStatement.setString(6, transaction.getTimeStamp());
                preparedStatement.executeUpdate();

                preparedStatement.close();
                connection.close();
            }
        }catch(SQLException e){
            System.out.println("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }

    }

    //Fetches a list of the transaction from the database
    private ArrayList<Transaction> loadTransactionLedger(Integer ledgerId) throws SQLException{

        ArrayList<Transaction> transactions = new ArrayList<>();


        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:/home/polyphery/Desktop/Fall2022/Honours/post-quantum-blockchain/blockchain/db/blockchain.db");
            PreparedStatement preparedStatement = connection.prepareStatement(" SELECT * FROM TRANSACTIONS WHERE LEDGER_ID = ?");
            preparedStatement.setInt(1, ledgerId);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                transactions.add(new Transaction(
                        resultSet.getBytes("FROM"),
                        resultSet.getBytes("TO"),
                        resultSet.getInt("VALUE"),
                        resultSet.getBytes("SIGNATURE"),
                        resultSet.getInt("LEDGER_ID"),
                        resultSet.getString("CREATED_ON")));
            }
            resultSet.close();
            preparedStatement.close();
            connection.close();
        }catch(SQLException e){
            e.printStackTrace();
        }
        return transactions;
    }

    public void loadBlockchain(){

        //Loads and adds all from the blockchain db
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:/home/polyphery/Desktop/Fall2022/Honours/post-quantum-blockchain/blockchain/db/blockchain.db");
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(" SELECT * FROM BLOCKCHAIN ");

            while (resultSet.next()) {
                this.currentBlockchain.add(new Block(
                        resultSet.getBytes("PREVIOUS_HASH"),
                        resultSet.getBytes("CURRENT_HASH"),
                        resultSet.getString("CREATED_ON"),
                        resultSet.getBytes("CREATED_BY"),
                        resultSet.getInt("LEDGER_ID"),
                        resultSet.getInt("MINING_POINTS"),
                        resultSet.getDouble("LUCK"),
                        loadTransactionLedger(resultSet.getInt("LEDGER_ID"))
                ));
            }

            //Gets current block from blockchain which will have been created earlier in this function
            latestBlock = currentBlockchain.getLast();
            //Creates new reward transaction for our future block
            Transaction transaction = new Transaction(new Wallet(), WalletData.getInstance().getWallet().getPublicKey().getEncoded(),
                    100, latestBlock.getLedgerId() + 1, signing);

            newBlockTransactions.clear();
            newBlockTransactions.add(transaction);

            //Checks validity of blockchain
            //Import step as it covers cases when we import someone else's database
            verifyBlockchain(currentBlockchain);
            resultSet.close();
            statement.close();
            connection.close();

        }catch(SQLException | NoSuchAlgorithmException e){
            System.out.println("Problem with DB: " + e.getMessage());
        }catch(GeneralSecurityException e){
            e.printStackTrace();
        }

    }

    public void mineBlock(){

        try{
            //Performs necessary steps to finish up latest block
            finalizeBlock(WalletData.getInstance().getWallet());
            //Adds block to currentBlockchain list
            addBlock(latestBlock);
        }catch(SQLException | GeneralSecurityException e){
            System.out.println("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }

    }

    //************ DON'T FULLY UNDERSTAND ********************
    private void finalizeBlock(Wallet minersWallet) throws GeneralSecurityException, SQLException{

        //Create new block and pass it to latestBlock
        latestBlock = new Block(BlockchainData.getInstance().currentBlockchain);
        //add newBlockTransactions to ledger of latestBlock
        latestBlock.setTransactionLedger(new ArrayList<>(newBlockTransactions));
        latestBlock.setTimeStamp(LocalDateTime.now().toString());
        //Set own wallet address as we are trying to mine this block as minedBy
        latestBlock.setMinedBy(minersWallet.getPublicKey().getEncoded());
        latestBlock.setMiningPoints(miningPoints);

        signing.initSign(minersWallet.getPrivateKey());
        signing.update(latestBlock.toString().getBytes());

        //set hash as last property as it relies on all other information of the blocks
        latestBlock.setCurrHash(signing.sign());
        currentBlockchain.add(latestBlock);
        miningPoints = 0;

        //Reward transaction
        latestBlock.getTransactionLedger().sort(transactionComparator);
        addTransaction(latestBlock.getTransactionLedger().get(0), true);

        Transaction transaction = new Transaction(new Wallet(), minersWallet.getPublicKey().getEncoded(), 100, latestBlock.getLedgerId() + 1, signing);
        newBlockTransactions.clear();
        newBlockTransactions.add(transaction);

    }

    private void addBlock(Block block){
        try{
            Connection connection = DriverManager.getConnection("jdbc:sqlite:/home/polyphery/Desktop/Fall2022/Honours/post-quantum-blockchain/blockchain/db/blockchain.db");
            PreparedStatement preparedStatement;
            preparedStatement = connection.prepareStatement("INSERT INTO BLOCK(PREVIOUS_HASH, CURRENT_HASH, LEDGER_ID, CREATED_ON, " +
                    " CREATED_BY, MINING_POINTS, LUCK) VALUES(?,?,?,?,?,?,?) ");
            preparedStatement.setBytes(1, block.getPrevHash());
            preparedStatement.setBytes(2, block.getCurrHash());
            preparedStatement.setInt(3, block.getLedgerId());
            preparedStatement.setString(4, block.getTimeStamp());
            preparedStatement.setBytes(5, block.getMinedBy());
            preparedStatement.setInt(6, block.getMiningPoints());
            preparedStatement.setDouble(7, block.getLuck());
            preparedStatement.executeUpdate();
            preparedStatement.close();
            connection.close();

        }catch(SQLException e){
            System.out.println("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //************ TRY MAKING MORE EFFICIENT BY CHANGING THE CALLS TO addBlock AS THEY OPEN AND CLOSE A CONNECTION EACH TIME ****************
    private void replaceBlockchainInDatabase(LinkedList<Block> receivedBlockchain){

        try{
            Connection connection = DriverManager.getConnection("jdbc:sqlite:/home/polyphery/Desktop/Fall2022/Honours/post-quantum-blockchain/blockchain/db/blockchain.db");
            Statement clearDBStatement = connection.createStatement();
            //Clears data from both tables
            clearDBStatement.executeUpdate("DELETE FROM BLOCKCHAIN ");
            clearDBStatement.executeUpdate("DELETE FROM TRANSACTIONS");
            clearDBStatement.close();

            for(Block block : receivedBlockchain){
                addBlock(block);
                boolean rewardTransaction = true;
                block.getTransactionLedger().sort(transactionComparator);

                for(Transaction transaction : block.getTransactionLedger()){
                    addTransaction(transaction, rewardTransaction);
                    rewardTransaction = false;
                }
            }
        }catch(SQLException | GeneralSecurityException e){
            System.out.println("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }

    }

    //************* RELEARN ****************
    //Assume all peers are bad actors and make sure the blockchain is correct
    public LinkedList<Block> getBlockchainConsensus(LinkedList<Block> receivedBlockchain){
        //The blockchain will be thrown out if an exception occurs
        try{
            //Verify validity of the received blockchain
            verifyBlockchain(receivedBlockchain);

            //Check if we have received an identical blockchain
            if(!Arrays.equals(receivedBlockchain.getLast().getCurrHash(), getCurrentBlockchain().getLast().getCurrHash())){
                if(checkIfOutdated(receivedBlockchain) != null){
                    return getCurrentBlockchain();
                } else {
                    if(checkWhichIsCreatedFirst(receivedBlockchain)!= null){
                        return getCurrentBlockchain();
                    }else{
                        if(compareMiningPointsAndLuck(receivedBlockchain) != null){
                            return getCurrentBlockchain();
                        }
                    }
                }
            //if only the transaction ledgers are different, then combine them
            }else if(!receivedBlockchain.getLast().getTransactionLedger().equals(getCurrentBlockchain().getLast().getTransactionLedger())){
                updateTransactionLedgers(receivedBlockchain);
                System.out.println("Transaction ledgers are updated");
            }else{
                System.out.println("Blockchains are identical");
            }
        }catch(GeneralSecurityException e){
            e.printStackTrace();
        }
        return receivedBlockchain;
    }

    private LinkedList<Block> checkIfOutdated(LinkedList<Block> receivedBlockchain){
        //Check how old the blockchains are
        long lastMinedLocalBlock = LocalDateTime.parse(getCurrentBlockchain().getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
        long lastMinedReceivedBlock = LocalDateTime.parse(receivedBlockchain.getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);

        //if both are old just do nothing
        if ((lastMinedLocalBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) &&
                (lastMinedReceivedBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) {
            System.out.println("Both are old, check other peers");

        //If your blockchain is old but the received on is new, use the received one
        }else if((lastMinedLocalBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) &&
                (lastMinedReceivedBlock + TIMEOUT_INTERVAL) >= LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)){
            //we reset the mining points since we weren't contributing until now
            setMiningPoints(0);
            replaceBlockchainInDatabase(receivedBlockchain);
            setCurrentBlockchain(new LinkedList<>());
            loadBlockchain();
            System.out.println("Received blockchain won! Local blockchain was old");

        //If received one is old but local is new send out to them
        }else if((lastMinedLocalBlock + TIMEOUT_INTERVAL) > LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) &&
                (lastMinedReceivedBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)){
            return getCurrentBlockchain();
        }
        return null;
    }

    private LinkedList<Block> checkWhichIsCreatedFirst(LinkedList<Block> receivedBlockchain){
        //Compare timestamps to see which one is created first
        long initReceivedBlockTime = LocalDateTime.parse(receivedBlockchain.getFirst().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
        long initLocalBlockTime = LocalDateTime.parse(getCurrentBlockchain().getFirst().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);

        if(initReceivedBlockTime < initLocalBlockTime){
            //we reset mining points since we weren't contributing until now
            setMiningPoints(0);
            replaceBlockchainInDatabase(receivedBlockchain);
            setCurrentBlockchain(new LinkedList<>());
            loadBlockchain();
            System.out.println("PeerClient blockchain won! PeerServer's blockchain was old.");
        }else if(initReceivedBlockTime > initLocalBlockTime){
            return getCurrentBlockchain();
        }
        return null;
    }

    private LinkedList<Block> compareMiningPointsAndLuck(LinkedList<Block> receivedBlockchain) throws GeneralSecurityException{
        //Check if both blockchains have the same prevHashes to confirm they are both
        //contending to mine the last block
        //If they are the same, compare the mining points and luck in case of equal mining points
        //of last block to see who wins
        if(receivedBlockchain.equals(getCurrentBlockchain())){
            //If received block has more mining points or luck in case of tie
            //transfer all transaction to the winning block and add them in db
            if(receivedBlockchain.getLast().getMiningPoints() > getCurrentBlockchain().getLast().getMiningPoints() ||
                    receivedBlockchain.getLast().getMiningPoints().equals(getCurrentBlockchain().getLast().getMiningPoints()) &&
                    receivedBlockchain.getLast().getLuck() > getCurrentBlockchain().getLast().getLuck()){
                //remove the reward transaction from our losing

                //transfer the transactions to the winning block
                getCurrentBlockchain().getLast().getTransactionLedger().remove(0);

                for(Transaction transaction : getCurrentBlockchain().getLast().getTransactionLedger()){

                    if(!receivedBlockchain.getLast().getTransactionLedger().contains(transaction)){
                        receivedBlockchain.getLast().getTransactionLedger().add(transaction);
                    }
                }

                receivedBlockchain.getLast().getTransactionLedger().sort(transactionComparator);
                //We are returning the mining points since our local block host.
                setMiningPoints(BlockchainData.getInstance().getMiningPoints() + getCurrentBlockchain().getLast().getMiningPoints());
                replaceBlockchainInDatabase(receivedBlockchain);
                setCurrentBlockchain(new LinkedList<>());
                loadBlockchain();;
                System.out.println("Received blockchain won!");
            }else{
                //remove the reward transaction from losing block and transfer
                //the transactions to our winning block
                receivedBlockchain.getLast().getTransactionLedger().remove(0);
                for(Transaction transaction : receivedBlockchain.getLast().getTransactionLedger()){
                    if(!getCurrentBlockchain().getLast().getTransactionLedger().contains(transaction)){
                        getCurrentBlockchain().getLast().getTransactionLedger().add(transaction);
                        addTransaction(transaction, false);
                    }
                }
                getCurrentBlockchain().getLast().getTransactionLedger().sort(transactionComparator);
                return getCurrentBlockchain();
            }
        }
        return null;
    }

    private void updateTransactionLedgers(LinkedList<Block> receivedBlockchain) throws GeneralSecurityException{
        for(Transaction transaction : receivedBlockchain.getLast().getTransactionLedger()){
            if(!getCurrentBlockchain().getLast().getTransactionLedger().contains(transaction)){
                getCurrentBlockchain().getLast().getTransactionLedger().add(transaction);
                System.out.println("Current ledger id = "+ currentBlockchain.getLast().getLedgerId() +
                        "transaction id = " + transaction.getLedgerId());
                addTransaction(transaction, false);
            }
        }
        getCurrentBlockchain().getLast().getTransactionLedger().sort(transactionComparator);
        for(Transaction transaction : getCurrentBlockchain().getLast().getTransactionLedger()){
            if(!receivedBlockchain.getLast().getTransactionLedger().contains(transaction)){
                receivedBlockchain.getLast().getTransactionLedger().add(transaction);
            }
        }
    }

    public LinkedList<Block> getCurrentBlockchain() {
        return currentBlockchain;
    }

    public void setCurrentBlockchain(LinkedList<Block> currentBlockchain){
        this.currentBlockchain = currentBlockchain;
    }

    public static int getTimeoutInterval(){
        return TIMEOUT_INTERVAL;
    }

    public static int getMiningInterval(){
        return MINING_INTERVAL;
    }

    public int getMiningPoints() {
        return miningPoints;
    }

    public void setMiningPoints(int miningPoints) {
        this.miningPoints = miningPoints;
    }

    public boolean isExit() {
        return exit;
    }

    public void setExit(boolean exit) {
        this.exit = exit;
    }


}
