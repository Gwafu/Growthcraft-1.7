package growthcraft.cellar.tileentity;

import growthcraft.api.cellar.CellarRegistry;
import growthcraft.cellar.GrowthCraftCellar;
import growthcraft.cellar.container.ContainerBrewKettle;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityBrewKettle extends TileEntity implements ISidedInventory, IFluidHandler
{
	// Constants
	private ItemStack[] invSlots   = new ItemStack[2];
	private int[]       maxCap     = new int[] {1000, 1000};
	private CellarTank[]   tank    = new CellarTank[] {new CellarTank(this.maxCap[0], this), new CellarTank(this.maxCap[1], this)};

	// Other Vars.
	private String  name;
	protected float residue  = 0.0F;
	protected int   time     = 0;
	protected boolean update = false;

	/************
	 * UPDATE
	 ************/	
	public void updateEntity()
	{
		super.updateEntity();
		if (update)
		{
			update = false;
			this.markDirty();
		}

		if (!this.worldObj.isRemote)
		{
			if (this.canBrew())
			{
				++this.time;

				if (this.time == CellarRegistry.instance().getBrewingTime(getFluidStack(0), this.invSlots[0]))
				{
					this.time = 0;
					this.brewItem();
				}
			}
			else
			{
				this.time = 0;
			}

			update = true;
		}

		//debugMsg();
	}

	private void sendUpdate()
	{
		//this.markDirty();
		this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
		//GrowthCraftCellar.packetPipeline.sendToAllAround(new PacketBrewKettleFluid(this.xCoord, this.yCoord, this.zCoord, this.tank[1].getFluid().fluidID, this.tank[1].getFluid().amount), new TargetPoint(this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord, 256.0D));
	}

	private void debugMsg()
	{
		if (this.worldObj.isRemote)
		{
			System.out.println("CLIENT: " + getFluidAmount(0) + " " + getFluidAmount(1));
		}
		if (!this.worldObj.isRemote)
		{
			System.out.println("SERVER: " + getFluidAmount(0) + " " + getFluidAmount(1));
		}
	}

	private boolean canBrew()
	{
		if (!hasFire()) return false;
		if (this.invSlots[0] == null) return false;
		if (this.isFluidTankFull(1)) return false;
		if (!CellarRegistry.instance().isBrewingRecipe(getFluidStack(0), this.invSlots[0])) return false;

		if (this.isFluidTankEmpty(1)) return true;

		FluidStack stack = CellarRegistry.instance().getBrewingFluidStack(getFluidStack(0), this.invSlots[0]);
		return stack.isFluidEqual(getFluidStack(1));
	}

	public boolean hasFire()
	{
		return CellarRegistry.instance().isBlockHeatSource(this.worldObj.getBlock(this.xCoord, this.yCoord - 1, this.zCoord)); 
	}

	public void brewItem()
	{
		// set spent grain
		float f = CellarRegistry.instance().getBrewingResidue(getFluidStack(0), this.invSlots[0]);
		this.residue = this.residue + f;
		if (this.residue >= 1.0F)
		{
			this.residue = this.residue - 1.0F;

			if (this.grainBool())
			{
				if (this.invSlots[1] == null)
				{
					this.invSlots[1] = GrowthCraftCellar.residue.copy();
				}
				else if (this.invSlots[1].isItemEqual(GrowthCraftCellar.residue))
				{
					this.invSlots[1].stackSize += GrowthCraftCellar.residue.stackSize;
				}
			}
		}

		FluidStack fluidstack = CellarRegistry.instance().getBrewingFluidStack(getFluidStack(0), this.invSlots[0]);
		int amount  = CellarRegistry.instance().getBrewingAmount(getFluidStack(0), this.invSlots[0]);
		fluidstack.amount = amount;
		this.tank[1].fill(fluidstack, true);
		this.tank[0].drain(amount, true);

		// subtract from the itemstack
		--this.invSlots[0].stackSize;

		if (this.invSlots[0].stackSize <= 0)
		{
			this.invSlots[0] = null;
		}

		sendUpdate();
	}

	private boolean grainBool()
	{
		if (this.invSlots[1] == null) return true;
		if (!this.invSlots[1].isItemEqual(GrowthCraftCellar.residue)) return false;
		int result = invSlots[1].stackSize + GrowthCraftCellar.residue.stackSize;
		return (result <= getInventoryStackLimit() && result <= GrowthCraftCellar.residue.getMaxStackSize());
	}

	@SideOnly(Side.CLIENT)
	public int getBrewProgressScaled(int par1)
	{
		if (this.canBrew())
		{
			return this.time * par1 / CellarRegistry.instance().getBrewingTime(getFluidStack(0), this.invSlots[0]);
		}

		return 0;
	}

	/************
	 * INVENTORY
	 ************/	
	@Override
	public ItemStack getStackInSlot(int index)
	{
		return this.invSlots[index];
	}

	@Override
	public ItemStack decrStackSize(int index, int par2)
	{
		if (this.invSlots[index] != null)
		{
			ItemStack itemstack;

			if (this.invSlots[index].stackSize <= par2)
			{
				itemstack = this.invSlots[index];
				this.invSlots[index] = null;
				return itemstack;
			}
			else
			{
				itemstack = this.invSlots[index].splitStack(par2);

				if (this.invSlots[index].stackSize == 0)
				{
					this.invSlots[index] = null;
				}

				return itemstack;
			}
		}
		else
		{
			return null;
		}
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int index)
	{
		if (this.invSlots[index] != null)
		{
			ItemStack itemstack = this.invSlots[index];
			this.invSlots[index] = null;
			return itemstack;
		}
		else
		{
			return null;
		}
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack itemstack)
	{
		this.invSlots[index] = itemstack;

		if (itemstack != null && itemstack.stackSize > this.getInventoryStackLimit())
		{
			itemstack.stackSize = this.getInventoryStackLimit();
		}
	}

	@Override
	public int getInventoryStackLimit()
	{
		return 64;
	}

	@Override
	public int getSizeInventory()
	{
		return this.invSlots.length;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player)
	{
		return this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord) != this ? false : player.getDistanceSq((double)this.xCoord + 0.5D, (double)this.yCoord + 0.5D, (double)this.zCoord + 0.5D) <= 64.0D;
	}

	@Override
	public void openInventory(){}

	@Override
	public void closeInventory(){}

	@Override
	public boolean isItemValidForSlot(int index, ItemStack itemstack)
	{
		return index == 1 ? itemstack.getItem() == GrowthCraftCellar.residue.getItem() && itemstack.getItemDamage() == GrowthCraftCellar.residue.getItemDamage() : true;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side)
	{
		// 0 = raw
		// 1 = residue
		return side == 0 ? new int[] {0, 1} : new int[] {0};
	}

	@Override
	public boolean canInsertItem(int index, ItemStack stack, int side)
	{
		return this.isItemValidForSlot(index, stack);
	}

	@Override
	public boolean canExtractItem(int index, ItemStack stack, int side)
	{
		return side != 0 || index == 1;
	}

	/************
	 * NBT
	 ************/
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		//INVENTORY
		NBTTagList tags = nbt.getTagList("items", 10);
		this.invSlots = new ItemStack[this.getSizeInventory()];
		for (int i = 0; i < tags.tagCount(); ++i)
		{
			NBTTagCompound nbttagcompound1 = tags.getCompoundTagAt(i);
			byte b0 = nbttagcompound1.getByte("Slot");

			if (b0 >= 0 && b0 < this.invSlots.length)
			{
				this.invSlots[b0] = ItemStack.loadItemStackFromNBT(nbttagcompound1);
			}
		}

		//TANK
		readTankFromNBT(nbt);

		//NAME
		if (nbt.hasKey("name"))
		{
			this.name = nbt.getString("name");
		}

		this.time = nbt.getShort("time");
		this.residue = nbt.getFloat("grain");
	}

	protected void readTankFromNBT(NBTTagCompound nbt)
	{
		for (int i = 0; i < this.tank.length; i++)
		{
			this.tank[i] = new CellarTank(this.maxCap[i], this);
			if (nbt.hasKey("Tank" + i))
			{
				this.tank[i].readFromNBT(nbt.getCompoundTag("Tank" + i));
			}
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);

		//INVENTORY
		NBTTagList nbttaglist = new NBTTagList();
		for (int i = 0; i < this.invSlots.length; ++i)
		{
			if (this.invSlots[i] != null)
			{
				NBTTagCompound nbttagcompound1 = new NBTTagCompound();
				nbttagcompound1.setByte("Slot", (byte)i);
				this.invSlots[i].writeToNBT(nbttagcompound1);
				nbttaglist.appendTag(nbttagcompound1);
			}
		}
		nbt.setTag("items", nbttaglist);

		//TANKS
		writeTankToNBT(nbt);

		//NAME
		if (this.hasCustomInventoryName())
		{
			nbt.setString("name", this.name);
		}

		nbt.setShort("time", (short)this.time);
		nbt.setFloat("grain", this.residue);
	}

	protected void writeTankToNBT(NBTTagCompound nbt)
	{
		for (int i = 0; i < this.tank.length; i++)
		{
			NBTTagCompound tag = new NBTTagCompound();
			this.tank[i].writeToNBT(tag);
			nbt.setTag("Tank" + i, tag);
		}
	}

	/************
	 * NAMES
	 ************/
	@Override
	public String getInventoryName()
	{
		return this.hasCustomInventoryName() ? this.name : "container.grc.brewKettle";
	}

	@Override
	public boolean hasCustomInventoryName()
	{
		return this.name != null && this.name.length() > 0;
	}

	public void setGuiDisplayName(String string)
	{
		this.name = string;
	}

	/************
	 * PACKETS
	 ************/	
	public void getGUINetworkData(int id, int v) 
	{
		switch (id) 
		{
		case 0:
			time = v;
			break;
		case 1:
			if (FluidRegistry.getFluid(v) == null) {
				return;
			}
			if (tank[0].getFluid() == null) 
			{
				tank[0].setFluid(new FluidStack(v, 0));
			} else 
			{
				tank[0].setFluid(new FluidStack(v, tank[0].getFluid().amount));
			}
			break;
		case 2:
			if (tank[0].getFluid() == null) 
			{
				tank[0].setFluid(new FluidStack(FluidRegistry.WATER, v));
			} else 
			{
				tank[0].getFluid().amount = v;
			}
			break;
		case 3:
			if (FluidRegistry.getFluid(v) == null) {
				return;
			}
			if (tank[1].getFluid() == null) 
			{
				tank[1].setFluid(new FluidStack(v, 0));
			} else 
			{
				tank[1].setFluid(new FluidStack(v, tank[1].getFluid().amount));
			}
			break;
		case 4:
			if (tank[1].getFluid() == null) 
			{
				tank[1].setFluid(new FluidStack(FluidRegistry.WATER, v));
			} else 
			{
				tank[1].getFluid().amount = v;
			}
			break;
		}
	}

	public void sendGUINetworkData(ContainerBrewKettle container, ICrafting iCrafting) 
	{
		iCrafting.sendProgressBarUpdate(container, 0, time);
		iCrafting.sendProgressBarUpdate(container, 1, tank[0].getFluid() != null ? tank[0].getFluid().getFluidID() : 0);
		iCrafting.sendProgressBarUpdate(container, 2, tank[0].getFluid() != null ? tank[0].getFluid().amount : 0);
		iCrafting.sendProgressBarUpdate(container, 3, tank[1].getFluid() != null ? tank[1].getFluid().getFluidID() : 0);
		iCrafting.sendProgressBarUpdate(container, 4, tank[1].getFluid() != null ? tank[1].getFluid().amount : 0);
	}

	@Override
	public Packet getDescriptionPacket() 
	{
		NBTTagCompound nbt = new NBTTagCompound();
		writeTankToNBT(nbt);
		return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) 
	{
		readTankFromNBT(packet.func_148857_g());
		this.worldObj.func_147479_m(this.xCoord, this.yCoord, this.zCoord);
	}

	/************
	 * FLUID
	 ************/	
	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill) 
	{
		int f = this.tank[0].fill(resource, doFill);

		if (f > 0)
		{
			sendUpdate();
		}

		return f;
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) 
	{
		if (resource == null || !resource.isFluidEqual(this.tank[1].getFluid())) 
		{
			return null;
		}

		return drain(from, resource.amount, doDrain);
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) 
	{
		FluidStack d = this.tank[1].drain(maxDrain, doDrain);

		if (d != null)
		{
			sendUpdate();
		}

		return d;
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid) 
	{
		return true;
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid) 
	{
		return true;
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from) 
	{
		return new FluidTankInfo[] { this.tank[0].getInfo(), this.tank[1].getInfo() };
	}

	public int getFluidAmountScaled(int par1, int slot)
	{
		return this.getFluidAmount(slot) * par1 / this.maxCap[slot];
	}

	public boolean isFluidTankFilled(int slot)
	{
		return this.getFluidAmount(slot) > 0;
	}

	public boolean isFluidTankFull(int slot)
	{
		return this.getFluidAmount(slot) == this.tank[slot].getCapacity();
	}

	public boolean isFluidTankEmpty(int slot)
	{
		return this.getFluidAmount(slot) == 0;
	}

	public int getFluidAmount(int slot)
	{
		return this.tank[slot].getFluidAmount();
	}

	public CellarTank getFluidTank(int slot)
	{
		return this.tank[slot];
	}

	public FluidStack getFluidStack(int slot)
	{
		return this.tank[slot].getFluid();
	}

	public Fluid getFluid(int slot)
	{
		return getFluidStack(slot).getFluid();
	}

	public void clearTank(int slot)
	{
		this.tank[slot].setFluid(null);

		sendUpdate();
	}

	public void switchTanks()
	{
		FluidStack f0 = null;
		FluidStack f1 = null;
		if (this.getFluidStack(0) != null)
		{
			f0 = this.getFluidStack(0).copy();
		}
		if (this.getFluidStack(1) != null)
		{
			f1 = this.getFluidStack(1).copy();
		}
		this.clearTank(0);
		this.clearTank(1);
		this.getFluidTank(0).fill(f1, true);
		this.getFluidTank(1).fill(f0, true);

		sendUpdate();
	}
}
