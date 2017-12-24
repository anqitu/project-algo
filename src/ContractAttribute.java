public enum ContractAttribute {
  CONTRACT_SYMBOL("symbol"),
  CONTRACT_INDUSTRY("industry"),
  CONTRACT_STOPLOSS("stopLoss");
//  CONTRACT_LASTTRADEDATE_OR_CONTRACTMONTH("lastTradedateOrContractMonth");

  private String name;
  ContractAttribute(String string) {
    this.name = string;
  }

  @Override
  public String toString() {
    return name;
  }

}
