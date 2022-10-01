package main;

import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
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

//    private ArrayList<Transaction> transactionLedger = new ArrayList<Transaction>();

}

