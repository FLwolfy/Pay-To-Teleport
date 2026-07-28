package com.flwolfy.paytp.modmenu.entrybuilder;

import com.flwolfy.paytp.data.script.PayTpScript;
import com.flwolfy.paytp.util.PayTpCalculator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public class PayTpScriptEntryBuilder {

  public AbstractFieldBuilder<?, ?, ?> create(
      ConfigEntryBuilder builder,
      PayTpScript value,
      Component label
  ) {
    return new ScriptFieldBuilder(builder, value, label);
  }

  private static class ScriptFieldBuilder extends AbstractFieldBuilder<
      PayTpScript,
      ScriptEntry,
      ScriptFieldBuilder
  > {

    private ScriptFieldBuilder(
        ConfigEntryBuilder builder,
        PayTpScript value,
        Component label
    ) {
      super(builder.getResetButtonKey(), label);
      this.value = Objects.requireNonNull(value);
    }

    @Override
    public ScriptEntry build() {
      return finishBuilding(new ScriptEntry(
          getFieldNameKey(),
          value,
          getDefaultValue().get(),
          getSaveConsumer(),
          getResetButtonKey(),
          getTooltipSupplier()
              .apply(value)
              .orElseGet(() -> new Component[0])
      ));
    }
  }

  private static class ScriptEntry extends StringListEntry {

    private static final int BUTTON_SPACING = 4;

    private final Button editButton;
    private final Button importButton;
    private Optional<Component> validationError = Optional.empty();
    private String validatedSource;

    private ScriptEntry(
        Component fieldName,
        PayTpScript script,
        PayTpScript defaultScript,
        Consumer<PayTpScript> saveConsumer,
        Component resetButton,
        Component... tooltip
    ) {
      super(
          fieldName,
          script.source(),
          resetButton,
          defaultScript::source,
          source -> saveConsumer.accept(new PayTpScript(source)),
          () -> Optional.of(tooltip),
          false
      );

      Component editText = Component.translatable(
          "paytp.config.price.algorithm.edit"
      );
      editButton = Button.builder(editText, button -> editScript())
          .size(Minecraft.getInstance().font.width(editText) + 6, 20)
          .tooltip(Tooltip.create(Component.translatable(
              "paytp.config.price.algorithm.edit.tooltip"
          )))
          .build();

      Component importText = Component.translatable(
          "paytp.config.price.algorithm.import"
      );
      importButton = Button.builder(importText, button -> importScript())
          .size(Minecraft.getInstance().font.width(importText) + 6, 20)
          .tooltip(Tooltip.create(Component.translatable(
              "paytp.config.price.algorithm.import.tooltip"
          )))
          .build();

      textFieldWidget.visible = false;
      widgets.add(0, editButton);
      widgets.add(0, importButton);
      setErrorSupplier(() -> validationError);
      validate();
    }

    @Override
    public Optional<Component> getError() {
      if (!getValue().equals(validatedSource)) {
        validate();
      }
      return super.getError();
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
        float tickDelta
    ) {
      resetButton.visible = false;
      textFieldWidget.visible = false;
      try {
        super.extractRenderState(
            graphics,
            index,
            y,
            x,
            entryWidth,
            entryHeight,
            mouseX,
            mouseY,
            hovered,
            tickDelta
        );
      } finally {
        resetButton.visible = true;
      }

      int buttonWidth =
          (textFieldWidget.getWidth() - BUTTON_SPACING) / 2;
      editButton.setWidth(buttonWidth);
      importButton.setWidth(buttonWidth);
      editButton.setX(textFieldWidget.getX());
      importButton.setX(
          editButton.getX() + editButton.getWidth() + BUTTON_SPACING
      );
      editButton.setY(textFieldWidget.getY());
      importButton.setY(textFieldWidget.getY());
      editButton.active = isEditable();
      importButton.active = isEditable();
      editButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
      importButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
      resetButton.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    private void editScript() {
      Minecraft.getInstance().setScreenAndShow(
          new ScriptEditor(getConfigScreen(), getValue(), source -> {
            setValue(source);
            validate();
          })
      );
    }

    private void importScript() {
      String path;
      try (MemoryStack stack = MemoryStack.stackPush()) {
        PointerBuffer filters = stack.mallocPointer(1);
        filters.put(stack.UTF8("*.jexl"));
        filters.flip();
        path = TinyFileDialogs.tinyfd_openFileDialog(
            "Import PayTp JEXL Algorithm",
            "",
            filters,
            "JEXL scripts (*.jexl)",
            false
        );
      }
      if (path == null) return;

      try {
        setValue(Files.readString(Path.of(path)));
        validate();
      } catch (Exception e) {
        TinyFileDialogs.tinyfd_messageBox(
            "PayTp",
            "Failed to import JEXL file:\n" + e.getMessage(),
            "ok",
            "error",
            1
        );
      }
    }

    private void validate() {
      PayTpScript script = new PayTpScript(getValue());
      try {
        PayTpCalculator.validatePriceAlgorithm(script);
        validationError = Optional.empty();
      } catch (Exception e) {
        String error = e.getMessage() == null
            ? e.getClass().getSimpleName()
            : e.getMessage();
        validationError = Optional.of(Component.translatable(
            "paytp.config.price.algorithm.invalid",
            error
        ));
      }
      validatedSource = script.source();
    }
  }

  private static class ScriptEditor extends Screen {

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
      super(Component.translatable(
          "paytp.config.price.algorithm.editor.title"
      ));
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
          Component.translatable(
              "paytp.config.price.algorithm.editor.cancel"
          ),
          button -> onClose()
      ).bounds(
          width / 2 - BUTTON_WIDTH - BUTTON_SPACING / 2,
          height - 30,
          BUTTON_WIDTH,
          20
      ).build());

      confirmButton = addRenderableWidget(Button.builder(
          Component.translatable(
              "paytp.config.price.algorithm.editor.confirm"
          ),
          button -> {
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
      script.setValueListener(
          source -> confirmButton.active = !initialSource.equals(source)
      );
      setInitialFocus(script);
    }

    @Override
    public void extractRenderState(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float tickDelta
    ) {
      super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
      graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
      minecraft.setScreenAndShow(parent);
    }
  }
}
