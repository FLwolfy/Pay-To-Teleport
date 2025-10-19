package com.flwolfy.paytp.command;

import com.flwolfy.paytp.PayTpMod;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;


import java.util.concurrent.atomic.AtomicReference;

/**
 * Singleton class that manages teleport requests between players.
 * <p>
 * Features:
 * - Supports multiple requests per target (FILO stack).
 * - Precise expiration using a scheduled executor.
 * - Thread-safe using ConcurrentHashMap and ConcurrentLinkedDeque.
 * - Safe callbacks wrapped in try/catch.
 * <p>
 * Request lifecycle visualization:
 * <pre>
 * +--------+           +--------+           +------------+
 * | Sender |           | Target |           | Scheduler  |
 * +--------+           +--------+           +------------+
 *     |                    |                     |
 *     | sendRequest()      |                     |
 *     |------------------->|                     |
 *     | pushes RequestData |                     |
 *     |                    |                     |
 *     | accept()           |                     |
 *     |<-------------------|                     |
 *     | pops RequestData   |                     |
 *     | executes onAccept()|                     |
 *     | cancelByTarget()   |                     |
 *     |<-------------------|                     |
 *     | pops RequestData   |                     |
 *     | executes onCancel()|                     |
 *     | cancelBySender()   |                     |
 *     |----------------------------------------->|
 *     | searches all stacks|                     |
 *     | removes RequestData|                     |
 *     | executes onCancel()|                     |
 *     |                    |                     |
 *     |                    |      expireTime     |
 *     |                    |-------------------->|
 *     |                    | pops RequestData    |
 *     |                    | executes onCancel() |
 *     |                    | removes empty stack |
 * </pre>
 * This diagram shows that multiple threads (sender, target, scheduler)
 * can safely operate on the same requests without race conditions.
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

  private enum State { PENDING, ACCEPTED, CANCELLED }

  private static final class RequestData {
    final UUID senderId;
    final Runnable onAccept;
    final Runnable onCancel;
    final AtomicReference<State> state = new AtomicReference<>(State.PENDING);

    RequestData(UUID senderId, Runnable onAccept, Runnable onCancel) {
      this.senderId = senderId;
      this.onAccept = onAccept;
      this.onCancel = onCancel;
    }

    boolean accept() {
      if (state.compareAndSet(State.PENDING, State.ACCEPTED)) {
        try { onAccept.run(); }
        catch (Exception e) { LOGGER.error("Failed to run onAccept", e); }
        return true;
      }
      return false;
    }

    boolean cancel() {
      if (state.compareAndSet(State.PENDING, State.CANCELLED)) {
        try { onCancel.run(); }
        catch (Exception e) { LOGGER.error("Failed to run onCancel", e); }
        return true;
      }
      return false;
    }
  }

  private PayTpRequestManager() {}

  public static PayTpRequestManager getInstance() {
    if (instance == null) instance = new PayTpRequestManager();
    return instance;
  }

  // ======================= //
  // ====== Requests ======= //
  // ======================= //

  public void sendRequest(
      ServerPlayerEntity sender,
      ServerPlayerEntity target,
      Runnable onAccept,
      Runnable onCancel,
      int expireTimeSeconds
  ) {
    UUID targetId = target.getUuid();
    RequestData data = new RequestData(sender.getUuid(), onAccept, onCancel);

    pendingRequests.computeIfAbsent(targetId, k -> new ConcurrentLinkedDeque<>()).push(data);

    // schedule expiration
    scheduler.schedule(() -> {
      if (data.cancel()) { // only cancel if still pending
        Deque<RequestData> stack = pendingRequests.get(targetId);
        if (stack != null) stack.remove(data);
        if (stack != null && stack.isEmpty()) pendingRequests.remove(targetId);
      }
    }, expireTimeSeconds, TimeUnit.SECONDS);
  }

  public boolean accept(ServerPlayerEntity target) {
    Deque<RequestData> stack = pendingRequests.get(target.getUuid());
    if (stack == null) return false;

    for (Iterator<RequestData> it = stack.iterator(); it.hasNext();) {
      RequestData data = it.next();
      if (data.accept()) {
        it.remove();
        if (stack.isEmpty()) pendingRequests.remove(target.getUuid());
        return true;
      }
    }
    return false;
  }

  public boolean cancelByTarget(ServerPlayerEntity target) {
    Deque<RequestData> stack = pendingRequests.get(target.getUuid());
    if (stack == null) return false;

    for (Iterator<RequestData> it = stack.iterator(); it.hasNext();) {
      RequestData data = it.next();
      if (data.cancel()) {
        it.remove();
        if (stack.isEmpty()) pendingRequests.remove(target.getUuid());
        return true;
      }
    }
    return false;
  }

  public boolean cancelBySender(ServerPlayerEntity sender) {
    UUID senderId = sender.getUuid();
    for (Map.Entry<UUID, Deque<RequestData>> entry : pendingRequests.entrySet()) {
      Deque<RequestData> stack = entry.getValue();
      for (Iterator<RequestData> it = stack.iterator(); it.hasNext();) {
        RequestData data = it.next();
        if (data.senderId.equals(senderId) && data.cancel()) {
          it.remove();
          if (stack.isEmpty()) pendingRequests.remove(entry.getKey());
          return true;
        }
      }
    }
    return false;
  }

}
