/*
 * @file Fluidics.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * General fluid handling functionality.
 */
package wile.engineersdecor.libmc.detail;


import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;

public class Fluidics
{

  public static class SingleTankFluidHandler implements IFluidHandler
  {
    private final IFluidTank tank_;
    public SingleTankFluidHandler(IFluidTank tank) { tank_ = tank; }
    @Override public int getTanks() { return 1; }
    @Override public FluidStack getFluidInTank(int tank) { return tank_.getFluid(); }
    @Override public int getTankCapacity(int tank) { return tank_.getCapacity(); }
    @Override public boolean isFluidValid(int tank, @Nonnull FluidStack stack) { return tank_.isFluidValid(stack); }
    @Override public int fill(FluidStack resource, FluidAction action)  { return 0; }
    @Override public FluidStack drain(FluidStack resource, FluidAction action) { return tank_.drain(resource, action); }
    @Override public FluidStack drain(int maxDrain, FluidAction action) { return tank_.drain(maxDrain, action); }
  }


}
