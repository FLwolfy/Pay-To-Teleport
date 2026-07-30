package com.flwolfy.paytp.data.script;

import com.flwolfy.paytp.PayTpMod;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlScript;

/**
 * Mutable, execution-local chain of JEXL callbacks.
 */
public final class PayTpScriptCallbackChain {

  private final List<JexlScript> callbacks = new ArrayList<>();
  private JexlContext context;

  void bind(JexlContext context) {
    this.context = context;
  }

  PayTpScriptCallbackChain add(JexlScript callback) {
    callbacks.add(callback);
    return this;
  }

  PayTpScriptCallbackChain remove(JexlScript callback) {
    callbacks.remove(callback);
    return this;
  }

  /**
   * Executes a stable snapshot in registration order.
   */
  public void execute() {
    if (context == null) return;
    for (JexlScript callback : List.copyOf(callbacks)) {
      try {
        callback.execute(context);
      } catch (RuntimeException e) {
        PayTpMod.LOGGER.error("PayTp JEXL teleport callback failed", e);
      }
    }
  }
}
