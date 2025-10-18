package com.flwolfy.paytp.command;

import com.flwolfy.paytp.PayTpMod;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.*;

/**
 * Singleton class that manages teleport requests between players.
 * Supports multiple requests per target (FILO stack) with precise expire time.
 */
public class PayTpRequestManager {

  private static final Logger LOGGER = PayTpMod.LOGGER;
  private static PayTpRequestManager instance;

  private final Map<UUID, Deque<RequestData>> pendingRequests = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r);
    t.setDaemon(true);
    t.setName("PayTp-Scheduler");
    return t;
  });

  private record RequestData(UUID senderId, Runnable onAccept, Runnable onCancel) { }

  private PayTpRequestManager() {}
  public static PayTpRequestManager getInstance() {
    if (instance == null) instance = new PayTpRequestManager();
    return instance;
  }

  // ========================================= //
  // ============= Request Methods =========== //
  // ========================================= //

  public void sendRequest(
      ServerPlayerEntity sender,
      ServerPlayerEntity target,
      Runnable onAccept,
      Runnable onCancel,
      int expireTime
  ) {
    UUID targetId = target.getUuid();
    RequestData data = new RequestData(sender.getUuid(), onAccept, onCancel);

    pendingRequests.computeIfAbsent(targetId, k -> new ConcurrentLinkedDeque<>()).push(data);
    scheduler.schedule(() -> {
      Deque<RequestData> stack = pendingRequests.get(targetId);
      if (stack != null && stack.remove(data)) {
        try {
          data.onCancel.run();
        } catch (Exception e) {
          LOGGER.error("Failed to run onCancel for expired request", e);
        }

        if (stack.isEmpty()) pendingRequests.remove(targetId);
      }
    }, expireTime, TimeUnit.SECONDS);
  }

  public boolean accept(ServerPlayerEntity target) {
    Deque<RequestData> stack = pendingRequests.get(target.getUuid());
    if (stack == null || stack.isEmpty()) return false;

    RequestData data = stack.pop();
    try {
      data.onAccept.run();
    } catch (Exception e) {
      LOGGER.error("Failed to execute onAccept", e);
      return false;
    }

    if (stack.isEmpty()) pendingRequests.remove(target.getUuid());
    return true;
  }

  public boolean cancel(ServerPlayerEntity target) {
    Deque<RequestData> stack = pendingRequests.get(target.getUuid());
    if (stack == null || stack.isEmpty()) return false;

    RequestData data = stack.pop();
    try {
      data.onCancel.run();
    } catch (Exception e) {
      LOGGER.error("Failed to execute onCancel", e);
      return false;
    }

    if (stack.isEmpty()) pendingRequests.remove(target.getUuid());
    return true;
  }
}
