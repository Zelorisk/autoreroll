# Villager Trade Reroller

A client-side Minecraft Fabric mod that automates and optimizes villager trading by intelligently rerolling trades until desired conditions are met.

## Features

### Core Functionality
- **Automated Rerolling System**: Automatically break and replace villager job site blocks to reroll trades
- **Configurable Delay**: Set delays between reroll attempts (100ms - 5000ms)
- **Three Operation Modes**:
  - **Manual**: Highlight good trades only (no automation)
  - **Semi-Auto**: Automatically breaks job sites (manual replacement required)
  - **Full Auto**: Complete automation (breaks and replaces job sites)
- **Smart Detection**: Automatically stops when target trade is found
- **Prevention System**: Avoid locking villagers accidentally

### Trade Detection & Analysis
- Real-time trade scanning when villager GUI opens
- Trade evaluation against user-defined criteria
- Visual highlighting of trades meeting requirements
- Quality scoring system (S, A, B, C, D grades)

### Profile System
- Save multiple filter presets
- Quick-switch between profiles
- Import/export profile JSON files
- Custom profile creation

### Statistics Tracking
- Total rerolls performed
- Success/failure tracking
- Average rolls to success
- Emeralds saved estimation
- Per-session and all-time stats
- Exportable statistics

## Installation

### Requirements
- Minecraft 1.21.x (tested on 1.21.1)
- Java 21 or higher
- Fabric Loader 0.16.5+
- Fabric API 0.105.0+
- Mod Menu 11.0.3+ (optional, for config GUI)
- Cloth Config 15.0.128+ (optional, for advanced config screens)

### Install Steps
1. Install [Fabric Loader](https://fabricmc.net/use/)
2. Download [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
3. Download [Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu) (optional)
4. Download [Cloth Config](https://www.curseforge.com/minecraft/mc-mods/cloth-config) (optional)
5. Place all mod JARs in your `.minecraft/mods` folder
6. Launch Minecraft with Fabric profile

## Building from Source

```bash
# Clone the repository
git clone <repository-url>
cd villager-trade-client

# Build the mod
./gradlew build

# The built JAR will be in build/libs/
```

## Configuration

### Accessing Config
- In-game: Mods menu → Villager Trade Reroller → Config
- Manual: Edit `.minecraft/config/villagerreroller/config.json`

### General Settings
- **Enabled**: Toggle mod on/off (Hotkey: R)
- **Operation Mode**: Manual / Semi-Auto / Full Auto
- **Reroll Delay**: Milliseconds between reroll attempts (default: 500ms)
- **Max Reroll Attempts**: Maximum attempts per villager (default: 100)
- **Sound Notifications**: Play sounds when trades are found
- **Notification Style**: Chat / Action Bar / Overlay

### Trade Filters
- **Max Emeralds**: Set price thresholds for different item categories
  - Enchanted Books (default: 10)
  - Tools (default: 5)
  - Armor (default: 5)
  - Miscellaneous (default: 10)
- **Item Whitelist**: Only accept specific items (e.g., `minecraft:enchanted_book`)
- **Item Blacklist**: Never accept specific items
- **Enchantment Filters**: Filter for specific enchantments (e.g., `minecraft:mending:1`)
- **Filter Logic**: AND (all conditions) / OR (any condition)

### Advanced Filters
- **Require Max Enchantment Level**: Only accept max-level enchantments
- **Combined Enchantments**: Require multiple enchantments on same book
- **Preferred First Slot Items**: Prefer specific items in first trade slot
- **Excluded Professions**: Don't reroll certain professions

### Safety Features
- **Max Attempts Limiter**: Prevent infinite loops (default: 100)
- **Villager Cooldown**: Delay between processing different villagers
- **Pause if Inventory Full**: Auto-pause when inventory is full
- **Server Friendly Throttling**: Use longer delays for multiplayer

## Usage

### Basic Usage
1. Enable the mod (default hotkey: R)
2. Select your operation mode in config
3. Configure trade filters (what you're looking for)
4. Approach a villager and open their trade GUI
5. The mod will automatically scan and evaluate trades
6. In Semi/Full Auto modes, the mod will reroll until a match is found

### Hotkeys (Configurable)
- **R**: Toggle mod on/off
- **ESC**: Emergency stop
- **G**: Manual reroll trigger
- **P**: Cycle through profiles

### Creating Profiles
Profiles are stored in `.minecraft/config/villagerreroller/profiles/`

Example profile (`mending.json`):
```json
{
  "name": "Mending Hunt",
  "description": "Find cheap Mending books",
  "itemWhitelist": ["minecraft:enchanted_book"],
  "enchantmentFilters": ["minecraft:mending:1"],
  "maxEmeraldsBooks": 10,
  "requireMaxEnchantmentLevel": true
}
```

### Trade Quality Indicators
- **S Grade**: Exceptional trade (200+ points)
- **A Grade**: Great trade (150-199 points)
- **B Grade**: Good trade (100-149 points)
- **C Grade**: Average trade (50-99 points)
- **D Grade**: Below average (0-49 points)

## Multiplayer Usage

### Important Warnings
- Always check server rules before using automation mods
- Use appropriate delays to respect server TPS
- Some servers may prohibit automation mods
- The mod is designed for client-side use only

### Recommended Settings for Multiplayer
- **Reroll Delay**: 1000ms or higher
- **Server Friendly Throttling**: Enabled
- **Operation Mode**: Manual or Semi-Auto (safer than Full Auto)

## Statistics

### View Statistics
- In-game: Check the HUD overlay for session stats
- Detailed stats: Mod config → Statistics tab
- Export: `.minecraft/config/villagerreroller/statistics.json`

### Tracked Metrics
- Total rerolls performed
- Successful vs failed rerolls
- Average attempts to success
- Estimated emeralds saved
- Best trades (top 10, fewest attempts)

## Troubleshooting

### Mod Not Working
- Check if mod is enabled (R key or config)
- Verify Fabric API is installed
- Check logs: `.minecraft/logs/latest.log`

### Job Site Not Found
- Ensure job site block is within 8 blocks of villager
- Villager must not be already locked (has traded before)
- Check if correct profession's job site is present

### Trades Not Matching
- Review your filter settings
- Check filter logic (AND vs OR)
- Verify enchantment filter syntax: `namespace:enchantment:level`

### Performance Issues
- Increase reroll delay
- Reduce max attempts
- Disable HUD overlays

## File Structure

```
.minecraft/config/villagerreroller/
├── config.json           # Main configuration
├── statistics.json       # Statistics data
└── profiles/            # Trade filter profiles
    ├── profile1.json
    └── profile2.json
```

## Safety & Ethics

### Built-in Limitations
- Respects server rules with configurable delays
- Option to disable on certain servers
- Clear indication when mod is active
- No packet manipulation (pure client-side simulation)

### Disclaimer
This mod automates player actions in Minecraft. Always:
- Check server rules before using
- Use appropriate delays on multiplayer servers
- Be respectful to other players and server resources
- Understand that some servers may prohibit automation mods

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is licensed under the MIT License.

## Credits

- Built with [Fabric](https://fabricmc.net/)
- Uses [Mod Menu](https://github.com/TerraformersMC/ModMenu) for config integration
- Uses [Cloth Config](https://github.com/shedaniel/ClothConfig) for config screens

## Support

- Report issues: [GitHub Issues](<repository-url>/issues)
- Join our community: [Discord/Forum link]
- Documentation: This README and in-game tooltips

## Changelog

### Version 1.0.0
- Initial release
- Core rerolling functionality
- Profile system
- Statistics tracking
- Mod Menu integration
- Full configuration system

---

**Note**: This mod is for educational and personal use. Always respect server rules and play fairly.
