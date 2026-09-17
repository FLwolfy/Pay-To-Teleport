package com.flwolfy.paytp.command.warp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PayTpWarpCommandInputTest {

  @Test
  void acceptsUnicodeAndSpecialCharacterNamesWithoutSpaces() {
    assertEquals("中文路径点（主城）#1", PayTpWarpCommandInput.name("中文路径点（主城）#1"));
    assertEquals("主城/东门:一号", PayTpWarpCommandInput.name("主城/东门:一号"));
  }

  @Test
  void requiresQuotesForNamesContainingSpaces() {
    assertEquals("", PayTpWarpCommandInput.name("中文 路径点"));
    assertEquals("中文 路径点", PayTpWarpCommandInput.name("\"中文 路径点\""));
  }

  @Test
  void keepsEscapedQuotesCompatible() {
    assertEquals("a\"b", PayTpWarpCommandInput.name("\"a\\\"b\""));
  }

  @Test
  void requiresEachRenameNameWithSpacesToBeQuoted() {
    var input = PayTpWarpCommandInput.rename("\"主城 东门\" \"新的 入口\"");
    assertTrue(input.valid());
    assertEquals("主城 东门", input.name());
    assertEquals("新的 入口", input.newName());

    assertFalse(PayTpWarpCommandInput.rename("主城 东门 新入口").valid());
  }

  @Test
  void requiresQuotedWaypointNamesBeforePlayerNames() {
    var input = PayTpWarpCommandInput.playerAction("\"私人 小屋\" Steve");
    assertTrue(input.valid());
    assertEquals("私人 小屋", input.name());
    assertEquals("Steve", input.playerName());

    assertFalse(PayTpWarpCommandInput.playerAction("私人 小屋 Steve").valid());
  }

  @Test
  void distinguishesQuotedNamesFromForcedDeletionSuffix() {
    var normal = PayTpWarpCommandInput.delete("\"管理 forced\"");
    assertTrue(normal.valid());
    assertFalse(normal.forced());
    assertEquals("管理 forced", normal.name());

    var forced = PayTpWarpCommandInput.delete("\"管理 forced\" forced");
    assertTrue(forced.valid());
    assertTrue(forced.forced());
    assertEquals("管理 forced", forced.name());
  }

  @Test
  void locatesTheFollowingArgumentForCompletion() {
    assertEquals(3, PayTpWarpCommandInput.nextArgumentStart("中文 "));
    assertEquals(8, PayTpWarpCommandInput.nextArgumentStart("\"主城 东门\" p"));
    assertEquals(-1, PayTpWarpCommandInput.nextArgumentStart("中文"));
    assertEquals(-1, PayTpWarpCommandInput.nextArgumentStart("\"未结束"));
  }

  @Test
  void formatsEveryCompletedWaypointNameWithQuotes() {
    assertEquals("\"中文路径点\"", PayTpWarpCommandInput.formatNameSuggestion("中文路径点"));
    assertEquals("\"主城 东门\"", PayTpWarpCommandInput.formatNameSuggestion("主城 东门"));
    assertEquals(
        "\"主城 \\\\东门\"",
        PayTpWarpCommandInput.formatNameSuggestion("主城 \\东门")
    );
  }

  @Test
  void rejectsMalformedOrExtraInput() {
    assertEquals("", PayTpWarpCommandInput.name("\"未结束"));
    assertFalse(PayTpWarpCommandInput.rename("\"旧名称\"").valid());
    assertFalse(PayTpWarpCommandInput.playerAction("路径点").valid());
    assertFalse(PayTpWarpCommandInput.delete("路径点 forced extra").valid());
  }
}
