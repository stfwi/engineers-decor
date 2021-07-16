
## Engineer's Decor (MC1.16.x)

Mod sources for Minecraft version 1.16.x.

- Description, credits, and features: Please see the readme in the repository root.

- Compiled mod distribution channel is curseforge: https://www.curseforge.com/minecraft/mc-mods/engineers-decor/files.

----

## Version history

    ~ v1.1.15-b1 [F]

    - v1.1.14    [R] Release build v1.1.14.

    - v1.1.14-b3 [A] Spanish language support added (PR#180, thx FrannDzs).

    - v1.1.14-b2 [F] Block Placer: Attempt circumventing external placement prevention.

    - v1.1.14-b1 [F] Fixed Fluid Barrel container item definition (issue #178, thx Wormbo).

    - v1.1.13    [R] Release build v1.1.13.
                 [A] Added debug logging feature.

    - v1.1.13-b2 [F] Fixed explosion resistance values for standard blocks.
                 [F] Sandstone Ornated Clinker loot table fixed (ty czbuendel, Valen).

    - v1.1.13-b1 [F] Hotfix Electrical Furnace inventory import from Storage Drawers (issue #174, ty anto-fire/IchigoGames).

    - v1.1.12    [F] Chisels&Bits compatibility addressed (issue #172, ty rodg88).
                 [F] Labeled Crate drop list made stateless (issue #173, ty HopsandBarley && Harmonised).

    - v1.1.11    [F] Fixed Window placement dupe (issue #170, ty NillerMedDild).

    - v1.1.10    [A] Added Small Lab Furnace config for accepted speed-boost heaters (PR#165, ty mrh0).
                 [F] Fixed Labeled Crate mouse scrolling crash (issue #169, ty vaelzan).

    - v1.1.9     [A] Dark Shingle Roof Wire Conduit recipe added.
                 [F] Fixed Ladder climbing (affects forge>=36.0.45, issue #167, thx ZED).

    - v1.1.8     [F] Crafting Table Output slot sync rework (issue #138).
                 [A] Dark Shingle Roof Wire Conduit added (CFR#347).

    - v1.1.8-b2  [F] Fixed Iron Hatch isLadder bug (thx jerryw09).
                 [F] Fixed Block Placer block placing pre-conditions (issue #160, ty XFactHD).
                 [F] Added explicit scheduled Crafting Table client sync.
                 [F] Fixed directional waterloggable block default state forwarding (issue #162, ty b52src).

    - v1.1.8-b1  [F] Fluid Funnel waterlogged fluid picking fixed (issue #158, thx ZoMadeStuff).
                 [F] Roof rendering fixes (issues #153/#159, thx Salamance73/Murph).
                 [A] Recessed Clinkers, Vertically Slit Clinkers, and Structured Vertical Clinker Slab added.

    - v1.1.7     [M] 1.16.5 support.
                 [F] Fixed Labeled Crate include (issue #157, ty NillerMedDild).

    - v1.1.6     [F] Added common-config opt-out specification for pack level opt-outs (issue #154,
                     ty gekkone), will replace server config opt-out in MC1.17.

    - v1.1.6-b3  [M] Config logging edited, E-Furnace GUI capacitor tooltip added, E-Furnace power consumption
                     independent of config speed setting (issue #152 ty Staegrin).

    - v1.1.6-b2  [M] Alternative Clinker Brick recipe (swapped Bricks/Nether Bricks) added.
                 [M] Furnace XP handling simplified (simply stores/releases XP for each smelting process).
                 [M] Mod devices do not propagate strong Redstone power to adjacent blocks.
                 [M] Minor "librarizing" changes under the hood.

    - v1.1.6-b1  [F] Fixed Metal Crafting Table Hopper access (issue #147, ty umerrr).
                 [F] Fixed Dark Shingle Roof Chimney placement restriction (issue #149, thx WenXin20).
                 [F] Door tags added for Wood Door and Metal Sliding Door (issue #150, thx WenXin20).
                 [A] Electrical Furnace automatically chokes speed and power consumption when the internally
                     stored power is below 20%.

    - v1.1.5     [R] Release build v1.1.5.
                 [F] Fixed Crafting Table JEI storage slot count.
                 [F] Fixed Factory Hopper removed item collection dupe bug (issue #146, thx FatheredPuma81).
                 [F] Increased device GUI access ranges beyond the player block selection range.
                 [A] Window placement handling improved.
                 [M] Steel/Wood Pole and Double-T support placement improved (issue #139, thx Biviho).
                 [M] Metal Sliding Door bottom/top shape when opened added.

    - v1.1.4     [R] Release build v1.1.4.
                 [F] Solar Panel balancing threshold tuned.
                 [F] Fixed Catwalk default state (issue #140, thx hvdklauw).
                 [M] Updated lang ru_ru file (PR#137, Smollet777).
                 [M] Factory Dropper: Added Ignore-External-Redstone mode.

    - v1.1.4-b2  [A] Steel Catwalks added (top and bottom aligned).
                 [A] Steel Railings added.
                 [F] Fixed Empty Fluid Barrel crafting crash (ty inflamedsebi).
                 [A] Added Solar Panel power balancing.
                 [M] GUI Button tooltip delay reduced to 800ms.
                 [M] Hopper and Placer: Added "Redstone ignored" mode, changed icons from signal-like to Redstone-Torch-like.
                 [M] Treated Wood Ladder now crafted from Old Industrial Wood, as Treated Wood Sticks now count as normal Sticks.

    - v1.1.4-b1  [U] Ported to 1.16.4.

    - v1.1.3     [R] Release build v1.1.3.

    - v1.1.3-b3  [A] Metal Sliding Door added (double door wing style).
                 [A] Doors implicitly open/close adjacent wings of double doors.
                 [A] Disabled injected buttons from other mods in container GUIs.
                 [A] Mob spawning on Rebar/Gas Concrete inhibited (IE Concrete Compliancy).
                 [M] Small Tree Cutter chopping improved (loosened tree volume search restrictions).

    - v1.1.3-b2  [A] Crafting table shift/ctrl click item move actions tweaked to new metal slot design.
                 [A] Factory Dropper and Block Placer now also support quick-move-all (shift-ctrl-click).
                 [F] Fixed Small Lab Furnace speed boost factor (with IE Heater in aux slot).

    - v1.1.3-b1  [A] The Factory Block Breaker can insert items into Hoppers underneath it (issue #121, winsrp).
                 [F] Help tooltips manually wrapped.
                 [F] Fixed Labeled Crate item name persistence (issue #127, ty inqie).
                 [F] Help text typo fixed (issue #129, ty Smollet777).

    - v1.1.2     [U] Updated to Forge 1.16.3-34.1.0.
                 [A] Added Factory Hopper insertion/extraction for entities like Minecarts (issue #125, ty boneskull).

    - v1.1.2-b8  [F] Fixed Double-T support thick steel pole connection (thx @CastCrafter).
                 [A] Concrete and Clinker walls connect to windows and glass panes.

    - v1.1.2-b7  [U] Ported to MC1.16.3.
                 [F] Roof lighting improved.

    - v1.1.2-b6  [A] Signs added: Generic Caution, Magical Hazard, Radioactive Hazard, Laser Hazard,
                     Fire Hazard, Caution Hot Surface, Magnetic Field Caution, Frost Warning.
                 [A] Water Freezer added (generates Ice, Packed Ice, Blue Ice from water).
                 [F] Mineral Smelter GUI model facing fixed.
                 [M] Hatch handling improved.
                 [M] Ladder fast-move improved.
                 [F] Roof Chimney Trunk shape hollow to allow feeding IE Wire power over the roof into the building.
                 [A] Roof Chimney added (smoking offset for Dark Shingle Roof Chimney Trunk block).
                 [A] Metal Bar (ingredient item) added to circumvent recipe collisions with other mods.
                 [M] Recipes for metallic blocks modified accordingly to depend on Metal Bars.

    - v1.1.2-b5  [A] Sandstone Ornamented Clinker Brick added.
                 [A] Old Industrial Wood Planks/Stairs/Slabs/Slab Slices added.
                 [A] Old Industrial Wood Door added.
                 [M] Wood textures made slightly darker.
                 [F] Milking Machine fluid transfer re-added (thx gebcrafter).
                 [F] Fluid Barrel status overlay message format fixed.
                 [F] Fixed missing Dense Grit Dirt loot table (issue #124, thx vaelzan).

    - v1.1.2-b4  [F] Mapping adaption to Forge 1.16.2-33.0.22/20200723-1.16.1.
                 [F] Fixed conditional recipe tag dependency (thx Blu, Cyborgmas).

    - v1.1.2-b3  [F] Mapping adaption to Forge 1.16.2-33.0.20.

    - v1.1.2-b2  [A] Dark Shingle Roof added.

    - v1.1.2-b1  [U] Ported to MC1.16.2.

----
