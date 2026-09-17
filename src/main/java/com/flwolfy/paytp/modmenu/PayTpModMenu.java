package com.flwolfy.paytp.modmenu;

import com.flwolfy.paytp.modmenu.screen.PayTpClothConfigScreenController;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public final class PayTpModMenu implements ModMenuApi {

  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    if (!FabricLoader.getInstance().isModLoaded("cloth-config2")) {
      return parent -> parent;
    }

    return PayTpClothConfigScreenController::create;
  }
}
