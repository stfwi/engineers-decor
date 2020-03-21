
## Engineer's Decor (MC1.15.1)

Mod sources for Minecraft version 1.15.1.

- Description, credits, and features: Please see the readme in the repository root.

- Compiled mod distribution channel is curseforge: https://www.curseforge.com/minecraft/mc-mods/engineers-decor/files.

----

## Version history

    ~ v1.0.20-b2  [A] Added Labeled Crate (storage crate with built-in item frame).

    - v1.0.20-b1  [A] Electrical Furnace: Added four-position speed switch (off, 100%, 150%, 200%), power consumption
                      increases at higher rate (off, 100%, 200%, 400%).
                  [A] Added Steel Mesh Fence Gate (single or double height gate fitting to the Steel Mesh Fence).
                  [M] Waste Incinerator processing speed tweaked.
                  [F] Fixed steel table visual glitch (thx Urbanxx001).
                  [M] MCP/Forge mappings updated.

    - v1.0.19-b5  [A] Added right-click display of power and progress information for Block Breaker, Solar Panel, and Tree Cutter.
                  [A] Solar Panel power curve tuned.
                  [A] Mod manual 1st edition release recipe added.
                  [A] Factory Hopper: Resetting NBT when breaking with empty inventory (for stacking), enabled item cap for all sides.
                  [M] Electrical Furnace model polished.

    - v1.0.19-b4  [A] Ported primary Immersive Engineering dependent recipes (alternative recipes
                      will still work if IE is not installed).
                  [F] Blacklisted Treated Wood Crafting Table in inventorysorter mod (issue #88, thx Nachtflame).
                  [M] Furni comparator output overrides reflect input slots and empty fuel state/power-cutoff.
                  [M] Solar Panel config: Default value for internal battery capacity increased.
                  [F] Block Placer: Shifted GUI player slots 1px to the right.
                  [A] Added mod block tags for slabs, stairs, and walls (PR#89, thanks CrudeAustin for the data).
                  [A] Added experimental Patchouli manual (creative only).

    - v1.0.19-b3  [M] Config tweaks: Value limit ranges increased to facilitate modpacking.
                  [A] Factory Hopper: Added bottom item handler (CR#227).
                  [M] Block shapes refined.
                  [F] Fixed duping bug (issue #87, thx Nachtflame)

    - v1.0.19-b2  [F] Fixed Floor Grating item pass-through jitters (thx Cid).
                  [M] Removed obsolete recipe collision testing recipes.
                  [F] Fixed missing Block Breaker dynamic block drops.
                  [F] Block Placer planting race condition issue fixed (issue #83, thx jcardii).
                  [F] Factory Hopper: Added second standard insertion run after smart-insert to circumcent compat issues (issue #84, thx NillerMedDild).
                  [A] Enabled JEI plugin (issue #85, thx ProsperCraft/Goshen).

    - v1.0.19-b1  [U] Update to 1.15.2.
                  [F] Fixed Tree Cutter / Block Breaker not accepting small energy transfers (thx WindFox, issue #82).

    - v1.0.18-b4  [A] Ported Treated Wood Crafting Table item rendering.
                  [F] Fixed Milking machine cow path issue, added milking delay cow tracking.
                  [F] Slab / Slab Slice placement adapted to vanilla standard.
                  [M] Lang update ru_ru (PR#77, thanks Smollet777).

    - v1.0.18-b3  [A] Added Treated Wood Crafting Table tweaks (ctrl-shift moves all same stacks from the
                      inventory, mouse wheel over crafting slot increases/decreases crafting grid stacks).
                  [F] EN Lang file fixed (issue #76, thx Riverstar907).
                  [F] Fixed Tree Cutter not respecting power-required config (thx federsavo, issue #77).
                  [F] Fixed Small Solar Panel not exposing energy capability (thx MatthiasMann, issue #78).

    - v1.0.18-b2  [M] Lang ru_ru updated (Smollet777).

    - v1.0.18-b1  [U] Updated to Forge 1.15.1-30.0.16/20190719-1.14.3.
                  [F] Client setup Dist annotation fixed (issue #73, thx hitsu420).
                  [F] Double newline escapes in lang files fixed ("\n" in a tooltip).
                  [M] Updated zh_cn lang file (scikirbypoke).
                  [A] Added opt-out config for the Small Tree Cutter

    - v1.0.17-b2  [A] Initial port.

----
