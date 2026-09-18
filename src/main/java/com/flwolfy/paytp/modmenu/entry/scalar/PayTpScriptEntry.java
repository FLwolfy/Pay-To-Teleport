package com.flwolfy.paytp.modmenu.entry.scalar;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.script.PayTpScript;
import com.flwolfy.paytp.modmenu.builder.PayTpEntryContext;
import com.flwolfy.paytp.modmenu.entry.common.PayTpControlLayout;
import com.flwolfy.paytp.modmenu.entry.common.PayTpTooltipEntry;
import com.flwolfy.paytp.util.PayTpFileDialog;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PayTpScriptEntry extends PayTpTooltipEntry<PayTpScript> {

  private static final SystemToast.SystemToastId IMPORT_RESULT = new SystemToast.SystemToastId();

  private final PayTpEntryContext context;
  private final PayTpScript original;
  private final PayTpScript defaultValue;
  private final Button editButton;
  private final Button importButton;
  private final Button resetButton;

  public PayTpScriptEntry(PayTpEntryContext context) {
    super(context.label(), context::tooltipValue);
    this.context = context;
    original = (PayTpScript) context.field().value();
    defaultValue = (PayTpScript) context.field().defaultValue();
    Component editText = Component.translatable("paytp.config.price.algorithm.edit");
    editButton = Button.builder(editText, ignored -> editScript())
        .bounds(0, 0, 0, 20)
        .tooltip(Tooltip.create(Component.translatable(
            "paytp.config.price.algorithm.edit.tooltip"
        )))
        .build();
    Component importText = Component.translatable("paytp.config.price.algorithm.import");
    importButton = Button.builder(importText, ignored -> importScript())
        .bounds(0, 0, 0, 20)
        .tooltip(Tooltip.create(Component.translatable(
            "paytp.config.price.algorithm.import.tooltip"
        )))
        .build();
    resetButton = Button.builder(context.resetText(), ignored -> {
      context.model().set(context.field().path(), defaultValue);
    }).bounds(
        0, 0, Minecraft.getInstance().font.width(context.resetText()) + 6, 20
    ).build();
    setErrorSupplier(() -> context.model().error(
        context.field().path(), context.suppressErrors()
    ));
  }

  @Override
  public PayTpScript getValue() {
    return context.model().get(context.field().path(), PayTpScript.class);
  }

  @Override
  public Optional<PayTpScript> getDefaultValue() {
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
    graphics.text(
        Minecraft.getInstance().font, getDisplayedFieldName(), x, y + 6, getPreferredTextColor()
    );
    int resetX = PayTpControlLayout.resetX(x, entryWidth, resetButton.getWidth());
    int valueX = PayTpControlLayout.valueX(x, entryWidth);
    int valueWidth = PayTpControlLayout.valueWidth(resetButton.getWidth());
    int buttonWidth = (valueWidth - PayTpControlLayout.gap()) / 2;
    editButton.setX(valueX);
    editButton.setY(y);
    editButton.setWidth(buttonWidth);
    importButton.setX(valueX + buttonWidth + PayTpControlLayout.gap());
    importButton.setY(y);
    importButton.setWidth(valueWidth - buttonWidth - PayTpControlLayout.gap());
    resetButton.setX(resetX);
    resetButton.setY(y);
    editButton.active = isEditable();
    importButton.active = isEditable();
    resetButton.active = isEditable() && !getValue().equals(defaultValue);
    editButton.extractRenderState(graphics, mouseX, mouseY, delta);
    importButton.extractRenderState(graphics, mouseX, mouseY, delta);
    resetButton.extractRenderState(graphics, mouseX, mouseY, delta);
  }

  @Override
  public List<? extends GuiEventListener> children() {
    return List.of(editButton, importButton, resetButton);
  }

  @Override
  public List<? extends NarratableEntry> narratables() {
    return List.of(editButton, importButton, resetButton);
  }

  private void editScript() {
    Minecraft.getInstance().setScreenAndShow(new ScriptEditor(
        getConfigScreen(),
        getValue().source(),
        source -> context.model().set(context.field().path(), new PayTpScript(source))
    ));
  }

  private void importScript() {
    PayTpFileDialog.openFile(
        "Import PayTp JEXL Algorithm",
        "JEXL scripts",
        "jexl",
        Minecraft.getInstance().getWindow().handle(),
        this::loadScriptFrom
    );
  }

  private void loadScriptFrom(String path) {
    if (path == null) {
      return;
    }

    try {
      context.model().set(context.field().path(), new PayTpScript(Files.readString(Path.of(path))));
    } catch (Exception exception) {
      PayTpMod.LOGGER.error("Failed to import PayTp JEXL script", exception);
      SystemToast.add(
          Minecraft.getInstance().gui.toastManager(),
          IMPORT_RESULT,
          Component.translatable("paytp.config.price.algorithm.import.failed"),
          Component.literal(exception.getMessage() == null
              ? exception.getClass().getSimpleName() : exception.getMessage())
      );
    }
  }

  private static final class ScriptEditor extends Screen {

    private static final int MARGIN = 20;
    private static final int HEADER_HEIGHT = 32;
    private static final int FOOTER_HEIGHT = 40;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_SPACING = 4;

    private final Screen parent;
    private final String initialSource;
    private final Consumer<String> confirmConsumer;
    private Button confirmButton;

    private ScriptEditor(
        Screen parent,
        String initialSource,
        Consumer<String> confirmConsumer
    ) {
      super(Component.translatable("paytp.config.price.algorithm.editor.title"));
      this.parent = parent;
      this.initialSource = initialSource;
      this.confirmConsumer = confirmConsumer;
    }

    @Override
    protected void init() {
      MultiLineEditBox script = MultiLineEditBox.builder()
          .setX(MARGIN)
          .setY(HEADER_HEIGHT)
          .setShowBackground(true)
          .setShowDecorations(true)
          .build(
              font,
              width - MARGIN * 2,
              height - HEADER_HEIGHT - FOOTER_HEIGHT,
              title
          );
      script.setValue(initialSource);
      addRenderableWidget(script);

      addRenderableWidget(Button.builder(
          Component.translatable("paytp.config.price.algorithm.editor.cancel"),
          ignored -> onClose()
      ).bounds(
          width / 2 - BUTTON_WIDTH - BUTTON_SPACING / 2,
          height - 30,
          BUTTON_WIDTH,
          20
      ).build());

      confirmButton = addRenderableWidget(Button.builder(
          Component.translatable("paytp.config.price.algorithm.editor.confirm"),
          ignored -> {
            confirmConsumer.accept(script.getValue());
            onClose();
          }
      ).bounds(
          width / 2 + BUTTON_SPACING / 2,
          height - 30,
          BUTTON_WIDTH,
          20
      ).build());
      confirmButton.active = false;
      script.setValueListener(source -> confirmButton.active = !initialSource.equals(source));
      setInitialFocus(script);
    }

    @Override
    public void extractRenderState(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float delta
    ) {
      super.extractRenderState(graphics, mouseX, mouseY, delta);
      graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
      minecraft.setScreenAndShow(parent);
    }
  }
}
