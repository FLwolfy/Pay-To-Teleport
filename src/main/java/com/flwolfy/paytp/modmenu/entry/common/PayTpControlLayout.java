package com.flwolfy.paytp.modmenu.entry.common;

public final class PayTpControlLayout {

  private static final int TOTAL_WIDTH = 150;
  private static final int GAP = 2;

  private PayTpControlLayout() {}

  public static int valueX(int x, int entryWidth) {
    return x + entryWidth - TOTAL_WIDTH;
  }

  public static int valueWidth(int resetWidth) {
    return TOTAL_WIDTH - resetWidth - GAP;
  }

  public static int resetX(int x, int entryWidth, int resetWidth) {
    return x + entryWidth - resetWidth;
  }

  public static int gap() {
    return GAP;
  }
}
