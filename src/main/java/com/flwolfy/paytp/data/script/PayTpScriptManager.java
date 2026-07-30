package com.flwolfy.paytp.data.script;

import com.flwolfy.paytp.data.PayTpContext;
import com.flwolfy.paytp.data.PayTpCallback;
import com.flwolfy.paytp.data.PayTpPlayer;
import com.flwolfy.paytp.util.PayTpShellExecutor;
import com.flwolfy.paytp.util.PayTpCommandExecutor;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlFeatures;
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
  private static final Pattern CALLBACK_ASSIGNMENT = Pattern.compile(
      "callback\\s*\\.\\s*(onSuccess|onFailure)\\s*\\(\\s*\\)\\s*([+-])="
  );

  private final JexlEngine engine;
  private final Map<String, JexlScript> scriptCache;

  private PayTpScriptManager() {
    engine = new JexlBuilder()
        .strict(true)
        .silent(false)
        .features(JexlFeatures.createScript().sideEffectGlobal(true))
        .arithmetic(new PayTpScriptArithmetic())
        .permissions(new JexlPermissions.ClassPermissions(
            JexlPermissions.SECURE,
            includeNestMembers(
                PayTpScriptPosition.class,
                PayTpContext.class,
                PayTpCallback.class,
                PayTpPlayer.class,
                PayTpShellExecutor.class,
                PayTpCommandExecutor.class
            )
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
      if (argument.getValue() instanceof PayTpScriptCallbackChain chain) {
        chain.bind(context);
      } else if (argument.getValue() instanceof PayTpCallback callback) {
        callback.onSuccess().bind(context);
        callback.onFailure().bind(context);
      }
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
        source -> engine.createScript(normalizeCallbackAssignments(source))
    );
  }

  /**
   * Converts callback compound assignments into arithmetic expressions.
   *
   * <p>JEXL does not accept a method call as the target of {@code +=} or {@code -=}. Callback
   * chains are mutable, so discarding the arithmetic result preserves the requested script syntax.
   * Quoted text and comments are copied verbatim.</p>
   */
  private static String normalizeCallbackAssignments(String source) {
    StringBuilder normalized = new StringBuilder(source.length());
    LexicalState state = LexicalState.CODE;

    for (int index = 0; index < source.length();) {
      char current = source.charAt(index);
      char next = index + 1 < source.length() ? source.charAt(index + 1) : '\0';

      if (state == LexicalState.CODE) {
        Matcher matcher = CALLBACK_ASSIGNMENT.matcher(source).region(index, source.length());
        boolean standaloneIdentifier = index == 0
            || (!Character.isJavaIdentifierPart(source.charAt(index - 1))
                && source.charAt(index - 1) != '.');
        if (standaloneIdentifier && matcher.lookingAt()) {
          normalized
              .append("callback.")
              .append(matcher.group(1))
              .append("() ")
              .append(matcher.group(2));
          index = matcher.end();
          continue;
        }
        if (current == '/' && next == '/') {
          state = LexicalState.LINE_COMMENT;
        } else if (current == '/' && next == '*') {
          state = LexicalState.BLOCK_COMMENT;
        } else if (current == '\'') {
          state = LexicalState.SINGLE_QUOTE;
        } else if (current == '"') {
          state = LexicalState.DOUBLE_QUOTE;
        } else if (current == '`') {
          state = LexicalState.BACKTICK;
        }
      } else if (state == LexicalState.LINE_COMMENT && current == '\n') {
        state = LexicalState.CODE;
      } else if (state == LexicalState.BLOCK_COMMENT && current == '*' && next == '/') {
        normalized.append(current).append(next);
        index += 2;
        state = LexicalState.CODE;
        continue;
      } else if (isQuoteState(state) && current == '\\' && next != '\0') {
        normalized.append(current).append(next);
        index += 2;
        continue;
      } else if ((state == LexicalState.SINGLE_QUOTE && current == '\'')
          || (state == LexicalState.DOUBLE_QUOTE && current == '"')
          || (state == LexicalState.BACKTICK && current == '`')) {
        state = LexicalState.CODE;
      }

      normalized.append(current);
      index++;
    }
    return normalized.toString();
  }

  private static boolean isQuoteState(LexicalState state) {
    return state == LexicalState.SINGLE_QUOTE
        || state == LexicalState.DOUBLE_QUOTE
        || state == LexicalState.BACKTICK;
  }

  private enum LexicalState {
    CODE,
    LINE_COMMENT,
    BLOCK_COMMENT,
    SINGLE_QUOTE,
    DOUBLE_QUOTE,
    BACKTICK
  }

  private static Class<?>[] includeNestMembers(Class<?>... rootClasses) {
    return Arrays.stream(rootClasses)
        .flatMap(rootClass -> Arrays.stream(rootClass.getNestMembers()))
        .distinct()
        .toArray(Class<?>[]::new);
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
