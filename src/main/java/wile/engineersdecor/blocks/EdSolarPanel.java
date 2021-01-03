/*
 * @file EdSolarPanel.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Smaller (cutout) block with a defined facing.
 */
package wile.engineersdecor.blocks;

import net.minecraft.world.IWorldReader;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import wile.engineersdecor.libmc.detail.Overlay;
import net.minecraft.world.World;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.LightType;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


public class EdSolarPanel
{
  public static void on_config(int peak_power_per_tick)
  { SolarPanelTileEntity.on_config(peak_power_per_tick); }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class SolarPanelBlock extends DecorBlock.Normal implements IDecorBlock
  {
    public static final IntegerProperty EXPOSITION = IntegerProperty.create("exposition", 0, 4);

    public SolarPanelBlock(long config, Block.Properties builder, final AxisAlignedBB[] unrotatedAABB)
    {
      super(config, builder, unrotatedAABB);
      setDefaultState(super.getDefaultState().with(EXPOSITION, 1));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    { super.fillStateContainer(builder); builder.add(EXPOSITION); }

    @Override
    public boolean hasTileEntity(BlockState state)
    { return true; }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    { return new EdSolarPanel.SolarPanelTileEntity(); }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
    {
      if(world.isRemote()) return ActionResultType.SUCCESS;
      TileEntity te = world.getTileEntity(pos);
      if(te instanceof SolarPanelTileEntity) ((SolarPanelTileEntity)te).state_message(player);
      return ActionResultType.CONSUME;
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, IWorldReader world, BlockPos pos, Direction side)
    { return false; }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class SolarPanelTileEntity extends TileEntity implements ITickableTileEntity, ICapabilityProvider, IEnergyStorage
  {
    public static final int DEFAULT_PEAK_POWER = 40;
    public static final int TICK_INTERVAL = 4;
    public static final int ACCUMULATION_INTERVAL = 8;
    private static final Direction transfer_directions_[] = {Direction.DOWN, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH };
    private static int peak_power_per_tick_ = DEFAULT_PEAK_POWER;
    private static int max_power_storage_ = 64000;
    private static int max_feed_power = 8192;
    private static int feeding_threshold = max_power_storage_/5;
    private static int balancing_threshold = max_power_storage_/10;
    private int tick_timer_ = 0;
    private int recalc_timer_ = 0;
    private int accumulated_power_ = 0;
    private int current_production_ = 0;
    private int current_feedin_ = 0;
    private boolean output_enabled_ = false;

    public static void on_config(int peak_power_per_tick)
    {
      peak_power_per_tick_ = MathHelper.clamp(peak_power_per_tick, 2, 8192);
      feeding_threshold = Math.max(max_power_storage_/5, 1000);
      balancing_threshold = Math.max(max_power_storage_/10, 1000);
      ModEngineersDecor.logger().info("Config small solar panel: Peak production:" + peak_power_per_tick_ + "/t.");
    }

    //------------------------------------------------------------------------------------------------------------------

    public SolarPanelTileEntity()
    { this(ModContent.TET_SMALL_SOLAR_PANEL); }

    public SolarPanelTileEntity(TileEntityType<?> te_type)
    { super(te_type); }

    public void readnbt(CompoundNBT nbt, boolean update_packet)
    { accumulated_power_ = nbt.getInt("energy"); }

    protected void writenbt(CompoundNBT nbt, boolean update_packet)
    { nbt.putInt("energy", accumulated_power_); }

    public void state_message(PlayerEntity player)
    {
      String soc = Integer.toString(MathHelper.clamp((accumulated_power_*100/max_power_storage_),0,100));
      Overlay.show(player, Auxiliaries.localizable("block.engineersdecor.small_solar_panel.status", new Object[]{soc, max_power_storage_, current_production_, current_feedin_ }));
    }

    // IEnergyStorage --------------------------------------------------------------------------

    @Override
    public boolean canExtract()
    { return true; }

    @Override
    public boolean canReceive()
    { return false; }

    @Override
    public int getMaxEnergyStored()
    { return max_power_storage_; }

    @Override
    public int getEnergyStored()
    { return accumulated_power_; }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate)
    {
      int p = Math.min(accumulated_power_, maxExtract);
      if(!simulate) accumulated_power_ -= p;
      return p;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate)
    { return 0; }

    // ICapabilityProvider ---------------------------------------------------------------------

    protected LazyOptional<IEnergyStorage> energy_handler_ = LazyOptional.of(() -> (IEnergyStorage)this);

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability== CapabilityEnergy.ENERGY) return energy_handler_.cast();
      return super.getCapability(capability, facing);
    }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public void read(BlockState state, CompoundNBT nbt)
    { super.read(state, nbt); readnbt(nbt, false); }

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    { super.write(nbt); writenbt(nbt, false); return nbt; }

    @Override
    public void remove()
    {
      super.remove();
      energy_handler_.invalidate();
    }

    @Override
    public void tick()
    {
      if((world.isRemote) || (--tick_timer_ > 0)) return;
      tick_timer_ = TICK_INTERVAL;
      BlockState state = world.getBlockState(pos);
      if(!(state.getBlock() instanceof SolarPanelBlock)) return;
      current_feedin_ = 0;
      final List<SolarPanelTileEntity> adjacent_panels = new ArrayList<>();
      if(output_enabled_) {
        for(int i=0; (i<transfer_directions_.length) && (accumulated_power_>0); ++i) {
          final Direction f = transfer_directions_[i];
          TileEntity te = world.getTileEntity(pos.offset(f));
          if(te==null) continue;
          IEnergyStorage es = te.getCapability(CapabilityEnergy.ENERGY, f.getOpposite()).orElse(null);
          if(es==null) continue;
          if(!es.canReceive()) {
            if(!(te instanceof SolarPanelTileEntity)) continue;
            adjacent_panels.add((SolarPanelTileEntity)te);
            continue;
          }
          final int feed_power = (accumulated_power_ > (max_power_storage_/10)) ? max_feed_power : Math.max(current_production_*2, (peak_power_per_tick_/4));
          int fed = es.receiveEnergy(Math.min(accumulated_power_, feed_power * TICK_INTERVAL), false);
          accumulated_power_ = MathHelper.clamp(accumulated_power_-fed,0, accumulated_power_);
          current_feedin_ += fed;
        }
      }
      current_feedin_ /= TICK_INTERVAL;
      if((current_feedin_ <= 0) && ((accumulated_power_ >= balancing_threshold) || (current_production_ <= 0))) {
        for(SolarPanelTileEntity panel: adjacent_panels) {
          if(panel.accumulated_power_ >= (accumulated_power_-balancing_threshold)) continue;
          panel.accumulated_power_ += balancing_threshold;
          accumulated_power_ -= balancing_threshold;
          if(accumulated_power_ < balancing_threshold) break;
        }
      }
      if(!world.canBlockSeeSky(pos)) {
        tick_timer_ = TICK_INTERVAL * 10;
        current_production_ = 0;
        if((accumulated_power_ > 0)) output_enabled_ = true;
        if(state.get((SolarPanelBlock.EXPOSITION))!=2) world.setBlockState(pos, state.with(SolarPanelBlock.EXPOSITION, 2));
        return;
      }
      if(accumulated_power_ <= 0) output_enabled_ = false;
      if(--recalc_timer_ > 0) return;
      recalc_timer_ = ACCUMULATION_INTERVAL + ((int)(Math.random()+.5));
      int theta = ((((int)(world.getCelestialAngleRadians(1f) * (180.0/Math.PI)))+90) % 360);
      int e = 2;
      if(theta > 340)      e = 2;
      else if(theta <  45) e = 0;
      else if(theta <  80) e = 1;
      else if(theta < 100) e = 2;
      else if(theta < 135) e = 3;
      else if(theta < 190) e = 4;
      BlockState nstate = state.with(SolarPanelBlock.EXPOSITION, e);
      if(nstate != state) world.setBlockState(pos, nstate, 1|2);
      final double eff = (1.0-((world.getRainStrength(1f)*0.6)+(world.getThunderStrength(1f)*0.3)));
      final double ll = ((double)(world.getLightManager().getLightEngine(LightType.SKY).getLightFor(getPos())))/15;
      final double rf = Math.sin((Math.PI/2) * Math.sqrt(((double)(((theta<0)||(theta>180))?(0):((theta>90)?(180-theta):(theta))))/90));
      current_production_ = (int)(Math.min(rf*rf*eff*ll, 1) * peak_power_per_tick_);
      accumulated_power_ = Math.min(accumulated_power_ + (current_production_*(TICK_INTERVAL*ACCUMULATION_INTERVAL)), max_power_storage_);
      if(accumulated_power_ >= (feeding_threshold)) output_enabled_ = true;
    }
  }
}
