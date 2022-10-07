package main.ServiceData;

import main.Models.Wallet;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.*;

public class WalletData {

    private Wallet wallet;
    //singleton class
    //Ensures that it can only be instantiated once and only once
    private static WalletData instance;

    static{
        instance = new WalletData();
    }

    //This method is used instead of instantiating the class
    public static WalletData getInstance(){
        return instance;
    }

    //This will load your wallet from the database
    public void loadWallet() throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException{

        Connection walletConnection = DriverManager.getConnection("jdbc:sqlite:/home/polyphery/Desktop/Fall2022/Honours/post-quantum-blockchain/blockchain/db/wallet.db");
        Statement walletStatement = walletConnection.createStatement();

        ResultSet resultSet;
        resultSet = walletStatement.executeQuery("SELECT * FROM WALLET ");

        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        PublicKey publicKey = null;
        PrivateKey privateKey = null;
        while(resultSet.next()){
            publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(resultSet.getBytes("PUBLIC_KEY")));
            privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(resultSet.getBytes("PRIVATE_KEY")));
        }
        this.wallet = new Wallet(publicKey,privateKey);
    }

    public Wallet getWallet(){
        return wallet;
    }

}
