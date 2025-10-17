package com.flwolfy.paytp.command;

import com.flwolfy.paytp.PayTpMod;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.concurrent.*;
import org.slf4j.Logger;

/**
 * Singleton class that manages teleport requests between players.
 * Designed for clean readability and lambda-based callback handling.
 */
public class PayTpRequest {

  private static final Logger LOGGER = PayTpMod.LOGGER;
  private static final int CLEANER_TIMER_PERIOD = 5;
  private static final int EXPIRE_TIME = 10;
  private static PayTpRequest instance;

  private final Map<UUID, RequestData> pendingRequests = new ConcurrentHashMap<>();
  private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

  private record RequestData(UUID senderId, Runnable onAccept, Runnable onCancel, long expireAt) { }
  private PayTpRequest() {
    cleaner.scheduleAtFixedRate(this::cleanupExpired, CLEANER_TIMER_PERIOD, CLEANER_TIMER_PERIOD, TimeUnit.SECONDS);
  }

  public static PayTpRequest getInstance() {
    if (instance == null) instance = new PayTpRequest();
    return instance;
  }

  private void cleanupExpired() {
    long now = System.currentTimeMillis();
    pendingRequests.entrySet().removeIf(entry -> {
      if (entry.getValue().expireAt < now) {
        RequestData data = pendingRequests.get(entry.getKey());
        if (data.onCancel != null) {
          try {
            data.onCancel.run();
          } catch (Exception e) {
            LOGGER.error("Error while executing onCancel() for payTp request!", e);
          }
        }
        return true;
      }
      return false;
    });
  }

  public void sendRequest(
      ServerPlayerEntity sender,
      ServerPlayerEntity target,
      Runnable onAccept,
      Runnable onCancel
  ) {
    UUID targetId = target.getUuid();
    UUID senderId = sender.getUuid();

    RequestData data = new RequestData(senderId, onAccept, onCancel, System.currentTimeMillis() + EXPIRE_TIME * 1000);
    pendingRequests.put(targetId, data);
  }

  public boolean accept(ServerPlayerEntity target) {
    RequestData data = pendingRequests.remove(target.getUuid());
    if (data == null) {
      return false;
    }

    try {
      data.onAccept.run();
    } catch (Exception e) {
      LOGGER.error("Failed to accept request", e);
      return false;
    }
    return true;
  }

  public boolean cancel(ServerPlayerEntity target) {
    RequestData data = pendingRequests.get(target.getUuid());
    if (data != null) {
      pendingRequests.remove(target.getUuid());

      try {
        data.onCancel.run();
      } catch (Exception e) {
        LOGGER.error("Failed to cancel request", e);
        return false;
      }

      return true;
    }
    return false;
  }
}
