package com.flwolfy.paytp.data.config;

import com.flwolfy.paytp.data.lang.PayTpLang;
import com.flwolfy.paytp.data.PayTpContext;
import com.flwolfy.paytp.data.PayTpCallback;
import com.flwolfy.paytp.data.PayTpPlayer;
import com.flwolfy.paytp.data.script.PayTpScript;
import com.flwolfy.paytp.data.script.PayTpScriptManager;
import com.flwolfy.paytp.data.script.PayTpScriptPosition;
import com.flwolfy.paytp.data.warp.PayTpWarpPermission;
import com.flwolfy.paytp.flag.Flags;
import com.flwolfy.paytp.flag.PayTpDeductionFlags;
import com.flwolfy.paytp.util.PayTpCommandExecutor;
import com.flwolfy.paytp.util.PayTpItemHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record PayTpConfigData(
    General general,
    Teleport teleport,
    Request request,
    Home home,
    Back back,
    Warp warp,
    Price price
) {

  public record General(
      PayTpLang language,
      String helpCommand,
      boolean safeTeleport,
      int safeTeleportRange,
      Effect effect
  ) {
    public record Effect(
        boolean particleEffect,
        boolean soundEffect
    ) {}
  }

  public record Teleport(
      String coordinateCommand,
      boolean allowCrossDim
  ) {}

  public record Request(
      RequestCommand requestCommand,
      int expireTime
  ) {
    public record RequestCommand(
        String toCommand,
        String hereCommand,
        String acceptCommand,
        String denyCommand,
        String cancelCommand
    ) {}
  }

  public record Home(
      String homeCommand,
      boolean setRespawnPoint
  ) {}

  public record Back(
      String backCommand,
      int maxBackStack
  ) {}

  public record Warp(
      String warpCommand,
      PayTpWarpPermission serverWarpPermission,
      int maxInactiveTicks,
      int checkPeriodTicks
  ) {}

  public record Price(
      String currencyItem,
      int minPrice,
      int maxPrice,
      PayTpScript algorithm,
      Deduction deduction
  ) {
    public record Deduction(
        boolean allowEnderChest,
        boolean prioritizeEnderChest,
        boolean allowShulkerBox,
        boolean prioritizeShulkerBox
    ) {}

  }

  public static final PayTpConfigData DEFAULT = new PayTpConfigData(
      new General(
          PayTpLang.ENGLISH,
          "ptphelp",
          false,
          5,
          new General.Effect(
              true,
              true
          )
      ),
      new Teleport(
          "ptp",
          true
      ),
      new Request(
          new Request.RequestCommand(
              "ptpto",
              "ptphere",
              "ptpaccept",
              "ptpdeny",
              "ptpcancel"
          ),
          10
      ),
      new Home(
          "ptphome",
          false
      ),
      new Back(
          "ptpback",
          10
      ),
      new Warp(
          "ptpwarp",
          PayTpWarpPermission.GAMEMASTERS,
          100,
          20
      ),
      new Price(
          "minecraft:diamond",
          1,
          64,
          new PayTpScript("""
              // Available variables:
              //
              // Variable | Available methods
              // -------- | -----------------------------------------------------------
              // from     | .x(), .y(), .z(), .dimension()
              // to       | .x(), .y(), .z(), .dimension()
              // context  | .coordinate(), .home(), .back(), .request(), .warp()
              // player   | .uuid(), .name()
              // callback | .onSuccess(), .onFailure()
              //
              // Java's built-in Math methods are available through the "math" namespace.
              // Minecraft commands are available through minecraft:execute("command").
              // Full system shell access is available through the "shell" namespace.

              var basePrice = 1;
              var baseRadius = 10.0;
              var pricePerBlock = 0.01;
              var crossDimensionMultiplier = 1.5;
              var homeMultiplier = 0.5;
              var backMultiplier = 0.8;
              var warpMultiplier = 0.5;
              var netherCoordinateScale = 8.0;

              var crossDimension = from.dimension() != to.dimension();
              var deltaX = from.x() - to.x();
              var deltaY = from.y() - to.y();
              var deltaZ = from.z() - to.z();

              if (crossDimension) {
                if (from.dimension() == "minecraft:the_end") {
                  deltaX = from.x();
                  deltaY = from.y();
                  deltaZ = from.z();
                } else if (to.dimension() == "minecraft:the_end") {
                  deltaX = to.x();
                  deltaY = to.y();
                  deltaZ = to.z();
                } else if (from.dimension() == "minecraft:the_nether") {
                  deltaX = from.x() * netherCoordinateScale - to.x();
                  deltaY = from.y() * netherCoordinateScale - to.y();
                  deltaZ = from.z() * netherCoordinateScale - to.z();
                } else if (to.dimension() == "minecraft:the_nether") {
                  deltaX = from.x() - to.x() / netherCoordinateScale;
                  deltaY = from.y() - to.y() / netherCoordinateScale;
                  deltaZ = from.z() - to.z() / netherCoordinateScale;
                }
              }

              var distance = math:sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
              var multiplier = crossDimension ? crossDimensionMultiplier : 1.0;

              if (context.home() != null) {
                multiplier = multiplier * homeMultiplier;
              } else if (context.back() != null) {
                multiplier = multiplier * backMultiplier;
              } else if (context.warp() != null) {
                multiplier = multiplier * warpMultiplier;
              }

              var distanceBeyondBase = distance > baseRadius ? distance - baseRadius : 0;

              callback.onSuccess() += () -> {
                minecraft:execute(
                    "effect give " + player.name() + " minecraft:weakness 1 1"
                );
              };

              math:round((basePrice + distanceBeyondBase * pricePerBlock) * multiplier).intValue();
              """.stripTrailing()),
          new Price.Deduction(
              true,
              true,
              false,
              false
          )
      )
  );

  /**
   * Encodes the configured payment-storage options as a bit mask.
   *
   * @return the combined {@link PayTpDeductionFlags} mask
   */
  public int deductionFlags() {
    Price.Deduction deduction = price.deduction();
    return Flags.combine(
        deduction.allowEnderChest() ? PayTpDeductionFlags.ALLOW_ENDER_CHEST : null,
        deduction.prioritizeEnderChest() ? PayTpDeductionFlags.PRIORITIZE_ENDER_CHEST : null,
        deduction.allowShulkerBox() ? PayTpDeductionFlags.ALLOW_SHULKER_BOX : null,
        deduction.prioritizeShulkerBox() ? PayTpDeductionFlags.PRIORITIZE_SHULKER_BOX : null
    );
  }

  /**
   * Validates the complete configuration and returns every invalid field path.
   *
   * @return field paths whose current values are invalid
   */
  public List<String> validate() {
    List<String> invalidFields = new ArrayList<>();

    // Price Range
    if (price.minPrice() < 0 || price.minPrice() > price.maxPrice()) {
      invalidFields.add("price.minPrice");
    }
    if (price.maxPrice() < 0 || price.maxPrice() < price.minPrice()) {
      invalidFields.add("price.maxPrice");
    }

    // Time and Capacity
    if (general.safeTeleportRange() < 1 || general.safeTeleportRange() > 64) {
      invalidFields.add("general.safeTeleportRange");
    }
    if (request.expireTime() < 0) {
      invalidFields.add("request.expireTime");
    }
    if (back.maxBackStack() <= 0) {
      invalidFields.add("back.maxBackStack");
    }
    if (warp.maxInactiveTicks() < 0) {
      invalidFields.add("warp.maxInactiveTicks");
    }
    if (warp.checkPeriodTicks() <= 0) {
      invalidFields.add("warp.checkPeriodTicks");
    }
    if (warp.serverWarpPermission() == null) {
      invalidFields.add("warp.serverWarpPermission");
    }

    // Currency Item
    try {
      PayTpItemHandler.getItemByStringId(price.currencyItem());
    } catch (RuntimeException e) {
      invalidFields.add("price.currencyItem");
    }

    // Price Algorithm
    try {
      PayTpCallback callback = new PayTpCallback();
      PayTpScriptManager.getInstance().evaluate(
          price.algorithm(), Integer.class,
          Map.of("minecraft", PayTpCommandExecutor.validationOnly()),
          Map.entry("from", new PayTpScriptPosition(0.0, 64.0, 0.0, "minecraft:overworld")),
          Map.entry("to", new PayTpScriptPosition(100.0, 64.0, 100.0, "minecraft:overworld")),
          Map.entry("context", PayTpContext.coordinate(
              new PayTpContext.Coordinate()
          )),
          Map.entry("player", new PayTpPlayer(
              "00000000-0000-0000-0000-000000000000",
              "Player"
          )),
          Map.entry("callback", callback)
      );
    } catch (RuntimeException e) {
      invalidFields.add("price.algorithm");
    }

    // Command Names
    Map<String, String> commands = new HashMap<>();
    Map.ofEntries(
        Map.entry("general.helpCommand", general.helpCommand()),
        Map.entry("teleport.coordinateCommand", teleport.coordinateCommand()),
        Map.entry("request.requestCommand.toCommand", request.requestCommand().toCommand()),
        Map.entry("request.requestCommand.hereCommand", request.requestCommand().hereCommand()),
        Map.entry("request.requestCommand.acceptCommand", request.requestCommand().acceptCommand()),
        Map.entry("request.requestCommand.denyCommand", request.requestCommand().denyCommand()),
        Map.entry("request.requestCommand.cancelCommand", request.requestCommand().cancelCommand()),
        Map.entry("home.homeCommand", home.homeCommand()),
        Map.entry("back.backCommand", back.backCommand()),
        Map.entry("warp.warpCommand", warp.warpCommand())
    ).forEach((fieldPath, command) -> {
      if (command.isEmpty()) return;
      String duplicate = commands.putIfAbsent(command, fieldPath);
      if (duplicate != null) {
        if (!invalidFields.contains(duplicate)) invalidFields.add(duplicate);
        if (!invalidFields.contains(fieldPath)) invalidFields.add(fieldPath);
      }
    });

    // Payment Priorities
    if (price.deduction().prioritizeEnderChest() && !price.deduction().allowEnderChest()) {
      invalidFields.add("price.deduction.prioritizeEnderChest");
    }
    if (price.deduction().prioritizeShulkerBox() && !price.deduction().allowShulkerBox()) {
      invalidFields.add("price.deduction.prioritizeShulkerBox");
    }

    return List.copyOf(invalidFields);
  }

}
