package com.flwolfy.paytp.data;

import java.util.Objects;

/**
 * Script-facing player identity.
 *
 * @param uuid stable UUID string
 * @param name current player name
 */
public record PayTpPlayer(
    String uuid,
    String name
) {

  public PayTpPlayer {
    Objects.requireNonNull(uuid, "uuid");
    Objects.requireNonNull(name, "name");
  }
}
