package com.flwolfy.paytp.data.script;

import java.util.List;
import java.util.Objects;

public final class PayTpScript {

  private final String source;

  public PayTpScript(String source) {
    this.source = Objects.requireNonNull(source, "source");
  }

  public String source() {
    return source;
  }

  public List<String> lines() {
    return source.lines().toList();
  }

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
