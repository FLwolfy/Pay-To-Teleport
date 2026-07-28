package com.flwolfy.paytp.data;

public enum PayTpTeleportType {
  COORDINATE("coordinate"),
  REQUEST("request"),
  HOME("home"),
  BACK("back"),
  WARP("warp");

  private final String name;

  PayTpTeleportType(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
