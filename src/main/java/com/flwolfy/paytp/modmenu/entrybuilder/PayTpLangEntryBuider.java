package com.flwolfy.paytp.modmenu.entrybuilder;

import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder;
import net.minecraft.network.chat.Component;

public class PayTpLangEntryBuider extends PayTpEntryBuilderBase<String> {

  @Override
  public AbstractFieldBuilder<String, ?, ?> create(
      ConfigEntryBuilder builder,
      String value,
      Component label
  ) {
    return builder.startStrField(label, value);
  }
}
