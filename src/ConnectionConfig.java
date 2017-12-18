public class ConnectionConfig {

  private String account = "ccas0000";
//  private String account = "aqtutu777";
  private int clientId = 1;
  private int socketPort = 7496;
  private String host = "127.0.0.1";

  public ConnectionConfig() {

  }

  public ConnectionConfig(String account, int clientId, int socketPort,
      String host) {
    this.account = account;
    this.clientId = clientId;
    this.socketPort = socketPort;
    this.host = host;
  }

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
  }

  public int getClientId() {
    return clientId;
  }

  public void setClientId(int clientId) {
    this.clientId = clientId;
  }

  public int getSocketPort() {
    return socketPort;
  }

  public void setSocketPort(int socketPort) {
    this.socketPort = socketPort;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }
}
