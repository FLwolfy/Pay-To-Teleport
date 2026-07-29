package com.flwolfy.paytp.util;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.PermissionSet;

/**
 * Provides unrestricted Minecraft command execution to the PayTp JEXL
 * {@code minecraft} namespace.
 */
public final class PayTpCommandExecutor {

  private final CommandSourceStack source;

  private PayTpCommandExecutor() {
    source = null;
  }

  /**
   * Creates an executor using the server console as the command source.
   *
   * @param server the server whose registered commands will be executed
   */
  public PayTpCommandExecutor(MinecraftServer server) {
    this.source = Objects.requireNonNull(server)
        .createCommandSourceStack()
        .withMaximumPermission(PermissionSet.ALL_PERMISSIONS)
        .withSuppressedOutput();
  }

  /**
   * Creates a no-op executor for validating scripts without changing server state.
   *
   * @return an executor whose commands return {@code 0} without being run
   */
  public static PayTpCommandExecutor validationOnly() {
    return new PayTpCommandExecutor();
  }

  /**
   * Executes any command registered with the Minecraft server as the server console.
   *
   * <p>The leading slash is optional. The returned value is the command result reported by
   * Minecraft, or {@code 0} when the command reports no successful result. Since the command
   * source has no entity, {@code @s} cannot refer to the teleported player.</p>
   *
   * @param command the complete Minecraft command
   * @return the command result
   */
  @SuppressWarnings("unused")
  public int execute(String command) {
    String normalizedCommand = Commands.trimOptionalPrefix(
        Objects.requireNonNull(command, "command")
    );
    if (source == null) return 0;

    AtomicInteger result = new AtomicInteger();
    source.getServer().getCommands().performPrefixedCommand(
        source.withCallback((successful, value) -> {
          if (successful) result.set(value);
        }),
        normalizedCommand
    );
    return result.get();
  }
}
