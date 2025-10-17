package com.flwolfy.paytp.util;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PayTpTextFormatter {

  public static final Formatting DEFAULT_TEXT_COLOR = Formatting.YELLOW;
  public static final Formatting DEFAULT_HIGHLIGHT_COLOR = Formatting.GREEN;
  public static final Formatting DEFAULT_WARN_COLOR = Formatting.RED;

  public static Text format(String key, Formatting textColor, Formatting highlightColor, Object... args) {
    Object[] formattedArgs = new Object[args.length];
    for (int i = 0; i < args.length; i++) {
      Object arg = args[i];
      if (arg instanceof Text text) {
        formattedArgs[i] = text;
      } else {
        formattedArgs[i] = Text.literal(String.valueOf(arg)).formatted(highlightColor);
      }
    }

    return Text.translatable(key, formattedArgs).formatted(textColor);
  }


  public static Text format(String key, Object... args) {
    return format(key, DEFAULT_TEXT_COLOR, DEFAULT_HIGHLIGHT_COLOR, args);
  }
}

