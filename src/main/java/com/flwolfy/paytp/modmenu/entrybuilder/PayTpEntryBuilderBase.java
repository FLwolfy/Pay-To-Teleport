package com.flwolfy.paytp.modmenu.entrybuilder;

import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder;

import net.minecraft.network.chat.Component;

/**
 * Creates a Cloth Config field builder for a particular configuration value type.
 *
 * @param <T> the configuration value type handled by this builder
 */
public abstract class PayTpEntryBuilderBase<T> {

  /**
   * Creates the field builder for the supplied value.
   *
   * @param builder the Cloth Config entry builder
   * @param value the current field value
   * @param label the localized field label
   * @return the field builder
   */
  public abstract AbstractFieldBuilder<T, ?, ?> create(
      ConfigEntryBuilder builder,
      T value,
      Component label
  );
}
