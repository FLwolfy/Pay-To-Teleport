package com.flwolfy.paytp.data;

import com.mojang.brigadier.Command;

/**
 * Describes the outcome of a PayTp teleport operation.
 */
public enum PayTpTeleportResult {
  SUCCESS,
  INSUFFICIENT_FUNDS,
  CROSS_DIMENSION_DISABLED,
  FAILED;

  /**
   * Converts this teleport outcome to a Brigadier command result.
   *
   * @return the successful command result for {@link #SUCCESS}, otherwise {@code 0}
   */
  public int commandResult() {
    return this == SUCCESS ? Command.SINGLE_SUCCESS : 0;
  }
}
