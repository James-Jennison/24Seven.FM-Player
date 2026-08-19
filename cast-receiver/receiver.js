/* global cast */

(() => {
  "use strict";

  const context = cast.framework.CastReceiverContext.getInstance();
  const debugLogger = cast.debug.CastDebugLogger.getInstance();
  const logTag = "24Seven.FM Receiver";

  context.addEventListener(cast.framework.system.EventType.READY, () => {
    debugLogger.setEnabled(true);
    debugLogger.showDebugLogs(true);
    debugLogger.info(logTag, "Receiver framework ready");
  });

  context.addEventListener(cast.framework.system.EventType.SENDER_CONNECTED, () => {
    debugLogger.info(logTag, "Sender connected");
  });

  context.addEventListener(cast.framework.system.EventType.SENDER_DISCONNECTED, () => {
    debugLogger.warn(logTag, "Sender disconnected");
  });

  // Keep CAF's built-in live-audio player and its media controls. The Android sender
  // supplies the station artwork and media metadata for the currently selected station.
  context.start({
    statusText: "24Seven.FM is ready",
  });
})();
