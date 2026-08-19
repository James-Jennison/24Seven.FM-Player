# 24Seven.FM Cast receiver

This is the local, static source for the branded 24Seven.FM Web Receiver. It uses the
Google Cast Application Framework (CAF) v3 media player, so the receiver retains its
supported live-audio playback and remote-control behavior while presenting 24Seven.FM
branding instead of the generic Default Media Receiver screen.

## Current boundary

- This directory contains no credentials or stream URLs.
- The Android sender uses the registered Custom Receiver application through its Android resource configuration.
- The receiver is served as static content from the existing Player web origin under the Cast path, using the
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
