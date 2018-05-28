package io.blk.erc20;

import org.web3j.protocol.Web3j;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;

import java.math.BigInteger;

public class ExtendedContract extends Contract {


  protected ExtendedContract(String contractBinary, String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
    super(contractBinary, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
  }


}
