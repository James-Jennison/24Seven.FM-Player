# 24Seven.FM Cast receiver

This is the local, static source for the branded 24Seven.FM Web Receiver. It uses the
Google Cast Application Framework (CAF) v3 media player, so the receiver retains its
supported live-audio playback and remote-control behavior while presenting 24Seven.FM
branding instead of the generic Default Media Receiver screen.

## Current boundary

- This directory is not deployed and contains no receiver application ID, credentials, or stream URLs.
- A Custom Receiver application is registered in the Cast Developer Console, but its application ID is not copied
  into source or configuration. The Android sender remains configured for the Default Media Receiver until a
  separately approved sender update.
- The intended deployment is a static `/cast/` path on the existing Player web origin, using the
  established atomic website deployment workflow.

## Behavior

- CAF renders the current media title, artist, and artwork supplied by the sender.
- The receiver uses a branded idle/launch screen, background, playback watermark, typography, and accent color.
- The receiver does not poll station APIs or write operational state.

## Local validation

Run `node --check cast-receiver/receiver.js`. Browser/Cast-device acceptance requires the separately approved
staging deployment and Android sender update.

## Deferred follow-up

- [ ] When a native Android TV product is approved, define its final package name, implement its Cast Connect
  receiver and `MediaSession` lifecycle, then associate that package with this Cast application in the Developer
  Console. Do not reserve or register a package name before the Android TV app exists.
