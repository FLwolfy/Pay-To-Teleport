package com.flwolfy.paytp.data;

import com.flwolfy.paytp.data.script.PayTpScriptCallbackChain;

/**
 * Callback chains registered by one price-algorithm execution.
 */
public record PayTpCallback(
    PayTpScriptCallbackChain onSuccess,
    PayTpScriptCallbackChain onFailure
) {

  public PayTpCallback() {
    this(new PayTpScriptCallbackChain(), new PayTpScriptCallbackChain());
  }
}
