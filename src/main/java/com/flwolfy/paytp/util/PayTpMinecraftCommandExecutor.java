package com.flwolfy.paytp.util;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.PermissionSet;

/**
 * Provides unrestricted Minecraft command execution to the PayTp JEXL
 * {@code minecraft} namespace.
 */
public final class PayTpMinecraftCommandExecutor {

  private final CommandSourceStack source;

  private PayTpMinecraftCommandExecutor() {
    source = null;
  }

  /**
   * Creates an executor using the supplied command source with maximum permissions.
   *
   * @param source the player-relative source used to execute commands
   */
  public PayTpMinecraftCommandExecutor(CommandSourceStack source) {
    this.source = Objects.requireNonNull(source)
        .withMaximumPermission(PermissionSet.ALL_PERMISSIONS)
        .withSuppressedOutput();
  }

  /**
   * Creates a no-op executor for validating scripts without changing server state.
   *
   * @return an executor whose commands return {@code 0} without being run
   */
  public static PayTpMinecraftCommandExecutor validationOnly() {
    return new PayTpMinecraftCommandExecutor();
  }

  /**
   * Executes any command registered with the Minecraft server.
   *
   * <p>The leading slash is optional. The returned value is the command result reported by
   * Minecraft, or {@code 0} when the command reports no successful result.</p>
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
