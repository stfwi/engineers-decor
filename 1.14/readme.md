
## Engineer's Decor (MC1.14.4)

Mod sources for Minecraft version 1.14.4.

- Description, credits, and features: Please see the readme in the repository root.

- Compiled mod distribution channel is curseforge: https://www.curseforge.com/minecraft/mc-mods/engineers-decor/files.

----

## Version history

    ~ v1.0.16-b2 [F] Fixed Small Block Breaker active model updated.

    - v1.0.16-b1 [U] Updated to Forge 1.14.4-28.1.79/20190719-1.14.3.
                 [A] Added Fluid Collection Funnel.

    - v1.0.15-b3 [A] Added Small Block Breaker.
                 [M] Mineral Smelter fluid handler/transfer added.

    - v1.0.15-b2 [!] Forge version requirement set to 1.14.4-28.1.68 or higher.
                 [A] Added Factory Block Placer and Planter.
                 [A] Added Small Tree Cutter.

    - v1.0.15-b1 [A] Added Floor Edge Light.
                 [U] Updated to Forge 1.14.4-28.1.68/20190719-1.14.3.

    - v1.0.14-b1 [U] Updated to Forge 1.14.4-28.1.40/20190719-1.14.3.
                 [A] Factory Hopper added (configurable hopper and item collector).
                 [M] Switched to integrated loot table generation.
                 [M] Lang file zh_cn updated (scikirbypoke, PR#53).

    - v1.0.13-b2 [A] Added Steel Mesh Fence.
                 [A] Added Broad Window Sill.

    - v1.0.12-b3 [U] Updated to Forge 1.14.4-28.1.10/20190719-1.14.3.
                 [A] Crafting Table: Added recipe collision resolver,
                     also applies to crafting history refabrication.
                 [A] Crafting Table: Added rendering of placed items
                     on the top surface of the table.
                 [A] Waterlogging of non-full-blocks added.

    - v1.0.12-b2 [U] Updated to Forge 1.14.4-28.0.105/20190719-1.14.3.
                 [A] Small Solar Panel added.
                 [M] Items fall through the Steel Floor Grating like in 1.12.2 version.
                 [M] Factory Dropper: Added pulse/continuous mode in GUI (issue #51,
                     thx Aristine for the CR).

    - v1.0.12-b1 [U] Updated to Forge 1.14.4-28.0.93/20190719-1.14.3.
                 [M] Logo location fixed.

    - v1.0.11-b3 [U] Updated to Forge 1.14.4-28.0.81/20190719-1.14.3.
                 [F] Adapted recipe condition to Forge version (issue #49).

    - v1.0.11-b2 [U] JEI dependency update 1.14.4:6.0.0.10.
                 [F] Fixed creative ghost block issue (issue #48).
                 [M] Updated ru_ru lang file (Shellyoung, PR#47).

    - v1.0.11-b1 [A] Added Steel Table
                 [A] Added Treated Wood Side Table
                 [A] Added Exit Sign
                 [A] Added Steel Floor Grating
                 [M] Sign orientation fixed, only blocked vertical placement.

    - v1.0.9-b9  [U] Update to Forge 1.14.4-28.0.40/20190719-1.14.3 for Forge
                     testing.

    - v1.0.9-b8  [U] UPDATE TO 1.14.4. Forge 1.14.4-28.0.11/20190719-1.14.3.

    - v1.0.9-b7  [U] Updated to Forge 1.14.3-27.0.60/20190719-1.14.3.
                 [F] Disabled all early implemented fuild handling of valves
                     and the Fluid Accumulator to prevent world loading
                     hang-ups (issue #42, thx TheOhmegha for reporting).
                     Will be re-enabled after fluid handling released in Forge.
                 [F] Fixed blockstate model locations for signs and crafting
                     table (issue #43, thx ProsperCraft for the beta test).

    - v1.0.9-b6  [U] Updated to Forge 1.14.3-27.0.50/20190621-1.14.2.

    - v1.0.9-b5  [A] Added missing recipes for slabs, stained clinker, half-slabs, valves.
                 [M] Standalone recipes adapted.
                 [F] Lang files: Fixed double newline escape.
                 [A] Implicit opt-out of hard IE dependent blocks ported (e.g. Concrete Wall).
                 [M] Basic mod config features, opt-outs and tweaks ported.

    - v1.0.9-b4  [E] Experimental: Config skel ported (!not all options have effect yet).
                 [E] Experimental: JEI integration for opt-outs and crafting table ported
                     (also addresses issue #38).

    - v1.0.9-b3  [F] Additional item drop fixes when blocks are destroyed (issue #39).

    - v1.0.9-b2  [U] Updated to Forge 1.14.3-27.0.25/20190621-1.14.2.
                 [F] Fixed recipe collision of Metal Rung Ladder (issue #37,
                     thx ProsperCraft for reporting).
                 [F] Fixed opening crafting table, furni, dropper server crash
                     issue #35 (thx ProsperCraft also here).
                 [F] Fixed missing pole/support item drops (issue #36, ProsperCraft).

    - v1.0.9-b1  [U] Updated to MC1.14.3, Forge 1.14.3-27.0.17/20190621-1.14.2.
                 [A] Added Small Mineral Smelter.

    - v1.0.8-b3  [A] Ported slabs and slab slices from 1.12.2.
                 [A] IE independent ("standalone") recipes ported.

    - v1.0.8-b2  [U] Updated to Forge BETA 1.14.2-26.0.63/20190621-1.14.2,
                     code adapted to new mappings.
                 [M] Updated 1st/3rd person item model rotations/translations.

    - v1.0.8-b1  [V] Feature set of 1.12 ported.
                 [A] CTRL-SHIFT tooltips ported.
                 [A] Ported stained clinker block/stairs.
                 [M] Updated textures.
                 [I] Issue: Scoped recipe constants still not working.

    - v1.0.7-b5  [U] Updated to Forge BETA 1.14.2-26.0.35/20190608-1.14.2.
                 [A] Factory dropper functionality ported.
                 [A] Small lab furnace functionality ported.
                 [A] Small electrical lab furnace functionality ported.
                 [A] Small waste incinerator functionality ported.
                 [A] Fluid valves, Passive Fluid Accumulator ported.
                 [I] Issue: Scoped recipe constants still not working.

    - v1.0.7-b4  [U] Updated to Forge BETA 1.14.2-26.0.32/20190608-1.14.2.
                 [A] Sitting on the stool ported.
                 [A] Ladder climbing speed boost ported.
                 [A] Crafting table functionality ported.
                 [I] Issue: Scoped recipe constants not working yet with
                     the current Forge version (or somehow changed).

    - v1.0.7-b3  [A] Initial 1.14.2 port of decorative blocks.

----
