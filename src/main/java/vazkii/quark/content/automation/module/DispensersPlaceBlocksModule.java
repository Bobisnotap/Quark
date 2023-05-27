package vazkii.quark.content.automation.module;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraftforge.registries.ForgeRegistries;
import vazkii.quark.base.module.LoadModule;
import vazkii.quark.base.module.ModuleCategory;
import vazkii.quark.base.module.QuarkModule;
import vazkii.quark.base.module.config.Config;

@LoadModule(category = ModuleCategory.AUTOMATION)
public class DispensersPlaceBlocksModule extends QuarkModule {

	@Config public static List<String> blacklist = Lists.newArrayList("minecraft:water", "minecraft:lava", "minecraft:fire");

	@Config(description = "Set to false to refrain from registering any behaviors for blocks that have optional dispense behaviors already set.\n"
			+ "An optional behavior is one that will defer to the generic dispense item behavior if its condition fails.\n"
			+ "e.g. the Shulker Box behavior is optional, because it'll throw out the item if it fails, whereas TNT is not optional.\n"
			+ "If true, it'll attempt to use the previous behavior before trying to place the block in the world.\n"
			+ "Requires a game restart to re-apply.") 
	public static boolean wrapExistingBehaviors = true;
	
	@Override
	public void setup() {
		if(!enabled)
			return;

		BlockBehavior baseBehavior = new BlockBehavior();

		enqueue(() -> {
			Map<Item, DispenseItemBehavior> registry = DispenserBlock.DISPENSER_REGISTRY;
			
			for(Block b : ForgeRegistries.BLOCKS) {
				ResourceLocation res = ForgeRegistries.BLOCKS.getKey(b);
				if(!blacklist.contains(Objects.toString(res))) {
					Item item = b.asItem();
					if(item instanceof BlockItem) {
						DispenseItemBehavior original = registry.get(item);
						boolean exists = original != null;
						
						if(exists) {
							if(wrapExistingBehaviors && original instanceof OptionalDispenseItemBehavior opt)
								registry.put(item, new BlockBehavior(opt));
						} 
						else 
							registry.put(item, baseBehavior);
					}
					
				}
			}
		});
	}

	public static class BlockBehavior extends OptionalDispenseItemBehavior {

		private final OptionalDispenseItemBehavior wrapped;
		
		public BlockBehavior() {
			this(null);
		}
		
		public BlockBehavior(OptionalDispenseItemBehavior wrapped) {
			this.wrapped = wrapped;
		}
		
		@Nonnull
		@Override
		public ItemStack execute(BlockSource source, ItemStack stack) {
			if(wrapped != null) {
				ItemStack wrappedResult = wrapped.dispense(source, stack);
				if(wrapped.isSuccess()) {
					setSuccess(true);
					return wrappedResult;
				}
			}
			
			setSuccess(false);

			Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
			Direction against = direction;
			BlockPos pos = source.getPos().relative(direction);

			BlockItem item = (BlockItem) stack.getItem();
			Block block = item.getBlock();
			if(block instanceof StairBlock && direction.getAxis() != Axis.Y)
				direction = direction.getOpposite();
			else if(block instanceof SlabBlock)
				against = Direction.UP;

			setSuccess(item.place(new NotStupidDirectionalPlaceContext(source.getLevel(), pos, direction, stack, against)) == InteractionResult.SUCCESS);

			return stack;
		}

	}

	// DirectionPlaceContext results in infinite loops when using slabs
	private static class NotStupidDirectionalPlaceContext extends DirectionalPlaceContext {

		protected boolean replaceClicked;
		protected Direction direction;

		public NotStupidDirectionalPlaceContext(Level worldIn, BlockPos pos, Direction facing, ItemStack stack, Direction against) {
			super(worldIn, pos, facing, stack, against);
			replaceClicked = worldIn.getBlockState(getHitResult().getBlockPos()).canBeReplaced(this); // getHitResult = getRayTraceResult
			this.direction = facing;
		}

		@Override
		public boolean canPlace() {
			return replaceClicked;
		}

		@Nonnull
		@Override
		public Direction getNearestLookingDirection() {
			return direction.getOpposite();
		}

	}

}
