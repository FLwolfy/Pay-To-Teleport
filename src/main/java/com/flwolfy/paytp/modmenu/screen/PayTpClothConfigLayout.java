package com.flwolfy.paytp.modmenu.screen;

import com.flwolfy.paytp.data.config.PayTpConfigData;
import com.flwolfy.paytp.modmenu.builder.PayTpConfigUiRegistry;
import com.flwolfy.paytp.modmenu.entry.common.PayTpAllCategoryEntry;
import com.flwolfy.paytp.modmenu.entry.common.PayTpPendingEntry;
import com.flwolfy.paytp.modmenu.entry.common.PayTpSubCategoryEntry;
import com.flwolfy.paytp.modmenu.model.PayTpConfigEditorModel;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class PayTpClothConfigLayout {

  private static final String BASE_KEY = "paytp.config.";

  private final ConfigBuilder builder;
  private final ConfigEntryBuilder entries;
  private final PayTpConfigEditorModel model;
  private final PayTpConfigUiRegistry registry;
  private final List<PayTpPendingEntry> pendingEntries = new ArrayList<>();

  public PayTpClothConfigLayout(ConfigBuilder builder, PayTpConfigData current) {
    this.builder = builder;
    entries = builder.entryBuilder();
    model = new PayTpConfigEditorModel(current, PayTpConfigData.DEFAULT);
    registry = PayTpConfigUiRegistry.createDefault();
  }

  public void build() {
    ConfigCategory all = builder.getOrCreateCategory(Component.translatable(BASE_KEY + "all"));
    all.addEntry(entries.startTextDescription(
        Component.translatable(BASE_KEY + "local_only")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)
    ).build());

    for (RecordComponent component : PayTpConfigData.class.getRecordComponents()) {
      String path = component.getName();
      if (component.getType().isRecord()) {
        ConfigCategory category = builder.getOrCreateCategory(
            Component.translatable(BASE_KEY + path)
        );
        buildRecordCategory(category, all, component.getType(), path);
      }
    }
  }

  public PayTpConfigEditorModel model() {
    return model;
  }

  public void flush() {
    pendingEntries.forEach(PayTpPendingEntry::flush);
  }

  private void buildRecordCategory(
      ConfigCategory category,
      ConfigCategory all,
      Class<?> recordType,
      String path
  ) {
    List<AbstractConfigListEntry<?>> categoryEntries = recordEntries(
        recordType, path, false
    );
    categoryEntries.forEach(category::addEntry);
    List<AbstractConfigListEntry<?>> allEntries = recordEntries(
        recordType, path, true
    );
    all.addEntry(new PayTpAllCategoryEntry(
        entries,
        Component.translatable(BASE_KEY + path)
            .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
        allEntries
    ));
  }

  private List<AbstractConfigListEntry<?>> recordEntries(
      Class<?> recordType,
      String prefix,
      boolean suppressErrors
  ) {
    List<AbstractConfigListEntry<?>> result = new ArrayList<>();
    for (RecordComponent component : recordType.getRecordComponents()) {
      String path = prefix + "." + component.getName();
      if (component.getType().isRecord()) {
        List<AbstractConfigListEntry<?>> nested = recordEntries(
            component.getType(), path, suppressErrors
        );
        result.add(new PayTpSubCategoryEntry(
            entries,
            Component.translatable(BASE_KEY + path),
            nested,
            true,
            suppressErrors
        ));
      } else {
        result.add(buildEntry(path, suppressErrors));
      }
    }
    return result;
  }

  private AbstractConfigListEntry<?> buildEntry(String path, boolean suppressErrors) {
    AbstractConfigListEntry<?> entry = registry.build(
        model, path, entries.getResetButtonKey(), suppressErrors
    );
    if (entry instanceof PayTpPendingEntry pending) {
      pendingEntries.add(pending);
    }
    return entry;
  }
}
