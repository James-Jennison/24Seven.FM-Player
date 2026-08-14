# Closed-alpha onboarding: verified working path

## Purpose

Use the existing Play Console email list as the source of eligibility for the
24Seven.FM Player Alpha track. The portal approves a tester first, prepares the
official opt-in link handoff, and records only coordinator actions, mail
transport outcomes, and tester confirmations. Play opt-in itself remains an
explicit tester action; the portal must never claim that it has observed an
individual opt-in unless a permitted source proves it.

## Boundary

The portal must never add a person from a raw public signup directly to the
Play eligibility list. The working transition is:

```text
Turnstile-verified signup → coordinator approval → coordinator adds address
to the existing Alpha Testers email list → opt-in link handoff → tester
confirmation → focused Tester Task
```

The existing private Tester Queue remains the coordinator approval point.
Tester emails, device data, and all identity credentials remain private.

## Observed Group limitation

Cloud Identity Free was set up and its private custom-domain group currently
contains the established Alpha testers. Play Console did not recognize that
custom-domain group as a selectable tester source, even after propagation was
allowed for and rechecked. Therefore, do not use that group as the current
Play eligibility mechanism and do not remove it or its members as a corrective
step.

A future, separately approved experiment may validate a standard Google Group
whose address ends in `@googlegroups.com`. Until its Play compatibility and a
supported membership-management route are both verified, it is not part of the
production onboarding design.

## Portal states

| Portal state | Meaning | Evidence retained |
| --- | --- | --- |
| `pending_review` | Signup is awaiting a coordinator. | Intake record only. |
| `approved` | Coordinator accepted the tester, but no external change occurred. | Approval timestamp. |
| `play_list_pending` | Approved tester awaits coordinator addition to the existing Alpha Testers email list. | Requested timestamp. |
| `play_list_added` | Coordinator recorded that the address was added to the existing Alpha Testers list. | Coordinator timestamp; no Play credentials. |
| `opt_in_link_sent` | The official Play opt-in link was handed to the tester. | Mail transport outcome and timestamp. |
| `tester_confirmed` | Tester attested that they opted in. | Tester confirmation timestamp. |
| `ready_for_task` | Coordinator may assign a focused Tester Task. | Coordinator state. |
| `play_list_action_needed` | The coordinator must resolve a list-addition issue before invitation. | Safe note and timestamp. |

## Coordinator action contract

The coordinator workspace must:

- expose the exact Alpha Testers list addition as a distinct, manual
  coordinator action;
- make the official Play opt-in handoff available only after that action is
  recorded;
- record a safe outcome category and timestamp, not raw Play Console data or
  authorization material;
- never create Groups, alter Play tracks, modify releases, or remove members
  unless a separately approved action requests it.

## Non-goals

- No automatic enrollment from an unreviewed signup.
- No password collection for testers; portal access uses a short-lived,
  single-use sign-in link delivered to the registered address.
- No automatic claim that a tester installed the beta or opted into Play.
- No Play release, track, DNS, Google Cloud, Group, or eligibility-list
  mutation by this repository until a specific production authorization is
  given.
