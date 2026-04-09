# Echo Mining

A Minecraft Fabric mod that adds a sonar pulse effect when mining blocks, revealing nearby ore locations through walls.

## Features

- **Sonar Pulse**: When you break any block, a visual pulse radiates outward from the broken block position
- **Ore Detection**: All ore blocks within a 10-block radius become visible with colored particle effects for 2 seconds
- **Color-coded Ores**: Different ores glow different colors for easy identification:
  - Iron: White
  - Gold: Yellow
  - Diamond: Cyan
  - Emerald: Green
  - Redstone: Red
  - Lapis: Blue
  - Copper: Orange
  - Coal: Dark Gray
- **Cooldown**: 5-second cooldown between pulses to balance gameplay



## Requirements

- Minecraft 1.21.1
- Fabric Loader 0.16.0+
- Fabric API

## Installation

1. Install Fabric Loader for Minecraft 1.21.1
2. Download and install Fabric API
3. Download the mod JAR from the releases
4. Place the JAR file in your `mods` folder
5. Launch Minecraft

## Building from Source

```bash
git clone https://github.com/Simplifine-gamedev/echo-mining.git
cd echo-mining
./gradlew build
```

The built JAR will be in `build/libs/`.

## Usage

Simply mine any block while playing. If the cooldown has elapsed, a sonar pulse will automatically activate, revealing nearby ores with colored particles visible through walls.

## License

MIT License
