package com.flwolfy.paytp.data.config;

import com.flwolfy.paytp.data.lang.PayTpLang;
import com.flwolfy.paytp.flag.Flags;
import com.flwolfy.paytp.flag.PayTpMultiplierFlags;
import com.flwolfy.paytp.flag.PayTpSettingFlags;

public record PayTpConfigData(
    General general,
    Request request,
    Home home,
    Back back,
    Warp warp,
    Price price,
    Setting setting
) {

  public record General(
      PayTpLang language,
      String mainCommand,
      double crossDimMultiplier
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
      double homeMultiplier
  ) {}

  public record Back(
      String backCommand,
      int maxBackStack,
      double backMultiplier
  ) {}

  public record Warp(
      String warpCommand,
      int maxInactiveTicks,
      int checkPeriodTicks,
      double warpMultiplier
  ) {}

  public record Price(
      String currencyItem,
      Parameter parameter
  ) {
    public record Parameter(
        int minPrice,
        int maxPrice,
        double baseRadius,
        double rate
    ) {}
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
          "ptp",
          1.5
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
          0.5
      ),
      new Back(
          "ptpback",
          10,
          0.8
      ),
      new Warp(
          "ptpwarp",
          100,
          20,
          0.5
      ),
      new Price(
          "minecraft:diamond",
          new Price.Parameter(
              1,
              64,
              10.0,
              0.01
          )
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

  public int combineSettingFlags() {
    Setting.Flag flag = setting.flag();
    return Flags.combine(
        flag.allowEnderChest() ? PayTpSettingFlags.ALLOW_ENDER_CHEST : null,
        flag.prioritizeEnderChest() ? PayTpSettingFlags.PRIORITIZE_ENDER_CHEST : null,
        flag.allowShulkerBox() ? PayTpSettingFlags.ALLOW_SHULKER_BOX : null,
        flag.prioritizeShulkerBox() ? PayTpSettingFlags.PRIORITIZE_SHULKER_BOX : null
    );
  }

  public double calculateMultiplier(int multiplierFlags) {
    double multiplier = 1.0;
    if (Flags.check(multiplierFlags, PayTpMultiplierFlags.CROSS_DIMENSION))
      multiplier *= general.crossDimMultiplier();
    if (Flags.check(multiplierFlags, PayTpMultiplierFlags.HOME))
      multiplier *= home.homeMultiplier();
    if (Flags.check(multiplierFlags, PayTpMultiplierFlags.BACK))
      multiplier *= back.backMultiplier();
    if (Flags.check(multiplierFlags, PayTpMultiplierFlags.WARP))
      multiplier *= warp.warpMultiplier();
    return multiplier;
  }
}
