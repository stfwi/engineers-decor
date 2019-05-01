
## Engineer's Decor (MC1.12.2)

Mod sources for Minecraft version 1.12.2.

- Description, credits, and features: Please see the readme in the repository root.

- Compiled mod distribution channel is curseforge: https://www.curseforge.com/minecraft/mc-mods/engineers-decor/files.

----
## Revision history

    - v1.0.4-b9 [F] Inserting fluids with pressurized tag only into IE piping.
                [F] Valve redstone connector rendering does not check for
                    "can connect redstone" but only for "can provide power".
                [M] Valves are adpted to be detected as pipe by IE.

    - v1.0.4-b8 [F] Fixed stairs rendering without smooth light (thanks rastot9).
                [E] Added passive fluid accumulator (experimental feature, see config).

    - v1.0.4-b7 [F] Fixed recipe loading issue if IE is not installed.
                [M] Valves support IE pressurized fluid transfer.

    - v1.0.4-b6 [A] Added redstone controlled fluid valve.
                [A] Added redstone controlled analog fluid valve.
                [M] Check valve recipe adapted (thanks majijn).

    - v1.0.4-b5 [A] Horizontal steel double-T support beam with pole connections.
                [A] Added fluid pipe check valve (straight, conducts only one way).
                [M] Internal registration block/te handling changed.

    - v1.0.4-b4 [F] Clinker/slag brick wall side cullfacing disabled to prevent
                    texture leaks when connecting to concrete walls.
                [F] Unused treated wood pole texture regions filled (optifine).
                [F] Using mipped cutout format for window multi-layer model
                    (issue #19, thanks rixmswey for reporting and details).
                [M] Recipe tuning, added standalone recipe for all mod blocks.
                [M] In-game CTRL-SHIFT tooltip documentation updated.
                [M] Panzer glass block: Ambient occlusion and light opacity tuned.
                [M] Stairs: Light opacity tuned.
                [A] Tooltip documentation added for mod stairs.
                [E] Horizontal steel double-T support beam (config:experimental).

    - v1.0.4-b3 [A] Added thin (4x4x16) and thick (6x6x16) steel hollow poles.
                [A] Added support head/foot components for thin and thick steel poles.

    - v1.0.4-b2 [A] Added position dependent texture variation to clinker wall,
                    slag brick wall and rebar concrete wall.

    - v1.0.4-b1 [A] Crafting table: JEI integration for recipe placement added.
                [A] Crafting table: History re-fab added, allowing to quickly select
                    and re-craft recent recipes. Selection with arrow buttons,
                    ingredient placement by clicking the result slot. Automatic
                    item distribution on shift-click. Quick-move buttons.
                [F] Crafting table textures modified to prevent optifine glitches
                    on the edges of the legs.

                -------------------------------------------------------------------
    - v1.0.3    [R] Release based on v1.0.3-b5. Release-to-release changes:
                    * Small laboratory furnace added.
                    * Extensive config options for mod packing and tuning added.
                    * Rendering issues fixes (window bleeding, optifine).
                    * Steel framed window added.
                    * Treated wood pole "end pieces" added (two support variants).
                    * Sitting on treated wood stool added including mobs (but not
                      villagers, as these are obviously very upright people).
                    * Lang ru_ru added (github contribution from Yaroslavik).
                    * Creative tab logo changed to mod logo.
                    * Table/crafting table bounding boxes refined.
                    * Standalone "escape" recipes added if IE is not installed.
                -------------------------------------------------------------------

    - v1.0.3-b5 [F] Fixed typo in en-en lang file.
                [F] Fixed IE concrete texture missing bailout in log if IE is not installed.
                [F] Using forge multi-layer models for windows to circumvent glitches.
                [M] Changed creative tab logo to the mod logo.
                [A] Added alternative recipes for crafting table and furnace if main
                    IE ingredients are missing (for "stand-alone" mod usage).

    - v1.0.3-b4 [A] Lab furnace supports electrical speedup when a IE external
                    is placed in one of the two auxiliary slots.
                [F] Fixed window rendering issue (issue #15, thanks to ILLOMIURGE).
                [M] Updated ru_ru lang file (Yaroslavik).

    - v1.0.3-b3 [A] Added sitting on treated wood stool, Zombies included.
                [A] Added steel framed window.
                [A] Added treated wood pole support head/foot and heavy duty support.
                [A] Added language Russian language support, thanks to yaroslav4167.
                [A] Added config for furnace smelting speed (percent of vanilla furnace).
                [A] Added config for furnace fuel efficiency (in percent, ref is vanilla).
                [F] Treated pole model changed to circumvent potential texture bleeding.
                [M] Treated wood table bounding box aligned with the legs.
                [M] Treated wood crafting table bounding box aligned with the legs.
                [M] Treated wood window can be vertically placed for rooflights.

    - v1.0.3-b2 [A] Added config options for selective feature opt-outs (soft opt-out).
                [A] Added config skip registration of opt-out features (hard opt-out).
                [A] Added config to disable all internal recipes (for packs).
                [A] Added JEI API adapter for soft opt-outs.
                [A] Added lab furnace recipe override config to smelt ores to nuggets
                    that would normally be smelted into ingots. Can be changed on-the-fly.

    - v1.0.3-b1 [A] Added small laboratory furnace.
                [M] Panzer glass opacity/light level set explicitly 0.

                -------------------------------------------------------------------
    - v1.0.2    [R] Release based on v1.0.2-b3
                    * Fixes: Spawning.
                    * Crafting table: Shift-click.
                    * Ladders: Faster climbing/descending.
                    * Concrete: Rebar tiles, tile stairs.
                    * Treated wood: window, windowsill.
                    * Slag brick: wall.
                    * Panzer glass: added.
                    * Recipes: Adaptions, added decompositions.
                -------------------------------------------------------------------

    - v1.0.2-b3 [A] Added slag brick wall.
                [A] Added wall decomposition recipes.
                [A] Added treated wood window.
                [M] Climbing/descending mod ladders is faster when
                    looking up or down and not sneaking.
                [M] Panzer glass material definition changed.
                [M] Explicitly preventing spawning in and on "non-full"
                    blocks of the mod.

    - v1.0.2-b2 [A] Added rebar concrete tile stairs.
                [A] Added treated wood window sill.
                [A] Added decomposition recipes for stairs and tiles.
                [M] Changed stair recipe yield quantity from 9 to 6.

    - v1.0.2-b1 [A] Added rebar concrete tile.
                [A] Added panzer glass (explosion-resistant reinforced glass).
                [M] Treated wood crafting table supports shift-click to transfer
                    stacks between player inventory and crafting table storage
                    (thanks majijn for the hint).

                -------------------------------------------------------------------
    - v1.0.1    [R] Release based on v1.0.1-b4
                    * Treated wood crafting table
                    * Clinker brick wall
                    * Treated wood stool
                    * Inset spot light
                    * Recipe fixes
                    * Logo updated
                -------------------------------------------------------------------

    - v1.0.1-b4 [M] Crafting table keeps inventory and has eight storage slots.
                [M] Adapted inset light strength and harvest tool.
                [M] Crafting table recipe adapted.

    - v1.0.1-b3 [A] Added inset light (glowstone-metal, light level like torch,
                    can be used as floor/ceiling/wall light).
                [M] Crafting table model updated (issue #7, thanks majijn).
                [M] Logo image updated.

    - v1.0.1-b2 [A] Added treated wood crafting table.
                [A] Added treated wood stool.
                [F] Fixed ladder bounding boxes to allow climbing connected trap doors
                    (issue #6, thanks to Forgilageord).
                [M] Improved wall-block connections (wall elements only connect to other
                    walls or gates, as well as to solid blocks if these blocks are in
                    a straight line with at least two wall elements).
                [M] Decor walls are defined "solid" on top, so that e.g. torches and
                    redstone tracks can be placed on them.

    - v1.0.1-b1 [F] Fixed missing condition for ie:stone_deco in recipe constants.
                [A] Added clinker brick wall.

                -------------------------------------------------------------------
    - v1.0.0    [R] Release based on v1.0.0-b4
                -------------------------------------------------------------------

    - v1.0.0-b4 [F] Fixed vanished recipe for the rebar concrete wall.
                [A] Concrete wall, material: IE concrete.

    - v1.0.0-b3 [A] Textures of rebar concrete and treated wood table improved.
                [A] Added rebar concrete wall.

    - v1.0.0-b2 [A] Added rebar concrete (steel reinforced, expensive, creeper-proof).

    - v1.0.0-b1 [A] Initial structure.
                [A] Added clinker bricks and clinker brick stairs.
                [A] Added slag bricks and slag brick stairs.
                [A] Added metal rung ladder.
                [A] Added staggered metal steps ladder.
                [A] Added treated wood ladder.
                [A] Added treated wood pole.
                [A] Added treated wood table.

----
