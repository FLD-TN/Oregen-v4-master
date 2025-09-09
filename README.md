# OreGen4 Permission Update System

## Overview
This is a standalone permission management system for the OreGen4 plugin. It provides efficient permission handling for ore generation levels with improved caching and automatic updates.

## Key Components

### 1. PermissionUpdater
The core class that manages permission caching and automatic updates.

Features:
- Efficient caching using ConcurrentHashMap
- Configurable update intervals
- Different permission handling for different environments (normal, nether, end)
- Asynchronous permission checking to avoid lag
- Automatic permission updates based on LuckPerms changes

### 2. PermissionCommand
A command interface for administrators to control the permission system.

Commands:
- `/permission status` - View current status of the permission system
- `/permission update` - Trigger an immediate permission update
- `/permission reload` - Reload the permission update system
- `/permission clear <player>` - Clear cached permissions for a player

### 3. Integration with OreGen4
The system is integrated with the main plugin through:
- EventOreGen - Uses PermissionUpdater to check player permissions
- Main class - Initializes and manages the permission system

## How It Works

1. When a player attempts to generate ore, the system:
   - Checks for cached permissions
   - If not cached, queries LuckPerms for the player's highest permission
   - Registers the permission in the cache for future use
   - Returns the appropriate ore level based on permissions

2. Periodic updates:
   - The system automatically checks for permission changes
   - Updates are performed asynchronously to avoid lag
   - Admins can force immediate updates

## Installation

1. Install the OreGen4 plugin
2. Make sure LuckPerms is installed and properly configured
3. Configure permission-update-interval in config.yml (default: 60000ms)

## Commands

- `/permission status` - View current status
- `/permission update` - Trigger immediate update
- `/permission reload` - Reload the system
- `/permission clear <player>` - Clear player cache

## Permissions

- `oregen.admin` - Access to all permission management commands
- `oregen.vip` - VIP ore generation level
- `oregen.lv3` - Level 3 ore generation
- `oregen.lv2` - Level 2 ore generation
- `oregen.lv1` - Level 1 ore generation

## Future Improvements

- Web interface for permission management
- More detailed logging options
- Support for more permission plugins
- Performance optimizations for larger servers
