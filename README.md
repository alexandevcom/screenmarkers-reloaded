# Screen Markers Reloaded

A [RuneLite](https://runelite.net) plugin for markers that appear only when you need them.

The stock Screen Markers plugin draws a static rectangle that is always on screen. This plugin adds two things: markers can be attached to things in the world (a tile, an object type, an item, an interface element) rather than just to screen coordinates, and every marker's visibility is driven by **triggers** — conditions like "the bank is open", "my inventory is full", or "I have fewer than 5 sharks left".

## Marker types

| Type | Attached to | How you create it |
| --- | --- | --- |
| **Screen rectangle** | Fixed canvas coordinates | Drag a box on the client |
| **Tile** | A world tile | Click the tile |
| **Object type** | Every instance of an object ID on screen | Click one of the objects |
| **Inventory item** | An item ID, in the inventory and in the bank | Click the item |
| **Widget / spell** | An interface element — a spell, prayer, button | Click the element |
| **Ground item** | Every tile holding a given item ID | Click the ground item |

Object, ground item and inventory markers track *the ID*, not one instance: mark one yew tree and every yew tree on screen is highlighted; mark a shark and it lights up in the inventory and in the bank.

## Triggers

Each marker holds a list of triggers combined with **ALL** (every trigger must match) or **ANY** (at least one). Any trigger can be individually inverted with a NOT checkbox.

| Trigger | Matches when |
| --- | --- |
| **Always** | Unconditionally — the default |
| **Bank open** | The bank interface is open |
| **Animation** | Your player is playing a given animation ID, or any animation at all if the ID is left at `-1` |
| **Inv full** | All 28 inventory slots are used |
| **Inv empty** | No inventory slots are used |
| **Has item** | Inventory holds an item, with a `≥` / `≤` / `=` count comparison |
| **Tab active** | A given side panel tab is open — inventory, prayer, magic, etc. |

Combining a couple of these covers most of what you'd otherwise write a bespoke plugin for. "Highlight the deposit-all button, but only while the bank is open and my inventory is full" is two triggers on a widget marker.

## Groups

Markers can be organised into groups. A group has its own enable toggle that gates every marker inside it, so you can switch a whole setup on and off at once.

Groups are shareable. **Export (copy to clipboard)** in a group's `⋮` menu puts the group and its markers on the clipboard as JSON; **Import** on the panel reads that JSON back. Imported groups and markers are assigned fresh IDs, so importing a group you already have will not clobber the existing one.

## Per-marker appearance

Border colour, fill colour, border thickness, and an optional name label rendered at the centre of the marker.

## Usage

The plugin adds a side panel (the toolbar icon). From there:

- **+ New marker ▾** — pick a marker type, then click or drag in the client to place it. `Esc` cancels.
- **+ Group** / **Import** — create a group, or paste one in from the clipboard.
- Each marker row expands (`▶`) to expose its group, triggers, and colours.
- The `⋮` menu on a marker moves it between groups, re-picks its target, or deletes it.

Markers and groups are saved to the RuneLite config under the `smreloaded` group and persist across sessions.

## Building

Requires **JDK 11 or newer**. The build resolves the RuneLite client from `repo.runelite.net` at `latest.release`.

```sh
./gradlew build
```

To launch a RuneLite client with the plugin loaded:

```sh
./gradlew run
```

On Windows, `launch.bat` does the same thing in the background.

If your `JAVA_HOME` points at a JDK 8 installation the build fails with `invalid flag: --release`. Point it at a newer JDK:

```sh
JAVA_HOME=/path/to/jdk-11 ./gradlew build
```

## License

BSD 2-Clause. See [LICENSE](LICENSE).
