package com.flwolfy.paytp.modmenu.builder;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.script.PayTpScript;
import com.flwolfy.paytp.data.warp.PayTpWarpPermission;
import com.flwolfy.paytp.modmenu.entry.scalar.PayTpBooleanEntry;
import com.flwolfy.paytp.modmenu.entry.scalar.PayTpEnumEntry;
import com.flwolfy.paytp.modmenu.entry.scalar.PayTpIntegerEntry;
import com.flwolfy.paytp.modmenu.entry.scalar.PayTpLanguageEntry;
import com.flwolfy.paytp.modmenu.entry.scalar.PayTpScriptEntry;
import com.flwolfy.paytp.modmenu.entry.scalar.PayTpStringEntry;
import com.flwolfy.paytp.modmenu.entry.scalar.PayTpUnsupportedEntry;
import com.flwolfy.paytp.modmenu.model.PayTpConfigEditorModel;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.network.chat.Component;

public final class PayTpConfigUiRegistry {

  private final Map<Class<?>, PayTpConfigEntryBuilder> typeBuilders = new HashMap<>();
  private final Map<String, PayTpConfigEntryBuilder> pathBuilders = new HashMap<>();

  public static PayTpConfigUiRegistry createDefault() {
    PayTpConfigUiRegistry registry = new PayTpConfigUiRegistry();
    registry.registerType(boolean.class, PayTpBooleanEntry::new);
    registry.registerType(Boolean.class, PayTpBooleanEntry::new);
    registry.registerType(int.class, context -> new PayTpIntegerEntry(context, null, null));
    registry.registerType(Integer.class, context -> new PayTpIntegerEntry(context, null, null));
    registry.registerType(String.class, PayTpStringEntry::new);
    registry.registerType(PayTpWarpPermission.class, context -> new PayTpEnumEntry(context,
        value -> Component.translatable(
            "paytp.config.warp.serverWarpPermission."
                + value.name().toLowerCase(Locale.ROOT)
        )));
    registry.registerType(PayTpScript.class, PayTpScriptEntry::new);

    registry.registerPath(
        "general.language",
        PayTpLanguageEntry::new
    );
    registry.registerPath(
        "general.safeTeleportRange",
        context -> new PayTpIntegerEntry(context, 1, 64)
    );
    registry.registerPath(
        "request.expireTime",
        context -> new PayTpIntegerEntry(context, 0, null)
    );
    registry.registerPath(
        "back.maxBackStack",
        context -> new PayTpIntegerEntry(context, 1, null)
    );
    registry.registerPath(
        "warp.maxInactiveTicks",
        context -> new PayTpIntegerEntry(context, 0, null)
    );
    registry.registerPath(
        "warp.checkPeriodTicks",
        context -> new PayTpIntegerEntry(context, 1, null)
    );
    registry.registerPath(
        "price.minPrice",
        context -> new PayTpIntegerEntry(context, 0, null)
    );
    registry.registerPath(
        "price.maxPrice",
        context -> new PayTpIntegerEntry(context, 0, null)
    );
    return registry;
  }

  public void registerType(Class<?> type, PayTpConfigEntryBuilder builder) {
    typeBuilders.put(type, builder);
  }

  public void registerPath(String path, PayTpConfigEntryBuilder builder) {
    pathBuilders.put(path, builder);
  }

  public AbstractConfigListEntry<?> build(
      PayTpConfigEditorModel model,
      String path,
      Component resetText,
      boolean suppressErrors
  ) {
    var field = model.field(path);
    PayTpEntryContext context = new PayTpEntryContext(
        model,
        field,
        Component.translatable("paytp.config." + path),
        new Component[]{Component.translatable("paytp.config." + path + ".tooltip")},
        resetText,
        suppressErrors
    );
    PayTpConfigEntryBuilder builder = pathBuilders.get(path);
    if (builder == null) {
      builder = typeBuilders.get(field.rawType());
    }
    if (builder == null && field.rawType().isEnum()) {
      builder = PayTpEnumEntry::new;
    }
    if (builder == null) {
      PayTpMod.LOGGER.error(
          "No Cloth Config entry builder for {} ({})",
          path,
          field.genericType().getTypeName()
      );
      return new PayTpUnsupportedEntry(context);
    }
    return builder.build(context);
  }
}
