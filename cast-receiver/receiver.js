/* global cast */

(() => {
  "use strict";

  const context = cast.framework.CastReceiverContext.getInstance();
  const playerManager = context.getPlayerManager();
  const debugLogger = cast.debug.CastDebugLogger.getInstance();
  const logTag = "24Seven.FM Receiver";
  const diagnosticPanel = document.getElementById("cast-diagnostics");

  function showDiagnostic(message) {
    diagnosticPanel.textContent = message;
    diagnosticPanel.hidden = false;
  }

  const eventTypes = cast.framework.events.EventType;
  playerManager.addEventListener(
    [
      eventTypes.PLAYER_LOADING,
      eventTypes.PLAYER_LOAD_COMPLETE,
      eventTypes.MEDIA_STATUS,
    ],
    (event) => {
      const message = `Player event: ${event.type}`;
      debugLogger.info(logTag, message);
      showDiagnostic(message);
    },
  );
  playerManager.addEventListener(eventTypes.ERROR, (event) => {
    // Keep diagnostics useful without logging a media URL or sender payload.
    const message =
      `Player error: detail=${event.detailedErrorCode ?? "unknown"} ` +
      `reason=${event.reason ?? "unknown"} severity=${event.severity ?? "unknown"}`;
    debugLogger.error(logTag, message);
    showDiagnostic(message);
  });

  context.addEventListener(cast.framework.system.EventType.READY, () => {
    debugLogger.setEnabled(true);
    debugLogger.showDebugLogs(true);
    debugLogger.info(logTag, "Receiver framework ready");
    showDiagnostic("Receiver ready; awaiting media load");
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
