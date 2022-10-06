package main.Controllers;

import com.sun.javafx.image.impl.General;
import main.Models.Transaction;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.w3c.dom.Text;

import java.security.GeneralSecurityException;
import java.security.Signature;
import java.util.Base64;

public class AddNewTransactionController {

    @FXML
    private TextField toAddress;

    @FXML
    private TextField value;

    @FXML
    public void createNewTransaction() throws GeneralSecurityException{
        Base64.Decoder decoder = Base64.getDecoder();
        Signature signing = Signature.getInstance("SHA256withDSA");
        Integer ledgerId = BlockchainData.getInstance().getTransactionLedgerFx().get(0).getLedgerId();
        byte[] sendB = decoder.decode(toAddress.getText());
        Transaction transaction= new Transaction(WalletData.getInstance().getWallet(),sendB ,Integer.parseInt(value.getText()),ledgerId,signing);
        BlockchainData.getInstance().addTransaction(transaction,false);
        BlockchainData.getInstance().addTransactionState(transaction);
    }
}
