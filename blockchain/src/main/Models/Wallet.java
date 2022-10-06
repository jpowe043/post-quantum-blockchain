package main.Models;

import java.io.Serializable;
import java.security.*;

public class Wallet implements Serializable{

    //Contains private and public keys
    private KeyPair keyPair;

    //Constructor for generating new KeyPair
    public Wallet() throws NoSuchAlgorithmException{
        //Calls second constructor which actually generates the keypair
        this(2048, KeyPairGenerator.getInstance("DSA"));
    }

    public Wallet(Integer keySize, KeyPairGenerator keyPairGen){
        keyPairGen.initialize(keySize);
        this.keyPair = keyPairGen.generateKeyPair();
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public PublicKey getPublicKey(){
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey(){
        return keyPair.getPrivate();
    }
}
