import Configuration.*;
import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.Exception.AlreadyAddedTransactionException;
import DeXTT.Exception.UnconfirmedTransactionExecutionException;
import DeXTT.Transaction.ClaimTransaction;
import DeXTT.Transaction.MintTransaction;
import DeXTT.Wallet;
import DeXTT.Cryptography;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.*;

public class WalletTests extends TransactionTestDataProvider {

    public WalletTests() {
        super();
    }

    @BeforeEach
    void setUp() throws AlreadyAddedTransactionException {
        super.createAndSaveClaimTransaction();
    }

    @Test
    public void mintTokensSuccessTest() {
        Wallet wallet = new Wallet(new EventBus(), "");
        BigInteger tokensToMint = BigInteger.valueOf(100);
        DeXTTAddress addressToMintTo = new DeXTTAddress("1111111111111111111111111111111111111111");
        MintTransaction mintTransaction = new MintTransaction(addressToMintTo, tokensToMint, Constants.MINTING_ADDRESS);

        wallet.executeMintTransaction(mintTransaction);

        assertThat(wallet.balanceOf(addressToMintTo)).isEqualTo(tokensToMint);
        assertThat(wallet.totalSupply()).isEqualTo(tokensToMint);
    }

    @Test
    public void mintTokensUnsuccessfulTest() {
        Wallet wallet = new Wallet(new EventBus(), "");
        BigInteger tokensToMint = BigInteger.valueOf(100);
        DeXTTAddress addressToMintTo = new DeXTTAddress("1111111111111111111111111111111111111111");
        MintTransaction mintTransaction = new MintTransaction(addressToMintTo, tokensToMint, addressToMintTo); // minting is NOT DONE by allowed address
        BigInteger zero = BigInteger.valueOf(0);

        wallet.executeMintTransaction(mintTransaction);

        assertThat(wallet.balanceOf(addressToMintTo)).isEqualTo(zero); // should have zero tokens still
        assertThat(wallet.totalSupply()).isEqualTo(zero);
    }

    @Test
    public void contestSuccessfulTest() throws AlreadyAddedTransactionException, UnconfirmedTransactionExecutionException {
        ClaimTransaction claimTransaction = super.claimTransaction;
        Wallet wallet = new Wallet(new EventBus(), "");
        wallet.executeMintTransaction(new MintTransaction(claimTransaction.getPoiFull().getPoiData().getSender(), BigInteger.valueOf(100), Constants.MINTING_ADDRESS));


        wallet.executeClaimContestTransaction(claimTransaction);

        BigInteger lockStatus = wallet.lockStatus(claimTransaction.getPoiFull().getPoiData().getSender());
        assertThat(lockStatus).isNotNull();
        assertThat(lockStatus).isEqualTo(Cryptography.calculateFullPoiHash(claimTransaction.getPoiFull().getPoiData()));
    }


}
