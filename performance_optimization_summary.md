# OreGen4 Performance Optimization for High Player Counts

## Problem Analysis

When many players (up to 60) are simultaneously mining on a server using the OreGen4 plugin, performance issues can arise due to:

1. **Multiple LuckPerms API Calls**: Each block generation event triggers a LuckPerms API call to check player permissions.
2. **Synchronous API Calls**: Some API calls were being made in a way that could block the main server thread.
3. **Redundant Permission Checks**: The same permissions were checked repeatedly for each player.
4. **Inefficient Caching**: While there was a caching system, it wasn't optimized for high player counts.

## Solution Architecture

Our optimization strategy has several components:

### 1. Pre-loading Player Permissions on Join
- Create a new `PlayerPermissionLoader` listener that caches permissions when players join
- This shifts permission checks from "block generation time" to "login time"
- Permissions are stored in an enhanced cache system with longer TTL (Time To Live)

### 2. Enhanced Permission Cache
- Created a more robust cache system with priority levels and TTL controls
- Added better memory management by reducing TTL for offline players
- Implemented a concurrent cache structure using `ConcurrentHashMap` for thread safety

### 3. Log Filtering System
- Added rate limiting for debug logs to prevent console spam
- Created a LogFilter utility that suppresses similar messages within configurable time windows
- Added LoggerUtil for centralized logging with filtering capabilities

### 4. Asynchronous Permission Loading
- Changed permission loading to be fully asynchronous
- Block generation no longer waits for permission checks to complete
- Instead, we use cached values and update the cache in the background

## Code Changes

### New Classes:
1. **PlayerPermissionLoader.java**
   - Preloads and caches player permissions on join
   - Reduces TTL for permissions when players disconnect

2. **LogFilter.java**
   - Implements rate limiting for log messages
   - Prevents console spam from repeated debug messages

3. **LoggerUtil.java**
   - Centralizes logging functionality with filtering
   - Makes debug logs configurable and rate-limited

### Modified Classes:
1. **LoadOreGen.java**
   - Completely redesigned the block generation method
   - Now prioritizes using cached permissions over API calls
   - Uses async tasks for background permission updates
   - Falls back to safe defaults when cache misses occur

2. **Main.java**
   - Added getInstance() and getLuckPermsApi() methods
   - Added isDebugEnabled() for more efficient debug checks
   - Modified onEnable to initialize enhanced cache systems

3. **EnhancedPermissionCache.java**
   - Added priority caching with longer TTL
   - Added methods to manage cache TTL for offline players
   - Optimized cache lookups and cleanup

## Performance Benefits

1. **Reduced API Calls**: LuckPerms API is only called when a player joins or when permissions change, not for every block generated.

2. **Faster Block Generation**: Since permissions are cached, block generation becomes a simple cache lookup rather than an API call.

3. **Lower Memory Usage**: TTL controls ensure we're not keeping unnecessary data in memory.

4. **Cleaner Logs**: Rate limiting prevents console spam, making it easier to spot real issues.

5. **Better Scalability**: The system now scales much better with increasing player counts.

## Future Improvements

1. **Batch Processing**: Consider batching permission updates for even better performance.

2. **Config Options**: Add more configuration options for cache TTL and rate limiting.

3. **Metrics**: Add metrics tracking to monitor cache hit rates and performance.

4. **Permission Change Monitoring**: Enhance the system to monitor for permission changes in LuckPerms directly.

This optimization should significantly improve server performance when many players are online, especially on island servers where ore generators are heavily used.
