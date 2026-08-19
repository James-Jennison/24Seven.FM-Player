# Marketing asset capture

Use an attached Android device and `scripts/capture-marketing-assets.sh` to make review-only files under the gitignored `captures/` directory. The script enters System UI Demo Mode, captures the explicit physical display, crops status/navigation chrome, and exits Demo Mode through its cleanup trap.

Find the active display first:

```bash
adb shell dumpsys SurfaceFlinger --display-id
```

Then capture a reviewed screen. Only promote the derived PNG to `docs/play-store-assets/marketing/` after visually checking it and confirming that it contains no private account, identity, timestamp, notification, or other sensitive information.

```bash
ANDROID_DISPLAY_ID=<physical-display-id> scripts/capture-marketing-assets.sh now-playing
ANDROID_DISPLAY_ID=<physical-display-id> scripts/capture-marketing-assets.sh queue
```

## Chat boundary

Never capture an organic station room for public marketing. Use a temporary native Compose instrumentation scene with fictional display names, messages, and `Demo` timestamps, render it in the Player's dark theme, and then run:

```bash
MARKETING_CHAT_MODE=synthetic ANDROID_DISPLAY_ID=<physical-display-id> scripts/capture-marketing-assets.sh chat
```

The Chat image in this branch was captured on the connected Motorola Razr from such a controlled synthetic scene. It uses fictional screen names and messages only. The current source assets were captured live on-device: `now-playing-live.png`, `queue-live.png`, and `favorites-live.png`; `chat-synthetic.png` is the controlled Chat capture.

## Video fallback

The connected Razr's native `screenrecord` command was attempted on its explicit primary display at both its native and reduced capture sizes. Each attempt completed its time limit but encoded zero video frames. Do not synthesize a recording from a static frame or claim a loop exists. Until a device/encoder path produces actual frames, the homepage's **See it playing** control remains an honest anchor to the real Queue and controlled Chat captures.

Raw captures, temporary recordings, and unreviewed conversions stay under `captures/` or `/tmp` and are never committed.
