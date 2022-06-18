/*
 * @file EdChair.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Full block characteristics class.
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.blocks.StandardBlocks;

import java.util.List;


public class EdChair
{
  private static boolean sitting_enabled = true;
  private static double sitting_probability = 0.1;
  private static double standup_probability = 0.01;

  public static void on_config(boolean without_sitting, boolean without_mob_sitting, double sitting_probability_percent, double standup_probability_percent)
  {
    sitting_enabled = (!without_sitting);
    sitting_probability = (without_sitting||without_mob_sitting) ? 0.0 : Mth.clamp(sitting_probability_percent/100, 0, 0.9);
    standup_probability = (without_sitting||without_mob_sitting) ? 1.0 : Mth.clamp(standup_probability_percent/100, 1e-6, 1e-2);
    ModConfig.log("Config chairs: sit:" + sitting_enabled + ", mob-sit: " + (sitting_probability*100) + "%, standup: " + (standup_probability) + "%.");
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class ChairBlock extends StandardBlocks.HorizontalWaterLoggable
  {
    public ChairBlock(long config, BlockBehaviour.Properties builder, final AABB[] unrotatedAABBs)
    { super(config, builder.randomTicks(), unrotatedAABBs); }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult rayTraceResult)
    {
      if(!sitting_enabled) return InteractionResult.PASS;
      if(world.isClientSide()) return InteractionResult.SUCCESS;
      EntityChair.sit(world, player, pos);
      return InteractionResult.CONSUME;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity)
    {
      if(sitting_enabled && (Math.random() < sitting_probability) && (entity instanceof Mob)) EntityChair.sit(world, (LivingEntity)entity, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rnd)
    {
      if((!sitting_enabled) || (sitting_probability < 1e-6)) return;
      final List<Mob> entities = world.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(2,1,2).expandTowards(0,1,0), e->true);
      if(entities.isEmpty()) return;
      int index = rnd.nextInt(entities.size());
      if((index < 0) || (index >= entities.size())) return;
      EntityChair.sit(world, entities.get(index), pos);
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class EntityChair extends Entity
  {
    public static final double x_offset = 0.5d;
    public static final double y_offset = 0.4d;
    public static final double z_offset = 0.5d;
    private int t_sit = 0;
    public BlockPos chair_pos = new BlockPos(0,0,0);

    public EntityChair(EntityType<? extends Entity> entityType, Level world)
    {
      super(entityType, world);
      blocksBuilding=true;
      setDeltaMovement(Vec3.ZERO);
      canUpdate(true);
      noPhysics=true;
    }

    public EntityChair(Level world)
    { this(ModContent.getEntityType("et_chair"), world); }

    public static EntityChair customClientFactory(PlayMessages.SpawnEntity spkt, Level world)
    { return new EntityChair(world); }

    public Packet<?> getAddEntityPacket()
    { return NetworkHooks.getEntitySpawningPacket(this); }

    public static boolean accepts_mob(LivingEntity entity)
    {
      if(!(entity instanceof Monster)) return false;
      if((entity.getType().getDimensions().height > 2.5) || (entity.getType().getDimensions().height > 2.0)) return false;
      if(entity instanceof Zombie) return true;
      if(entity instanceof ZombieVillager) return true;
      if(entity instanceof ZombifiedPiglin) return true;
      if(entity instanceof Piglin) return true;
      if(entity instanceof Husk) return true;
      if(entity instanceof Stray) return true;
      if(entity instanceof Skeleton) return true;
      if(entity instanceof WitherSkeleton) return true;
      return false;
    }

    public static void sit(Level world, LivingEntity sitter, BlockPos pos)
    {
      if(!sitting_enabled) return;
      if((world==null) || (world.isClientSide) || (sitter==null) || (pos==null)) return;
      if((!(sitter instanceof Player)) && (!accepts_mob(sitter))) return;
      if(!world.getEntitiesOfClass(EntityChair.class, new AABB(pos)).isEmpty()) return;
      if(sitter.isVehicle() || (!sitter.isAlive()) || (sitter.isPassenger()) ) return;
      if((!world.isEmptyBlock(pos.above())) || (!world.isEmptyBlock(pos.above(2)))) return;
      boolean on_top_of_block_position = true;
      boolean use_next_negative_y_position = false;
      EntityChair chair = new EntityChair(world);
      BlockPos chair_pos = chair.blockPosition();
      chair.chair_pos = pos;
      chair.t_sit = 5;
      chair.xo = chair_pos.getX();
      chair.yo = chair_pos.getY();
      chair.zo = chair_pos.getZ();
      chair.setPos(pos.getX()+x_offset,pos.getY()+y_offset,pos.getZ()+z_offset);
      world.addFreshEntity(chair);
      sitter.startRiding(chair, true);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {}

    @Override
    public boolean isPushable()
    { return false; }

    @Override
    public double getPassengersRidingOffset()
    { return 0.0; }

    @Override
    public void tick()
    {
      if(level.isClientSide) return;
      super.tick();
      if(--t_sit > 0) return;
      Entity sitter = getPassengers().isEmpty() ? null : getPassengers().get(0);
      if((sitter==null) || (!sitter.isAlive())) {
        this.remove(RemovalReason.DISCARDED);
        return;
      }
      boolean abort = (!sitting_enabled);
      final BlockState state = level.getBlockState(chair_pos);
      if((state==null) || (!(state.getBlock() instanceof ChairBlock))) abort = true;
      if(!level.isEmptyBlock(chair_pos.above())) abort = true;
      if((!(sitter instanceof Player)) && (Math.random() < standup_probability)) abort = true;
      if(abort) {
        for(Entity e:getPassengers()) {
          if(e.isAlive()) e.stopRiding();
        }
        this.remove(RemovalReason.DISCARDED);
      }
    }
  }

}
