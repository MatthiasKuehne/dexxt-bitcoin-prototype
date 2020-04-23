import DeXTT.Exception.AlreadyAddedTransactionException;
import DeXTT.Transaction.ClaimTransaction;
import DeXTT.Transaction.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class ClaimTransactionBuildTests extends TransactionTestDataProvider {

    public ClaimTransactionBuildTests() {
        super();
    }

    @BeforeEach
    void setUp() {
        super.createAndSaveClaimTransactionParts();
    }

    @Test
    public void buildClaimTransactionSuccessfulTest_Data_sigA_sigB() throws AlreadyAddedTransactionException {
        ClaimTransaction claimTransaction = new ClaimTransaction(super.claimDataTx);
        List<Transaction> matches = claimTransaction.tryToAddDeXTTBitcoinTransaction(super.sigATx);
        List<Transaction> matches2 = claimTransaction.tryToAddDeXTTBitcoinTransaction(super.sigBTx);

        assertThat(matches.size()).isEqualTo(1);
        assertThat(matches2.size()).isEqualTo(1);
        assertThat(matches.get(0)).isEqualTo(claimTransaction);
        assertThat(matches2.get(0)).isEqualTo(claimTransaction);

        assertThat(claimTransaction.isComplete()).isTrue();
    }

    @Test
    public void buildClaimTransactionSuccessfulTest_Data_sigB_sigA() throws AlreadyAddedTransactionException {
        ClaimTransaction claimTransaction = new ClaimTransaction(super.claimDataTx);
        List<Transaction> matches = claimTransaction.tryToAddDeXTTBitcoinTransaction(super.sigBTx);
        List<Transaction> matches2 = claimTransaction.tryToAddDeXTTBitcoinTransaction(super.sigATx);

        assertThat(matches.size()).isEqualTo(1);
        assertThat(matches2.size()).isEqualTo(1);
        assertThat(matches.get(0)).isEqualTo(claimTransaction);
        assertThat(matches2.get(0)).isEqualTo(claimTransaction);

        assertThat(claimTransaction.isComplete()).isTrue();
    }

    @Test
    public void buildClaimTransactionSuccessfulTest_sigA_Data_sigB() throws AlreadyAddedTransactionException {
        ClaimTransaction claimTransaction = new ClaimTransaction(super.sigATx);
        List<Transaction> matches = claimTransaction.tryToAddDeXTTBitcoinTransaction(super.claimDataTx);
        List<Transaction> matches2 = claimTransaction.tryToAddDeXTTBitcoinTransaction(super.sigBTx);

        assertThat(matches.size()).isEqualTo(1);
        assertThat(matches2.size()).isEqualTo(1);
        assertThat(matches.get(0)).isEqualTo(claimTransaction);
        assertThat(matches2.get(0)).isEqualTo(claimTransaction);

        assertThat(claimTransaction.isComplete()).isTrue();
    }

    @Test
    public void buildClaimTransactionSuccessfulTest_sigA_sigB_Data() throws AlreadyAddedTransactionException {
        ClaimTransaction claimTransaction = new ClaimTransaction(super.sigATx);
        List<Transaction> matches = claimTransaction.tryToAddDeXTTBitcoinTransaction(super.sigBTx);
        List<Transaction> matches2 = claimTransaction.tryToAddDeXTTBitcoinTransaction(super.claimDataTx);

        assertThat(matches.size()).isEqualTo(1);
        assertThat(matches2.size()).isEqualTo(1);
        assertThat(matches.get(0)).isEqualTo(claimTransaction);
        assertThat(matches2.get(0)).isEqualTo(claimTransaction);

        assertThat(claimTransaction.isComplete()).isTrue();
    }

    @Test
    public void buildClaimTransactionSuccessfulTest_sigB_Data_sigA() throws AlreadyAddedTransactionException {
        ClaimTransaction claimTransaction = new ClaimTransaction(super.sigBTx);
        List<Transaction> matches = claimTransaction.tryToAddDeXTTBitcoinTransaction(super.claimDataTx);
        List<Transaction> matches2 = claimTransaction.tryToAddDeXTTBitcoinTransaction(super.sigATx);

        assertThat(matches.size()).isEqualTo(1);
        assertThat(matches2.size()).isEqualTo(1);
        assertThat(matches.get(0)).isEqualTo(claimTransaction);
        assertThat(matches2.get(0)).isEqualTo(claimTransaction);

        assertThat(claimTransaction.isComplete()).isTrue();
    }

    @Test
    public void buildClaimTransactionSuccessfulTest_sigB_sigA_Data() throws AlreadyAddedTransactionException {
        ClaimTransaction claimTransaction = new ClaimTransaction(super.sigBTx);
        List<Transaction> matches = claimTransaction.tryToAddDeXTTBitcoinTransaction(super.sigATx);
        List<Transaction> matches2 = claimTransaction.tryToAddDeXTTBitcoinTransaction(super.claimDataTx);

        assertThat(matches.size()).isEqualTo(1);
        assertThat(matches2.size()).isEqualTo(1);
        assertThat(matches.get(0)).isEqualTo(claimTransaction);
        assertThat(matches2.get(0)).isEqualTo(claimTransaction);

        assertThat(claimTransaction.isComplete()).isTrue();
    }

    @Test
    public void buildClaimTransactionAddDataAgainThrowsExceptionTest() {
        ClaimTransaction claimTransaction = new ClaimTransaction(super.claimDataTx);
        assertThatThrownBy(() -> claimTransaction.tryToAddDeXTTBitcoinTransaction(super.claimDataTx)).isInstanceOf(AlreadyAddedTransactionException.class);
    }

    @Test
    public void buildClaimTransactionAddSigAAgainThrowsExceptionTest() {
        ClaimTransaction claimTransaction = new ClaimTransaction(super.sigATx);
        assertThatThrownBy(() -> claimTransaction.tryToAddDeXTTBitcoinTransaction(super.sigATx)).isInstanceOf(AlreadyAddedTransactionException.class);
    }

    @Test
    public void buildClaimTransactionAddSigbAgainThrowsExceptionTest() {
        ClaimTransaction claimTransaction = new ClaimTransaction(super.sigBTx);
        assertThatThrownBy(() -> claimTransaction.tryToAddDeXTTBitcoinTransaction(super.sigBTx)).isInstanceOf(AlreadyAddedTransactionException.class);
    }

}
