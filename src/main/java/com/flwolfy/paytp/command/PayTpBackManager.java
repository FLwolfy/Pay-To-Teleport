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

  /**
   * Sets the maximum number of historical locations retained per player.
   *
   * @param max the maximum stack size
   */
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
   * Pushes one teleport location after flushing any cached destination from a previous pair.
   *
   * @param player the player whose history is updated
   * @param data the location to store
   */
  public void pushSingle(ServerPlayer player, PayTpData data) {
    if (player == null || data == null) return;
    pushCachedPair(player);
    pushIfValid(player, data);
  }

  /**
   * Records a teleport pair.
   *
   * <p>The source is pushed immediately and the destination is cached until the player's next
   * history update, preventing duplicate back-stack entries.</p>
   *
   * @param player the player whose history is updated
   * @param from the teleport source
   * @param to the teleport destination
   */
  public void pushPair(ServerPlayer player, PayTpData from, PayTpData to) {
    if (player == null || from == null || to == null) return;
    pushCachedPair(player);
    pushIfValid(player, from);
    pairCache.put(player.getUUID(), to);
  }

  /**
   * Removes and returns the most recent teleport location.
   *
   * @param player the player whose history is queried
   * @return the most recent location, or {@code null} when no history exists
   */
  public PayTpData popLastTp(ServerPlayer player) {
    if (player == null) return null;
    pairCache.remove(player.getUUID());

    Deque<PayTpData> stack = historyMap.get(player.getUUID());
    if (stack == null || stack.isEmpty()) return null;
    return stack.pop();
  }

  /**
   * Clears all stored and cached teleport history for a player.
   *
   * @param player the player whose history is removed
   */
  public void clearHistory(ServerPlayer player) {
    if (player == null) return;
    historyMap.remove(player.getUUID());
    pairCache.remove(player.getUUID());
  }
}
