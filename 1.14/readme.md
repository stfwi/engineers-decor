
## Engineer's Decor (MC1.14.4)

Mod sources for Minecraft version 1.14.4.

- Description, credits, and features: Please see the readme in the repository root.

- Compiled mod distribution channel is curseforge: https://www.curseforge.com/minecraft/mc-mods/engineers-decor/files.

----

## Version history

    - v1.1.0-b1   [F] Fixed Electrical Furnace speed sanitizing bug (issue #97, thx therobotmenace).
                  [A] Sheet Metal Slab Slices added (only available if IE is installed).
                  [M] Config options extended/updated.
                  [M] Dense Grit Sand textures altered to get slightly more visible structure
                      from distance.

    - v1.0.20-b7  [A] Dense Grit Sand added.
                  [!] Variant Slab compatibility fix. *This may alter placed slabs of this mod,
                      data fixers don't work yet unfortunately*.

    - v1.0.20-b6  [M] Slab Slice placement improved.
                  [M] Quite some naming/refractoring under the hood.

    - v1.0.20-b5  [A] Electrical Furnace can draw in smelting input items from an adjacent inventory when a
                      Hopper is placed in the auxiliary slot.
                  [M] Wrapped Labeled Crate label slot to prevent sorting mods from altering the label.

    - v1.0.20-b4  [F] Fixed Mineral Smelter fluid voiding on external draining (issue #92, thx papaworld, pupnewfster).

    - v1.0.20-b3  [M] Labeled Crate: GUI quick-move-all (ctrl-shift click) smart move tweaked, Manual page added.
                  [F] Fixed IE ingredients based default recipes for Factory Dropper and Small Electrical Furnace.
                  [M] Factory Hopper: GUI quick-move-all added.
                  [M] Code structure, simplifications, cap invalidation fixes.
                  [M] Valves: Removed performance caching for testing purposes.

    - v1.0.20-b2  [U] Forge version requirement set to >= 28.2.3.
                  [A] Added Labeled Crate (storage crate with built-in item frame).

    - v1.0.20-b1  [A] Electrical Furnace: Added four-position speed switch (off, 100%, 150%, 200%), power consumption
                      increases at higher rate (off, 100%, 200%, 400%).
                  [A] Added Steel Mesh Fence Gate (single or double height gate fitting to the Steel Mesh Fence).
                  [M] Waste Incinerator processing speed tweaked.

    - v1.0.19-b5  [A] Added right-click display of power and progress information for Block Breaker, Solar Panel, and Tree Cutter.
                  [A] Solar Panel power curve tuned.
                  [A] Mod manual 1st edition release recipe added.
                  [A] Factory Hopper: Resetting NBT when breaking with empty inventory (for stacking), enabled item cap for all sides.
                  [M] Electrical Furnace model polished.

    - v1.0.19-b4  [A] Ported primary Immersive Engineering dependent recipes (alternative recipes
                      will still work if IE is not installed).
                  [M] Furni comparator output overrides reflect input slots and empty fuel state/power-cutoff.
                  [M] Solar Panel config: Default value for internal battery capacity increased.
                  [F] Block Placer: Shifted GUI player slots 1px to the right.
                  [A] Added mod block tags for slabs, stairs, and walls (PR#89, thanks CrudeAustin for the data).
                  [A] Added experimental Patchouli manual (creative only).
                  [!] Skipped blacklisting Treated Wood Crafting Table slots for the inventorysorter mod due
                      to potential startup crashes for single player games (issue #88 fix deferred).

    - v1.0.19-b3  [M] Config tweaks: Value limit ranges increased to facilitate modpacking.
                  [A] Factory Hopper: Added bottom item handler (CR#227).
                  [M] Block shapes refined.
                  [F] Fixed duping bug (issue #87, thx Nachtflame)

    - v1.0.19-b2  [F] Fixed Floor Grating item pass-through jitters (thx Cid).
                  [M] Removed obsolete recipe collision testing recipes.
                  [F] Fixed missing Block Breaker dynamic block drops.
                  [F] Block Placer planting race condition issue fixed (issue #83, thx jcardii).
                  [F] Factory Hopper: Added second standard insertion after smart-insert to circumcent compat issues (issue #84, thx NillerMedDild).

    - v1.0.19-b1  [F] Fixed Tree Cutter / Block Breaker not accepting small energy transfers (thx WindFox, issue #82).

    - v1.0.18-b4  [M] Lang update ru_ru (PR#77, thanks Smollet777).
                  [F] Fixed Milking machine cow path issue, added milking delay cow tracking.
                  [F] Slab / Slab Slice placement adapted to vanilla standard.

    - v1.0.18-b3  [A] Added Treated Wood Crafting table tweaks (ctrl-shift moves all same stacks from the
                      inventory, mouse wheel over crafting slot increases/decreases crafting grid stacks).
                  [F] EN Lang file fixed (issue #76, thx Riverstar907).
                  [F] Fixed Tree Cutter not respecting power-required config (thx federsavo, issue #77).
                  [F] Fixed Small Solar Panel not exposing energy capability (thx MatthiasMann, issue #78).

    - v1.0.18-b2  [F] Fixed JEI integration warning if nothing is opt'ed out (thx @SDUBZ for reporting).
                  [M] Lang ru_ru updated (Smollet777).

    - v1.0.18-b1  [U] Updated to Forge 1.14.4-28.1.109/20190719-1.14.3.
                  [A] Added opt-out config for the Small Tree Cutter.

    - v1.0.17-b3  [F] Double newline escapes in lang files fixed ("\n" in a tooltip).
                  [M] Updated zh_cn lang file (scikirbypoke).

    - v1.0.17-b2  [A] Reverse recipes for slabs and slab slices added.
                  [M] Inset Floor Edge Light slightly thinner, looks better.

    - v1.0.17-b1  [A] Added Milking Machine.
                  [A] Added Mineral Smelter gravity fluid transfer.
                  [M] Window placement improved.
                  [M] Made Pipe Valve textures slightly darker to fit IE pipes better when shaded.
                  [F] Levers can be directly attached to redstone controller Pipe Valves.
                  [F] Replaced Pipe Valve early load with lazy initialized data (issue #69, thx @Siriuo).

    - v1.0.16-b7  [M] Forge blockstates ported from 1.12 transformed to vanilla.

    - v1.0.16-b6  [A] Made slab slice left-click pickup optional (default enabled).
                  [A] Added config option for device drops in creative mode (addresses #67),
                  [F] Fixed Panzer Glass Block submerged display (issue #68, thx WenXin20).

    - v1.0.16-b5  [F] Fixed recipe condition bug (issue #65, thx Nachtflame for the report,
                      and gigaherz & killjoy for the help).

    - v1.0.16-b4  [U] Updated to Forge 1.14.4-28.1.90/20190719-1.14.3.
                  [M] Increased slag brick recipe yield to 8.
                  [M] Parent specs in model files adapted.

    - v1.0.16-b3  [A] Config options (opt-outs and tweaks) added.
                  [M] Increased clinker brick recipe yield to 8 for the builders needs.

    - v1.0.16-b2  [A] Added Gas Concrete (including wall, stairs, slab, and slab slice).
                  [F] Fixed Small Block Breaker active model.
                  [F] Fixed item-on-ground display glitch (issue #61, thx Federsavo for the hint).
                  [F] Added two missing recipes.

    - v1.0.16-b1  [U] Updated to Forge 1.14.4-28.1.79/20190719-1.14.3.
                  [A] Added Fluid Collection Funnel.

    - v1.0.15-b3  [A] Added Small Block Breaker.
                  [M] Mineral Smelter fluid handler/transfer added.

    - v1.0.15-b2  [!] Forge version requirement set to 1.14.4-28.1.68 or higher.
                  [A] Added Factory Block Placer and Planter.
                  [A] Added Small Tree Cutter.

    - v1.0.15-b1  [A] Added Floor Edge Light.
                  [U] Updated to Forge 1.14.4-28.1.68/20190719-1.14.3.

    - v1.0.14-b1  [U] Updated to Forge 1.14.4-28.1.40/20190719-1.14.3.
                  [A] Factory Hopper added (configurable hopper and item collector).
                  [M] Switched to integrated loot table generation.
                  [M] Lang file zh_cn updated (scikirbypoke, PR#53).

    - v1.0.13-b2  [A] Added Steel Mesh Fence.
                  [A] Added Broad Window Sill.

    - v1.0.12-b3  [U] Updated to Forge 1.14.4-28.1.10/20190719-1.14.3.
                  [A] Crafting Table: Added recipe collision resolver,
                      also applies to crafting history refabrication.
                  [A] Crafting Table: Added rendering of placed items
                      on the top surface of the table.
                  [A] Waterlogging of non-full-blocks added.

    - v1.0.12-b2  [U] Updated to Forge 1.14.4-28.0.105/20190719-1.14.3.
                  [A] Small Solar Panel added.
                  [M] Items fall through the Steel Floor Grating like in 1.12.2 version.
                  [M] Factory Dropper: Added pulse/continuous mode in GUI (issue #51,
                      thx Aristine for the CR).

    - v1.0.12-b1  [U] Updated to Forge 1.14.4-28.0.93/20190719-1.14.3.
                  [M] Logo location fixed.

    - v1.0.11-b3  [U] Updated to Forge 1.14.4-28.0.81/20190719-1.14.3.
                  [F] Adapted recipe condition to Forge version (issue #49).

    - v1.0.11-b2  [U] JEI dependency update 1.14.4:6.0.0.10.
                  [F] Fixed creative ghost block issue (issue #48).
                  [M] Updated ru_ru lang file (Shellyoung, PR#47).

    - v1.0.11-b1  [A] Added Steel Table
                  [A] Added Treated Wood Side Table
                  [A] Added Exit Sign
                  [A] Added Steel Floor Grating
                  [M] Sign orientation fixed, only blocked vertical placement.

    - v1.0.9-b9   [U] Update to Forge 1.14.4-28.0.40/20190719-1.14.3 for Forge
                      testing.

    - v1.0.9-b8   [U] UPDATE TO 1.14.4. Forge 1.14.4-28.0.11/20190719-1.14.3.

    - v1.0.9-b7   [U] Updated to Forge 1.14.3-27.0.60/20190719-1.14.3.
                  [F] Disabled all early implemented fuild handling of valves
                      and the Fluid Accumulator to prevent world loading
                      hang-ups (issue #42, thx TheOhmegha for reporting).
                      Will be re-enabled after fluid handling released in Forge.
                  [F] Fixed blockstate model locations for signs and crafting
                      table (issue #43, thx ProsperCraft for the beta test).

    - v1.0.9-b6   [U] Updated to Forge 1.14.3-27.0.50/20190621-1.14.2.

    - v1.0.9-b5   [A] Added missing recipes for slabs, stained clinker, half-slabs, valves.
                  [M] Standalone recipes adapted.
                  [F] Lang files: Fixed double newline escape.
                  [A] Implicit opt-out of hard IE dependent blocks ported (e.g. Concrete Wall).
                  [M] Basic mod config features, opt-outs and tweaks ported.

    - v1.0.9-b4   [E] Experimental: Config skel ported (!not all options have effect yet).
                  [E] Experimental: JEI integration for opt-outs and crafting table ported
                      (also addresses issue #38).

    - v1.0.9-b3   [F] Additional item drop fixes when blocks are destroyed (issue #39).

    - v1.0.9-b2   [U] Updated to Forge 1.14.3-27.0.25/20190621-1.14.2.
                  [F] Fixed recipe collision of Metal Rung Ladder (issue #37,
                      thx ProsperCraft for reporting).
                  [F] Fixed opening crafting table, furni, dropper server crash
                      issue #35 (thx ProsperCraft also here).
                  [F] Fixed missing pole/support item drops (issue #36, ProsperCraft).

    - v1.0.9-b1   [U] Updated to MC1.14.3, Forge 1.14.3-27.0.17/20190621-1.14.2.
                  [A] Added Small Mineral Smelter.

    - v1.0.8-b3   [A] Ported slabs and slab slices from 1.12.2.
                  [A] IE independent ("standalone") recipes ported.

    - v1.0.8-b2   [U] Updated to Forge BETA 1.14.2-26.0.63/20190621-1.14.2,
                      code adapted to new mappings.
                  [M] Updated 1st/3rd person item model rotations/translations.

    - v1.0.8-b1   [V] Feature set of 1.12 ported.
                  [A] CTRL-SHIFT tooltips ported.
                  [A] Ported stained clinker block/stairs.
                  [M] Updated textures.
                  [I] Issue: Scoped recipe constants still not working.

    - v1.0.7-b5   [U] Updated to Forge BETA 1.14.2-26.0.35/20190608-1.14.2.
                  [A] Factory dropper functionality ported.
                  [A] Small lab furnace functionality ported.
                  [A] Small electrical lab furnace functionality ported.
                  [A] Small waste incinerator functionality ported.
                  [A] Fluid valves, Passive Fluid Accumulator ported.
                  [I] Issue: Scoped recipe constants still not working.

    - v1.0.7-b4   [U] Updated to Forge BETA 1.14.2-26.0.32/20190608-1.14.2.
                  [A] Sitting on the stool ported.
                  [A] Ladder climbing speed boost ported.
                  [A] Crafting table functionality ported.
                  [I] Issue: Scoped recipe constants not working yet with
                      the current Forge version (or somehow changed).

    - v1.0.7-b3   [A] Initial 1.14.2 port of decorative blocks.

----
