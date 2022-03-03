## **(V.2.0.0 Changes) (1.18.2 Minecraft)**

##### Major:
Updated to 1.18.2


## **(V.1.4.4 Changes) (1.18.1 Minecraft)**

##### Command:
Fixed /spawnstructure and /spawnrawstructure not keeping passed in randomseed's effect consistent across world saves.


## **(V.1.4.3 Changes) (1.18.1 Minecraft)**

##### Command:
Fixed autocomplete dying for /spawnpieces command if there's a nbt file not inside a folder within the /structures/ folder.


## **(V.1.4.2 Changes) (1.18.1 Minecraft)**

##### Command:
Added sendchunklightingpacket argument to /spawnconfiguredfeature, /spawnplacedfeature, /spawnrawstructure, and spawnstructure command.
 If off, it may provide significant performance boost but if the structure or feature places blocks in a very weird way, those blocks 
 may not show up until the chunk is reloaded. Example, minecraft:forest_rock feature places blocks without sending changes to client.
 Most features and structures will be fine and sendchunklightingpacket can be set to false.


## **(V.1.4.1 Changes) (1.18.1 Minecraft)**

##### Command:
Improved error message from all commands.


## **(V.1.4.0 Changes) (1.18.1 Minecraft)**

##### Command:
Added /spawnrawstructure for trying to spawn configuredstructures. Some vanilla configuredstructures are hardcoded in the command so the command bypasses some of their annoying internal terrain/probability checks.

/spawnstructure command will refresh chunks on clients now so all blocks are visible on client if it was placed strangely by modded serverside structure code


## **(V.1.3.0 Changes) (1.18.1 Minecraft)**

##### Command:
Added location argument to /spawnstructure command.

Created /spawnfeature and /spawnplacedfeature commands.


## **(V.1.2.2 Changes) (1.18.1 Minecraft)**

##### Command:
Cached the suggestions for /spawnpieces command. Much less waiting after initial load of suggested folders to generate pieces from.


### **(V.1.2.1 Changes) (1.18.1 Minecraft)**

##### Misc:
Fixed issue where mod cannot load in prod due to it checking for mixins config file that doesn't exist.


### **(V.1.2.0 Changes) (1.18.1 Minecraft)**

##### Command:
Optimized /spawnpieces arg more and will always place structure void where the structure piece had structure void.

/spawnpieces now has a savepiece argument to auto save pieces to the world's generated folder.


### **(V.1.1.0 Changes) (1.18.1 Minecraft)**

##### Command:
Added argument to allow disabling processors when generating structures.

Added /spawnpieces command that was helped with by Patrigan! 


### **(V.1.0.1 Changes) (1.18.1 Minecraft)**

##### Command:
Resourcelocation argument does not need quotes anymore


### **(V.1.0.0 Changes) (1.18.1 Minecraft)**

##### Major:
Initial release

