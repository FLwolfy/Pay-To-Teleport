package com.flwolfy.paytp.modmenu.entry.scalar;

import com.flwolfy.paytp.modmenu.builder.PayTpEntryContext;
import com.flwolfy.paytp.modmenu.entry.common.PayTpControlLayout;
import com.flwolfy.paytp.modmenu.entry.common.PayTpPendingEntry;
import com.flwolfy.paytp.modmenu.entry.common.PayTpTooltipEntry;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

public final class PayTpStringEntry extends PayTpTooltipEntry<String>
    implements PayTpPendingEntry {

  private final PayTpEntryContext context;
  private final String original;
  private final String defaultValue;
  private final EditBox textField;
  private final Button resetButton;
  private long observedRevision;
  private String observedText;

  public PayTpStringEntry(PayTpEntryContext context) {
    super(context.label(), context::tooltipValue);
    this.context = context;
    original = (String) context.field().value();
    defaultValue = (String) context.field().defaultValue();

    int resetWidth = Minecraft.getInstance().font.width(context.resetText()) + 6;
    textField = new EditBox(
        Minecraft.getInstance().font,
        0,
        0,
        PayTpControlLayout.valueWidth(resetWidth),
        20,
        context.label()
    );
    textField.setValue(original);
    resetButton = Button.builder(context.resetText(), ignored -> textField.setValue(defaultValue))
        .bounds(0, 0, resetWidth, 20)
        .build();

    observedRevision = context.model().revision();
    observedText = original;
  }

  @Override
  public String getValue() {
    return textField.getValue();
  }

  @Override
  public Optional<String> getDefaultValue() {
    return Optional.of(defaultValue);
  }

  @Override
  public Optional<net.minecraft.network.chat.Component> getError() {
    return context.model().error(context.field().path(), context.suppressErrors());
  }

  @Override
  public boolean isEdited() {
    return !getValue().equals(original);
  }

  @Override
  public void extractRenderState(
      GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth,
      int entryHeight, int mouseX, int mouseY, boolean hovered, float delta
  ) {
    synchronize();
    super.extractRenderState(
        graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, delta
    );
    graphics.text(
        Minecraft.getInstance().font, getDisplayedFieldName(), x, y + 6, getPreferredTextColor()
    );
    int resetX = PayTpControlLayout.resetX(x, entryWidth, resetButton.getWidth());
    resetButton.setX(resetX);
    resetButton.setY(y);
    resetButton.active = isEditable() && !getValue().equals(defaultValue);
    textField.setX(PayTpControlLayout.valueX(x, entryWidth));
    textField.setY(y);
    textField.setWidth(PayTpControlLayout.valueWidth(resetButton.getWidth()));
    textField.setEditable(isEditable());
    textField.extractRenderState(graphics, mouseX, mouseY, delta);
    resetButton.extractRenderState(graphics, mouseX, mouseY, delta);
  }

  @Override
  public List<? extends GuiEventListener> children() {
    return List.of(textField, resetButton);
  }

  @Override
  public List<? extends NarratableEntry> narratables() {
    return List.of(textField, resetButton);
  }

  @Override
  public void flush() {
    String current = textField.getValue();
    if (!current.equals(observedText)) {
      observedText = current;
      context.model().set(context.field().path(), current);
      observedRevision = context.model().revision();
    }
  }

  private void synchronize() {
    if (!textField.getValue().equals(observedText)) {
      flush();
    } else if (observedRevision != context.model().revision()) {
      textField.setValue(context.model().get(context.field().path(), String.class));
      observedText = textField.getValue();
      observedRevision = context.model().revision();
    }
  }
}
