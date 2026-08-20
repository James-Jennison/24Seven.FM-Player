/* global cast */

(() => {
  "use strict";

  const nowPlayingNamespace = "urn:x-cast:com.codeframe78.twentyfourseven.player.nowplaying";
  const context = cast.framework.CastReceiverContext.getInstance();
  const experience = document.getElementById("receiver-experience");
  const artwork = document.getElementById("now-playing-artwork");
  const title = document.getElementById("now-playing-title");
  const station = document.getElementById("now-playing-station");

  function safeText(value, fallback) {
    return typeof value === "string" && value.trim() ? value.trim().slice(0, 240) : fallback;
  }

  function safeArtworkUrl(value) {
    if (typeof value !== "string") return null;
    try {
      const url = new URL(value);
      return url.protocol === "https:" ? url.href : null;
    } catch (_) {
      return null;
    }
  }

  function renderNowPlaying(data) {
    const payload = typeof data === "string" ? JSON.parse(data) : data;
    if (!payload || payload.type !== "now-playing") return;

    title.textContent = safeText(payload.title, "Live radio");
    station.textContent = safeText(payload.stationName, "24Seven.FM");
    const artworkUrl = safeArtworkUrl(payload.artworkUrl);
    if (artworkUrl) {
      artwork.src = artworkUrl;
      artwork.hidden = false;
    } else {
      artwork.removeAttribute("src");
      artwork.hidden = true;
    }
    experience.hidden = false;
  }

  context.addCustomMessageListener(nowPlayingNamespace, (event) => {
    try {
      renderNowPlaying(event.data);
    } catch (_) {
      // Ignore malformed sender data and keep the current receiver presentation.
    }
  });

  const options = new cast.framework.CastReceiverOptions();
  options.customNamespaces = {
    [nowPlayingNamespace]: cast.framework.system.MessageType.JSON,
  };
  context.start(options);
})();
