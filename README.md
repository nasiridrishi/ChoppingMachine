# Chopping Machine

## Overview

<!-- Provide a brief introduction or overview of the plugin, its purpose, and its functionality. -->
A plugin that allows players to automatically chop down trees. There are currently three different
chopping machines:

1. Gold Chopping Machine:
    * Default tree detection range: 9
    * Speed: 2x per second
    * Yield Multiplier: 1.5x
2. Emerald Chopping Machine
    * Default tree detection range: 17
    * Speed: 3x per second
    * Gives random xp to players in range of machine.
3. Diamond Chopping Machine
    * Default tree detection range: 25
    * Speed: 4x per second.

## Time Taken

<!-- Specify the total time taken to develop and complete the plugin test. -->
~ 6 hours (this does not including the time taken to write this README.md and creating video etc)

## Challenges and Issues

<!-- Describe any challenges, roadblocks, or issues encountered during the development process and how they were addressed or resolved. -->

1. I was not able to handle holograms and particles efficiently so had to use public libraries for
   that as mentioned below.
2. I was not able to efficiently search for trees in a given range and thus ran into some hoops and
   did some research to makge it work properly.
3. Keeping in mind the time constraint, I was not able test situations where if creepers blows up at
   machine and how it would affect the machine.
4. I was not able to test the plugin on a server with multiple players and thus I am not sure if the
   plugin would work properly in that case I am quite certain it would handle very well.
5. Have had difficulty in handling machine instances properly on chunk loads and unloads where I
   would see dublicate instances being thrown, Spent sometime to fix this issue.

## Features and Functionalities

1. Players can place a chopping machine and it will automatically chop down trees in a given range.
2. Nice particle trail line to show where exactly thhe block is being broken.
3. Hologram to show the range of the machine and its status.
4. Lag free and efficient plugin due to holograms and particles being packet based.

## Installation

1. Requires spigot version 1.17 or above. I am quite certain it will work as low as for 1.3 but have
   not
   tested. (Tests were done on 1.20.2)
2. Make sure to install dependent hologram plugin:
   DecentHolograms: https://www.spigotmc.org/resources/decentholograms-1-8-1-20-2-papi-support-no-dependencies.96927/

## Special features

1. If there is a mushroom/flowers around the machine, machine will destroy itself and drop machine
   item.
2. Machine will get a insta boost and chop down all found tree logs instantly if it comes in contact
   with Spong or Ice Block.

## Commands(/chopper)

- /chopper give <player> <machineType> - Gives player a chopping machine of given type. [Permission:
  choppingmachine.command.give]
- /chopper settings <machineType> <settings> <value> - Sets the given setting to the given value.
  [Permission: choppingmachine.admin]

## Permissions

- `ChoppingMachine.*` - Gives access to all permissions.
- `ChoppingMachine.use.*` - Gives access to all use permissions like place, break etc.
- `ChoppingMachine.use.place` - Allows the player to place the chopping machine.
- `ChoppingMachine.use.break` - Allows the player to break the chopping machine.
- `choppingmachine.break.owneroverride` - Allows the player to break the chopping machine even if
  they are not the owner.
- `choppingmachine.command.give` - Allows the player to give themselves a chopping machine.

## Public Libraries Used

- DecentHolograms as plugin dependency for plugin holgorams.
- NBTAPI for handling NBT data: https://github.com/tr7zw/Item-NBT-API
- ParticleNativeAPI for efficient particle
  handling: https://github.com/Fierioziy/ParticleNativeAPI
- CommandApi for handling commands: https://github.com/JorelAli/CommandAPI
- Pathetic for handling path finding: https://github.com/patheloper/pathetic

---


**Note:** I was not able to complete task: Special Block Interactions -> "Water: Should float and
keep going on." because I could not understand what it actually meant. I am assuming it meant that
the machine should float on water and keep going on. I tried to implement this but was not able to
do so. I am not sure if I understood the task correctly.

