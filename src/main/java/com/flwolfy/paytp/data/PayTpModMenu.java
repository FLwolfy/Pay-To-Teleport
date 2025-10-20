package com.flwolfy.paytp.data;

import com.flwolfy.paytp.flag.PayTpMultiplierFlags;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import net.minecraft.text.Text;

public class PayTpModMenu implements ModMenuApi {

  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return parent -> {
      ConfigBuilder builder = ConfigBuilder.create()
          .setParentScreen(parent)
          .setTitle(Text.translatable("title.paytp.config"));

      builder.setSavingRunnable(this::saveConfig);

      // 分类
      ConfigCategory general = builder.getOrCreateCategory(Text.translatable("category.paytp.general"));
      ConfigEntryBuilder entryBuilder = builder.entryBuilder();

      // 添加条目
      addIntEntries(entryBuilder, general);
      addBooleanEntries(entryBuilder, general);
      addEnumEntries(entryBuilder, general);

      return builder.build();
    };
  }

  // =============================
  // ==== 私有条目方法（dummy）====
  // =============================

  private void addIntEntries(ConfigEntryBuilder entryBuilder, ConfigCategory category) {
    // 这里先用 dummy 值
    category.addEntry(entryBuilder
        .startIntField(Text.literal("Dummy Int"), 123)
        .setDefaultValue(100)
        .setTooltip(Text.literal("This is a dummy int field"))
        .setSaveConsumer(newValue -> {
          // TODO: 保存逻辑
        })
        .build());
  }

  private void addBooleanEntries(ConfigEntryBuilder entryBuilder, ConfigCategory category) {
    category.addEntry(entryBuilder
        .startBooleanToggle(Text.literal("Dummy Boolean"), true)
        .setTooltip(Text.literal("This is a dummy boolean"))
        .setSaveConsumer(newValue -> {
          // TODO: 保存逻辑
        })
        .build());
  }

  private void addEnumEntries(ConfigEntryBuilder entryBuilder, ConfigCategory category) {
    category.addEntry(entryBuilder
        .startEnumSelector(Text.literal("Dummy Enum"), PayTpMultiplierFlags.class, PayTpMultiplierFlags.HOME)
        .setSaveConsumer(newValue -> {
          // TODO: 保存逻辑
        })
        .build());
  }

  private void saveConfig() {
    // TODO: 点击 Done 时保存逻辑
    System.out.println("Saving config...");
  }
}
