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

