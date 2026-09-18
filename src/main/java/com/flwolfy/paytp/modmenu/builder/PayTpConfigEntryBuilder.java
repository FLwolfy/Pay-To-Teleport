package com.flwolfy.paytp.modmenu.builder;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;

public interface PayTpConfigEntryBuilder {

  AbstractConfigListEntry<?> build(PayTpEntryContext context);
}
