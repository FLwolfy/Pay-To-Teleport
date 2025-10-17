package com.flwolfy.paytp.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PayTpTextFormatter {


  public static final Formatting DEFAULT_TEXT_COLOR = Formatting.YELLOW;
  public static final Formatting DEFAULT_HIGHLIGHT_COLOR = Formatting.GREEN;
  public static final Formatting DEFAULT_WARN_COLOR = Formatting.RED;

  public static Text format(Text template, Formatting textColor, Formatting highlightColor, Object... args) {
    String raw = template.getString();
    String[] parts = raw.split("%s", -1);
    MutableText result = Text.literal("").formatted(textColor);

    for (int i = 0; i < parts.length; i++) {
      result.append(Text.literal(parts[i]).formatted(textColor));
      if (i < args.length) {
        Object arg = args[i];
        if (arg instanceof Text textArg) {
          result.append(textArg.copy().formatted(highlightColor));
        } else {
          result.append(Text.literal(String.valueOf(arg)).formatted(highlightColor));
        }
      }
    }

    return result;
  }

  public static Text format(Text template, Object... args) {
    return format(template, DEFAULT_TEXT_COLOR, DEFAULT_HIGHLIGHT_COLOR, args);
  }
}

