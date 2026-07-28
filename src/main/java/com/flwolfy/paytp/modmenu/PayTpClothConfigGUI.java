package com.flwolfy.paytp.modmenu;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.config.PayTpConfigData;
import com.flwolfy.paytp.data.config.PayTpConfigMapper;
import com.flwolfy.paytp.data.lang.PayTpLang;
import com.flwolfy.paytp.data.script.PayTpScript;

import com.flwolfy.paytp.modmenu.entrybuilder.PayTpScriptEntryBuilder;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import org.slf4j.Logger;

public class PayTpClothConfigGUI {

  private static final Logger LOGGER = PayTpMod.LOGGER;
  private static final String ENTRY_FACTORY_METHOD_NAME = "createField";
  private static final Map<String, AbstractConfigListEntry<?>> ENTRY_MAP = new HashMap<>();
  private static final Map<String, Object> INITIAL_VALUE_MAP = new HashMap<>();
  private static final PayTpScriptEntryBuilder SCRIPT_ENTRY_BUILDER = new PayTpScriptEntryBuilder();
  private static Map<String, Object> validatedValues = Map.of();
  private static List<String> invalidFields = List.of();

  /**
   * Clear all the registered entries in the cached ENTRY_MAP.
   */
  public static void clearCache() {
    ENTRY_MAP.clear();
    INITIAL_VALUE_MAP.clear();
    validatedValues = Map.of();
    invalidFields = List.of();
  }

  /**
   * Generic createEntry method using reflection to set defaultValue and tooltip.
   *
   * @param builder       ConfigEntryBuilder
   * @param value         current value
   * @param defaultValue  default value
   * @param fieldPath     field path in the record
   * @param label         label component
   * @param tooltip       optional tooltip
   * @return AbstractConfigListEntry
   */
  @SuppressWarnings("unchecked")
  public static AbstractConfigListEntry<?> createEntry(
      ConfigEntryBuilder builder,
      Object value,
      Object defaultValue,
      String fieldPath,
      Consumer<Object> fieldSetter,
      Component label,
      Component... tooltip
  ) {
    if (value == null) return null;

    if (ENTRY_MAP.containsKey(fieldPath)) return ENTRY_MAP.get(fieldPath);

    try {
      @SuppressWarnings("all")
      Method createFieldMethod = PayTpClothConfigGUI.class
          .getDeclaredMethod(
              ENTRY_FACTORY_METHOD_NAME,
              ConfigEntryBuilder.class,
              value.getClass(),
              Component.class
          );
      createFieldMethod.setAccessible(true);

      @SuppressWarnings("unchecked")
      AbstractFieldBuilder<Object, ?, ?> builderObj =
          (AbstractFieldBuilder<Object, ?, ?>) createFieldMethod.invoke(
              null,
              builder,
              value,
              label
          );

      builderObj.setDefaultValue(defaultValue);
      builderObj.setTooltip(tooltip);
      builderObj.setSaveConsumer(fieldSetter);

      var entry = builderObj.build();
      ENTRY_MAP.put(fieldPath, entry);
      INITIAL_VALUE_MAP.put(fieldPath, value);
      entry.setErrorSupplier(() -> getValidationError(fieldPath));
      return entry;

    } catch (NoSuchMethodException e) {
      LOGGER.error(
          "No GUI field for type: {}",
          value.getClass().getSimpleName(),
          e
      );
    } catch (Exception e) {
      LOGGER.error(
          "Error creating GUI entry for type: {}",
          value.getClass().getSimpleName(),
          e
      );
    }

    return builder.startTextDescription(
        Component.literal("ERROR").withStyle(ChatFormatting.RED)
    ).build();
  }

  private static Optional<Component> getValidationError(String fieldPath) {
    Map<String, Object> currentValues = new HashMap<>();
    ENTRY_MAP.forEach((path, entry) -> {
      Object value = entry.getValue();
      if (INITIAL_VALUE_MAP.get(path) instanceof PayTpScript) {
        value = new PayTpScript((String) value);
      }
      currentValues.put(path, value);
    });

    if (!currentValues.equals(validatedValues)) {
      PayTpConfigData data = PayTpConfigMapper.unflattenData(
          currentValues,
          PayTpConfigData.class
      );
      invalidFields = data.validate();
      validatedValues = Map.copyOf(currentValues);
    }

    return invalidFields.contains(fieldPath)
        ? Optional.of(Component.translatable(
            "paytp.config." + fieldPath + ".invalid"
        ))
        : Optional.empty();
  }

  @SuppressWarnings("unused")
  private static AbstractFieldBuilder<?, ?, ?> createField(
      ConfigEntryBuilder builder,
      Integer value,
      Component label
  ) {
    return builder.startIntField(label, value);
  }

  @SuppressWarnings("unused")
  private static AbstractFieldBuilder<?, ?, ?> createField(
      ConfigEntryBuilder builder,
      Double value,
      Component label
  ) {
    return builder.startDoubleField(label, value);
  }

  @SuppressWarnings("unused")
  private static AbstractFieldBuilder<?, ?, ?> createField(
      ConfigEntryBuilder builder,
      Boolean value,
      Component label
  ) {
    return builder.startBooleanToggle(label, value);
  }

  @SuppressWarnings("unused")
  private static AbstractFieldBuilder<?, ?, ?> createField(
      ConfigEntryBuilder builder,
      String value,
      Component label
  ) {
    return builder.startStrField(label, value);
  }

  @SuppressWarnings("unused")
  private static AbstractFieldBuilder<?, ?, ?> createField(
      ConfigEntryBuilder builder,
      PayTpLang value,
      Component label
  ) {
    return builder.startEnumSelector(label, PayTpLang.class, value);
  }

  @SuppressWarnings("unused")
  private static AbstractFieldBuilder<?, ?, ?> createField(
      ConfigEntryBuilder builder,
      PayTpScript value,
      Component label
  ) {
    return SCRIPT_ENTRY_BUILDER.create(builder, value, label);
  }
}
