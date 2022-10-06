package main.Models;

import sun.security.provider.DSAPublicKeyImpl;

import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

//Serializable so that we can share our blockchain through our network
public class Block implements Serializable{

    private byte[] prevHash;
    private byte[] currHash;
    private String timeStamp;

    //will contain the public key which doubles as the public address of the miner that managed to mine this block
    private byte[] minedBy;

    //A database is implemented with separate Block and Transaction tables
    //This id will help retrieve 
    private Integer ledgerId = 1;
    private Integer miningPoints = 0;
    private Double luck = 0.0;

    private ArrayList<Transaction> transactionLedger = new ArrayList<Transaction>();

    //This constructor is used when retieving a block from the database
    public Block(byte[] prevHash, byte[] currHash, String timeStamp, byte[] minedBy, Integer ledgerId,
        Integer miningPoints, Double luck, ArrayList<Transaction> transactionLedger){

            this.prevHash = prevHash;
            this.currHash = currHash;
            this.timeStamp = timeStamp;
            this.minedBy = minedBy;
            this.ledgerId = ledgerId;
            this.transactionLedger = transactionLedger;
            this.miningPoints = miningPoints;
            this.luck = luck;

    }

    //This constructor is used to initiate a block after retrieval (while application is running)
    public Block(LinkedList<Block> currentBlockChain){

        Block lastBlock = currentBlockChain.getLast();
        prevHash = lastBlock.getCurrHash();
        ledgerId = lastBlock.getLedgerId() + 1;
        luck = Math.random() * 1000000;

    }

    //For the first block in the blockchain (head of blockchain)
    public Block(){
        prevHash = new byte[]{0};
    }

    //Signature lets us encrypt/decrypt using various algorithms
    public boolean isVerified(Signature signing) throws InvalidKeyException, SignatureException{

        //Uses the mined public key to use to verify the data against the signature stored in currHash
        signing.initVerify(new DSAPublicKeyImpl(this.minedBy));
        signing.update(this.toString().getBytes());
        //Boolean whether verification was successful or not
        return signing.verify(this.currHash);

    }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if(!(o instanceof Block)) return false;
        Block block = (Block) o;
        return Arrays.equals(getPrevHash(), block.getPrevHash());
    }

    @Override
    public int hashCode(){
        return Arrays.hashCode(getPrevHash());
    }

    public byte[] getPrevHash(){
        return prevHash;
    }

    public byte[] getCurrHash(){
        return currHash;
    }

    public void setPrevHash(byte[] prevHash){ this.prevHash = prevHash; }

    public void setCurrHash(byte[] currHash) {
        this.currHash = currHash;
    }

    public byte[] getMinedBy(){
        return minedBy;
    }

    public Integer getMiningPoints() {
        return miningPoints;
    }

    public void setMiningPoints(Integer miningPoints) {
        this.miningPoints = miningPoints;
    }

    public Integer getLedgerId(){
        return ledgerId;
    }

    public void setLedgerId(Integer ledgerId) {
        this.ledgerId = ledgerId;
    }

    public Double getLuck() {
        return luck;
    }

    public String getTimeStamp(){
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setLuck(Double luck) {
        this.luck = luck;
    }

    @Override
    public String toString(){

        return "Block{" + "prevHash = " + Arrays.toString(prevHash) + ", timeStamp = " + timeStamp + "\'" +
                ", minedBy = " + Arrays.toString(minedBy) + ", ledgerId = " + ledgerId + ", miningPoints = " + miningPoints +
                ", luck = " + luck + "}";



    }

}

