package com.flwolfy.paytp.modmenu.screen;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.config.PayTpConfigManager;
import com.flwolfy.paytp.modmenu.model.PayTpConfigEditorModel;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PayTpClothConfigScreenController {

  private static final SystemToast.SystemToastId SAVE_RESULT = new SystemToast.SystemToastId();
  private static final SystemToast.SystemToastId WORLD_WARNING = new SystemToast.SystemToastId();

  private PayTpClothConfigScreenController() {}

  public static Screen create(Screen parent) {
    showWorldWarning();
    ConfigBuilder builder = ConfigBuilder.create()
        .setParentScreen(parent)
        .setTitle(Component.translatable("paytp.config.title"))
        .setDoesConfirmSave(true);
    PayTpClothConfigLayout layout = new PayTpClothConfigLayout(
        builder,
        PayTpConfigManager.getInstance().loadForEditing()
    );
    layout.build();
    builder.setSavingRunnable(() -> {
      layout.flush();
      save(layout.model());
    });
    return builder.build();
  }

  private static void showWorldWarning() {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.level == null) {
      return;
    }
    SystemToast.add(
        minecraft.getToastManager(),
        WORLD_WARNING,
        Component.translatable("paytp.config.world_warning"),
        Component.translatable(minecraft.getSingleplayerServer() == null
            ? "paytp.config.world_warning.remote"
            : "paytp.config.world_warning.local")
    );
  }

  private static void save(PayTpConfigEditorModel model) {
    try {
      if (!model.invalidFields().isEmpty()) {
        throw new IllegalArgumentException(String.join(", ", model.invalidFields()));
      }
      if (!PayTpConfigManager.getInstance().savePending(model.build())) {
        throw new IllegalStateException("The configuration could not be saved");
      }
      Minecraft minecraft = Minecraft.getInstance();
      String detail = minecraft.level == null
          ? "paytp.config.save_success.detail"
          : minecraft.getSingleplayerServer() == null
              ? "paytp.config.save_success.remote_detail"
              : "paytp.config.save_success.world_detail";
      SystemToast.add(
          minecraft.getToastManager(),
          SAVE_RESULT,
          Component.translatable("paytp.config.save_success"),
          Component.translatable(detail)
      );
    } catch (Exception exception) {
      PayTpMod.LOGGER.error("Failed to save PayTp client configuration", exception);
      SystemToast.add(
          Minecraft.getInstance().getToastManager(),
          SAVE_RESULT,
          Component.translatable("paytp.config.save_failed"),
          Component.literal(exception.getMessage() == null
              ? exception.getClass().getSimpleName() : exception.getMessage())
      );
    }
  }
}
