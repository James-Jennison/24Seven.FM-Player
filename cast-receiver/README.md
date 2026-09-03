# 24Seven.FM Cast receiver

This directory is the canonical static source for the custom Google Cast Web Receiver used by the native mobile
Player. `scripts/stage-cast-receiver.sh` copies only the reviewed runtime allowlist into `_site/cast/`; every full
Player-site artifact therefore preserves the receiver during atomic deployment.

The receiver contains no credentials, account state, or stream URLs. Google Cast supplies media and station metadata
from the native sender at runtime. Local validation checks the dependency graph, Content Security Policy, JavaScript
syntax, and required namespace before the composed site artifact can be deployed.
