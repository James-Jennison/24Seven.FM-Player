# M23.6 startup and large-Favorites performance validation

Validated July 16, 2026 for the `0.1.0-alpha01` candidate.

## Outcome

The app keeps a full 1,500-track VIP/RIP Favorites list stable while unrelated playback metadata and request state
change. Favorites availability is now resolved only when the selected Favorites or Queue data changes, and unchanged
availability reuses the exact existing state and track-list instances instead of copying every row. The default
position view also reuses an already ordered list, while filtering avoids a per-row sequence allocation.

The screen remains a Compose `LazyColumn`; the test now traverses to track 1,500 and back to the sort controls rather
than proving only the initial viewport. No visible layout or behavior was changed in this checkpoint.

## Diagnostic measurements

Measurements were taken on the API 35 phone AVD from a debug build with normal display and font settings:

| Check | Result |
| --- | --- |
| Five process-stopped launches (`am start -W`) | 948–986 ms `TotalTime`; 969 ms median |
| Immediate post-launch memory snapshot | 118,645 KiB total PSS; 218,812 KiB total RSS; 14,776 KiB Java heap PSS |
| Focused 1,500-track connected test | Passed in 2.071 seconds |

These values are local diagnostics, not release budgets. Android recommends measuring controlled startup and UI
performance with Macrobenchmark and notes that emulator measurements are not representative of physical-device
performance. Google Play Android vitals and a protected release candidate remain the authoritative release evidence.
See [App startup time](https://developer.android.com/topic/performance/vitals/launch-time),
[Benchmark your app](https://developer.android.com/topic/performance/benchmarking/benchmarking-overview),
[Macrobenchmark overview](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview), and
[`dumpsys` diagnostics](https://developer.android.com/tools/dumpsys).

## Automated verification

- 132/132 debug unit tests passed. New regression coverage proves that an unrelated now-playing update preserves the
  exact 1,500-track list instance and that position sorting reuses an already ordered list.
- The focused 1,500-track connected test passed after scrolling to the last track, returning to the controls, and
  exercising sorting.
- 40/40 connected tests passed on the API 35 Pixel Tablet at restored normal settings.
- Debug lint passed.

## Remaining release checks

Play-delivered startup and memory data, Android vitals, pre-launch crawler results, and a protected signed candidate
still belong to the M23 release gates. A dedicated Macrobenchmark module can be added if Play or physical-device data
shows a regression that requires repeatable performance tracking; the current local measurements do not justify the
additional test module and build surface by themselves.
