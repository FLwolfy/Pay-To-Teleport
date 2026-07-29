package com.flwolfy.paytp.modmenu;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.config.PayTpConfigData;
import com.flwolfy.paytp.data.config.PayTpConfigMapper;
import com.flwolfy.paytp.data.warp.PayTpWarpPermission;
import com.flwolfy.paytp.data.lang.PayTpLang;
import com.flwolfy.paytp.data.script.PayTpScript;

import com.flwolfy.paytp.modmenu.entrybuilder.PayTpEntryBuilderBase;
import com.flwolfy.paytp.modmenu.entrybuilder.PayTpLangEntryBuider;
import com.flwolfy.paytp.modmenu.entrybuilder.PayTpScriptEntryBuilder;
import com.flwolfy.paytp.modmenu.entrybuilder.PayTpWarpPermissionEntryBuilder;
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
  private static final Map<String, AbstractConfigListEntry<?>> ENTRY_MAP = new HashMap<>();
  private static final Map<String, Object> INITIAL_VALUE_MAP = new HashMap<>();
  private static final Map<Class<?>, PayTpEntryBuilderBase<?>> ENTRY_BUILDERS = new HashMap<>();

  private static Map<String, Object> validatedValues = Map.of();
  private static List<String> invalidFields = List.of();

  static {
    registerBuiltInEntryBuilders();

    // =======================================
    // Register customized entry builder here
    // =======================================
    registerEntryBuilder(PayTpLang.class, new PayTpLangEntryBuider());
    registerEntryBuilder(PayTpScript.class, new PayTpScriptEntryBuilder());
    registerEntryBuilder(
        PayTpWarpPermission.class,
        new PayTpWarpPermissionEntryBuilder()
    );
    // =======================================
  }

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
   * Creates a configuration entry using the builder registered for its value type.
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
      AbstractFieldBuilder<Object, ?, ?> builderObj =
          (AbstractFieldBuilder<Object, ?, ?>) createField(
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

    } catch (IllegalArgumentException e) {
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

  @SuppressWarnings("unchecked")
  private static AbstractFieldBuilder<?, ?, ?> createField(
      ConfigEntryBuilder builder,
      Object value,
      Component label
  ) {
    PayTpEntryBuilderBase<Object> entryBuilder =
        (PayTpEntryBuilderBase<Object>) ENTRY_BUILDERS.get(value.getClass());
    if (entryBuilder == null) {
      throw new IllegalArgumentException(
          "No entry builder registered for " + value.getClass().getName()
      );
    }
    return entryBuilder.create(builder, value, label);
  }

  private static void registerBuiltInEntryBuilders() {
    registerEntryBuilder(Integer.class, new PayTpEntryBuilderBase<>() {
      @Override
      public AbstractFieldBuilder<Integer, ?, ?> create(
          ConfigEntryBuilder builder,
          Integer value,
          Component label
      ) {
        return builder.startIntField(label, value);
      }
    });
    registerEntryBuilder(Double.class, new PayTpEntryBuilderBase<>() {
      @Override
      public AbstractFieldBuilder<Double, ?, ?> create(
          ConfigEntryBuilder builder,
          Double value,
          Component label
      ) {
        return builder.startDoubleField(label, value);
      }
    });
    registerEntryBuilder(Boolean.class, new PayTpEntryBuilderBase<>() {
      @Override
      public AbstractFieldBuilder<Boolean, ?, ?> create(
          ConfigEntryBuilder builder,
          Boolean value,
          Component label
      ) {
        return builder.startBooleanToggle(label, value);
      }
    });
    registerEntryBuilder(String.class, new PayTpEntryBuilderBase<>() {
      @Override
      public AbstractFieldBuilder<String, ?, ?> create(
          ConfigEntryBuilder builder,
          String value,
          Component label
      ) {
        return builder.startStrField(label, value);
      }
    });
  }

  private static <T> void registerEntryBuilder(
      Class<T> valueType,
      PayTpEntryBuilderBase<T> entryBuilder
  ) {
    ENTRY_BUILDERS.put(valueType, entryBuilder);
  }
}
