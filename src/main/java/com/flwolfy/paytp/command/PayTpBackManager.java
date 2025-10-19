package com.flwolfy.paytp.command;

import com.flwolfy.paytp.data.PayTpData;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.server.network.ServerPlayerEntity;

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
  private static int maxBackStack;

  private final Map<UUID, Deque<PayTpData>> historyMap = new ConcurrentHashMap<>();
  private final Map<UUID, PayTpData> pairCache = new ConcurrentHashMap<>();

  private PayTpBackManager() {}

  public static PayTpBackManager getInstance() {
    if (instance == null) {
      instance = new PayTpBackManager();
      maxBackStack = DEFAULT_MAX_BACK_STACK;

      // Register disconnect event
      ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
          PayTpBackManager.getInstance().clearHistory(handler.player)
      );

      // Register death event
      ServerLivingEntityEvents.AFTER_DEATH.register((entity, livingEntity) -> {
        if (entity instanceof ServerPlayerEntity player) {
          PayTpBackManager.getInstance().pushSingle(player, new PayTpData(player.getWorld(), player.getPos()));
        }
      });
    }

    return instance;
  }

  public static void setMaxBackStack(int max) {
    maxBackStack = max;
  }

  // =========================================== //
  // ============= Back Stack Method =========== //
  // =========================================== //

  private void pushCachedPair(ServerPlayerEntity player) {
    PayTpData cached = pairCache.remove(player.getUuid());
    if (cached == null) return;

    Deque<PayTpData> stack = historyMap.computeIfAbsent(player.getUuid(), k -> new ArrayDeque<>());
    if (stack.size() >= maxBackStack) stack.removeLast();
    stack.push(cached);
  }

  private void pushIfValid(ServerPlayerEntity player, PayTpData data) {
    if (player == null || data == null) return;

    Deque<PayTpData> stack = historyMap.computeIfAbsent(player.getUuid(), k -> new ArrayDeque<>());

    // Skip duplicate
    PayTpData last = stack.peek();
    if (last != null && last.equals(data)) return;

    if (stack.size() >= maxBackStack) stack.removeLast();
    stack.push(data);
  }

  /**
   * Push a single teleport point into the stack.
   */
  public void pushSingle(ServerPlayerEntity player, PayTpData data) {
    if (player == null || data == null) return;
    pushCachedPair(player);
    pushIfValid(player, data);
  }

  /**
   * Push a pair: from → to.
   * 'from' is immediately pushed, 'to' is cached for next push.
   */
  public void pushPair(ServerPlayerEntity player, PayTpData from, PayTpData to) {
    if (player == null || from == null || to == null) return;
    pushCachedPair(player);
    pushIfValid(player, from);
    pairCache.put(player.getUuid(), to);
  }

  /**
   * Pop the last teleport point from the stack.
   */
  public PayTpData popLastTp(ServerPlayerEntity player) {
    if (player == null) return null;
    pairCache.remove(player.getUuid());

    Deque<PayTpData> stack = historyMap.get(player.getUuid());
    if (stack == null || stack.isEmpty()) return null;
    return stack.pop();
  }

  /**
   * Clear all teleport history and cached pair for a player.
   */
  public void clearHistory(ServerPlayerEntity player) {
    if (player == null) return;
    historyMap.remove(player.getUuid());
    pairCache.remove(player.getUuid());
  }
}
