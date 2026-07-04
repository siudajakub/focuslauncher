# Anti-Distraction Landscape & Evidence Base

Reference material for FocusLauncher product decisions. Compiled 2026-07-04 from a multi-source
research pass (peer-reviewed HCI/behavioural-economics literature + competitive teardown of
launchers, blockers, and hardware). Not a status file — see `PROJECT_STATUS.md` for what is built.

## What the evidence says actually works

Ordered by strength of evidence, most-supported first.

1. **Friction / a pause + an explicit "abandon" choice before opening.** The strongest single
   result in the field. One Sec's self-nudge interstitial (breathe, then choose to continue or
   back out) was studied in a pre-registered field experiment (Grüning, Riedel & Lorenz-Spreen,
   *PNAS* 2023, N=280, 6 wks): **36% of interstitials ended in the user abandoning the open**, and
   attempts fell **37%** by week 6 — ~57% fewer opens overall, with improved momentary life
   satisfaction. Corroborated by a 1,039-user CHI 2024 longitudinal study. Mechanism: interrupt the
   System-1 autopilot so System-2 can veto (Kahneman; Fogg B=MAP — friction lowers "ability" below
   the impulse threshold).

2. **Restrictive beats advisory; but the friction of *overriding* is itself the mechanism.**
   GoalKeeper (Kim et al., ACM IMWUT 2019) and Almoallim & Sas (JMIR 2022): lockout mechanisms beat
   warning dialogs, but "if users could extend limits freely, they mostly did just that and clicked
   the reminder away." Hoong (*European Economic Review* 2021, N=629): *soft* commitment devices
   (nominally overridable, like app limits) significantly cut usage and persisted a month —
   *because* the small cost of overriding does the work.

3. **Grayscale.** The most replicated *visual* intervention. Holte & Ferraro (2020) ~38 min/day
   reduction; Dekker & Baumgartner (*Mobile Media & Communication* 2024, N=84) ~20 min/day, plus
   less "online vigilance" and lower stress. Recommended by the Center for Humane Technology.
   Important nuance: grayscale cuts **session duration**, not **pickup frequency** — so it must be
   *paired* with pickup-focused friction, not used alone.

4. **Self-monitoring with feedback (not mere tracking).** Awareness dashboards alone barely move
   behaviour ("Apple built a scale but no diet" — the Screen-Time critique; Berman: "125 pickups/day
   and I didn't change"). Feedback tied to a goal/plan is what the 204-study meta-analysis
   (PMC8447784) found best — Goals + Feedback + Planning.

5. **Implementation intentions ("if [cue] then [action]").** Gollwitzer & Sheeran 2006 meta-analysis,
   d≈0.65. Friction devices act as an externally-imposed if-then plan; intention prompts at the gate
   make it explicit.

6. **Commitment devices** (Ariely & Wertenbroch 2002; Bryan et al.). Real but *small* effects that
   often fade at follow-up, and **backfire when overly punitive** — anxiety undermines performance
   (the "Digital Detox Paradox", Galvan & Newman 2025, even found offering wellness tools can
   *increase* trust-driven use). Design implication: keep commitment gentle and reversible.

### What doesn't work / cautions
- **Awareness/tracking alone** is the weakest intervention class.
- **Friction habituates.** One Sec's pause "becomes an annoying formality by week 4" (Slate). Counter
  with *escalation, variability, or novelty* and positive reinforcement — not a fixed nag.
- **Gamification can backfire.** Diefenbach & Müssig 2019 (Habitica): all participants felt
  "punished"; streaks can recreate the compulsive-check loop they aim to break. Use reward
  mechanics sparingly and never punitively.
- **The easy "off" switch defeats everything.** The single most-exploited failure across Screen Time
  ("Ignore Limit" = one tap), Opal, Freedom: a low-cost override becomes a no-op. Raising the cost of
  the override is what holds.

## Competitive landscape (condensed)

**Minimalist launchers** — visual reduction, little/no blocking:
- *Olauncher / Olauncher CF* — text-only, no icons/folders/widgets, open-source, zero telemetry.
- *Niagara* — alphabetical scroll, "Usage Breaker" escalating in-app nudges, pop-up peeks.
- *Before Launcher* — standout **notification triage** (allow-list; rest to a low-urgency inbox).
- *Minimalist Phone* — grayscale + type-a-time-limit friction + blocking (closest hybrid).
- *LessPhone / Blank Spaces* — radical text-only home; LessPhone's 2025 delisting is a caution that
  pure-minimalism-no-monetisation is fragile.
- *Kvaesitso* (our upstream) — search-first, private, extensible; no focus features (we added them).
- Recurring failure across all pure-minimalist tools: *"I just searched the app name I was avoiding."*
  Visual calm alone doesn't stop a determined open — only friction/blocking does.

**Friction / blocking apps:**
- *One Sec* — the friction gold-standard (see evidence #1); breathing + intention + abandon.
- *Opal / Freedom / Roots / Cold Turkey* — session blocking with tiered strictness (Normal →
  escalating timeout → un-bypassable "Deep Focus/Monk/Frozen"), foolproofing (uninstall protection,
  settings-lock), and *documented* bypasses when the override is too cheap. Cold Turkey has the
  richest lock set (random-text/delay/restart/password locks).
- *ScreenZen* (free) — pause + escalating friction + intention prompt; ~30% usage drop then plateau.
- *Roots* — friction-first unlocks (breathe / meditate / nature / literally pet a dog via camera).
- *Clearspace* — do a breathing/exercise micro-task before opening; accountability partners.
- *Forest* — loss-aversion tree (cosmetic, not a real block); Deep Focus = soft app-switch lock.
- *AppBlock* — Strict Mode with a 2–10 min cooldown before it can be disabled; Wi-Fi/location triggers.

**Hardware (physical friction):** *Brick* ($59 one-time NFC puck), *Unpluq* (NFC tag + subscription),
*kSafe* (timed lockbox). Consistent finding: the power is *self-imposed physical distance*, and the
friction only holds if the software bypass path is also closed.

## Where FocusLauncher already stands

Unusually strong for this category: it combines real visual minimalism (calm home, hide/fade/
text-first distracting apps) **and** active friction (staged gate: pause → breathing/countdown →
intention + micro-step → session duration), **and** deterministic local policy (essential/distracting
classification, daily budget, focus sessions with DND, productivity windows, habit gates,
calendar-driven schedule blocks, recovery/resume, adaptive/escalating friction with an attention
drift score, weekly insights). No competitor combines minimalism + friction + schedule-awareness +
zero-telemetry + open-source. The gaps were mostly *unwired* good ideas.

## Feature backlog (prioritised)

**Shipped in this pass (2026-07-04):**
- Turn-away tracking + gentle positive reinforcement (evidence #1, #4). Gate + weekly insight.
- Grayscale that actually works: launcher-surface (always / during focus) + system-wide during
  focus via the ADB-granted daltonizer (evidence #3).
- Correctness: daily-budget & escalation no longer miscount non-launch events; gate DB write off the
  main thread; DND restore on the stale-session edge.

**High-value next (not yet built):**
1. **Gate "opened N times today" counter** (self-monitoring #4) — cheap, pairs with turn-aways.
2. **Wind-down schedule** — a nightly window that auto-applies grayscale + tighter friction (wire the
   dead quiet-hours setting; keep opt-in). Bedtime is the highest-leverage reduction window.
3. **Capped/high-cost emergency override** — raise the cost of the "off switch" (the #1 defeat path);
   the app already has an unused emergency-bypass field to build on.
4. **Notification triage** — Before Launcher's differentiator; allow-list + low-urgency inbox
   (larger: needs a NotificationListenerService).
5. **Real usage-stats reflection** — today's actual time-on-distracting-apps via UsageStats (already
   permissioned for time-blindness), shown as goal-anchored feedback.
6. **Escalating extension friction** — extra friction specifically on repeat "just 5 more minutes"
   unlocks (closes the loophole Screen Time/One Sec leave open).

**Deliberately avoided:** punitive streaks / heavy gamification (backfire evidence); cloud/accounts
(local-first principle); anything that makes essential apps inherit distracting-app friction.
