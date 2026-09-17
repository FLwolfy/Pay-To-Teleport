package com.flwolfy.paytp.command.warp;

import java.util.ArrayList;
import java.util.List;

public final class PayTpWarpCommandInput {

  public record Delete(String name, boolean forced, boolean valid) {}

  public record Rename(String name, String newName, boolean valid) {}

  public record PlayerAction(String name, String playerName, boolean valid) {}

  private record Tokens(List<String> values, boolean valid) {}

  private PayTpWarpCommandInput() {}

  public static String name(String input) {
    Tokens tokens = tokenize(input);
    return tokens.valid() && tokens.values().size() == 1 && valid(tokens.values().getFirst())
        ? tokens.values().getFirst()
        : "";
  }

  public static Delete delete(String input) {
    Tokens tokens = tokenize(input);
    if (!tokens.valid() || tokens.values().isEmpty() || tokens.values().size() > 2) {
      return new Delete("", false, false);
    }
    String name = tokens.values().getFirst();
    boolean forced = tokens.values().size() == 2
        && "forced".equalsIgnoreCase(tokens.values().get(1));
    boolean valid = valid(name) && (tokens.values().size() == 1 || forced);
    return new Delete(name, forced, valid);
  }

  public static Rename rename(String input) {
    Tokens tokens = tokenize(input);
    if (!tokens.valid() || tokens.values().size() != 2) {
      return new Rename("", "", false);
    }
    String name = tokens.values().getFirst();
    String newName = tokens.values().get(1);
    return new Rename(name, newName, valid(name) && valid(newName));
  }

  public static PlayerAction playerAction(String input) {
    Tokens tokens = tokenize(input);
    if (!tokens.valid() || tokens.values().size() != 2) {
      return new PlayerAction("", "", false);
    }
    String name = tokens.values().getFirst();
    String playerName = tokens.values().get(1);
    return new PlayerAction(name, playerName, valid(name) && valid(playerName));
  }

  public static int nextArgumentStart(String input) {
    int cursor = 0;
    while (cursor < input.length() && Character.isWhitespace(input.charAt(cursor))) {
      cursor++;
    }
    if (cursor == input.length()) {
      return -1;
    }

    if (input.charAt(cursor) == '"') {
      cursor++;
      boolean closed = false;
      while (cursor < input.length()) {
        char character = input.charAt(cursor++);
        if (character == '\\') {
          if (cursor == input.length()) {
            return -1;
          }
          char escaped = input.charAt(cursor++);
          if (escaped != '"' && escaped != '\\') {
            return -1;
          }
        } else if (character == '"') {
          closed = true;
          break;
        }
      }
      if (!closed) {
        return -1;
      }
    } else {
      while (cursor < input.length() && !Character.isWhitespace(input.charAt(cursor))) {
        cursor++;
      }
    }

    if (cursor == input.length() || !Character.isWhitespace(input.charAt(cursor))) {
      return -1;
    }
    while (cursor < input.length() && Character.isWhitespace(input.charAt(cursor))) {
      cursor++;
    }
    return cursor;
  }

  public static String formatNameSuggestion(String name) {
    return '"' + name.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
  }

  private static Tokens tokenize(String input) {
    String value = input.trim();
    List<String> values = new ArrayList<>();
    int cursor = 0;
    while (cursor < value.length()) {
      if (!values.isEmpty()) {
        if (!Character.isWhitespace(value.charAt(cursor))) {
          return new Tokens(List.of(), false);
        }
        while (cursor < value.length() && Character.isWhitespace(value.charAt(cursor))) {
          cursor++;
        }
        if (cursor == value.length()) {
          break;
        }
      }

      if (value.charAt(cursor) != '"') {
        int start = cursor;
        while (cursor < value.length() && !Character.isWhitespace(value.charAt(cursor))) {
          cursor++;
        }
        values.add(value.substring(start, cursor));
        continue;
      }

      StringBuilder quoted = new StringBuilder();
      cursor++;
      boolean closed = false;
      while (cursor < value.length()) {
        char character = value.charAt(cursor++);
        if (character == '"') {
          closed = true;
          break;
        }
        if (character == '\\') {
          if (cursor == value.length()) {
            return new Tokens(List.of(), false);
          }
          char escaped = value.charAt(cursor++);
          if (escaped != '"' && escaped != '\\') {
            return new Tokens(List.of(), false);
          }
          quoted.append(escaped);
        } else {
          quoted.append(character);
        }
      }
      if (!closed) {
        return new Tokens(List.of(), false);
      }
      values.add(quoted.toString());
    }
    return new Tokens(List.copyOf(values), true);
  }

  private static boolean valid(String value) {
    return value != null && !value.isBlank();
  }
}
