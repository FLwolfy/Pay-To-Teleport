package com.flwolfy.paytp.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Provides unrestricted system-shell execution to the PayTp JEXL {@code shell} namespace.
 *
 * <p>Commands run synchronously with the permissions and environment of the Minecraft process.
 * Standard output and standard error are consumed concurrently to prevent subprocess pipe
 * deadlocks.</p>
 */
public final class PayTpShellExecutor {

  private PayTpShellExecutor() {}

  /**
   * Executes a command through the native platform shell.
   *
   * @param command the complete shell command
   * @return the exit code, standard output, and standard error
   * @throws IllegalStateException if the process cannot start, is interrupted, or its output
   *     cannot be read
   */
  public static ShellResult execute(String command) {
    Process process;
    try {
      process = new ProcessBuilder(shellCommand(command)).start();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to start shell command", e);
    }

    CompletableFuture<String> stdout = readAsync(process.getInputStream());
    CompletableFuture<String> stderr = readAsync(process.getErrorStream());

    try {
      int exitCode = process.waitFor();
      return new ShellResult(exitCode, stdout.join(), stderr.join());
    } catch (InterruptedException e) {
      process.destroyForcibly();
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Shell command was interrupted", e);
    }
  }

  /**
   * Executes a command and returns its standard output.
   *
   * @param command the complete shell command
   * @return standard output exactly as emitted by the process
   * @throws IllegalStateException if execution fails or the process exits with a non-zero code
   */
  public static String run(String command) {
    ShellResult result = execute(command);
    if (result.exitCode() != 0) {
      throw new IllegalStateException(
          "Shell command exited with code " + result.exitCode()
              + ": " + result.stderr().strip()
      );
    }
    return result.stdout();
  }

  /**
   * Executes a command and parses its trimmed standard output as an integer.
   *
   * @param command the complete shell command
   * @return the integer emitted to standard output
   * @throws IllegalStateException if execution fails or returns a non-zero exit code
   * @throws IllegalArgumentException if standard output is not exactly one valid integer
   */
  public static Integer runInt(String command) {
    return parseInt(run(command));
  }

  private static Integer parseInt(String value) {
    String output = value.strip();
    try {
      return Integer.valueOf(output);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Shell command must output exactly one valid integer, but got: " + output,
          e
      );
    }
  }

  private static List<String> shellCommand(String command) {
    if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
      return List.of("cmd.exe", "/c", command);
    }
    return List.of("/bin/sh", "-c", command);
  }

  private static CompletableFuture<String> readAsync(java.io.InputStream stream) {
    return CompletableFuture.supplyAsync(
        () -> {
          try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
          } catch (IOException e) {
            throw new IllegalStateException("Failed to read shell command output", e);
          }
        },
        command -> Thread.ofVirtual().start(command)
    );
  }

  /**
   * Captures the complete observable result of a shell process.
   *
   * @param exitCode the process exit code
   * @param stdout the decoded UTF-8 standard output
   * @param stderr the decoded UTF-8 standard error
   */
  public record ShellResult(
      int exitCode,
      String stdout,
      String stderr
  ) {

    /**
     * Parses the trimmed standard output as an integer.
     *
     * @return the integer contained in {@link #stdout()}
     * @throws IllegalArgumentException if standard output is not exactly one valid integer
     */
    public Integer stdoutInt() {
      return parseInt(stdout);
    }
  }
}
