package io.blk.erc20.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Objects;

@Data
public class Bid {

  private String id;
  private String requester;
  private BigDecimal totalCoins;
  private BigDecimal amountPerCoin;
  private BigDecimal totalAmount;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Bid bid = (Bid) o;
    return Objects.equals(getId(), bid.getId());
  }

  @Override
  public int hashCode() {

    return Objects.hash(getId());
  }
}
