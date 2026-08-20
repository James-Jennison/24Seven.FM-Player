/* global cast */

(() => {
  "use strict";

  const nowPlayingNamespace = "urn:x-cast:com.codeframe78.twentyfourseven.player.nowplaying";
  const palettes = {
    sst: { accent: "#ffc65b", secondary: "#7188c7", glow: "#172448" },
    "1980s": { accent: "#ff4fd8", secondary: "#35dfff", glow: "#3a1647" },
    afm: { accent: "#ffb35c", secondary: "#ff755f", glow: "#432519" },
    dfm: { accent: "#b69cff", secondary: "#6e74e8", glow: "#251b49" },
    efm: { accent: "#55d9a4", secondary: "#2eb8b2", glow: "#123d38" },
  };
  const defaultPalette = { accent: "#c9a8ff", secondary: "#8b5cf6", glow: "#2b1b3f" };
  const context = cast.framework.CastReceiverContext.getInstance();
  // Start CAF player-library loading before receiver startup to reduce launch latency.
  context.loadPlayerLibraries();
  const experience = document.getElementById("receiver-experience");
  const artwork = document.getElementById("now-playing-artwork");
  const title = document.getElementById("now-playing-title");
  const station = document.getElementById("now-playing-station");
  const entityDecoder = document.createElement("textarea");

  function decodeHtmlEntities(value) {
    entityDecoder.innerHTML = value;
    return entityDecoder.value;
  }

  function safeText(value, fallback) {
    return typeof value === "string" && value.trim()
      ? decodeHtmlEntities(value).trim().slice(0, 240)
      : fallback;
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

  function applyPalette(stationId) {
    const palette = palettes[stationId] || defaultPalette;
    experience.style.setProperty("--station-accent", palette.accent);
    experience.style.setProperty("--station-secondary", palette.secondary);
    experience.style.setProperty("--station-glow", palette.glow);
  }

  function renderNowPlaying(data) {
    const payload = typeof data === "string" ? JSON.parse(data) : data;
    if (!payload || payload.type !== "now-playing") return;

    applyPalette(payload.stationId);
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
