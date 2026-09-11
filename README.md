# InvisiblePlayers

InvisiblePlayers is a Paper plugin that hides players affected by the Minecraft
Invisibility potion effect from other players' tab/player lists, and can replace an
invisible killer's identity in public death messages with a configurable placeholder.
It does not make the player entity itself invisible — that is governed entirely by
vanilla Minecraft rules.

## Features

### Tab-list invisibility

The plugin's only source of truth for "is this player invisible" is the live Bukkit/
Paper potion effect state — whether the player currently has `PotionEffectType.INVISIBILITY`.
There is no permission, name list, or cached flag involved.

- A player under the Invisibility effect is removed from every *other* online player's
  tab list. They are never removed from their own tab list.
- When the effect ends, by any means, the player is restored to everyone's tab list.
- A player who joins while someone else is already invisible never sees that player in
  the tab list — there is no flash of visibility.
- A player who joins while already invisible is immediately hidden from everyone
  already online.
- Any number of players can be invisible at once; each is tracked and shown or hidden
  independently, by UUID.
- The plugin reacts immediately to effect changes (application, removal, expiry, milk,
  commands, or another plugin's API calls), joins, quits, and respawns, and additionally
  runs a periodic reconciliation pass as a safety net so state cannot drift permanently
  even if an event is missed.
- If the tab-list feature is disabled, or the plugin itself is disabled, everyone the
  plugin was hiding is restored to full tab-list visibility.

This uses Paper's tab-list-scoped `Player#unlistPlayer` / `Player#listPlayer` API, which
only affects tab/player-list presentation. It does **not**:

- hide the actual player entity
- prevent other players from seeing the entity in the world
- alter combat
- alter targeting
- alter collision
- alter nametags
- alter skins
- alter display names

### Death-message redaction

When a player dies, the plugin determines whether a player (not a mob, environmental
cause, or unattributed projectile) is the responsible killer, using melee damage or a
player-owned projectile such as an arrow or trident.

If there is a responsible player killer, the plugin checks whether that killer was
invisible at the moment the lethal damage was dealt (tracked separately from the killer's
invisibility state at the exact moment the death event fires, since the effect could
toggle in between). If the killer was invisible at that moment, only the killer's
identity within the existing death message is replaced with the configured placeholder
— the default is `"?"`. The victim's name and the rest of the death context (weapon,
cause) are preserved wherever the message format allows it.

The replacement text is applied at the message-component level, not through string
substitution, so it cannot leak the original identity through hover text, click actions,
or nested message arguments.

The plugin only ever edits the death message that Minecraft/Paper already produced. If
another plugin has already suppressed or blanked the message, no new message is created.

If death-message redaction is disabled in the configuration, death messages are left
completely untouched.

### DiscordSRV compatibility

DiscordSRV is optional. InvisiblePlayers does not require it and functions identically
whether or not it is installed.

If DiscordSRV is installed and enabled, InvisiblePlayers keeps the standard Bukkit
`"vanished"` metadata key — the mechanism DiscordSRV itself checks to exclude players
from its Discord-facing online-player list — in sync with the same invisibility check
used for tab-list hiding. This requires no DiscordSRV configuration, reflection, or NMS
access; it only sets and removes a metadata entry owned by this plugin, so it coexists
safely with any other plugin that uses the same convention.

DiscordSRV is declared as a soft dependency, so compatibility is detected once at
startup if DiscordSRV is already loaded and enabled. If DiscordSRV is installed after
the server has already started, compatibility will not activate until the next restart.

DiscordSRV exposes no public API to filter, customize, or force-refresh its
Discord-facing player output beyond this metadata convention, so that convention is the
full extent of what this plugin can influence. DiscordSRV reads the metadata whenever it
next refreshes its own output; InvisiblePlayers cannot force an earlier refresh.

## Requirements

- Paper **26.1.2** (or a compatible build in that release line)
- Java 25 or newer
- DiscordSRV (optional; only required for Discord vanished-list synchronization)

## Installation

1. Download the plugin JAR.
2. Place it in the server's `plugins` directory.
3. Start or restart the server. This generates `plugins/InvisiblePlayers/config.yml`.
4. Edit the generated configuration if needed.
5. Run `/invisibleplayers reload` or restart the server after configuration changes.

## Commands

| Command | Aliases | Description |
|---|---|---|
| `/invisibleplayers` | `/ip` | Shows help (same as `help`). |
| `/invisibleplayers help` | `/ip help` | Shows command usage help. |
| `/invisibleplayers reload` | `/ip reload` | Reloads and validates `config.yml`, applies the changes immediately, and restarts the reconciliation task only if its settings changed. |
| `/invisibleplayers status` | `/ip status` | Shows plugin version, feature states, the number of currently invisible players, the number of players currently tracked as hidden, the reconciliation interval, and DiscordSRV compatibility state. |

## Permissions

| Permission | Default | Purpose |
|---|---|---|
| `invisibleplayers.admin` | op | Umbrella permission that grants all subcommands. |
| `invisibleplayers.reload` | op | Allows running `/invisibleplayers reload`. |
| `invisibleplayers.status` | op | Allows running `/invisibleplayers status`. |

Permissions only control who can run plugin commands. They do not affect the core
plugin behavior — an operator who is invisible is still hidden from other players' tab
lists, and their kills are still subject to death-message redaction. There is no
permission that exempts a player from either feature.

## Configuration

`plugins/InvisiblePlayers/config.yml`, generated automatically on first startup and
never overwritten by later plugin updates unless it is deleted:

```yaml
tab-list:
  enabled: true

death-redaction:
  enabled: true
  replacement: "?"

settings:
  reconciliation-interval: 1
```

| Key | Type | Default | Description |
|---|---|---|---|
| `tab-list.enabled` | boolean | `true` | Turns tab-list hiding on or off. When disabled, any players currently hidden by the plugin are restored to full tab-list visibility. |
| `death-redaction.enabled` | boolean | `true` | Turns death-message redaction on or off. When disabled, death messages are never modified. |
| `death-redaction.replacement` | string | `"?"` | Text shown in place of an invisible killer's identity in the death message. Must be non-empty. |
| `settings.reconciliation-interval` | integer (seconds) | `1` | How often the safety-net reconciliation pass re-checks every online player's actual invisibility state against the plugin's tracked state. Must be a positive integer. |

Invalid values (wrong type, empty replacement, non-positive interval) produce a warning
in the console and fall back to the default shown above rather than crashing the plugin.
Changes take effect after `/invisibleplayers reload` or a server restart; the
reconciliation task is only restarted if its enabled state or interval actually changed.

## What this plugin does not do

InvisiblePlayers is not a vanish plugin. It does not provide:

- entity invisibility or player hiding beyond vanilla Minecraft's own invisibility
  rendering rules
- combat protection
- targeting protection
- collision changes
- nametag changes
- skin changes
- display-name changes

Its scope is limited to tab-list presentation and public death-message redaction, with
optional DiscordSRV vanished-metadata synchronization.

## Building

This project uses the Gradle wrapper, so no local Gradle installation is required.

Windows:

```
.\gradlew.bat clean build
```

Unix-like systems:

```
./gradlew clean build
```

The output JAR is written to `build/libs/InvisiblePlayers-<version>.jar`.

The build runs the project's JUnit 5 test suite, which covers tab-list diff
computation, death-message component redaction, death-attribution tracking, and
configuration validation.

## License

[MIT](LICENSE)
