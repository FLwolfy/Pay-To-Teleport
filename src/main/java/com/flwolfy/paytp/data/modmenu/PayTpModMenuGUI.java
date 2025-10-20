package com.flwolfy.paytp.data.modmenu;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.lang.PayTpLang;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder;

import net.minecraft.text.Text;

import net.minecraft.util.Formatting;
import org.slf4j.Logger;

import java.lang.reflect.Method;

public class PayTpModMenuGUI {

  private static final Logger LOGGER = PayTpMod.LOGGER;
  private static final String ENTRY_FACTORY_METHOD_NAME = "createField";
  private static final Map<String, AbstractConfigListEntry<?>> ENTRY_MAP = new HashMap<>();

  /**
   * Generic createEntry method using reflection to set defaultValue and tooltip.
   *
   * @param builder       ConfigEntryBuilder
   * @param value         current value
   * @param defaultValue  default value
   * @param fieldPath     field path in the record
   * @param label         label text
   * @param tooltip       optional tooltip
   * @return AbstractConfigListEntry
   */
  public static AbstractConfigListEntry<?> createEntry(
      ConfigEntryBuilder builder,
      Object value,
      Object defaultValue,
      String fieldPath,
      Consumer<Object> fieldSetter,
      Text label,
      Text... tooltip) {
    if (value == null) return null;
    if (ENTRY_MAP.containsKey(fieldPath)) return ENTRY_MAP.get(fieldPath);

    try {
      @SuppressWarnings("all")
      Method createFieldMethod = PayTpModMenuGUI.class
          .getDeclaredMethod(ENTRY_FACTORY_METHOD_NAME, ConfigEntryBuilder.class, value.getClass(), Text.class);
      createFieldMethod.setAccessible(true);

      @SuppressWarnings("unchecked")
      AbstractFieldBuilder<Object, ?, ?> builderObj = (AbstractFieldBuilder<Object, ?, ?>)
          createFieldMethod.invoke(null, builder, value, label);

      builderObj.setDefaultValue(defaultValue);
      builderObj.setTooltip(tooltip);
      builderObj.setSaveConsumer(fieldSetter);

      var entry = builderObj.build();
      ENTRY_MAP.put(fieldPath, entry);
      return entry;

    } catch (NoSuchMethodException e) {
      LOGGER.error("No GUI field for type: {}", value.getClass().getSimpleName(), e);
    } catch (Exception e) {
      LOGGER.error("Error creating GUI entry for type: {}", value.getClass().getSimpleName(), e);
    }

    // fallback
    return builder.startTextDescription(Text.literal("ERROR").formatted(Formatting.RED)).build();
  }

  // ======== concrete createField methods ========
  // These return builder objects but NOT build()
  // ==============================================
  @SuppressWarnings("unused")
  private static AbstractFieldBuilder<?, ?, ?> createField(ConfigEntryBuilder builder, Integer value, Text label) {
    return builder.startIntField(label, value);
  }

  @SuppressWarnings("unused")
  private static AbstractFieldBuilder<?, ?, ?> createField(ConfigEntryBuilder builder, Double value, Text label) {
    return builder.startDoubleField(label, value);
  }

  @SuppressWarnings("unused")
  private static AbstractFieldBuilder<?, ?, ?> createField(ConfigEntryBuilder builder, Boolean value, Text label) {
    return builder.startBooleanToggle(label, value);
  }

  @SuppressWarnings("unused")
  private static AbstractFieldBuilder<?, ?, ?> createField(ConfigEntryBuilder builder, String value, Text label) {
    return builder.startStrField(label, value);
  }

  @SuppressWarnings("unused")
  private static AbstractFieldBuilder<?, ?, ?> createField(ConfigEntryBuilder builder, PayTpLang value, Text label) {
    return builder.startEnumSelector(label, PayTpLang.class, value);
  }
}
