# Changelog

**English** · [Русский](CHANGELOG.ru.md)

Newest version first. The entry of a version is also its GitHub release notes and its changelog on CurseForge.

## 1.1.0
- The weight shows one of three ways, `weight_display` in the client config: HUD, the bar, by default; TEXT, a message above the hotbar every 10 seconds; or OFF, neither. It replaces `enable_ui_bar`, whose value is not carried over: a client config that had it off comes back on the HUD bar.
- The HUD bar can sit above the health bar instead of the food bar, and its numbers can be hidden: `bar_position` and `show_bar_numbers` in the client config.
- Mods > TFC Weight Bar > Config edits the client options in game, and the HUD follows at once, without a restart. On 1.21.1 it is NeoForge's config screen, which in single player also edits the server config.
- A server can settle the HUD options for every player: `force_weight_display`, `force_bar_position` and `force_bar_numbers` in the server config. CLIENT, the default, leaves each one to the player.

## 1.0.3
- On a dedicated server the weight message above the hotbar no longer shows up every 10 seconds beside the HUD bar: the server ran the client's message code, without the client config that turns the message off.

## 1.0.2
- The `enable_food_exhaustion` option works as described: a load past the Exhausted threshold makes the player hungrier, up to twice TFC's passive hunger at the maximum weight. Before, such a load stopped the player from getting hungry at all.

## 1.0.1
- Creative and spectator players no longer see the weight message above the hotbar: weight does not apply to them.
- Closing a world no longer logs an error: the config stops reading its values while it unloads.

## 1.0.0
- Initial release of the mod. It adds weight calculations for all Player and Curios slots and all items.
- There is a config option to disable the whole mod or debuffs/food exhaustion.
