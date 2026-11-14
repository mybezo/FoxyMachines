# FoxyMachines

A [Slimefun](https://github.com/Slimefun/Slimefun4) addon that adds machines, tools, weapons, armor, a sacrificial altar, quests and custom bosses.

Originally made by **GallowsDove**. This fork is fully translated to English and turns off the heavier features by default.

## Requirements

| | |
| --- | --- |
| Server | Paper 1.20.4 or newer |
| Java | 17 or newer |
| Slimefun | [SlimefunGuguProject Slimefun4](https://github.com/SlimefunGuguProject/Slimefun4) (2025.1+) |
| GuizhanLibPlugin | Required, the plugin disables itself without it. [Download](https://50L.cc/gzlib) |
| AuraSkills | Optional, placed blocks from the Fill Wand are registered with it |

## Installation

1. Install Slimefun and GuizhanLibPlugin.
2. Put `FoxyMachines.jar` in your `plugins` folder.
3. Start the server once to generate `plugins/FoxyMachines/config.yml`.
4. Edit the config to enable the features you want, then restart.

You can disable any single item in `plugins/Slimefun/Items.yml`.

## Configuration

These features are **disabled by default**:

| Option | Default | Enables |
| --- | --- | --- |
| `ghost-blocks` | `false` | Ghost Blocks and the Ghost Block Remover |
| `chunk-loaders` | `false` | Chunk Loader |
| `forcefield-dome` | `false` | Forcefield Dome and Remote Controller |

Other options:

| Option | Default | Description |
| --- | --- | --- |
| `auto-update` | `true` | Automatic updates for official builds |
| `custom-mobs` | `true` | Bosses and custom mobs. When disabled, some recipes change and some items are removed |
| `max-chunk-loaders` | `8` | Chunk Loaders per player, `0` for unlimited |
| `gps-complexity-per-loader` | `7500` | GPS network complexity needed per Chunk Loader |
| `max-fill-wand-blocks` | `4096` | Maximum blocks the Fill Wand fills at once |
| `max-sponge-wand-blocks` | `16384` | Maximum blocks the Sponge Wand clears at once |
| `fill-wand-white-list` / `fill-wand-black-list` | | Materials or tags the Fill Wand allows or blocks |
| `quest-mobs` | | Mobs the swords can ask for in quests |
| `unbreakable-rune-blacklist` | | Addon items the Unbreakable Rune can't be used on |

> Upgrading from an older version? Your existing `config.yml` keeps `ghost-blocks: true`. The new `chunk-loaders` and `forcefield-dome` options start as `false`.

## Commands

The main command is `/foxymachines`, with aliases `/fm` and `/foxy`.

| Command | Permission | Description |
| --- | --- | --- |
| `/foxy quest` | `foxymachines.info` | Show the quest of the sword you are holding |
| `/foxy altar` | `foxymachines.info` | Link to a Sacrificial Altar build tutorial |
| `/foxy summon <MOB_ID>` | `foxymachines.admin` | Summon a custom mob |
| `/foxy listall` | `foxymachines.admin` | List all mobs spawned by FoxyMachines |
| `/foxy killall` | `foxymachines.admin` | Kill all FoxyMachines mobs in your world |

| Permission | Default | Description |
| --- | --- | --- |
| `foxymachines.info` | everyone | Basic commands |
| `foxymachines.admin` | op | Admin commands |
| `foxymachines.bypass-chunk-loader-limit` | op | Ignore the Chunk Loader limit |

## Content

### Machines
- Improvement Forge: upgrades the tier of Slimefun tools, weapons and armor
- Potion Mixer
- Electric Gold Refinery
- Chunk Loader *(disabled by default)*
- Boosted Rails: 2.5x minecart speed
- Forcefield Dome: barrier with a 32 block radius *(disabled by default)*

### Tools
- Electric Elemental Staff: Wind, Fire and Fire II
- Berry Bush Trimmer
- Poseidon's Fishing Rod
- Remote Controller *(disabled by default)*
- Ghost Block Remover *(disabled by default)*
- Position Selector, Fill Wand and Sponge Wand

### Weapons
- Healing Bow
- Cursed Sword and Celestial Sword, both give quests
- Elucidator
- Acri Arcum

### Armor
- Aquatic Helmet
- Resistant Chestplate
- Fiery Leggings
- Light Boots

### Bosses and mobs
- Pixie Queen *(boss)*
- Headless Horseman *(boss)*
- Pixie
- Helldog

### Other
- Sacrificial Altar: sacrifice mobs for special drops
- Ghost Blocks: blocks you can walk through *(disabled by default)*
- Unbreakable Rune

**Tip:** [MobCapturer](https://github.com/ybw0014/MobCapturer) makes it easy to bring mobs to the Sacrificial Altar.

## Building from source

Requires JDK 17+ and Maven.

```bash
git clone https://github.com/mybezo/FoxyMachines.git
cd FoxyMachines
mvn package
```

The jar is written to `target/FoxyMachines.jar`. Every CI run also uploads the jar as a build artifact.

## Credits

- [GallowsDove](https://github.com/GallowsDove/FoxyMachines): original author
- [SlimefunGuguProject](https://github.com/SlimefunGuguProject/FoxyMachines): maintained fork this is based on

See [LICENSE](LICENSE).
