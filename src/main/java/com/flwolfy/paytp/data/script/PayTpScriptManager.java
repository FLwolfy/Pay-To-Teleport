package com.flwolfy.paytp.data.script;

import com.flwolfy.paytp.util.PayTpShellExecutor;
import com.flwolfy.paytp.util.PayTpCommandExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlPermissions;

/**
 * Compiles and evaluates PayTp JEXL scripts.
 *
 * <p>The manager uses strict evaluation, caches compiled scripts by source, and exposes the
 * {@code math}, {@code shell}, and caller-provided namespaces. Compiled scripts are thread-safe
 * to retrieve from the cache, while callers remain responsible for supplying appropriate
 * argument values.</p>
 */
public class PayTpScriptManager {

  private static final PayTpScriptManager INSTANCE = new PayTpScriptManager();

  private final JexlEngine engine;
  private final Map<String, JexlScript> scriptCache;

  private PayTpScriptManager() {
    engine = new JexlBuilder()
        .strict(true)
        .silent(false)
        .permissions(new JexlPermissions.ClassPermissions(
            JexlPermissions.SECURE,
            PayTpScriptPosition.class,
            PayTpShellExecutor.class,
            PayTpShellExecutor.ShellResult.class,
            PayTpCommandExecutor.class
        ))
        .namespaces(Map.of(
            "math", Math.class,
            "shell", PayTpShellExecutor.class
        ))
        .create();
    scriptCache = new ConcurrentHashMap<>();
  }

  public static PayTpScriptManager getInstance() {
    return INSTANCE;
  }

  /**
   * Evaluates a script with the supplied named arguments and verifies its result type.
   *
   * @param script the script to compile and execute
   * @param resultType the exact reference type expected from the script
   * @param arguments key-value pairs exposed as variables in the JEXL context
   * @param <T> the expected result type
   * @return the evaluated result cast to {@code resultType}
   * @throws IllegalArgumentException if the result is null or has a different type
   * @throws org.apache.commons.jexl3.JexlException if compilation or execution fails
   */
  @SafeVarargs
  public final <T> T evaluate(
      PayTpScript script,
      Class<T> resultType,
      Map.Entry<String, ?>... arguments
  ) {
    return evaluate(script, resultType, Map.of(), arguments);
  }

  /**
   * Evaluates a script with additional namespaces available only for this execution.
   *
   * @param script the script to compile and execute
   * @param resultType the exact reference type expected from the script
   * @param namespaces namespace names mapped to their method providers
   * @param arguments key-value pairs exposed as variables in the JEXL context
   * @param <T> the expected result type
   * @return the evaluated result cast to {@code resultType}
   */
  @SafeVarargs
  public final <T> T evaluate(
      PayTpScript script,
      Class<T> resultType,
      Map<String, ?> namespaces,
      Map.Entry<String, ?>... arguments
  ) {
    ScriptContext context = new ScriptContext(namespaces);
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

  private JexlScript compile(PayTpScript script) {
    return scriptCache.computeIfAbsent(
        script.source(),
        engine::createScript
    );
  }

  private static final class ScriptContext
      extends MapContext
      implements JexlContext.NamespaceResolver {

    private final Map<String, ?> namespaces;

    private ScriptContext(Map<String, ?> namespaces) {
      this.namespaces = Map.copyOf(namespaces);
    }

    @Override
    public Object resolveNamespace(String name) {
      return namespaces.get(name);
    }
  }
}
