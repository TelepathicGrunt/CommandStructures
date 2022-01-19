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

