# Material Expressive UI Cleanup Design

Date: 2026-04-02
Project: FocusLauncher (`kvaesitso` fork)
Status: Approved design, pending implementation planning

## Goal

Bring all recently added or visually inconsistent UI back into a single, coherent Material 3 Expressive language.

The visual baseline for this cleanup is:

- the current launcher home where search still reflects the earlier design language
- the original clock widget presentation
- the settings experience in `Advanced`
- the settings experience in `Appearance`
- the settings experience in `Home Screen`

The cleanup should favor Material 3 Expressive system patterns over custom styling, while preserving the product direction of a calm, focus-first launcher.

## Product Principles

- Prefer calm over decorative.
- Prefer hierarchy over visual noise.
- Prefer Material Expressive components over custom-built lookalikes.
- Keep launcher surfaces recognizable as one product, not multiple mini-products.
- Let emphasis come from layout, typography, shape, tonal layers, and motion rather than heavy ornament.

## Visual Direction

The selected direction is `System First`.

This means:

- new focus and settings surfaces should align with Material 3 Expressive component behavior and spacing
- custom gradients, hand-built panel treatments, and competing container styles should be reduced
- sections should use clear surface levels and a smaller number of visual accents
- actions should have a stronger primary/secondary hierarchy

This direction is intentionally not `Settings Parity` for every screen. The launcher should still feel like a launcher, not a flat settings list. It is also not `Soft Refresh`; the goal is a stronger correction toward a system-led design language.

## Baseline Design Rules

### Containers

- Use one consistent card or surface hierarchy within a screen.
- Avoid mixing several equally prominent card styles in the same flow.
- Use tonal surfaces and shape tokens from the app theme instead of ad hoc borders, fills, and decorative backgrounds.
- Reserve the strongest emphasis for the most important module on screen.

### Actions

- Each section should have at most one clearly primary action.
- Secondary actions should move to tonal, outlined, or text treatments depending on importance.
- Avoid presenting multiple equal-weight buttons stacked together unless the task truly requires peer actions.

### Typography

- Use Material Expressive type scale as the main hierarchy tool.
- Section titles should be visually stable across focus screens, sheets, and settings.
- Supporting copy should use `onSurfaceVariant` and not compete with primary content.

### Spacing and Rhythm

- Standardize section spacing, internal padding, and vertical grouping across new focus surfaces.
- Match the calmer pacing already visible in `Appearance`, `Advanced`, and `Home Screen`.
- Reduce dense or overly loose compositions introduced in newer features.

### Components

- Prefer standard Material 3 Expressive components or close wrappers already used by the codebase.
- Avoid custom structures when a `Preference`, list row, card section, button group, or expressive selector already fits.
- When previews are needed, embed them inside a stable system container instead of inventing a separate visual language.

## Scope

### In Scope

- launcher focus home surfaces and supporting cards
- focus-related settings and newly added settings sections that visually diverge from the baseline
- recent sheets and custom settings UI that do not align with baseline Material behavior
- CTA hierarchy and container consistency across new focus flows
- visual polish needed to align newer functionality with Material 3 Expressive

### Out of Scope

- rewriting stable reference screens that already fit the desired baseline
- unrelated architectural refactors
- broad launcher behavior changes unrelated to visual or interaction consistency

## Screen-by-Screen Design

### Main Settings and Subscreens

Reference behavior comes from `PreferenceScreen`, `PreferenceCategory`, and the existing calm settings structure.

Rules:

- Use preference-driven layouts wherever the content is primarily configuration.
- Remove or simplify custom settings sections that visually read unlike the rest of settings.
- Convert divergent rows, cards, or custom panels back toward established settings patterns.
- Keep previews or rich controls only when they materially improve understanding, and contain them within the same visual system.

Target areas include newer focus-related settings and any changed settings surface that currently competes with `Appearance`, `Advanced`, or `Home Screen`.

### Focus Home

`FocusHomeComponent` should stop reading as a separate product surface.

Desired structure:

1. Clock widget remains the strongest anchor.
2. Daily schedule summary appears as the first supporting section.
3. Dock or suggested apps follows using the same section system.
4. Habits, focus session, and agenda continue as quiet modular sections.

Rules:

- Use one shared section model for focus cards.
- Keep each section to one title, one supporting explanation, and one primary action path.
- Reduce visual competition between sections.
- Keep action hierarchy explicit: one main action, one lower-emphasis alternative when needed.
- Reuse common spacing, shape, and tonal elevation across schedule, habits, session, and agenda.

Expected cleanup examples:

- session controls should no longer be multiple equal-weight outlined CTAs
- section containers should align in padding, shape, and prominence
- informational text should recede relative to primary task content

### Focus Gate

`FocusGate` is the one intentional exception to the more restrained surfaces because it is a full-screen interruption flow and the animation is part of the product experience.

What remains:

- the transition and launch animation
- the gradient treatment as the dramatic backdrop

What changes:

- foreground content should align more tightly with Material 3 Expressive
- button hierarchy should become clearer and more system-like
- text fields and selectors should use current Material components and layout behavior
- spacing, grouping, and surface treatments should feel related to the rest of the app
- the screen should scale better across compact and tall layouts

Principle:

The background may remain expressive and cinematic, but the interactive layer should feel like Material Expressive rather than a custom one-off interface.

### Sheets and Rich Editors

For newer sheets and configuration flows:

- prefer standard top app bar and bottom action structure already used successfully in the app
- reduce one-off panel styles unless the flow truly requires them
- keep content grouped into legible, stable sections
- align sheet CTA patterns with the same primary/secondary model used elsewhere

## Shared UI Pattern Work

Implementation should first introduce or consolidate shared patterns rather than fixing every screen independently.

Candidates for shared patterns:

- a common focus section container
- shared section header and action layout rules
- common CTA hierarchy for focus flows
- reusable preference-like row treatments for custom settings content
- expressive but system-aligned sheet scaffolding where needed

This should reduce repeated style drift and make later cleanup cheaper.

## Interaction and UX Guidance

- Prefer fewer decisions per surface.
- Make the next action obvious.
- Use motion to support state change, not to replace hierarchy.
- Avoid making every new feature feel “special”; reserve emphasis for moments that matter.
- Keep settings predictable and scannable.
- Keep launcher home supportive and low-noise.

## Technical Strategy

Implementation should happen in two passes.

### Pass 1: Shared Patterns

- identify the repeated focus and settings UI patterns that drifted
- extract or normalize reusable section structures
- define the intended CTA hierarchy and container usage
- centralize the rules that can prevent future inconsistency

### Pass 2: Screen Cleanup

- migrate focus home to the shared section system
- align focus settings and changed settings screens to the settings baseline
- revise sheets and rich editors that currently diverge
- polish `FocusGate` so it keeps its animation and gradient but uses a more Material foreground layer

## Validation Criteria

The cleanup is successful when:

- changed screens clearly feel like they belong to the same app as `Appearance`, `Advanced`, and `Home Screen`
- focus home no longer reads as a separate design language
- focus gate still feels dramatic, but its controls and layout read as Material Expressive
- settings screens return to a predictable, scannable system
- there are fewer custom one-off visual treatments and more reusable patterns

## Risks and Mitigations

### Risk: Over-flattening the launcher

If cleanup pushes everything too close to plain settings UI, the launcher can lose personality.

Mitigation:

- keep the clock widget as a visual anchor
- preserve launcher-specific composition
- allow `FocusGate` to remain the expressive exception

### Risk: Screen-by-screen fixes create new inconsistency

Ad hoc cleanup can simply replace one mismatch with another.

Mitigation:

- implement shared patterns first
- reuse them across focus sections and changed settings surfaces

### Risk: Material compliance without product fit

Blindly using components can still produce a clumsy experience.

Mitigation:

- treat Material Expressive as the system language
- continue matching the fork’s calm, focus-first intent

## Implementation Handoff

The next step is to create an implementation plan that:

- identifies the exact screens and components to refactor first
- defines shared UI abstractions to introduce or normalize
- separates safe behavior-preserving polish from flows that need deeper layout changes
- includes verification steps for visual consistency and regression risk
