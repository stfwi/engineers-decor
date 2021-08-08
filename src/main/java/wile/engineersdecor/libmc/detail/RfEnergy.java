/*
 * @file RfEnergy.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * General RF/FE energy handling functionality.
 */
package wile.engineersdecor.libmc.detail;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class RfEnergy
{
  public static int feed(Level world, BlockPos pos, @Nullable Direction side, int rf_energy)
  {
    final BlockEntity te = world.getBlockEntity(pos);
    if(te == null) return 0;
    final IEnergyStorage es = te.getCapability(CapabilityEnergy.ENERGY, side).orElse(null);
    if(es == null) return 0;
    return es.receiveEnergy(rf_energy, false);
  }

  public static class Battery implements IEnergyStorage
  {
    protected int capacity_;
    protected int charge_rate_;
    protected int discharge_rate_;
    protected int energy_;

    public Battery(int capacity)
    { this(capacity, capacity); }

    public Battery(int capacity, int transfer_rate)
    { this(capacity, transfer_rate, transfer_rate, 0); }

    public Battery(int capacity, int charge_rate, int discharge_rate)
    { this(capacity, charge_rate, discharge_rate, 0); }

    public Battery(int capacity, int charge_rate, int discharge_rate, int energy)
    {
      capacity_ = Math.max(capacity, 1);
      charge_rate_ = Mth.clamp(charge_rate, 0, capacity_);
      discharge_rate_ = Mth.clamp(discharge_rate, 0, capacity_);
      energy_ = Mth.clamp(energy, 0, capacity_);
    }

    // ---------------------------------------------------------------------------------------------------

    public Battery setMaxEnergyStored(int capacity)
    { capacity_ = Math.max(capacity, 1); return this; }

    public Battery setEnergyStored(int energy)
    { energy_ = Mth.clamp(energy, 0, capacity_); return this; }

    public Battery setChargeRate(int in_rate)
    { charge_rate_ = Mth.clamp(in_rate, 0, capacity_); return this; }

    public Battery setDischargeRate(int out_rate)
    { discharge_rate_ = Mth.clamp(out_rate, 0, capacity_); return this; }

    public int getChargeRate()
    { return charge_rate_; }

    public int getDischargeRate()
    { return discharge_rate_; }

    public boolean isEmpty()
    { return energy_ <= 0; }

    public boolean isFull()
    { return energy_ >= capacity_; }

    public int getSOC()
    { return (int)Mth.clamp((100.0 * energy_ / capacity_ + .5), 0, 100); }

    public int getComparatorOutput()
    { return (int)Mth.clamp((15.0 * energy_ / capacity_ + .2), 0, 15); }

    public boolean draw(int energy)
    {
      if(energy_ < energy) return false;
      energy_ -= energy;
      return true;
    }

    public boolean feed(int energy)
    {
      energy_ = Math.min(energy_+energy, capacity_);
      return energy_ >= capacity_;
    }

    public Battery clear()
    { energy_ = 0; return this; }

    public Battery load(CompoundTag nbt, String key)
    { setEnergyStored(nbt.getInt(key)); return this; }

    public Battery load(CompoundTag nbt)
    { return load(nbt, "Energy"); }

    public CompoundTag save(CompoundTag nbt, String key)
    { nbt.putInt(key, energy_); return nbt; }

    public CompoundTag save(CompoundTag nbt)
    { return save(nbt, "Energy"); }

    public LazyOptional<IEnergyStorage> createEnergyHandler()
    { return LazyOptional.of(() -> this); }

    // IEnergyStorage ------------------------------------------------------------------------------------

    @Override
    public int receiveEnergy(int feed_energy, boolean simulate)
    {
      if(!canReceive()) return 0;
      int e = Math.min(Math.min(charge_rate_, feed_energy), capacity_-energy_);
      if(!simulate) energy_ += e;
      return e;
    }

    @Override
    public int extractEnergy(int draw_energy, boolean simulate)
    {
      if(!canExtract()) return 0;
      int e = Math.min(Math.min(discharge_rate_, draw_energy), energy_);
      if(!simulate) energy_ -= e;
      return e;
    }

    @Override
    public int getEnergyStored()
    { return energy_; }

    @Override
    public int getMaxEnergyStored()
    { return capacity_; }

    @Override
    public boolean canExtract()
    { return discharge_rate_ > 0; }

    @Override
    public boolean canReceive()
    { return charge_rate_ > 0; }

  }
}
