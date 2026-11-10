# Changelog

**English** · [Русский](CHANGELOG.ru.md)

Newest version first. The entry of a version is also its GitHub release notes and its changelog on CurseForge.

## 1.1.0
- The weight shows one of three ways, `weight_display` in the client config: HUD, the bar, by default; TEXT, a message above the hotbar every 10 seconds; or OFF, neither. It replaces `enable_ui_bar`, whose value is not carried over: a client config that had it off comes back on the HUD bar.
- The HUD bar can sit above the health bar instead of the food bar, and its numbers can be hidden: `bar_position` and `show_bar_numbers` in the client config.
- Mods > TFC Weight Bar > Config edits the client options in game, and the HUD follows at once, without a restart. The server options stay in the server config file.
- A server can settle the HUD options for every player: `force_weight_display`, `force_bar_position` and `force_bar_numbers` in the server config. CLIENT, the default, leaves each one to the player, and the config screen shows an option the server settles as locked.
- With TFC Astikor Carts or Pull Cart, a brown bar above the weight bar shows the load of the cart you pull, or the animal you ride pulls, against the loads at which you would be Exhausted and Overburdened. The load is weighed as your inventory is, every item by its weight class and a container with what it holds, so a bag of ore weighs its ore. The cart's load still does not count towards your weight. A player pulling one cart of each sees the one closer to its limit. `show_cart_bar` in the client config turns the bar off, and `force_cart_bar` in the server config settles that for every player.
- A cart has a limit of weight: `astikor_supply_cart_max_load` for a TFC Astikor Carts supply cart and `pull_cart_max_load` for a Pull Cart cart, 10000 and 5000 by default, exhausting from `exhausted_weight_percentage` of it. Past it an animal refuses the cart: it lets go of the cart and rears, and the players around read why over the hotbar, with the numbers. A player pulling such a cart on foot is burdened as by a load they carry. Meanwhile TFC Astikor Carts no longer burdens by its own sum for a supply cart's cargo, a mass that never looks into a container; its animal cart, which carries animals, keeps that mod's thresholds. `enable_cart_limits` in the server config turns all this off, and TFC Astikor Carts then weighs and burdens as it does by itself.
- Two more cart options in the server config, both off by default. `count_cart_riders`: with TFC Astikor Carts, a player sitting on a cart adds to its load and sees the cart's bar: on a supply cart the weight they carry, as their own bar shows it; on an animal cart, from 1.1.9, their body and what they carry, as that mod weighs a passenger and cargo. `astikor_max_large_item_slots` and `pull_cart_max_large_item_slots`: a cart with more slots of large items than this is overloaded whatever it weighs, and an animal refuses it with its own message; `large_item_size`, LARGE by default, is the smallest size that counts.
- A 1.0.x client cannot join a 1.1.0 server, nor the other way round: the weight the server sends now carries the cart load too, so the client and the server both need 1.1.0.

## 1.0.3
- On a dedicated server the weight message above the hotbar no longer shows up every 10 seconds beside the HUD bar: the server ran the client's message code, without the client config that turns the message off.

## 1.0.2
- The `enable_food_exhaustion` option works as described: a load past the Exhausted threshold makes the player hungrier, up to twice TFC's passive hunger at the maximum weight. Before, the option did nothing.

## 1.0.1
- Creative and spectator players no longer see the weight message above the hotbar: weight does not apply to them.
- Closing a world no longer logs an error: the config stops reading its values while it unloads.

## 1.0.0
- Initial release of the mod. It adds weight calculations for all Player and Curios slots and all items.
- There is a config option to disable the whole mod or debuffs/food exhaustion.
