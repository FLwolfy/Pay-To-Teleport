package com.flwolfy.paytp.modmenu.entry.common;

import java.util.List;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class PayTpAllCategoryEntry extends PayTpSubCategoryEntry {

  private static final int BRIGHT_YELLOW = 0xFFFFFF55;

  public PayTpAllCategoryEntry(
      ConfigEntryBuilder builder,
      Component title,
      List<AbstractConfigListEntry<?>> entries
  ) {
    // The style is applied here rather than left to the caller, so a first level header is always
    // rendered bold yellow regardless of which screen built it.
    super(
        builder,
        title.copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
        entries,
        true,
        true,
        BRIGHT_YELLOW
    );
  }
}
