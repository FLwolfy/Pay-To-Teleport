package com.flwolfy.paytp.modmenu.entrybuilder;

import com.flwolfy.paytp.data.warp.PayTpWarpPermission;

import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder;

import net.minecraft.network.chat.Component;

import java.util.Locale;

public class PayTpWarpPermissionEntryBuilder
    extends PayTpEntryBuilderBase<PayTpWarpPermission> {

  @Override
  public AbstractFieldBuilder<PayTpWarpPermission, ?, ?> create(
      ConfigEntryBuilder builder,
      PayTpWarpPermission value,
      Component label
  ) {
    return builder
        .startEnumSelector(label, PayTpWarpPermission.class, value)
        .setEnumNameProvider(permission -> Component.translatable(
            "paytp.config.warp.serverWarpPermission."
                + permission.name().toLowerCase(Locale.ROOT)
        ));
  }
}
