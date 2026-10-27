# TFC Weight Bar

**English** · [Русский](README.ru.md)

## About

This is a simple TFC addon that adds a weight points to all items in player inventory and Curios slots. If weight points exceed some amount player will receive debuff

## Requirements

- Minecraft 1.21.1 on NeoForge 21.1.0+ (built against 21.1.265)
- TerraFirmaCraft 4.2.0+ (built against 4.2.12)
- Curios API 9.5.0+ (built against 9.5.1+1.21.1)
- TFC Curios Weight 1.0.0+, built from its own 1.21.x branch

## Changelog

### 1.0.0
- Initial release of the mod. It adds weight calculations for all Player and Curios slots and all items.
- There is a config option to disable the whole mod or debuffs/food exhaustion.

## To Do List

- [x] Implement a gui/hud to display the weight bar
- [x] Add weight info to the item tooltip
- [ ] Limit the list of item ids to be checked for weight calculations

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the branch layout, how to build and what to check before opening a pull request.

## License

This mod is under the MIT License. This means that you can include it in your modpack without asking my permission.

TFC, which this mod is an addon for, is under the EUPL license. When using portions of this mod, be aware that the TFC license may apply. You can view that license on that mod page. The HUD weight bar texture is the weight bar from TFCSU (TFC Russia server mod for 1.7.10), itself a recolor of the TFC HUD bar.

TFCCuriosWeight is under the MIT License. You can view that license on that [mod page](https://github.com/koenigstag/tfc-curios-weight).

Curios, which this mod depends on, is under the LGPL. You can view that license on that mod page.
