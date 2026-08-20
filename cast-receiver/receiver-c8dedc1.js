/* global cast */

(function startReceiver() {
  "use strict";

  const context = cast.framework.CastReceiverContext.getInstance();
  const debugLogger = cast.debug.CastDebugLogger.getInstance();

  context.addEventListener(cast.framework.system.EventType.READY, () => {
    // Keep the diagnostic dependency available for this launch-validation release,
    // without rendering an on-screen debug overlay for listeners.
    debugLogger.setEnabled(true);
    debugLogger.showDebugLogs(false);
  });

  // Keep CAF's built-in live-audio player and its media controls. The Android sender
  // supplies the station artwork and media metadata for the currently selected station.
  context.start({
    statusText: "24Seven.FM is ready",
  });
})();
