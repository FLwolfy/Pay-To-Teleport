package com.flwolfy.paytp.command.request;

import com.flwolfy.paytp.PayTpMod;

import net.minecraft.server.level.ServerPlayer;

import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;


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
 *     |&lt;-------------------|                     |
 *     | pops RequestData   |                     |
 *     | executes onAccept()|                     |
 *     | cancelByTarget()   |                     |
 *     |&lt;-------------------|                     |
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
    if (instance == null) {
      instance = new PayTpRequestManager();
    }
    return instance;
  }

  // ======================= //
  // ====== Requests ======= //
  // ======================= //

  /**
   * Adds a request and schedules its automatic cancellation.
   *
   * @param sender the player who sent the request
   * @param target the player who may accept or deny the request
   * @param onAccept action executed exactly once when accepted
   * @param onCancel action executed exactly once when denied, cancelled, or expired
   * @param expireTimeSeconds request lifetime in seconds
   */
  public void sendRequest(
      ServerPlayer sender,
      ServerPlayer target,
      Runnable onAccept,
      Runnable onCancel,
      int expireTimeSeconds
  ) {
    UUID targetId = target.getUUID();
    RequestData data = new RequestData(sender.getUUID(), onAccept, onCancel);

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

  /**
   * Accepts the newest pending request from a specific sender.
   *
   * @param target the request recipient
   * @param sender the required request sender
   * @return {@code true} if a pending request was accepted
   */
  public boolean accept(ServerPlayer target, ServerPlayer sender) {
    Deque<RequestData> stack = pendingRequests.get(target.getUUID());
    if (stack == null) return false;

    for (Iterator<RequestData> it = stack.iterator(); it.hasNext();) {
      RequestData data = it.next();
      if (data.senderId.equals(sender.getUUID()) && data.accept()) {
        it.remove();
        if (stack.isEmpty()) pendingRequests.remove(target.getUUID());
        return true;
      }
    }
    return false;
  }

  /**
   * Accepts the newest pending request received by a player.
   *
   * @param target the request recipient
   * @return {@code true} if a pending request was accepted
   */
  public boolean acceptLatest(ServerPlayer target) {
    Deque<RequestData> stack = pendingRequests.get(target.getUUID());
    if (stack == null) return false;

    for (Iterator<RequestData> it = stack.iterator(); it.hasNext();) {
      RequestData data = it.next();
      if (data.accept()) {
        it.remove();
        if (stack.isEmpty()) pendingRequests.remove(target.getUUID());
        return true;
      }
    }
    return false;
  }

  /**
   * Denies the newest pending request from a specific sender.
   *
   * @param target the request recipient
   * @param sender the required request sender
   * @return {@code true} if a pending request was denied
   */
  public boolean deny(ServerPlayer target, ServerPlayer sender) {
    Deque<RequestData> stack = pendingRequests.get(target.getUUID());
    if (stack == null) return false;

    for (Iterator<RequestData> it = stack.iterator(); it.hasNext();) {
      RequestData data = it.next();
      if (data.senderId.equals(sender.getUUID()) && data.cancel()) {
        it.remove();
        if (stack.isEmpty()) pendingRequests.remove(target.getUUID());
        return true;
      }
    }
    return false;
  }

  /**
   * Denies the newest pending request received by a player.
   *
   * @param target the request recipient
   * @return {@code true} if a pending request was denied
   */
  public boolean denyLatest(ServerPlayer target) {
    Deque<RequestData> stack = pendingRequests.get(target.getUUID());
    if (stack == null) return false;

    for (Iterator<RequestData> it = stack.iterator(); it.hasNext();) {
      RequestData data = it.next();
      if (data.cancel()) {
        it.remove();
        if (stack.isEmpty()) pendingRequests.remove(target.getUUID());
        return true;
      }
    }
    return false;
  }

  /**
   * Cancels a pending request sent to a specific target.
   *
   * @param sender the request sender
   * @param target the required request recipient
   * @return {@code true} if a pending request was cancelled
   */
  public boolean cancel(ServerPlayer sender, ServerPlayer target) {
    UUID senderId = sender.getUUID();
    Deque<RequestData> stack = pendingRequests.get(target.getUUID());
    if (stack == null) return false;

    for (Iterator<RequestData> it = stack.iterator(); it.hasNext();) {
      RequestData data = it.next();
      if (data.senderId.equals(senderId) && data.cancel()) {
        it.remove();
        if (stack.isEmpty()) pendingRequests.remove(target.getUUID());
        return true;
      }
    }

    return false;
  }

  /**
   * Cancels the newest pending request sent by a player.
   *
   * @param sender the request sender
   * @return {@code true} if a pending request was cancelled
   */
  public boolean cancelLatest(ServerPlayer sender) {
    UUID senderId = sender.getUUID();
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
