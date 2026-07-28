package com.flwolfy.paytp.data.script;

import java.util.List;
import java.util.Objects;

/**
 * Immutable value object containing the source of a PayTp JEXL script.
 */
public final class PayTpScript {

  private final String source;

  /**
   * Creates a script from its complete source.
   *
   * @param source the non-null JEXL source
   * @throws NullPointerException if {@code source} is null
   */
  public PayTpScript(String source) {
    this.source = Objects.requireNonNull(source, "source");
  }

  /**
   * Returns the complete script source.
   *
   * @return the original source string
   */
  public String source() {
    return source;
  }

  /**
   * Returns the source split into lines.
   *
   * @return an immutable list in source order
   */
  public List<String> lines() {
    return source.lines().toList();
  }

  /**
   * Creates a script by joining source lines with newline characters.
   *
   * @param lines the source lines in order
   * @return a script containing the joined lines
   */
  public static PayTpScript fromLines(List<String> lines) {
    return new PayTpScript(String.join("\n", lines));
  }

  @Override
  public boolean equals(Object object) {
    return object instanceof PayTpScript script && source.equals(script.source);
  }

  @Override
  public int hashCode() {
    return source.hashCode();
  }

  @Override
  public String toString() {
    return source;
  }
}
