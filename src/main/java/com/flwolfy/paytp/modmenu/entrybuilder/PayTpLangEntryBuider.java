package com.flwolfy.paytp.modmenu.entrybuilder;

import com.flwolfy.paytp.data.lang.PayTpLang;

import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder;

import net.minecraft.network.chat.Component;

public class PayTpLangEntryBuider extends PayTpEntryBuilderBase<PayTpLang> {

  @Override
  public AbstractFieldBuilder<PayTpLang, ?, ?> create(
      ConfigEntryBuilder builder,
      PayTpLang value,
      Component label
  ) {
    return builder.startEnumSelector(label, PayTpLang.class, value);
  }
}
