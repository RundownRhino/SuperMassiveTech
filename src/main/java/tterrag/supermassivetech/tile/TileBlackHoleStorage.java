package tterrag.supermassivetech.tile;

import powercrystals.minefactoryreloaded.api.IDeepStorageUnit;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import tterrag.supermassivetech.SuperMassiveTech;
import tterrag.supermassivetech.util.Utils;

/**
 * @author Garrett Spicer-Davis
 * 
 */
public class TileBlackHoleStorage extends TileSMTInventory implements ISidedInventory, IFluidHandler, IDeepStorageUnit
{
    public final static long max = 1099511627776L;

    /* Item handling */
    public long storedAmount;
    private ItemStack storedItem;

    /* Fluid handling */
    private BlackHoleTank tank = new BlackHoleTank();

    public TileBlackHoleStorage()
    {
        super();
        inventory = new ItemStack[3];
    }

    public class SlotInput extends Slot
    {
        public SlotInput(IInventory par1iInventory, int par2, int par3, int par4)
        {
            super(par1iInventory, par2, par3, par4);
        }

        @Override
        public boolean isItemValid(ItemStack par1ItemStack)
        {
            return storedItem == null || Utils.stacksEqual(par1ItemStack, storedItem);
        }
    }

    public class SlotFluidContainer extends Slot
    {
        public SlotFluidContainer(IInventory inv, int x, int y, int z)
        {
            super(inv, x, y, z);
        }

        @Override
        public boolean isItemValid(ItemStack par1ItemStack)
        {
            if (FluidContainerRegistry.isContainer(par1ItemStack) && FluidContainerRegistry.isFilledContainer(par1ItemStack))
            {
                return tank.fluidStored == null || FluidContainerRegistry.containsFluid(par1ItemStack, tank.fluidStored);
            }

            return false;
        }
    }

    @Override
    public void updateEntity()
    {
        if (tank.amountStored > max)
            tank.amountStored = max;

        for (int i = 0; i < inventory.length; i++)
            if (inventory[i] != null && inventory[i].stackSize <= 0)
                inventory[i] = null;

        if (inventory[1] != null && storedAmount < max)
        {
            if (Utils.stacksEqual(inventory[1], storedItem))
            {
                int inputToStorage = inventory[1].stackSize;
                if ((storedAmount + inputToStorage) > max)
                {
                    inventory[1].stackSize = (int) (inputToStorage + storedAmount - max);
                    storedAmount = max;
                }
                else
                {
                    storedAmount += inputToStorage;
                    inventory[1] = null;
                }
            }
            else if (storedItem == null)
            {
                storedItem = inventory[1].copy();
                storedAmount = inventory[1].stackSize;
                inventory[1] = null;
            }
            else
            {
                SuperMassiveTech.logger.error(String.format("Input does not match storage, \"%s\" was not expected in this input! \"%s\" was expected!, X:%d, Y:%d, Z:%d",
                        StatCollector.translateToLocal(inventory[1].getUnlocalizedName() + ".name"),
                        StatCollector.translateToLocal(storedItem.getUnlocalizedName() + ".name"), xCoord, yCoord, zCoord));
                spitInputItem();
            }
        }

        if (storedAmount != 0)
        {
            int maxStack = storedItem.getMaxStackSize();
            if (inventory[2] == null)
            {
                inventory[2] = storedItem.copy();
                if (storedAmount > maxStack)
                {
                    inventory[2].stackSize = maxStack;
                    storedAmount -= maxStack;
                }
                else
                {
                    inventory[2].stackSize = (int) storedAmount;
                    storedAmount = 0;
                    storedItem = null;
                }
            }
            else if (inventory[2].stackSize < maxStack && Utils.stacksEqual(inventory[2], storedItem))
            {
                int outputFromStorage = maxStack - inventory[2].stackSize;
                if (outputFromStorage < storedAmount)
                {
                    inventory[2].stackSize = maxStack;
                    storedAmount -= outputFromStorage;
                }
                else
                {
                    inventory[2].stackSize += (int) storedAmount;
                    storedAmount = 0;
                    storedItem = null;
                }
            }
        }

        super.updateEntity();
    }

    @Override
    protected float getStrengthMultiplier()
    {
        return 1f + (((float) storedAmount + (float) tank.amountStored) / (max * 2f));
    }

    @Override
    public boolean isGravityWell()
    {
        return true;
    }

    @Override
    public boolean showParticles()
    {
        return true;
    }

    private void spitInputItem()
    {
        if (worldObj.isRemote)
            return;

        float f = (float) Math.random();
        float f1 = (float) Math.random();
        float f2 = (float) Math.random();

        EntityItem entityitem = new EntityItem(worldObj, this.xCoord + f, this.yCoord + f1, this.zCoord + f2, inventory[1]);

        entityitem.motionX = (int) Math.random() * 2;
        entityitem.motionY = (int) Math.random() * 2;
        entityitem.motionZ = (int) Math.random() * 2;
        worldObj.spawnEntityInWorld(entityitem);

        inventory[1] = null;
    }

    @Override
    public String getInventoryName()
    {
        return "tterrag.inv.storageBlock";
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int var1)
    {
        return new int[] { 1, 2 };
    }

    @Override
    public boolean canInsertItem(int var1, ItemStack var2, int var3)
    {
        return var1 == 1;
    }

    @Override
    public boolean canExtractItem(int var1, ItemStack var2, int var3)
    {
        if (var1 == 2)
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack)
    {
        if (i == 1)
        {
            return storedItem == null || Utils.stacksEqual(storedItem, itemstack);
        }

        if (i == 0)
        {
            return FluidContainerRegistry.isContainer(itemstack);
        }

        return false;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setLong("stored", storedAmount);
        NBTTagCompound itemstackNBT = new NBTTagCompound();

        if (storedItem != null)
        {
            storedItem.writeToNBT(itemstackNBT);
        }

        nbt.setTag("itemstack", itemstackNBT);

        nbt.setLong("fluidStored", tank.amountStored);
        NBTTagCompound fluidstackNBT = new NBTTagCompound();

        if (tank.fluidStored != null)
        {
            tank.fluidStored.writeToNBT(fluidstackNBT);

        }
        nbt.setTag("fluidstack", fluidstackNBT);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);

        storedAmount = nbt.getLong("stored");
        storedItem = ItemStack.loadItemStackFromNBT((NBTTagCompound) nbt.getTag("itemstack"));

        tank.amountStored = nbt.getLong("fluidStored");
        tank.fluidStored = FluidStack.loadFluidStackFromNBT((NBTTagCompound) nbt.getTag("fluidstack"));
    }

    /**
     * @return A copy of the stored item in this block, can be null
     */
    public ItemStack getStoredItem()
    {
        return storedItem == null ? null : storedItem.copy();
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
    {
        if (canFill(from, resource.getFluid()))
            return tank.fill(resource, doFill);

        return 0;
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
    {
        if (canDrain(from, resource.getFluid()))
            return tank.drain(resource.amount, doDrain);

        return null;
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
    {
        return tank.drain(maxDrain, doDrain);
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid)
    {
        return tank.amountStored < max && (tank.fluidStored == null || fluid == tank.fluidStored.getFluid());
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid)
    {
        return tank.amountStored != 0;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from)
    {
        return new FluidTankInfo[] { tank.getInfo() };
    }

    public BlackHoleTank getTank()
    {
        return this.tank;
    }

    public void setStoredItemOnPlace(ItemStack stackStored)
    {
        if (storedItem == null)
            storedItem = stackStored;
    }

    /* IDeepStorageUnit */

    @Override
    public ItemStack getStoredItemType()
    {
        ItemStack stack = storedItem.copy();
        stack.stackSize = (int) Math.min(Integer.MAX_VALUE, storedAmount);
        return stack;
    }

    @Override
    public void setStoredItemCount(int amount)
    {
        this.storedAmount = amount;
    }

    @Override
    public void setStoredItemType(ItemStack type, int amount)
    {
        this.storedItem = type;
        this.storedAmount = amount;
    }

    @Override
    public int getMaxStoredCount()
    {
        return Integer.MAX_VALUE;
    }
}
