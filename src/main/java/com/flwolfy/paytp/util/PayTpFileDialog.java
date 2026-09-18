package com.flwolfy.paytp.util;

import com.flwolfy.paytp.PayTpMod;

import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;

import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLMessageBox;
import org.lwjgl.sdl.SDLProperties;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.system.MemoryUtil;

/**
 * Native file picker and message box access for the client-side configuration screens.
 *
 * <p>Earlier game versions reached the operating system file picker through LWJGL's
 * {@code tinyfd} module. Minecraft 26.3 replaced GLFW with SDL3 and no longer ships
 * {@code lwjgl-tinyfd}, but it does ship {@code lwjgl-sdl} together with its native
 * libraries, so the dialogs are now driven by SDL3's file dialog API.</p>
 *
 * <p>Unlike {@code tinyfd_openFileDialog}, SDL's dialog API is asynchronous: the chosen
 * path arrives through a native callback that SDL invokes on the thread that pumps
 * events. Everything the callback touches - the filter struct, the UTF-8 strings it
 * points at, and the SDL property set - therefore has to stay reachable until the
 * callback fires, which is what {@link #PENDING} tracks.</p>
 */
public final class PayTpFileDialog {

  private static final AtomicLong NEXT_REQUEST_ID = new AtomicLong(1L);

  /** Requests handed to SDL that have not reported back yet, keyed by the userdata id. */
  private static final Map<Long, Request> PENDING = new ConcurrentHashMap<>();

  /** Callbacks whose dialog already answered, released once they are certainly off the stack. */
  private static final Deque<SDL_DialogFileCallback> RETIRED = new ConcurrentLinkedDeque<>();

  private PayTpFileDialog() {}

  /**
   * Opens the operating system file picker and reports the selection asynchronously.
   *
   * @param title the dialog window title
   * @param filterLabel the human readable filter description, e.g. {@code "JEXL scripts"}
   * @param filterPattern the semicolon separated extension list, e.g. {@code "jexl"} or
   *     {@code "jpg;jpeg;png"}. SDL only accepts {@code [a-zA-Z0-9_.-]} or a lone {@code "*"}
   *     here, so glob patterns such as the tinyfd style {@code "*.jexl"} are rejected.
   * @param parentWindow the {@code SDL_Window*} the dialog should be modal for, or
   *     {@link MemoryUtil#NULL} to let SDL pick one
   * @param onResult receives the selected path, or {@code null} when the user cancels; it is
   *     always invoked on the client thread
   */
  public static void openFile(
      String title,
      String filterLabel,
      String filterPattern,
      long parentWindow,
      Consumer<String> onResult
  ) {
    releaseRetiredCallbacks();

    PayTpMod.LOGGER.info(
        "PayTp: opening SDL file dialog (videoInit={}, videoDriver={})",
        SDLInit.SDL_WasInit(SDLInit.SDL_INIT_VIDEO),
        SDLVideo.SDL_GetCurrentVideoDriver()
    );

    long requestId = NEXT_REQUEST_ID.getAndIncrement();

    SDL_DialogFileFilter.Buffer filters = SDL_DialogFileFilter.calloc(1);
    ByteBuffer filterLabelUtf8 = MemoryUtil.memUTF8(filterLabel);
    ByteBuffer filterPatternUtf8 = MemoryUtil.memUTF8(filterPattern);
    filters.name(filterLabelUtf8);
    filters.pattern(filterPatternUtf8);

    int properties = SDLProperties.SDL_CreateProperties();
    SDLProperties.SDL_SetStringProperty(
        properties,
        SDLDialog.SDL_PROP_FILE_DIALOG_TITLE_STRING,
        title
    );
    SDLProperties.SDL_SetPointerProperty(
        properties,
        SDLDialog.SDL_PROP_FILE_DIALOG_FILTERS_POINTER,
        filters.address()
    );
    SDLProperties.SDL_SetNumberProperty(
        properties,
        SDLDialog.SDL_PROP_FILE_DIALOG_NFILTERS_NUMBER,
        1L
    );
    SDLProperties.SDL_SetBooleanProperty(
        properties,
        SDLDialog.SDL_PROP_FILE_DIALOG_MANY_BOOLEAN,
        false
    );
    if (parentWindow != MemoryUtil.NULL) {
      SDLProperties.SDL_SetPointerProperty(
          properties,
          SDLDialog.SDL_PROP_FILE_DIALOG_WINDOW_POINTER,
          parentWindow
      );
    }

    SDL_DialogFileCallback callback = SDL_DialogFileCallback.create(
        (userdata, filelist, filter) -> complete(userdata, filelist)
    );

    Request request = new Request(
        filters,
        filterLabelUtf8,
        filterPatternUtf8,
        properties,
        callback,
        new AtomicBoolean(false),
        onResult
    );
    PENDING.put(requestId, request);

    SDLError.SDL_ClearError();
    SDLDialog.SDL_ShowFileDialogWithProperties(
        SDLDialog.SDL_FILEDIALOG_OPENFILE,
        callback,
        requestId,
        properties
    );

    // SDL reports "I refuse to show this dialog" by invoking the callback with a NULL list
    // *before* the show call returns. Only after it returns can a NULL list mean "cancelled".
    request.dispatched().set(true);

    String error = SDLError.SDL_GetError();
    PayTpMod.LOGGER.info(
        "PayTp: SDL file dialog requested (parentWindow={}, sdlError='{}')",
        parentWindow,
        error == null ? "(none)" : error
    );
  }

  /**
   * Shows a blocking native error dialog.
   *
   * @param title the dialog window title
   * @param message the message body
   */
  public static void showError(String title, String message) {
    SDLMessageBox.SDL_ShowSimpleMessageBox(
        SDLMessageBox.SDL_MESSAGEBOX_ERROR,
        title,
        message,
        MemoryUtil.NULL
    );
  }

  private static void complete(long userdata, long filelist) {
    Request request = PENDING.remove(userdata);
    if (request == null) {
      PayTpMod.LOGGER.warn("PayTp: file dialog callback for unknown request {}", userdata);
      return;
    }

    // Read SDL's error first: the release calls below touch SDL state and would clobber it.
    String sdlError = SDLError.SDL_GetError();
    boolean dispatched = request.dispatched().get();

    // The path list is owned by SDL and only valid for the duration of the callback.
    String selected = null;
    if (filelist != MemoryUtil.NULL) {
      long first = MemoryUtil.memGetAddress(filelist);
      if (first != MemoryUtil.NULL) {
        selected = MemoryUtil.memUTF8(first);
      }
    }

    if (selected == null && !dispatched) {
      PayTpMod.LOGGER.error(
          "PayTp: SDL refused to show the file dialog (sdlError='{}')",
          sdlError == null ? "(none)" : sdlError
      );
    } else {
      PayTpMod.LOGGER.info(
          "PayTp: file dialog returned '{}'",
          selected == null ? "(cancelled)" : selected
      );
    }

    request.releaseDialogState();

    // The trampoline cannot be freed here; it is still on the stack. Queue it instead and
    // let the next dialog call, which necessarily happens after this one returned, free it.
    RETIRED.add(request.callback());

    if (request.onResult() != null) {
      deliver(request.onResult(), selected);
    }
  }

  /**
   * Runs the caller's callback on the client thread.
   *
   * <p>SDL runs file dialogs on its own thread, so {@code complete} is reached off the render
   * thread. Callers touch configuration models and UI state, which must not happen there.</p>
   */
  private static void deliver(Consumer<String> onResult, String selected) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.isSameThread()) {
      onResult.accept(selected);
      return;
    }

    final String path = selected;
    minecraft.execute(() -> onResult.accept(path));
  }

  private static void releaseRetiredCallbacks() {
    SDL_DialogFileCallback callback;
    while ((callback = RETIRED.poll()) != null) {
      callback.free();
    }
  }

  /** Native state that has to outlive {@link #openFile} until SDL calls back. */
  private record Request(
      SDL_DialogFileFilter.Buffer filters,
      ByteBuffer filterLabel,
      ByteBuffer filterPattern,
      int properties,
      SDL_DialogFileCallback callback,
      AtomicBoolean dispatched,
      Consumer<String> onResult
  ) {

    /**
     * Frees the dialog's plain native memory.
     *
     * <p>The callback trampoline is deliberately left alone here: LWJGL backs callbacks with
     * libffi closures, and freeing one runs {@code ffi_closure_free} plus a JNI global
     * reference release on the very closure that is executing this method.</p>
     */
    private void releaseDialogState() {
      filters.free();
      MemoryUtil.memFree(filterLabel);
      MemoryUtil.memFree(filterPattern);
      SDLProperties.SDL_DestroyProperties(properties);
    }
  }
}
