package growthcraft.bamboo;

import growthcraft.core.GrowthCraftCore;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeavesBase;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.ColorizerFoliage;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockBambooLeaves extends BlockLeavesBase implements IShearable
{
	@SideOnly(Side.CLIENT)
	public static IIcon[] tex;
	//public static Boolean graphicFlag;
	int[] adjacentTreeBlocks;

	public BlockBambooLeaves()
	{
		super(Material.leaves, false);
		this.setLightOpacity(1);
		this.setStepSound(soundTypeGrass);
		this.setHardness(0.2F);
		this.setTickRandomly(true);
		this.setCreativeTab(GrowthCraftCore.tab);
		this.setBlockName("grc.bambooLeaves");
	}

	/************
	 * TICK
	 ************/
	@Override
	public void updateTick(World world, int x, int y, int z, Random rand)
	{
		if (!world.isRemote)
		{
			int meta = world.getBlockMetadata(x, y, z);

			if ((meta & 8) != 0 && (meta & 4) == 0)
			{
				byte b0 = 4;
				int i1 = b0 + 1;
				byte b1 = 32;
				int j1 = b1 * b1;
				int k1 = b1 / 2;

				if (this.adjacentTreeBlocks == null)
				{
					this.adjacentTreeBlocks = new int[b1 * b1 * b1];
				}

				int l1;

				if (world.checkChunksExist(x - i1, y - i1, z - i1, x + i1, y + i1, z + i1))
				{
					int i2;
					int j2;
					int k2;

					for (l1 = -b0; l1 <= b0; ++l1)
					{
						for (i2 = -b0; i2 <= b0; ++i2)
						{
							for (j2 = -b0; j2 <= b0; ++j2)
							{
								Block block = world.getBlock(x + l1, y + i2, z + j2);

								if (block != null && block.canSustainLeaves(world, x + l1, y + i2, z + j2))
								{
									this.adjacentTreeBlocks[(l1 + k1) * j1 + (i2 + k1) * b1 + j2 + k1] = 0;
								}
								else if (block != null && block.isLeaves(world, x + l1, y + i2, z + j2))
								{
									this.adjacentTreeBlocks[(l1 + k1) * j1 + (i2 + k1) * b1 + j2 + k1] = -2;
								}
								else
								{
									this.adjacentTreeBlocks[(l1 + k1) * j1 + (i2 + k1) * b1 + j2 + k1] = -1;
								}
							}
						}
					}

					for (l1 = 1; l1 <= 4; ++l1)
					{
						for (i2 = -b0; i2 <= b0; ++i2)
						{
							for (j2 = -b0; j2 <= b0; ++j2)
							{
								for (k2 = -b0; k2 <= b0; ++k2)
								{
									if (this.adjacentTreeBlocks[(i2 + k1) * j1 + (j2 + k1) * b1 + k2 + k1] == l1 - 1)
									{
										if (this.adjacentTreeBlocks[(i2 + k1 - 1) * j1 + (j2 + k1) * b1 + k2 + k1] == -2)
										{
											this.adjacentTreeBlocks[(i2 + k1 - 1) * j1 + (j2 + k1) * b1 + k2 + k1] = l1;
										}

										if (this.adjacentTreeBlocks[(i2 + k1 + 1) * j1 + (j2 + k1) * b1 + k2 + k1] == -2)
										{
											this.adjacentTreeBlocks[(i2 + k1 + 1) * j1 + (j2 + k1) * b1 + k2 + k1] = l1;
										}

										if (this.adjacentTreeBlocks[(i2 + k1) * j1 + (j2 + k1 - 1) * b1 + k2 + k1] == -2)
										{
											this.adjacentTreeBlocks[(i2 + k1) * j1 + (j2 + k1 - 1) * b1 + k2 + k1] = l1;
										}

										if (this.adjacentTreeBlocks[(i2 + k1) * j1 + (j2 + k1 + 1) * b1 + k2 + k1] == -2)
										{
											this.adjacentTreeBlocks[(i2 + k1) * j1 + (j2 + k1 + 1) * b1 + k2 + k1] = l1;
										}

										if (this.adjacentTreeBlocks[(i2 + k1) * j1 + (j2 + k1) * b1 + (k2 + k1 - 1)] == -2)
										{
											this.adjacentTreeBlocks[(i2 + k1) * j1 + (j2 + k1) * b1 + (k2 + k1 - 1)] = l1;
										}

										if (this.adjacentTreeBlocks[(i2 + k1) * j1 + (j2 + k1) * b1 + k2 + k1 + 1] == -2)
										{
											this.adjacentTreeBlocks[(i2 + k1) * j1 + (j2 + k1) * b1 + k2 + k1 + 1] = l1;
										}
									}
								}
							}
						}
					}
				}

				l1 = this.adjacentTreeBlocks[k1 * j1 + k1 * b1 + k1];

				if (l1 >= 0)
				{
					world.setBlockMetadataWithNotify(x, y, z, meta & -9, 4);
				}
				else
				{
					this.removeLeaves(world, x, y, z);
				}
			}
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void randomDisplayTick(World world, int x, int y, int z, Random random)
	{
		super.randomDisplayTick(world, x, y, z, random);
		if (world.canLightningStrikeAt(x, y + 1, z) && !World.doesBlockHaveSolidTopSurface(world, x, y - 1, z) && random.nextInt(15) == 1)
		{
			double d0 = (double)((float)x + random.nextFloat());
			double d1 = (double)y - 0.05D;
			double d2 = (double)((float)z + random.nextFloat());
			world.spawnParticle("dripWater", d0, d1, d2, 0.0D, 0.0D, 0.0D);
		}
	}

	/************
	 * TRIGGERS
	 ************/
	@Override
	public void breakBlock(World world, int x, int y, int z, Block par5, int par6)
	{
		byte b0 = 1;
		int j1 = b0 + 1;

		if (world.checkChunksExist(x - j1, y - j1, z - j1, x + j1, y + j1, z + j1))
		{
			for (int k1 = -b0; k1 <= b0; ++k1)
			{
				for (int l1 = -b0; l1 <= b0; ++l1)
				{
					for (int i2 = -b0; i2 <= b0; ++i2)
					{
						Block j2 = world.getBlock(x + k1, y + l1, z + i2);

						if (j2 != null)
						{
							j2.beginLeavesDecay(world, x + k1, y + l1, z + i2);
						}
					}
				}
			}
		}
	}

	private void removeLeaves(World world, int x, int y, int z)
	{
		this.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
		world.setBlockToAir(x, y, z);
	}

	/************
	 * STUFF
	 ************/
	@Override
	public void beginLeavesDecay(World world, int x, int y, int z)
	{
		world.setBlockMetadataWithNotify(x, y, z, world.getBlockMetadata(x, y, z) | 8, 4);
	}

	@Override
	public boolean isLeaves(IBlockAccess world, int x, int y, int z)
	{
		return true;
	}

	@Override
	protected ItemStack createStackedBlock(int meta)
	{
		return new ItemStack(this, 1, meta & 3);
	}


	/************
	 * DROPS
	 ************/
	@Override
	public Item getItemDropped(int par1, Random par2Random, int par3)
	{
		return null;
	}

	@Override
	public int quantityDropped(Random par1Random)
	{
		return 0;
	}

	/************
	 * TEXTURES
	 ************/
	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg)
	{
		tex = new IIcon[2];

		tex[0] = reg.registerIcon("grcbamboo:leaves");
		tex[1] = reg.registerIcon("grcbamboo:leaves_opaque");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta)
	{
		//this.graphicFlag = ((BlockLeaves)Block.blocksList[Block.leaves.blockID]).graphicsLevel;
		//return this.graphicFlag ? this.tex[0] : this.tex[1];
		return this.tex[this.isOpaqueCube() ? 1 : 0];
	}


	/************
	 * RENDERS
	 ************/
	@Override
	public boolean isOpaqueCube()
	{
		//this.graphicFlag = ((BlockLeaves)Block.blocksList[Block.leaves.blockID]).graphicsLevel;
		//return !this.graphicFlag;
		return Blocks.leaves.isOpaqueCube();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side)
	{
		//int id = world.getBlockId(x, y, z);
		//this.graphicFlag = ((BlockLeaves)Block.blocksList[Block.leaves.blockID]).graphicsLevel;
		//return !this.graphicFlag && id == this.blockID ? false : super.shouldSideBeRendered(world, x, y, z, side);
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getBlockColor()
	{
		double d0 = 0.5D;
		double d1 = 1.0D;
		return ColorizerFoliage.getFoliageColor(d0, d1);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getRenderColor(int meta)
	{
		return ColorizerFoliage.getFoliageColorBasic();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int colorMultiplier(IBlockAccess world, int x, int y, int z)
	{
		int meta = world.getBlockMetadata(x, y, z);
		int r = 0;
		int g = 0;
		int b = 0;

		for (int x1 = -1; x1 <= 1; ++x1)
		{
			for (int z1 = -1; z1 <= 1; ++z1)
			{
				int j2 = world.getBiomeGenForCoords(x + z1, z + x1).getBiomeFoliageColor(x + z1, y, z + x1);
				r += (j2 & 16711680) >> 16;
			g += (j2 & 65280) >> 8;
					b += j2 & 255;
			}
		}

		return (r / 9 & 255) << 16 | (g / 9 & 255) << 8 | b / 9 & 255;
	}

	/************
	 * SHEARS
	 ************/	
	@Override
	public boolean isShearable(ItemStack item, IBlockAccess world, int x, int y, int z)
	{
		return true;
	}

	@Override
	public ArrayList<ItemStack> onSheared(ItemStack item, IBlockAccess world, int x, int y, int z, int fortune)
	{
		ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
		ret.add(new ItemStack(Blocks.leaves, 1, world.getBlockMetadata(x, y, z) & 3));
		return ret;
	}
}
