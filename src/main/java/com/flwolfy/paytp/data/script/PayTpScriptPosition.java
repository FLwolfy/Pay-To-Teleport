package com.flwolfy.paytp.data.script;

import com.flwolfy.paytp.data.PayTpData;

import java.util.Objects;

/**
 * Immutable script-facing teleport position exposed through the {@code from} and {@code to}
 * variables.
 *
 * @param x the X coordinate
 * @param y the Y coordinate
 * @param z the Z coordinate
 * @param dimension the namespaced dimension identifier
 */
public record PayTpScriptPosition(
    double x,
    double y,
    double z,
    String dimension
) {

  public PayTpScriptPosition {
    Objects.requireNonNull(dimension, "dimension");
  }

  /**
   * Converts stored teleport data into its script-facing representation.
   *
   * @param data the teleport data to convert
   * @return a script position containing coordinates and the dimension identifier
   */
  public static PayTpScriptPosition from(PayTpData data) {
    return new PayTpScriptPosition(
        data.pos().x,
        data.pos().y,
        data.pos().z,
        data.world().identifier().toString()
    );
  }
}
