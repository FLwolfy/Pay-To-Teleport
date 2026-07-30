package com.flwolfy.paytp.data;

import java.util.Objects;

/**
 * Immutable, type-specific context exposed to a teleport price script.
 *
 * <p>Exactly one of the five context records is non-null. Scripts identify the operation by
 * checking the corresponding record and then read its type-specific data through standard record
 * accessors.</p>
 *
 * @param coordinate coordinate-command context, or {@code null}
 * @param home home context, or {@code null}
 * @param back back context, or {@code null}
 * @param request teleport-request context, or {@code null}
 * @param warp warp context, or {@code null}
 */
public record PayTpTeleportContext(
    Coordinate coordinate,
    Home home,
    Back back,
    Request request,
    Warp warp
) {

  // =======================================================
  // Number of types checker (uniqueness)
  // =======================================================

  public PayTpTeleportContext {
    int present = (coordinate == null ? 0 : 1)
        + (home == null ? 0 : 1)
        + (back == null ? 0 : 1)
        + (request == null ? 0 : 1)
        + (warp == null ? 0 : 1);
    if (present != 1) {
      throw new IllegalArgumentException(
          "Exactly one teleport context must be present"
      );
    }
  }

  // =======================================================
  // All types of builders
  // =======================================================

  public static PayTpTeleportContext coordinate(Coordinate coordinate) {
    return new PayTpTeleportContext(
        Objects.requireNonNull(coordinate, "coordinate"),
        null,
        null,
        null,
        null
    );
  }

  public static PayTpTeleportContext home(Home home) {
    return new PayTpTeleportContext(
        null,
        Objects.requireNonNull(home, "home"),
        null,
        null,
        null
    );
  }

  public static PayTpTeleportContext back(Back back) {
    return new PayTpTeleportContext(
        null,
        null,
        Objects.requireNonNull(back, "back"),
        null,
        null
    );
  }

  public static PayTpTeleportContext request(Request request) {
    return new PayTpTeleportContext(
        null,
        null,
        null,
        Objects.requireNonNull(request, "request"),
        null
    );
  }

  public static PayTpTeleportContext warp(Warp warp) {
    return new PayTpTeleportContext(
        null,
        null,
        null,
        null,
        Objects.requireNonNull(warp, "warp")
    );
  }

  // =======================================================
  // Nested record types (concrete teleport types)
  // =======================================================

  public record Coordinate() {}

  public record Home() {}

  public record Back() {}

  /**
   * @param otherPlayer the other participant
   * @param isRequester whether the player being teleported initiated the request
   */
  public record Request(
      PayTpPlayer otherPlayer,
      boolean isRequester
  ) {

    public Request {
      Objects.requireNonNull(otherPlayer, "otherPlayer");
    }
  }

  /**
   * @param name globally unique warp name
   * @param accessType one of {@code owned}, {@code invited}, {@code server}, or {@code public}
   * @param owner owner identity, or {@code null} for an ownerless server warp
   */
  public record Warp(
      String name,
      String accessType,
      PayTpPlayer owner
  ) {

    public Warp {
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(accessType, "accessType");
    }
  }

}
