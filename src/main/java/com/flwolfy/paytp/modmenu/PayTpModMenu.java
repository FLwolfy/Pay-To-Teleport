package com.flwolfy.paytp.modmenu;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.config.PayTpConfigData;
import com.flwolfy.paytp.data.config.PayTpConfigManager;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

import org.slf4j.Logger;

public class PayTpModMenu implements ModMenuApi {

  private static final Logger LOGGER = PayTpMod.LOGGER;

  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return parent -> {
      ConfigBuilder builder = ConfigBuilder.create()
          .setParentScreen(parent)
          .setTitle(Component.translatable("paytp.config.title"));

      ConfigEntryBuilder entryBuilder = builder.entryBuilder();
      PayTpClothConfigBuilder menuBuilder = new PayTpClothConfigBuilder(builder, entryBuilder);

      PayTpConfigData currentData = PayTpConfigManager.getInstance().data();
      PayTpConfigData defaultData = PayTpConfigData.DEFAULT;
      Supplier<PayTpConfigData> dataSupplier = menuBuilder.buildConfigUI(currentData, defaultData);

      builder.setDoesConfirmSave(true);
      builder.setSavingRunnable(() -> {
        try {
          PayTpConfigData newData = dataSupplier.get();
          if (newData != null) {
            PayTpConfigManager.getInstance().update(newData);
            LOGGER.info("Config saved successfully with changes");
          } else {
            LOGGER.error("Failed to get config data from UI - dataSupplier returned null");
          }
        } catch (Exception e) {
          LOGGER.error("Error occurred while saving config", e);
        }
      });

      return builder.build();
    };
  }
}