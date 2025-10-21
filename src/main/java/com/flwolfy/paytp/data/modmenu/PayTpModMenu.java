package com.flwolfy.paytp.data.modmenu;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.config.PayTpConfigData;
import com.flwolfy.paytp.data.config.PayTpConfigManager;

import java.util.function.Supplier;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import net.minecraft.text.Style;
import net.minecraft.text.Text;

import net.minecraft.util.Formatting;
import org.slf4j.Logger;

public class PayTpModMenu implements ModMenuApi {

  private static final Logger LOGGER = PayTpMod.LOGGER;
  private static final Style DEFAULT_WARN_STYLE = Style.EMPTY.withColor(Formatting.GOLD).withItalic(true);

  private Supplier<PayTpConfigData> dataSupplier;

  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return parent -> {
      ConfigBuilder builder = ConfigBuilder.create()
          .setParentScreen(parent)
          .setTitle(Text.translatable("paytp.config.title"));
      ConfigEntryBuilder entryBuilder = builder.entryBuilder();
      PayTpClothConfigBuilder menuBuilder = new PayTpClothConfigBuilder(builder, entryBuilder);

      PayTpConfigData data = PayTpConfigManager.getInstance().data();
      PayTpConfigData defaultData = PayTpConfigData.DEFAULT;

      dataSupplier = menuBuilder.buildConfigUI(data, defaultData);

      builder.setDoesConfirmSave(true);
      builder.setSavingRunnable(this::saveConfig);

      return builder.build();
    };
  }

  private void saveConfig() {
    PayTpConfigManager.getInstance().update(dataSupplier.get());
    LOGGER.info("Saving PayTpConfig...");
  }
}
