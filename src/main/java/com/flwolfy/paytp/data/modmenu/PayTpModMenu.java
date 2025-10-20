package com.flwolfy.paytp.data.modmenu;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.config.PayTpConfigData;
import com.flwolfy.paytp.data.config.PayTpConfigManager;

import com.flwolfy.paytp.data.config.PayTpConfigMapper;
import com.flwolfy.paytp.data.lang.PayTpLangManager;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import java.util.HashMap;
import java.util.Map;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

public class PayTpModMenu implements ModMenuApi {

  private static final Logger LOGGER = PayTpMod.LOGGER;
  private static final PayTpLangManager LANG_LOADER = PayTpLangManager.getInstance();

  private Map<String, Object> currentFlattenedData = new HashMap<>();

  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return parent -> {
      ConfigBuilder builder = ConfigBuilder.create()
          .setParentScreen(parent)
          .setTitle(LANG_LOADER.getText("paytp.config.title"));

      ConfigEntryBuilder entryBuilder = builder.entryBuilder();
      PayTpConfigData data = PayTpConfigManager.getInstance().data();
      PayTpConfigData defaultData = PayTpConfigData.DEFAULT;

      currentFlattenedData = PayTpConfigMapper.flattenData(data);

      buildConfigUI(builder, entryBuilder, data, defaultData);
      builder.setSavingRunnable(this::saveConfig);

      return builder.build();
    };
  }

  private void buildConfigUI(
      ConfigBuilder builder,
      ConfigEntryBuilder entryBuilder,
      Record record,
      Record defaultRecord
  ) {
    if (record == null) return;

    Class<?> clazz = record.getClass();
    List<AbstractConfigListEntry<?>> otherEntries = new ArrayList<>();
    ConfigCategory allCategory = builder.getOrCreateCategory(LANG_LOADER.getText("paytp.config.all"));

    for (RecordComponent component : clazz.getRecordComponents()) {
      try {
        Method accessor = component.getAccessor();
        Object value = accessor.invoke(record);
        Object defaultValue = accessor.invoke(defaultRecord);

        if (value != null && value.getClass().isRecord()) {
          ConfigCategory category = builder.getOrCreateCategory(LANG_LOADER.getText("paytp.config." + component.getName()));
          buildCategoryUI(entryBuilder, category, (Record) value, (Record) defaultValue, component.getName() + ".");

          SubCategoryBuilder subCatInAllCategory = entryBuilder
              .startSubCategory(LANG_LOADER.getText("paytp.config." + component.getName()))
              .setExpanded(true);

          buildSubCategoryUI(entryBuilder, subCatInAllCategory, (Record) value, (Record) defaultValue, component.getName() + ".");
          allCategory.addEntry(subCatInAllCategory.build());
        } else {
          String warningKey = "paytp.config." + component.getName() + ".warning";
          Text warningText = LANG_LOADER.getText(warningKey).formatted(Formatting.GOLD, Formatting.ITALIC);
          warningText = warningText.getString().equals(warningKey) ? Text.empty() : warningText;
          otherEntries.add(PayTpModMenuGUI.createEntry(
              entryBuilder,
              value,
              defaultValue,
              component.getName(),
              newValue -> currentFlattenedData.put(component.getName(), newValue),
              LANG_LOADER.getText("paytp.config." + component.getName()),
              LANG_LOADER.getText("paytp.config." + component.getName() + ".tooltip"),
              warningText
          ));
        }
      } catch (Exception e) {
        LOGGER.error("Error while creating config entry for type: {}", clazz.getSimpleName(), e);
      }
    }

    if (!otherEntries.isEmpty()) {
      ConfigCategory otherCategory = builder.getOrCreateCategory(LANG_LOADER.getText("paytp.config.other"));
      for (AbstractConfigListEntry<?> entry : otherEntries) {
        otherCategory.addEntry(entry);
      }

      SubCategoryBuilder otherSubCat = entryBuilder.startSubCategory(LANG_LOADER.getText("paytp.config.other")).setExpanded(true);
      otherSubCat.addAll(otherEntries);
      allCategory.addEntry(otherSubCat.build());
    }
  }

  private void buildCategoryUI(
      ConfigEntryBuilder entryBuilder,
      ConfigCategory category,
      Record record,
      Record defaultRecord,
      String prefix
  ) {
    if (record == null) return;
    Class<?> clazz = record.getClass();
    for (RecordComponent component : clazz.getRecordComponents()) {
      try {
        Method accessor = component.getAccessor();
        Object value = accessor.invoke(record);
        Object defaultValue = accessor.invoke(defaultRecord);
        if (value != null && value.getClass().isRecord()) {
          SubCategoryBuilder subCategory = entryBuilder
              .startSubCategory(LANG_LOADER.getText("paytp.config." + component.getName()))
              .setExpanded(true);

          buildSubCategoryUI(entryBuilder, subCategory, (Record) value, (Record) defaultValue, prefix + component.getName() + ".");
          category.addEntry(subCategory.build());
        } else {
          String warningKey = "paytp.config." + component.getName() + ".warning";
          Text warningText = LANG_LOADER.getText(warningKey).formatted(Formatting.GOLD, Formatting.ITALIC);
          warningText = warningText.getString().equals(warningKey) ? Text.empty() : warningText;
          category.addEntry(PayTpModMenuGUI.createEntry(
              entryBuilder,
              value,
              defaultValue,
              prefix + component.getName(),
              newValue -> currentFlattenedData.put(prefix + component.getName(), newValue),
              LANG_LOADER.getText("paytp.config." + component.getName()),
              LANG_LOADER.getText("paytp.config." + component.getName() + ".tooltip"),
              warningText
          ));
        }
      } catch (Exception e) {
        LOGGER.error("Error while creating category entry for type: {}", clazz.getSimpleName(), e);
      }
    }
  }

  private void buildSubCategoryUI(
      ConfigEntryBuilder entryBuilder,
      SubCategoryBuilder subCatBuilder,
      Record record,
      Record defaultRecord,
      String prefix
  ) {
    if (record == null) return;
    Class<?> clazz = record.getClass();
    for (RecordComponent component : clazz.getRecordComponents()) {
      try {
        Method accessor = component.getAccessor();
        Object value = accessor.invoke(record);
        Object defaultValue = accessor.invoke(defaultRecord);
        if (value != null && value.getClass().isRecord()) {
          subCatBuilder.add(entryBuilder.startTextDescription(
                  LANG_LOADER.getText("paytp.config." + component.getName()).formatted(
                      Formatting.YELLOW, Formatting.BOLD))
              .build());
          List<AbstractConfigListEntry<?>> sectionEntries = new ArrayList<>();
          buildSectionUI(entryBuilder, sectionEntries, (Record) value, (Record) defaultValue, prefix + component.getName() + ".");
          subCatBuilder.addAll(sectionEntries);
        } else {
          String warningKey = "paytp.config." + component.getName() + ".warning";
          Text warningText = LANG_LOADER.getText(warningKey).formatted(Formatting.GOLD, Formatting.ITALIC);
          warningText = warningText.getString().equals(warningKey) ? Text.empty() : warningText;
          subCatBuilder.add(PayTpModMenuGUI.createEntry(
              entryBuilder,
              value,
              defaultValue,
              prefix + component.getName(),
              newValue -> currentFlattenedData.put(prefix + component.getName(), newValue),
              LANG_LOADER.getText("paytp.config." + component.getName()),
              LANG_LOADER.getText("paytp.config." + component.getName() + ".tooltip"),
              warningText
          ));
        }
      } catch (Exception e) {
        LOGGER.error("Error while creating sub category entry for type: {}", clazz.getSimpleName(), e);
      }
    }
  }

  private void buildSectionUI(
      ConfigEntryBuilder entryBuilder,
      List<AbstractConfigListEntry<?>> sectionEntries,
      Record record,
      Record defaultRecord,
      String prefix
  ) {
    if (record == null) return;
    Class<?> clazz = record.getClass();
    for (RecordComponent component : clazz.getRecordComponents()) {
      try {
        Method accessor = component.getAccessor();
        Object value = accessor.invoke(record);
        Object defaultValue = accessor.invoke(defaultRecord);
        if (value != null && value.getClass().isRecord()) {
          buildSectionUI(entryBuilder, sectionEntries, (Record) value, (Record) defaultValue, prefix + component.getName() + ".");
        } else {
          String warningKey = "paytp.config." + component.getName() + ".warning";
          Text warningText = LANG_LOADER.getText(warningKey).formatted(Formatting.GOLD, Formatting.ITALIC);
          warningText = warningText.getString().equals(warningKey) ? Text.empty() : warningText;
          sectionEntries.add(PayTpModMenuGUI.createEntry(
              entryBuilder,
              value,
              defaultValue,
              prefix + component.getName(),
              newValue -> currentFlattenedData.put(prefix + component.getName(), newValue),
              LANG_LOADER.getText("paytp.config." + component.getName()),
              LANG_LOADER.getText("paytp.config." + component.getName() + ".tooltip"),
              warningText
          ));
        }
      } catch (Exception e) {
        LOGGER.error("Error while creating sub category entry for type: {}", clazz.getSimpleName(), e);
      }
    }
  }

  private void saveConfig() {
    PayTpConfigManager.getInstance().update(PayTpConfigMapper.unflattenData(PayTpConfigData.class, currentFlattenedData));
    LOGGER.info("Saving PayTpConfig...");
  }
}
