package com.flwolfy.paytp.command;

import com.flwolfy.paytp.data.PayTpData;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton manager that tracks teleport history for each player.
 * Supports single push and pair push for /ptpback functionality.
 * Simplified: only popLastTp() exists, no peek/second-last.
 */
public class PayTpBackManager {

  private static final int DEFAULT_MAX_BACK_STACK = 10;

  private static PayTpBackManager instance;

  private final Map<UUID, Deque<PayTpData>> historyMap = new ConcurrentHashMap<>();
  private final Map<UUID, PayTpData> pairCache = new ConcurrentHashMap<>();

  private int maxBackStack;

  private PayTpBackManager() {}

  public static PayTpBackManager getInstance() {
    if (instance == null) {
      instance = new PayTpBackManager();
      instance.maxBackStack = DEFAULT_MAX_BACK_STACK;
    }
    return instance;
  }

  public void setMaxBackStack(int max) {
    maxBackStack = max;
  }

  // =========================================== //
  // ============= Back Stack Method =========== //
  // =========================================== //

  private void pushCachedPair(ServerPlayer player) {
    PayTpData cached = pairCache.remove(player.getUUID());
    if (cached == null) return;

    Deque<PayTpData> stack = historyMap.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
    if (stack.size() >= maxBackStack) stack.removeLast();
    stack.push(cached);
  }

  private void pushIfValid(ServerPlayer player, PayTpData data) {
    if (player == null || data == null) return;

    Deque<PayTpData> stack = historyMap.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());

    // Skip duplicate
    PayTpData last = stack.peek();
    if (last != null && last.equals(data)) return;

    if (stack.size() >= maxBackStack) stack.removeLast();
    stack.push(data);
  }

  /**
   * Push a single teleport point into the stack.
   */
  public void pushSingle(ServerPlayer player, PayTpData data) {
    if (player == null || data == null) return;
    pushCachedPair(player);
    pushIfValid(player, data);
  }

  /**
   * Push a pair: from → to.
   * 'from' is immediately pushed, 'to' is cached for next push.
   */
  public void pushPair(ServerPlayer player, PayTpData from, PayTpData to) {
    if (player == null || from == null || to == null) return;
    pushCachedPair(player);
    pushIfValid(player, from);
    pairCache.put(player.getUUID(), to);
  }

  /**
   * Pop the last teleport point from the stack.
   */
  public PayTpData popLastTp(ServerPlayer player) {
    if (player == null) return null;
    pairCache.remove(player.getUUID());

    Deque<PayTpData> stack = historyMap.get(player.getUUID());
    if (stack == null || stack.isEmpty()) return null;
    return stack.pop();
  }

  /**
   * Clear all teleport history and cached pair for a player.
   */
  public void clearHistory(ServerPlayer player) {
    if (player == null) return;
    historyMap.remove(player.getUUID());
    pairCache.remove(player.getUUID());
  }
}
