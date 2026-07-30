package com.flwolfy.paytp.modmenu;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.config.PayTpConfigMapper;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import org.slf4j.Logger;

/**
 * Reflectively converts the nested PayTp configuration records into Cloth Config categories.
 */
public class PayTpClothConfigBuilder {

  private static final Logger LOGGER = PayTpMod.LOGGER;
  private static final Style DEFAULT_TITLE_STYLE = Style.EMPTY.withColor(ChatFormatting.YELLOW).withBold(true);
  private static final Style DEFAULT_WARN_STYLE = Style.EMPTY.withColor(ChatFormatting.GOLD).withItalic(true);
  private static final String BASE_KEY = "paytp.config.";

  private final ConfigBuilder builder;
  private final ConfigEntryBuilder entryBuilder;
  private final Map<String, Object> currentFlattenedData;

  /**
   * Creates a configuration UI builder bound to the supplied Cloth Config objects.
   *
   * @param builder the owning config screen builder
   * @param entryBuilder the factory used to create individual entries
   */
  public PayTpClothConfigBuilder(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
    this.builder = builder;
    this.entryBuilder = entryBuilder;
    this.currentFlattenedData = new ConcurrentHashMap<>();
  }

  /**
   * Populates the configuration screen and returns a supplier for the edited record.
   *
   * @param data the current configuration record
   * @param defaultData the matching default configuration record
   * @param <T> the root record type
   * @return a supplier that reconstructs the edited record, or {@code null} when data is null
   */
  @SuppressWarnings("unchecked")
  public <T extends Record> Supplier<T> buildConfigUI(T data, T defaultData) {
    if (data == null) return null;

    PayTpClothConfigGUI.clearCache();
    currentFlattenedData.clear();
    currentFlattenedData.putAll(PayTpConfigMapper.flattenData(data));

    Class<?> clazz = data.getClass();
    List<AbstractConfigListEntry<?>> otherEntries = new ArrayList<>();
    ConfigCategory allCategory = builder.getOrCreateCategory(Component.translatable(BASE_KEY + "all"));

    String warningKey = BASE_KEY + "title.warning";
    MutableComponent warningText = Component.translatable(warningKey).setStyle(DEFAULT_WARN_STYLE);
    if (!warningText.getString().equals(warningKey)) {
      allCategory.addEntry(entryBuilder.startTextDescription(warningText).build());
    }

    for (RecordComponent component : clazz.getRecordComponents()) {
      processComponent(data, defaultData, component, allCategory, otherEntries);
    }

    if (!otherEntries.isEmpty()) {
      ConfigCategory otherCategory = builder.getOrCreateCategory(Component.translatable(BASE_KEY + "other"));
      otherEntries.forEach(otherCategory::addEntry);

      String otherWarningKey = BASE_KEY + "other.warning";
      MutableComponent otherWarningText = Component.translatable(otherWarningKey).setStyle(DEFAULT_WARN_STYLE);
      if (!otherWarningText.getString().equals(otherWarningKey)) {
        otherCategory.addEntry(entryBuilder.startTextDescription(otherWarningText).build());
      }

      SubCategoryBuilder otherSubCat = entryBuilder.startSubCategory(Component.translatable(BASE_KEY + "other"))
          .setExpanded(true);
      otherSubCat.addAll(otherEntries);
      allCategory.addEntry(new AllCategoryEntry(
          Component.translatable(BASE_KEY + "other"),
          otherSubCat
      ));
    }

    return () -> {
      PayTpClothConfigGUI.clearCache();
      return (T) PayTpConfigMapper.unflattenData(currentFlattenedData, data.getClass());
    };
  }

  private void processComponent(Record record, Record defaultRecord, RecordComponent component,
      ConfigCategory allCategory, List<AbstractConfigListEntry<?>> otherEntries) {
    try {
      Method accessor = component.getAccessor();
      Object value = accessor.invoke(record);
      Object defaultValue = accessor.invoke(defaultRecord);

      if (value != null && value.getClass().isRecord()) {
        processRecordComponent(value, defaultValue, component, allCategory);
      } else {
        otherEntries.add(createEntry(value, defaultValue, component, ""));
      }
    } catch (Exception e) {
      LOGGER.error("Error while processing component: {}", component.getName(), e);
    }
  }

  private void processRecordComponent(
      Object value,
      Object defaultValue,
      RecordComponent component,
      ConfigCategory allCategory
  ) {
    String key = component.getName();
    ConfigCategory category = builder.getOrCreateCategory(Component.translatable(BASE_KEY + key));

    String warningKey = BASE_KEY + key + ".warning";
    MutableComponent warningText = Component.translatable(warningKey).setStyle(DEFAULT_WARN_STYLE);
    if (!warningText.getString().equals(warningKey)) {
      category.addEntry(entryBuilder.startTextDescription(warningText).build());
    }

    processCategory(category, (Record) value, (Record) defaultValue, key + ".");

    SubCategoryBuilder subCatInAllCategory = entryBuilder
        .startSubCategory(Component.translatable(BASE_KEY + key))
        .setExpanded(true);
    processSubCategory(subCatInAllCategory, (Record) value, (Record) defaultValue, key + ".");
    allCategory.addEntry(new AllCategoryEntry(
        Component.translatable(BASE_KEY + key),
        subCatInAllCategory
    ));
  }

  private void processCategory(ConfigCategory category, Record record, Record defaultRecord, String prefix) {
    for (RecordComponent component : record.getClass().getRecordComponents()) {
      try {
        Method accessor = component.getAccessor();
        Object value = accessor.invoke(record);
        Object defaultValue = accessor.invoke(defaultRecord);

        if (value != null && value.getClass().isRecord()) {
          SubCategoryBuilder subCategory = entryBuilder
              .startSubCategory(Component.translatable(BASE_KEY + prefix + component.getName()))
              .setExpanded(true);
          processSubCategory(subCategory, (Record) value, (Record) defaultValue, prefix + component.getName() + ".");
          category.addEntry(subCategory.build());
        } else {
          category.addEntry(createEntry(value, defaultValue, component, prefix));
        }
      } catch (Exception e) {
        LOGGER.error("Error while building category UI for component: {}", component.getName(), e);
      }
    }
  }

  private void processSubCategory(SubCategoryBuilder subCatBuilder, Record record, Record defaultRecord, String prefix) {
    for (RecordComponent component : record.getClass().getRecordComponents()) {
      try {
        Method accessor = component.getAccessor();
        Object value = accessor.invoke(record);
        Object defaultValue = accessor.invoke(defaultRecord);

        if (value != null && value.getClass().isRecord()) {
          subCatBuilder.add(entryBuilder.startTextDescription(
              Component.translatable(BASE_KEY + prefix + component.getName()).setStyle(DEFAULT_TITLE_STYLE)).build()
          );
          List<AbstractConfigListEntry<?>> sectionEntries = new ArrayList<>();
          processSection(sectionEntries, (Record) value, (Record) defaultValue, prefix + component.getName() + ".");
          subCatBuilder.addAll(sectionEntries);
        } else {
          subCatBuilder.add(createEntry(value, defaultValue, component, prefix));
        }
      } catch (Exception e) {
        LOGGER.error("Error while building subcategory UI for component: {}", component.getName(), e);
      }
    }
  }

  private void processSection(List<AbstractConfigListEntry<?>> sectionEntries, Record record, Record defaultRecord, String prefix) {
    for (RecordComponent component : record.getClass().getRecordComponents()) {
      try {
        Method accessor = component.getAccessor();
        Object value = accessor.invoke(record);
        Object defaultValue = accessor.invoke(defaultRecord);

        if (value != null && value.getClass().isRecord()) {
          processSection(sectionEntries, (Record) value, (Record) defaultValue, prefix + component.getName() + ".");
        } else {
          sectionEntries.add(createEntry(value, defaultValue, component, prefix));
        }
      } catch (Exception e) {
        LOGGER.error("Error while building section UI for component: {}", component.getName(), e);
      }
    }
  }

  private AbstractConfigListEntry<?> createEntry(
      Object value,
      Object defaultValue,
      RecordComponent component,
      String prefix
  ) {
    return PayTpClothConfigGUI.createEntry(
        entryBuilder,
        value,
        defaultValue,
        prefix + component.getName(),
        newValue -> currentFlattenedData.put(prefix + component.getName(), newValue),
        Component.translatable(BASE_KEY + prefix + component.getName()),
        Component.translatable(BASE_KEY + prefix + component.getName() + ".tooltip")
    );
  }

  /**
   * Prevents the All category from reporting errors already reported by each field's own category.
   */
  private static class AllCategoryEntry extends SubCategoryListEntry {

    private AllCategoryEntry(
        Component label,
        List<AbstractConfigListEntry> entries
    ) {
      super(label, entries, true);
    }

    @Override
    public Optional<Component> getError() {
      return Optional.empty();
    }
  }
}
