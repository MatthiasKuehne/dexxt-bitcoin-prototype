package DeXTT.DataStructure;

import org.web3j.abi.datatypes.Address;
import org.web3j.utils.Numeric;

import java.io.Serializable;

public class DeXTTAddress extends org.web3j.abi.datatypes.Address {

    public DeXTTAddress(String hexValue) {
        super(hexValue);
        super.toUint();
    }

    /**
     * Does not put a "0x" prefix on address
     * @return
     */
    public String getAddressWithoutPrefix() {
        return Numeric.toHexStringNoPrefixZeroPadded(super.toUint().getValue(), super.toUint().getBitSize() >> 2);
    }

    public byte[] getAddressBytesWithoutPrefix() {
        return Numeric.hexStringToByteArray(this.getAddressWithoutPrefix());
    }

}
