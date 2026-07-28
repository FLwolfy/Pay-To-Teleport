package com.flwolfy.paytp.data.config;

import com.flwolfy.paytp.data.lang.PayTpLang;
import com.flwolfy.paytp.data.script.PayTpScript;
import com.flwolfy.paytp.flag.Flags;
import com.flwolfy.paytp.flag.PayTpSettingFlags;

public record PayTpConfigData(
    General general,
    Teleport teleport,
    Request request,
    Home home,
    Back back,
    Warp warp,
    Price price,
    Setting setting
) {

  public record General(
      PayTpLang language,
      String helpCommand
  ) {}

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
      String homeCommand
  ) {}

  public record Back(
      String backCommand,
      int maxBackStack
  ) {}

  public record Warp(
      String warpCommand,
      int maxInactiveTicks,
      int checkPeriodTicks
  ) {}

  public record Price(
      String currencyItem,
      int minPrice,
      int maxPrice,
      PayTpScript algorithm
  ) {
    public static final PayTpScript DEFAULT_ALGORITHM = new PayTpScript("""
        // Available variables:
        // from, to: positions with .x, .y, .z, and .dimension
        // teleportType: "coordinate", "request", "home", "back", or "warp"
        // player: name of the player being teleported
        // otherPlayer: name of the other request player, or an empty string
        //
        // Java's built-in Math methods are available through the "math" namespace.
        // Full system shell access is available through the "shell" namespace.

        var basePrice = 1;
        var baseRadius = 10.0;
        var pricePerBlock = 0.01;
        var crossDimensionMultiplier = 1.5;
        var homeMultiplier = 0.5;
        var backMultiplier = 0.8;
        var warpMultiplier = 0.5;
        var netherCoordinateScale = 8.0;

        var crossDimension = from.dimension != to.dimension;
        var deltaX = from.x - to.x;
        var deltaY = from.y - to.y;
        var deltaZ = from.z - to.z;

        if (crossDimension) {
          if (from.dimension == "minecraft:the_end") {
            deltaX = from.x;
            deltaY = from.y;
            deltaZ = from.z;
          } else if (to.dimension == "minecraft:the_end") {
            deltaX = to.x;
            deltaY = to.y;
            deltaZ = to.z;
          } else if (from.dimension == "minecraft:the_nether") {
            deltaX = from.x * netherCoordinateScale - to.x;
            deltaY = from.y * netherCoordinateScale - to.y;
            deltaZ = from.z * netherCoordinateScale - to.z;
          } else if (to.dimension == "minecraft:the_nether") {
            deltaX = from.x - to.x / netherCoordinateScale;
            deltaY = from.y - to.y / netherCoordinateScale;
            deltaZ = from.z - to.z / netherCoordinateScale;
          }
        }

        var distance = math:sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        var multiplier = crossDimension ? crossDimensionMultiplier : 1.0;

        if (teleportType == "home") {
          multiplier = multiplier * homeMultiplier;
        } else if (teleportType == "back") {
          multiplier = multiplier * backMultiplier;
        } else if (teleportType == "warp") {
          multiplier = multiplier * warpMultiplier;
        }

        var distanceBeyondBase = distance > baseRadius ? distance - baseRadius : 0;

        math:round((basePrice + distanceBeyondBase * pricePerBlock) * multiplier).intValue();
        """.stripTrailing());

    public Price {
      if (currencyItem == null) {
        throw new IllegalArgumentException("currencyItem must not be null");
      }
      if (algorithm == null) {
        throw new IllegalArgumentException("algorithm must not be null");
      }
      if (minPrice < 0) {
        throw new IllegalArgumentException("minPrice must not be negative");
      }
      if (maxPrice < minPrice) {
        throw new IllegalArgumentException(
            "maxPrice must be greater than or equal to minPrice"
        );
      }
    }
  }

  public record Setting(
      Effect effect,
      Flag flag
  ) {
    public record Effect(
        boolean particleEffect,
        boolean soundEffect
    ) {}

    public record Flag(
        boolean allowEnderChest,
        boolean prioritizeEnderChest,
        boolean allowShulkerBox,
        boolean prioritizeShulkerBox
    ) {}
  }

  public static final PayTpConfigData DEFAULT = new PayTpConfigData(
      new General(
          PayTpLang.ENGLISH,
          "ptphelp"
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
          "ptphome"
      ),
      new Back(
          "ptpback",
          10
      ),
      new Warp(
          "ptpwarp",
          100,
          20
      ),
      new Price(
          "minecraft:diamond",
          1,
          64,
          Price.DEFAULT_ALGORITHM
      ),
      new Setting(
          new Setting.Effect(
              true,
              true
          ),
          new Setting.Flag(
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
   * @return the combined {@link PayTpSettingFlags} mask
   */
  public int combineSettingFlags() {
    Setting.Flag flag = setting.flag();
    return Flags.combine(
        flag.allowEnderChest() ? PayTpSettingFlags.ALLOW_ENDER_CHEST : null,
        flag.prioritizeEnderChest() ? PayTpSettingFlags.PRIORITIZE_ENDER_CHEST : null,
        flag.allowShulkerBox() ? PayTpSettingFlags.ALLOW_SHULKER_BOX : null,
        flag.prioritizeShulkerBox() ? PayTpSettingFlags.PRIORITIZE_SHULKER_BOX : null
    );
  }

}
