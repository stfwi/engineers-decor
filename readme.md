
## Engineer's Decor

A [Minecraft](https://minecraft.net) (Java Edition) mod based on
[`Forge`](http://www.minecraftforge.net/), adding cosmetic blocks
for the Engineer's factory, workshop, and home.

![](documentation/engineers-decor-v100a-summary.png)

----
### Details

The mod has its focus on non-functional, decorative blocks. If anyhow possible,
no tile entities or user interactions are used. Current feature set:

- *Clinker bricks*: Slightly darker and more colorful version of the vanilla brick
  block. Eight position dependent texture variations are implemented to make the
  wall look more "alive". Crafted 3x3 with a brick block in the centre and any
  combination of bricks and nether bricks around (actually, anything where the
  ore dictionary says it's a "brick ingot"). Higher explosion resistance than the
  vanilla brick wall. Also available as stairs, crafted as usual. There is a
  reverse recipe to get three clinker brick blocks back from four stairs.

- *Slag bricks*: Gray-brownish brick, also eight texture variations. Crafted 3x3
  from slag in the centre and any kind of bricks ("brick ingot") around. Has a higher
  explosion resistance than the vanilla brick wall. Also available as stairs, also
  with reverse recipe.

- *Treated wood ladder*: Crafted 3x3 with the known ladder pattern, items are
  treated wood sticks.

- *Metal rung ladder*: Industrial wall-fixed ladder with horizontal bent rods.
  Crafted 3x3 with five iron or steel rods in a "U" pattern.

- *Staggered metal steps*: Industrial wall-fixed sparse ladder with steps in a
  zip pattern. Crafted 3x3 with six iron or steel rods in a zip pattern.

- *Treated wood table*: Four leg table made out of treated wood. Crafted 3x3
  with three treated wood slabs and four treated wood poles. Guess the pattern.

- *Treated wood pole*: Pole fragment that can be placed in all directions. It
  does intentionally not connect to posts, fences, etc - just a straigt pole.
  Can be used e.g. for structural support or wire relay post, where the height
  of the IE wire posts does not match.

More to come slowly but steadily.

----
### Mod pack integration, forking, back ports, bug reports, testing

  - Packs: If your mod pack ***is open source as well and has no installer***,
    you don't need to ask and simply integrate this mod.

  - Bug reports: Yes, please let me know. Drop a mail or better open an issue
    for the repository.

  - Pull requests: Happily accepted. Please make sure that use the ***develop
    branch*** for pull requests. The master branch is for release versions only.
    I might merge the pull request locally if I'm ahead of the github repository,
    we will communicate this in the pull request thread then.

  - The mod config has an "include testing features" option. Enabling this causes
    blocks under development to be registered as well.

----
## Revision history

    - v1.0.0-b1 [A] Initial structure.
                [A] Added clinker bricks and clinker brick stairs.
                [A] Added slag bricks and slag brick stairs.
                [A] Added metal rung ladder.
                [A] Added staggered metal steps ladder.
                [A] Added treated wood ladder.
                [A] Added treated wood pole.
                [A] Added treated wood table.

### Community references

Mods covering similar features, or may fit well together with IE and the decorations of this mod:

- [Immersive Engineering](https://github.com/BluSunrize/ImmersiveEngineering/): Without IE, my
  little mod here does not make much sense ;). It works without IE, but quite a few blocks are
  not craftable.

- [Engineer's doors](https://www.curseforge.com/minecraft/mc-mods/engineers-doors) has brilliant
  doors, trapdoors, and fence doors, all made of the IE materials.

- [Dirty Bricks](https://www.curseforge.com/minecraft/texture-packs/dirty-bricks-vanilla-add-on) applies
  position dependent variations to the vanilla bricks, similar to the clinkers and slag bricks in this
  mod.

- [Chisel](https://www.curseforge.com/minecraft/mc-mods/chisel) needless to say, Chisel has a variety
  of factory blocks.
