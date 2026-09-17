package com.flwolfy.paytp.modmenu.entry.scalar;

import com.flwolfy.paytp.data.lang.PayTpLangManager;
import com.flwolfy.paytp.modmenu.builder.PayTpEntryContext;
import com.flwolfy.paytp.modmenu.entry.common.PayTpControlLayout;
import com.flwolfy.paytp.modmenu.entry.common.PayTpTooltipEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

public final class PayTpLanguageEntry extends PayTpTooltipEntry<String> {

  private final PayTpEntryContext context;
  private final String original;
  private final String defaultValue;
  private final List<String> locales;
  private final Button valueButton;
  private final Button resetButton;

  public PayTpLanguageEntry(PayTpEntryContext context) {
    super(context.label(), context::tooltipValue);
    this.context = context;
    original = (String) context.field().value();
    defaultValue = (String) context.field().defaultValue();
    Set<String> discovered = new HashSet<>(
        PayTpLangManager.getInstance().availableLocales()
    );
    discovered.add(original);
    discovered.add(defaultValue);
    locales = new ArrayList<>(discovered);
    locales.sort(Comparator
        .comparingInt(PayTpLanguageEntry::priority)
        .thenComparing(value -> value));
    valueButton = Button.builder(Component.empty(), ignored -> advance())
        .bounds(0, 0, 0, 20).build();
    resetButton = Button.builder(context.resetText(), ignored -> {
      context.model().set(context.field().path(), defaultValue);
      updateLabel();
    }).bounds(
        0, 0, Minecraft.getInstance().font.width(context.resetText()) + 6, 20
    ).build();
    setErrorSupplier(() -> context.model().error(
        context.field().path(), context.suppressErrors()
    ));
    updateLabel();
  }

  @Override
  public String getValue() {
    return context.model().get(context.field().path(), String.class);
  }

  @Override
  public Optional<String> getDefaultValue() {
    return Optional.of(defaultValue);
  }

  @Override
  public boolean isEdited() {
    return !getValue().equals(original);
  }

  @Override
  public void extractRenderState(
      GuiGraphicsExtractor graphics,
      int index,
      int y,
      int x,
      int entryWidth,
      int entryHeight,
      int mouseX,
      int mouseY,
      boolean hovered,
      float delta
  ) {
    super.extractRenderState(
        graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, delta
    );
    updateLabel();
    graphics.text(
        Minecraft.getInstance().font, getDisplayedFieldName(), x, y + 6, getPreferredTextColor()
    );
    int resetX = PayTpControlLayout.resetX(x, entryWidth, resetButton.getWidth());
    resetButton.setX(resetX);
    resetButton.setY(y);
    resetButton.active = isEditable() && !getValue().equals(defaultValue);
    valueButton.setX(PayTpControlLayout.valueX(x, entryWidth));
    valueButton.setY(y);
    valueButton.setWidth(PayTpControlLayout.valueWidth(resetButton.getWidth()));
    valueButton.active = isEditable();
    valueButton.extractRenderState(graphics, mouseX, mouseY, delta);
    resetButton.extractRenderState(graphics, mouseX, mouseY, delta);
  }

  @Override
  public List<? extends GuiEventListener> children() {
    return List.of(valueButton, resetButton);
  }

  @Override
  public List<? extends NarratableEntry> narratables() {
    return List.of(valueButton, resetButton);
  }

  private void advance() {
    int current = locales.indexOf(getValue());
    context.model().set(context.field().path(), locales.get((current + 1) % locales.size()));
    updateLabel();
  }

  private void updateLabel() {
    valueButton.setMessage(Component.literal(
        PayTpLangManager.getInstance().languageName(getValue())
    ));
  }

  private static int priority(String locale) {
    return "en_us".equals(locale) ? 0 : 1;
  }
}
