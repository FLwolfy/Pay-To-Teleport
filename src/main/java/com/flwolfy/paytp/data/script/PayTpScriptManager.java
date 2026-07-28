package com.flwolfy.paytp.data.script;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;

public class PayTpScriptManager {

  private static final PayTpScriptManager INSTANCE = new PayTpScriptManager();

  private final JexlEngine engine;
  private final Map<String, JexlScript> scriptCache;

  private PayTpScriptManager() {
    engine = new JexlBuilder()
        .strict(true)
        .silent(false)
        .namespaces(Map.of("math", Math.class))
        .create();
    scriptCache = new ConcurrentHashMap<>();
  }

  public static PayTpScriptManager getInstance() {
    return INSTANCE;
  }

  @SafeVarargs
  public final <T> T evaluate(
      PayTpScript script,
      Class<T> resultType,
      Map.Entry<String, ?>... arguments
  ) {
    JexlContext context = new MapContext();
    for (Map.Entry<String, ?> argument : arguments) {
      context.set(argument.getKey(), argument.getValue());
    }

    Object result = compile(script).execute(context);
    if (!resultType.isInstance(result)) {
      String actualType = result == null
          ? "null"
          : result.getClass().getSimpleName();
      throw new IllegalArgumentException(
          "Expected script result of type " + resultType.getSimpleName()
              + ", but got " + actualType
      );
    }
    return resultType.cast(result);
  }

  public void validate(PayTpScript script) {
    compile(script);
  }

  public Optional<String> validationError(PayTpScript script) {
    try {
      validate(script);
      return Optional.empty();
    } catch (Exception e) {
      String message = e.getMessage();
      return Optional.of(
          message == null || message.isBlank()
              ? e.getClass().getSimpleName()
              : message
      );
    }
  }

  private JexlScript compile(PayTpScript script) {
    return scriptCache.computeIfAbsent(
        script.source(),
        engine::createScript
    );
  }
}
