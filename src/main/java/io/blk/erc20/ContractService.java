package io.blk.erc20;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import io.blk.erc20.generated.HumanStandardToken;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.*;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.quorum.Quorum;
import org.web3j.quorum.tx.ClientTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.utils.Numeric;

import static org.web3j.tx.Contract.GAS_LIMIT;
import static org.web3j.tx.ManagedTransaction.GAS_PRICE;

/**
 * Our smart contract service.
 */
@Service
public class ContractService {

  private final Quorum quorum;
  @Autowired
  @Qualifier("rawBean")
  private JsonRpc2_0Web3j web3j;
  String key = "{\"version\":3,\"id\":\"c56df251-4f08-48da-a1e6-3f5be0696573\",\"address\":\"d6a6f4a2ba052c23dc258e5712e15c721fae7c2c\",\"Crypto\":{\"ciphertext\":\"a80aed6e10ad915c771e370dacc73c086189005854af8b711ecf40f3a3c89c47\",\"cipherparams\":{\"iv\":\"fda3285a180c0d586e108db59fa1701a\"},\"cipher\":\"aes-128-ctr\",\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"salt\":\"8caeb3a6efc8753867583495c45f82ea5ee179af8fce0417bccd4cd7485f255a\",\"n\":8192,\"r\":8,\"p\":1},\"mac\":\"ce3eede294f81de6f31a1417d1f1eb52c468868b5c7298d10761aa524d93f728\"}}";

  private final NodeConfiguration nodeConfiguration;

  @Autowired
  public ContractService(Quorum quorum, NodeConfiguration nodeConfiguration) {
    this.quorum = quorum;
    this.nodeConfiguration = nodeConfiguration;
  }

  public NodeConfiguration getConfig() {
    return nodeConfiguration;
  }

  public String deploy(
      List<String> privateFor, BigInteger initialAmount, String tokenName, BigInteger decimalUnits,
      String tokenSymbol) throws Exception {
    try {
      TransactionManager transactionManager = new ClientTransactionManager(
          quorum, nodeConfiguration.getFromAddress(), privateFor);
      HumanStandardToken humanStandardToken = HumanStandardToken.deploy(
          quorum, transactionManager, GAS_PRICE, GAS_LIMIT,
          initialAmount, tokenName, decimalUnits,
          tokenSymbol).send();
      return humanStandardToken.getContractAddress();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public String name(String contractAddress) throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress);
    try {
      return humanStandardToken.name().send();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public String approve(
      List<String> privateFor, String contractAddress, String spender, BigInteger value) throws Exception {


    org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
        "approve",
        Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(spender),
            new org.web3j.abi.datatypes.generated.Uint256(value)),
        Collections.<TypeReference<?>>emptyList());

    EthGetTransactionCount ethGetTransactionCount = quorum.ethGetTransactionCount(
        nodeConfiguration.getFromAddress(), DefaultBlockParameterName.LATEST).sendAsync().get();


    BigInteger nonce = ethGetTransactionCount.getTransactionCount();
    String data = FunctionEncoder.encode(function);
    RawTransaction rawTransaction = RawTransaction.createTransaction(
        nonce, GAS_PRICE, GAS_LIMIT, contractAddress, data);


    byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, getCredentials());
    String hexValue = Numeric.toHexString(signedMessage);

    EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
    return "https://ropsten.etherscan.io/tx/" + ethSendTransaction.getTransactionHash();
  }

  public long totalSupply(String contractAddress) throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress);
    try {
      return extractLongValue(humanStandardToken.totalSupply().send());
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public String transferFrom(
      List<String> privateFor,String contractAddress,Controller.SellRequest sellRequest) throws Exception {

    Credentials credentials = Database.getInstance().getCred(sellRequest.getBid().getRequester());

    EthGetTransactionCount ethGetTransactionCount = quorum.ethGetTransactionCount(
       sellRequest.getBid().getRequester(), DefaultBlockParameterName.LATEST).sendAsync().get();

    BigInteger nonce = ethGetTransactionCount.getTransactionCount();

    org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
        "transferFrom",
        Arrays.asList(new org.web3j.abi.datatypes.Address(sellRequest.getBid().getRequester()),
            new org.web3j.abi.datatypes.Address(sellRequest.getTo()),
            new org.web3j.abi.datatypes.generated.Uint256(sellRequest.getBid().getTotalCoins().toBigInteger())),
        Collections.emptyList());

    String data = FunctionEncoder.encode(function);
    RawTransaction rawTransaction = RawTransaction.createTransaction(
        nonce, GAS_PRICE, GAS_LIMIT, contractAddress, data);

    byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
    String hexValue = Numeric.toHexString(signedMessage);

    EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
    String transactionHash = ethSendTransaction.getTransactionHash();
    return "https://ropsten.etherscan.io/tx/" + transactionHash;
  }

  private Credentials getCredentials() throws IOException, CipherException {
    File file = new File("test1");
    FileWriter fileWriter = new FileWriter(file);
    fileWriter.write(key);
    fileWriter.flush();
    fileWriter.close();

    return WalletUtils.loadCredentials(
        "1234567890",
        file);
  }

  public TransactionResponse<TransferEventResponse> transferFrom(
      List<String> privateFor, String contractAddress, String from, String to, BigInteger value) throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress, privateFor);
    try {
      TransactionReceipt transactionReceipt = humanStandardToken
          .transferFrom(from, to, value).send();
      return processTransferEventsResponse(humanStandardToken, transactionReceipt);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public long decimals(String contractAddress) throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress);
    try {
      return extractLongValue(humanStandardToken.decimals().send());
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public String version(String contractAddress) throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress);
    try {
      return humanStandardToken.version().send();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public BigInteger balanceOf(String contractAddress, String ownerAddress) throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress);
    try {
      BigInteger send = humanStandardToken.balanceOf(ownerAddress).send();
      return send;
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public String symbol(String contractAddress) throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress);
    try {
      return humanStandardToken.symbol().send();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public TransactionResponse<TransferEventResponse> transfer(
      List<String> privateFor, String contractAddress, String to, BigInteger value) throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress, privateFor);
    try {
      TransactionReceipt transactionReceipt = humanStandardToken
          .transfer(to, value).send();
      return processTransferEventsResponse(humanStandardToken, transactionReceipt);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public String transfer2(
      List<String> privateFor, String contractAddress, String to, BigInteger value) throws Exception {

    Credentials credentials = getCredentials();

    EthGetTransactionCount ethGetTransactionCount = quorum.ethGetTransactionCount(
        nodeConfiguration.getFromAddress(), DefaultBlockParameterName.LATEST).sendAsync().get();

    BigInteger nonce = ethGetTransactionCount.getTransactionCount();

    org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
        "transfer",
        Arrays.asList(new org.web3j.abi.datatypes.Address(to),
            new org.web3j.abi.datatypes.generated.Uint256(value)),
        Collections.emptyList());


    String data = FunctionEncoder.encode(function);

    RawTransaction rawTransaction = RawTransaction.createTransaction(
        nonce, GAS_PRICE, GAS_LIMIT, contractAddress, data);

    byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
    String hexValue = Numeric.toHexString(signedMessage);

    EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
    String transactionHash = ethSendTransaction.getTransactionHash();
    return "https://ropsten.etherscan.io/tx/" + transactionHash;

  }

  public TransactionResponse<ApprovalEventResponse> approveAndCall(
      List<String> privateFor, String contractAddress, String spender, BigInteger value,
      String extraData) throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress, privateFor);
    try {
      TransactionReceipt transactionReceipt = humanStandardToken
          .approveAndCall(
              spender, value,
              extraData.getBytes())
          .send();
      return processApprovalEventResponse(humanStandardToken, transactionReceipt);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public long allowance(String contractAddress, String ownerAddress, String spenderAddress) throws Exception {
    HumanStandardToken humanStandardToken = load(contractAddress);
    try {
      return extractLongValue(humanStandardToken.allowance(
          ownerAddress, spenderAddress)
          .send());
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private HumanStandardToken load(String contractAddress, List<String> privateFor) {
    TransactionManager transactionManager = new ClientTransactionManager(
        quorum, nodeConfiguration.getFromAddress(), privateFor);
    return HumanStandardToken.load(
        contractAddress, quorum, transactionManager, GAS_PRICE, GAS_LIMIT);
  }

  private HumanStandardToken load(String contractAddress) {
    TransactionManager transactionManager = new ClientTransactionManager(
        quorum, nodeConfiguration.getFromAddress(), Collections.emptyList());
    return HumanStandardToken.load(
        contractAddress, quorum, transactionManager, GAS_PRICE, GAS_LIMIT);
  }

  private long extractLongValue(BigInteger value) {
    return value.longValueExact();
  }

  private TransactionResponse<ApprovalEventResponse>
  processApprovalEventResponse(
      HumanStandardToken humanStandardToken,
      TransactionReceipt transactionReceipt) {

    return processEventResponse(
        humanStandardToken.getApprovalEvents(transactionReceipt),
        transactionReceipt,
        ApprovalEventResponse::new);
  }

  private TransactionResponse<TransferEventResponse>
  processTransferEventsResponse(
      HumanStandardToken humanStandardToken,
      TransactionReceipt transactionReceipt) {

    return processEventResponse(
        humanStandardToken.getTransferEvents(transactionReceipt),
        transactionReceipt,
        TransferEventResponse::new);
  }

  private <T, R> TransactionResponse<R> processEventResponse(
      List<T> eventResponses, TransactionReceipt transactionReceipt, Function<T, R> map) {
    if (!eventResponses.isEmpty()) {
      return new TransactionResponse<>(
          transactionReceipt.getTransactionHash(),
          map.apply(eventResponses.get(0)));
    } else {
      return new TransactionResponse<>(
          transactionReceipt.getTransactionHash());
    }
  }

  @Getter
  @Setter
  public static class TransferEventResponse {
    private String from;
    private String to;
    private long value;

    public TransferEventResponse() {
    }

    public TransferEventResponse(
        HumanStandardToken.TransferEventResponse transferEventResponse) {
      this.from = transferEventResponse._from;
      this.to = transferEventResponse._to;
      this.value = transferEventResponse._value.longValueExact();
    }
  }

  @Getter
  @Setter
  public static class ApprovalEventResponse {
    private String owner;
    private String spender;
    private long value;

    public ApprovalEventResponse() {
    }

    public ApprovalEventResponse(
        HumanStandardToken.ApprovalEventResponse approvalEventResponse) {
      this.owner = approvalEventResponse._owner;
      this.spender = approvalEventResponse._spender;
      this.value = approvalEventResponse._value.longValueExact();
    }
  }
}
