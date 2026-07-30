package com.flwolfy.paytp.data.script;

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlScript;

/**
 * Adds {@code +=} and {@code -=} support for JEXL callback chains.
 */
public final class PayTpScriptArithmetic extends JexlArithmetic {

  public PayTpScriptArithmetic() {
    super(true);
  }

  @Override
  public Object add(Object left, Object right) {
    if (left instanceof PayTpScriptCallbackChain chain
        && right instanceof JexlScript callback) {
      return chain.add(callback);
    }
    return super.add(left, right);
  }

  @Override
  public Object subtract(Object left, Object right) {
    if (left instanceof PayTpScriptCallbackChain chain
        && right instanceof JexlScript callback) {
      return chain.remove(callback);
    }
    return super.subtract(left, right);
  }
}
