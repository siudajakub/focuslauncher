# Favorites

Favorites allow even faster access to selected items.

Favorites consist of:

- Manually sorted pinned items
- Automatically sorted pinned items (sorted by how frequently you use them)
- Frequently used items (if there is room)

## Pin apps to favorites

You can pin any app and any search result to favorites by long pressing it and selecting the <span class="material-symbols-rounded">star_border</span> icon. Remove items by pressing the same icon again.

Some apps allow the creation of home screen shortcuts. These shortcuts will also appear in the favorites section.

::: warning
Pinned shortcuts currently bypass the owning application's focus gate. This is a known pre-release
bug tracked in [issue #98](https://github.com/siudajakub/focuslauncher/issues/98).
:::

## Access favorites

Favorites can appear in three different places:

- Above (or below) the app grid when the favorites panel is enabled
- In the [favorites widget](/docs/user-guide/widgets/favorites-widget) (not enabled by default)
- In the [clock widget](/docs/user-guide/widgets/clock#dynamic-components) (not enabled by default)

> [!INFO]
> If you have the calendar widget enabled, pinned calendar events will not appear in any of the above places. Instead, they will appear in the calendar widget.

## Customization

Favorites are customized from the favorites panel or widget: open its edit/configure action to show
the Edit Favorites sheet. The current settings hub has no separate Favorites route.

In this panel, you can

- rearrange items / change their order (note that reordering items in the “automatically sorted” section will have no effect, you need to move them to the “manually sorted” section first)
- add app shortcuts
- manage [tags](tags)
- configure frequently used items
  - how many rows of frequently used items should be shown (including the last row of pinned items which will be filled first)
  - disable non-pinned frequently used altogether
