/*
 * @file EdSolarPanel.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Smaller (cutout) block with a defined facing.
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.blocks.StandardEntityBlocks;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import wile.engineersdecor.libmc.detail.Overlay;
import wile.engineersdecor.libmc.detail.RfEnergy;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class EdSolarPanel
{
  public static final int DEFAULT_PEAK_POWER = 40;
  private static int peak_power_per_tick_ = DEFAULT_PEAK_POWER;
  private static int max_power_storage_ = 64000;
  private static int max_feed_power = 4096;
  private static int feeding_threshold = max_power_storage_/5;
  private static int balancing_threshold = max_power_storage_/10;

  public static void on_config(int peak_power_per_tick, int battery_capacity, int max_feed_in_power)
  {
    final int t = SolarPanelTileEntity.TICK_INTERVAL;
    peak_power_per_tick_ = Mth.clamp(peak_power_per_tick, 12, 8192);
    feeding_threshold = Math.max(max_power_storage_/5, 1000);
    balancing_threshold = Math.max(max_power_storage_/10, 1000);
    max_power_storage_ = battery_capacity;
    max_feed_power = max_feed_in_power * t;
    ModConfig.log("Config small solar panel: Peak production:" + peak_power_per_tick_ + "/t, capacity:" + max_power_storage_ + "rf, max-feed:" + (max_feed_power/t) + "rf/t");
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class SolarPanelBlock extends StandardBlocks.Cutout implements StandardEntityBlocks.IStandardEntityBlock<SolarPanelTileEntity>
  {
    public static final IntegerProperty EXPOSITION = IntegerProperty.create("exposition", 0, 4);

    public SolarPanelBlock(long config, BlockBehaviour.Properties builder, final AABB[] unrotatedAABB)
    {
      super(config, builder, unrotatedAABB);
      registerDefaultState(super.defaultBlockState().setValue(EXPOSITION, 1));
    }

    @Override
    public boolean isBlockEntityTicking(Level world, BlockState state)
    { return true; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(EXPOSITION); }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
      if(world.isClientSide()) return InteractionResult.SUCCESS;
      BlockEntity te = world.getBlockEntity(pos);
      if(te instanceof SolarPanelTileEntity) ((SolarPanelTileEntity)te).state_message(player);
      return InteractionResult.CONSUME;
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, LevelReader world, BlockPos pos, Direction side)
    { return false; }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class SolarPanelTileEntity extends StandardEntityBlocks.StandardBlockEntity
  {
    public static final int TICK_INTERVAL = 4;
    public static final int ACCUMULATION_INTERVAL = 8;
    private static final Direction[] transfer_directions_ = {Direction.DOWN, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH };

    private int tick_timer_ = 0;
    private int recalc_timer_ = 0;
    private int current_production_ = 0;
    private int current_feedin_ = 0;
    private boolean output_enabled_ = false;

    private final RfEnergy.Battery battery_ = new RfEnergy.Battery(max_power_storage_, 0, 1024);
    private final LazyOptional<IEnergyStorage> energy_handler_ = battery_.createEnergyHandler();

    //------------------------------------------------------------------------------------------------------------------

    public SolarPanelTileEntity(BlockPos pos, BlockState state)
    { super(ModContent.getBlockEntityTypeOfBlock(state.getBlock()), pos, state); }

    public void readnbt(CompoundTag nbt, boolean update_packet)
    { battery_.load(nbt); }

    protected void writenbt(CompoundTag nbt, boolean update_packet)
    { battery_.save(nbt); }

    public void state_message(Player player)
    {
      String soc = Integer.toString(Mth.clamp((battery_.getEnergyStored()*100/max_power_storage_),0,100));
      Overlay.show(player, Auxiliaries.localizable("block.engineersdecor.small_solar_panel.status", soc, max_power_storage_, current_production_, current_feedin_));
    }

    // ICapabilityProvider ---------------------------------------------------------------------

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability== CapabilityEnergy.ENERGY) return energy_handler_.cast();
      return super.getCapability(capability, facing);
    }

    // BlockEntity ------------------------------------------------------------------------------

    @Override
    public void load(CompoundTag nbt)
    { super.load(nbt); readnbt(nbt, false); }

    @Override
    protected void saveAdditional(CompoundTag nbt)
    { super.saveAdditional(nbt); writenbt(nbt, false); }

    @Override
    public void setRemoved()
    {
      super.setRemoved();
      energy_handler_.invalidate();
    }

    @Override
    public void tick()
    {
      if((level.isClientSide) || (--tick_timer_ > 0)) return;
      tick_timer_ = TICK_INTERVAL;
      BlockState state = level.getBlockState(worldPosition);
      if(!(state.getBlock() instanceof SolarPanelBlock)) return;
      current_feedin_ = 0;
      final List<SolarPanelTileEntity> adjacent_panels = new ArrayList<>();
      if(output_enabled_) {
        for(int i=0; (i<transfer_directions_.length) && (!battery_.isEmpty()); ++i) {
          final Direction f = transfer_directions_[i];
          BlockEntity te = level.getBlockEntity(worldPosition.relative(f));
          if(te==null) continue;
          IEnergyStorage es = te.getCapability(CapabilityEnergy.ENERGY, f.getOpposite()).orElse(null);
          if(es==null) continue;
          if(!es.canReceive()) {
            if(!(te instanceof SolarPanelTileEntity)) continue;
            adjacent_panels.add((SolarPanelTileEntity)te);
            continue;
          }
          final int feed_power = (battery_.getEnergyStored() > (max_power_storage_/10)) ? max_feed_power : Math.max(current_production_*2, (peak_power_per_tick_/4));
          final int fed = es.receiveEnergy(Math.min(battery_.getEnergyStored(), feed_power * TICK_INTERVAL), false);
          battery_.draw(fed);
          current_feedin_ += fed;
        }
      }
      current_feedin_ /= TICK_INTERVAL;
      if((current_feedin_ <= 0) && ((battery_.getEnergyStored() >= balancing_threshold) || (current_production_ <= 0))) {
        for(SolarPanelTileEntity panel: adjacent_panels) {
          if(panel.battery_.getEnergyStored() >= (battery_.getEnergyStored()-balancing_threshold)) continue;
          panel.battery_.setEnergyStored(panel.battery_.getEnergyStored() + balancing_threshold);
          battery_.setEnergyStored(battery_.getEnergyStored() - balancing_threshold);
          if(battery_.getEnergyStored() < balancing_threshold) break;
        }
      }
      if(!level.canSeeSkyFromBelowWater(worldPosition)) {
        tick_timer_ = TICK_INTERVAL * 10;
        current_production_ = 0;
        if((!battery_.isEmpty())) output_enabled_ = true;
        if(state.getValue((SolarPanelBlock.EXPOSITION))!=2) level.setBlockAndUpdate(worldPosition, state.setValue(SolarPanelBlock.EXPOSITION, 2));
        return;
      }
      if(battery_.isEmpty()) output_enabled_ = false;
      if(--recalc_timer_ > 0) return;
      recalc_timer_ = ACCUMULATION_INTERVAL + ((int)(Math.random()+.5));
      int theta = ((((int)(level.getSunAngle(1f) * (180.0/Math.PI)))+90) % 360);
      int e = 2;
      if(theta > 340)      e = 2;
      else if(theta <  45) e = 0;
      else if(theta <  80) e = 1;
      else if(theta < 100) e = 2;
      else if(theta < 135) e = 3;
      else if(theta < 190) e = 4;
      BlockState nstate = state.setValue(SolarPanelBlock.EXPOSITION, e);
      if(nstate != state) level.setBlock(worldPosition, nstate, 1|2);
      final double eff = (1.0-((level.getRainLevel(1f)*0.6)+(level.getThunderLevel(1f)*0.3)));
      final double ll = ((double)(level.getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(getBlockPos())))/15;
      final double rf = Math.sin((Math.PI/2) * Math.sqrt(((double)(((theta<0)||(theta>180))?(0):((theta>90)?(180-theta):(theta))))/90));
      current_production_ = (int)(Math.min(rf*rf*eff*ll, 1) * peak_power_per_tick_);
      battery_.setEnergyStored(Math.min(battery_.getEnergyStored() + (current_production_*(TICK_INTERVAL*ACCUMULATION_INTERVAL)), max_power_storage_));
      if(battery_.getEnergyStored() >= (feeding_threshold)) output_enabled_ = true;
    }
  }
}
