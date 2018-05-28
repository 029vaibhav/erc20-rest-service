package io.blk.erc20.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CoinValue {

  private BigDecimal totalCoins;
  private BigDecimal amount;
  private BigDecimal perCoinValue;
}
