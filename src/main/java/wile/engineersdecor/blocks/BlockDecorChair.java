/*
 * @file BlockDecorFull.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Full block characteristics class.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModEngineersDecor;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;


public class BlockDecorChair extends BlockDecorDirected
{
  //--------------------------------------------------------------------------------------------------------------------
  // Config
  //--------------------------------------------------------------------------------------------------------------------

  private static boolean sitting_enabled = true;
  private static double sitting_probability = 0.1;
  private static double standup_probability = 0.01;

  public static void on_config(boolean without_sitting, boolean without_mob_sitting, double sitting_probability_percent, double standup_probability_percent)
  {
    sitting_enabled = (!without_sitting);
    sitting_probability = (without_sitting||without_mob_sitting) ? 0.0 : MathHelper.clamp(sitting_probability_percent/100, 0, 0.9);
    standup_probability = (without_sitting||without_mob_sitting) ? 1.0 : MathHelper.clamp(standup_probability_percent/100, 1e-6, 1e-2);
    ModEngineersDecor.logger.info("Config chairs: " + sitting_enabled + ", sit: " + sitting_probability, ", stand up: " + standup_probability);
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public BlockDecorChair(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  { super(registryName, config, material, hardness, resistance, sound, unrotatedAABB); setLightOpacity(0); setTickRandomly(true); }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
  { if(sitting_enabled && (!world.isRemote)) { EntityChair.sit(world, player, pos); } return true; }

  @Override
  public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity)
  { if(sitting_enabled && (Math.random() < sitting_probability) && (entity instanceof EntityMob)) EntityChair.sit(world, (EntityLivingBase)entity, pos); }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand)
  {
    if((!sitting_enabled) || (sitting_probability < 1e-6)) return;
    final List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityMob.class, new AxisAlignedBB(pos).grow(2,1,2).expand(0,1,0));
    if(entities.isEmpty()) return;
    int index = rand.nextInt(entities.size());
    if((index < 0) || (index >= entities.size())) return;
    EntityChair.sit(world, entities.get(index), pos);
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Riding entity for sitting
  //--------------------------------------------------------------------------------------------------------------------

  public static class EntityChair extends Entity
  {
    public final double x_offset = 0.5d;
    public final double y_offset = 0.4d;
    public final double z_offset = 0.5d;
    private int t_tick = 0;
    private int t_sit = 0;
    public BlockPos chair_pos = new BlockPos(0,0,0);

    public EntityChair(World world)
    { super(world); preventEntitySpawning=true; noClip=true; setSize(2e-3f, 2e-3f); }

    public EntityChair(World world, BlockPos pos)
    {
      this(world);
      setPosition(pos.getX()+x_offset,pos.getY()+y_offset,pos.getZ()+z_offset);
      chair_pos = pos;
      t_sit = 5;
    }

    public static boolean accepts_mob(EntityLivingBase entity)
    {
      if(!(entity instanceof net.minecraft.entity.monster.EntityMob)) return false;
      if((entity.height > 2.5) || (entity.width > 2.0)) return false;
      if(entity instanceof net.minecraft.entity.monster.EntityZombie) return true;
      if(entity instanceof net.minecraft.entity.monster.EntityZombieVillager) return true;
      if(entity instanceof net.minecraft.entity.monster.EntityPigZombie) return true;
      if(entity instanceof net.minecraft.entity.monster.EntityHusk) return true;
      if(entity instanceof net.minecraft.entity.monster.EntityStray) return true;
      if(entity instanceof net.minecraft.entity.monster.EntitySkeleton) return true;
      if(entity instanceof net.minecraft.entity.monster.EntityWitherSkeleton) return true;
      return false;
    }

    public static void sit(World world, EntityLivingBase sitter, BlockPos pos)
    {
      if(!sitting_enabled) return;
      if((world==null) || (world.isRemote) || (sitter==null) || (pos==null)) return;
      if((!(sitter instanceof EntityPlayer)) && (!accepts_mob(sitter))) return;
      if(!world.getEntitiesWithinAABB(EntityChair.class, new AxisAlignedBB(pos)).isEmpty()) return;
      if(sitter.isBeingRidden() || (sitter.isDead) || (sitter.isRiding())) return;
      if((!world.isAirBlock(pos.up())) || (!world.isAirBlock(pos.up(2)))) return;
      EntityChair chair = new EntityChair(world, pos);
      if(world.spawnEntity(chair)) sitter.startRiding(chair);
    }

    @Override
    protected void entityInit()
    {}

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound)
    {}

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound)
    {}

    @Override
    public double getMountedYOffset()
    { return 0.0; }

    @Override
    public void onUpdate()
    {
      if((world.isRemote) || (--t_tick > 0)) return;
      t_tick = 20;
      if(--t_sit > 0) return;
      Entity sitter = getPassengers().isEmpty() ? null : getPassengers().get(0);
      if((sitter==null) || (sitter.isDead)) { setDead(); return; }
      boolean abort = !sitting_enabled;
      final IBlockState state = world.getBlockState(chair_pos);
      if((state==null) || (!(state.getBlock() instanceof BlockDecorChair))) abort = true;
      if(!world.isAirBlock(chair_pos.up())) abort = true;
      if((!(sitter instanceof EntityPlayer)) && (Math.random() < standup_probability)) abort = true;
      if(abort) {
        for(Entity e:getPassengers()) e.dismountRidingEntity();
        setDead();
      }
    }
  }
}
