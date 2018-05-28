package io.blk.erc20;

import io.blk.erc20.model.Bid;
import io.blk.erc20.model.CoinValue;
import org.web3j.crypto.Credentials;

import java.math.BigDecimal;
import java.util.*;

public class Database {
  private static Database ourInstance = new Database();
  private List<Bid> bids = new ArrayList<>();
  private CoinValue coinValue = new CoinValue();
  private Map<String, String> publicPrivateMap = new HashMap<>();

  public static Database getInstance() {
    return ourInstance;
  }

  public void addMap(String privateKey, String publicKey) {
    publicPrivateMap.put(publicKey, privateKey);
  }

  public Credentials getCred(String publicKey) {
    String privateKey = publicPrivateMap.get(publicKey);
    return Credentials.create(privateKey);
  }

  private Database() {
  }

  public Bid addBid(Bid bid) {
    bid.setId(UUID.randomUUID().toString());
    bids.add(bid);
    return bid;
  }

  public List<Bid> getAllBids() {
    return bids;
  }

  public void sell(Bid bid) {

    BigDecimal subtract = coinValue.getAmount().subtract(bid.getTotalCoins().multiply(coinValue.getPerCoinValue()));
    BigDecimal newTotalCoinValue = subtract.add(bid.getTotalAmount());
    coinValue.setAmount(newTotalCoinValue);
    coinValue.setPerCoinValue(newTotalCoinValue.divide(coinValue.getTotalCoins()));
    bids.remove(bid);

  }


}
