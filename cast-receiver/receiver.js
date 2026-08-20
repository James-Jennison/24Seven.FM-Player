/* global cast */

(() => {
  "use strict";

  const context = cast.framework.CastReceiverContext.getInstance();
  // Keep CAF's built-in live-audio player and its media controls. The Android sender
  // supplies the station artwork and media metadata for the currently selected station.
  context.start({
    statusText: "24Seven.FM is ready",
  });
})();
