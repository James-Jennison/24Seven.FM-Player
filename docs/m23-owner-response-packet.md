# M28–M35 site-owner response packet

Prepared July 18, 2026

Status: M28 moderation destination/receipt and M30 authorization confirmed; M29 and M47 owner facts remain

This packet consolidates the factual confirmations still needed from an authorized 24seven.FM representative. Send it
privately. Retain the original response and its sender/date privately; commit only a sanitized completion note. Do not
put reviewer passwords, cookies, tokens, private endpoints, unredacted Console captures, or private correspondence in
Git, Discord, or a public issue.

The moderation authorization already supplied for M28 authorizes app-originated abuse reports. It does not, by
itself, establish permission to use third-party names, logos, artwork, streams, screenshots, or to distribute the app.

## 1. Completed M28 Player email handoff

On July 18, 2026, the project owner confirmed that a dedicated destination is monitored for 24Seven.FM Player
moderation reports. The installed physical-Razr build then prepared one newly authorized, clearly labeled harmless
report for that fixed destination. The user reviewed and sent it exactly once from the email app, and the owner
confirmed receipt. Original correspondence is retained privately. The repository intentionally omits the address,
message headers, sender identity, and report body.

The owner directed both Contact Us and moderation-report actions to that monitored destination. The Player continues to
describe composer opening only as a handoff; it never claims delivery, reads the user's email account, embeds SMTP
credentials, or adds administrator access. Do not repeat the physical delivery subcase without a new coordinator
assignment.

## 2. Brand, content, stream, screenshot, and distribution permission

The authorized rights holder should reply with the following statement, adding their name, title/role, and date. If any
scope is not authorized, they should identify the exact excluded item rather than giving a general approval.

> I confirm that I am authorized to grant this permission on behalf of the 24seven.FM network. The 24Seven.FM Player project has non-exclusive, revocable permission to use the 24seven.FM network name; the names StreamingSoundtracks.com, 1980s.FM, Adagio.FM, Death.FM, and Entranced.FM; network and station logos; station-provided artwork and metadata; and the network's authorized public live-stream access in the native app and in privacy-reviewed Google Play materials. This permission covers development, Google Play internal, closed, and open testing, and distribution through Google Play, including a production release, of the unofficial, non-commercial 24Seven.FM Player native Android app. The network confirms that it operates or is authorized to provide the station streams used by the app and permits the app to access those streams for live playback. This permission does not transfer ownership, does not authorize audio downloading or redistribution outside live playback, does not make the app official or endorsed, remains subject to applicable network terms and third-party rights, and may be revoked in writing.
>
> Authorized representative: [full name]
>
> Title/role: [role establishing authority]
>
> Date: [date]

This statement deliberately does not claim ownership of music, album artwork, or other third-party material that the
network does not own. Any station-provided asset that is not actually covered must be omitted or replaced before Play
submission. Public store screenshots should avoid private account data and unnecessary user-generated content even
when the station permits the underlying interface.

M30 closed on August 14, 2026 with an equivalent grant from an authorized network representative. The sanitized
repository record is [m30-brand-content-distribution-authorization.md](m30-brand-content-distribution-authorization.md).
Original authority evidence remains outside Git. Use Google Play's advance-notice route if Play asks for the private
evidence before reviewing the app or listing.

## 3. Station retention facts for Privacy and Data Safety

The network-wide policy was adopted on August 14, 2026. It is recorded in
[m23-data-safety.md](m23-data-safety.md) and the public privacy notice: verified deletion requests remove or anonymize
active account/profile data and Favorites/preferences within 30 days; ordinary operational/request, Chat/forum, and
normal diagnostic/server/IP records are retained no more than 90 days; investigation records are limited to active
work and at most one year; backups are retained no more than 90 days; and lawful legal/regulatory obligations may
require longer retention. The station Contact/Feedback system reaches Network/Station Administration for verified
access, correction, and deletion requests.

Do not request a second answer to this table unless the shared policy changes or a station needs to record a documented
exception. The historical request format is preserved below for that purpose:

| Data/process | Retained? | Duration or deletion rule | Who can access it? | Shared with any processor/third party? |
| --- | --- | --- | --- | --- |
| Successful and failed sign-in submissions | | | | |
| Session records/cookies and account access logs | | | | |
| IP/security/server logs | | | | |
| Station-library search terms | | | | |
| Public Chat posts | | | | |
| Song requests, requester identity, and optional request messages | | | | |
| Abuse reports, reporter contact details, and bounded reported content | | | | |

Also state any documented exception, including a different contact path, before it is published. Do not infer an
exception from older research or describe station-side processing as ephemeral.

## 4. Google Play reviewer accounts

Create or designate one reusable, least-privileged, non-administrator account for each station. The owner should confirm:

> I confirm that reusable reviewer accounts are active for StreamingSoundtracks.com, 1980s.FM, Adagio.FM, Death.FM, and Entranced.FM; they can sign in from a clean install and a non-owner network; they do not require email OTP, IP allowlisting, an expiring password, or moderator privileges; and they will remain active throughout Google Play review and subsequent updates. The current three-character, case-sensitive CAPTCHA is completed by the reviewer for each sign-in attempt.

Send the actual usernames and passwords only through the protected Play Console App access form. Do not send them in
the site-owner response retained for project evidence. If practical, give each account a small harmless Favorites list
so reviewers can see a non-empty authenticated surface.

## 5. Private Messaging repair confirmation

M47 remains excluded from the shipping build. Resume native investigation only after the owner can provide this factual
confirmation, with real test details replacing the brackets:

> The 24seven.FM Private Messaging send path has been repaired. On [date], a harmless test message sent from [sender test account] to [recipient test account] appeared in the sender's Sent Box and the recipient's Inbox. The normal member-facing Inbox, Sent Box, compose/profile PM, read, reply, and send forms may be investigated for the native app using non-administrator test accounts. The applicable recipient, message-length, rate, storage, and account-tier limits are: [limits].

If compose/profile sends behave differently, describe each path separately. No native PM endpoint or parser will be
implemented from a success page alone; both sender and recipient delivery must be verified.

## Evidence handling and completion gates

- Keep the original rights response, sender identity, authority/role, date, and any attachment in private project records.
- Keep reviewer credentials only in Play Console and the owner's password manager.
- Record only a sanitized repository note such as: “Written authorization covering the documented M30 scope was
  received from an authorized network representative on [date]; original evidence retained privately.”
- M28 is complete: the fixed-recipient implementation passed automated/device checks and one authorized Player email
  handoff was reconciled with owner-confirmed receipt on July 18, 2026.
- M29's retention/deletion policy, processor boundary, IP-use facts, and privacy/Data Safety reconciliation are now
  documented. M29 still requires reviewer access, exact-candidate reconciliation, the final foreground-service video,
  and all active Console questionnaires to be saved and reviewed.
- M30 is complete. The authorization excludes stream redistribution and ownership transfer; third-party artwork remains
  subject to the applicable rights holder.
- M47 restarts only after verified PM delivery and limits are supplied.

## Google Play references

- [Intellectual Property policy](https://support.google.com/googleplay/android-developer/answer/9888072)
- [Provide advance notice to the Google Play App Review team](https://support.google.com/googleplay/android-developer/answer/6320428)
- [Impersonation policy](https://support.google.com/googleplay/android-developer/answer/9888374)
- [Metadata policy](https://support.google.com/googleplay/android-developer/answer/9898842)

This is a policy-evidence template, not legal advice or a guarantee of Google Play approval. Counsel should review the
final grant if there is uncertainty about ownership, authority, music licensing, trademark scope, or distribution.
