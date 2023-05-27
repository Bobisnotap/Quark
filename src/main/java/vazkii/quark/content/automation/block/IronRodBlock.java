package vazkii.quark.content.automation.block;

import java.util.function.BooleanSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Material;
import vazkii.arl.util.RegistryHelper;
import vazkii.quark.api.ICollateralMover;
import vazkii.quark.base.block.IQuarkBlock;
import vazkii.quark.base.handler.RenderLayerHandler;
import vazkii.quark.base.handler.RenderLayerHandler.RenderTypeSkeleton;
import vazkii.quark.base.module.QuarkModule;

public class IronRodBlock extends EndRodBlock implements ICollateralMover, IQuarkBlock {

	private final QuarkModule module;
	private BooleanSupplier enabledSupplier = () -> true;

	public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");

	public IronRodBlock(QuarkModule module) {
		super(Block.Properties.of(Material.METAL, DyeColor.GRAY)
				.strength(5F, 10F)
				.sound(SoundType.METAL)
				.noOcclusion());

		RegistryHelper.registerBlock(this, "iron_rod");
		RegistryHelper.setCreativeTab(this, CreativeModeTabs.REDSTONE_BLOCKS);

		RenderLayerHandler.setRenderType(this, RenderTypeSkeleton.CUTOUT);

		this.module = module;
	}

	@Nullable
	@Override
	public QuarkModule getModule() {
		return module;
	}

	@Override
	public IronRodBlock setCondition(BooleanSupplier enabledSupplier) {
		this.enabledSupplier = enabledSupplier;
		return this;
	}

	@Override
	public boolean doesConditionApply() {
		return enabledSupplier.getAsBoolean();
	}

	@Override
	protected void createBlockStateDefinition(@Nonnull Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(CONNECTED);
	}

	@Override
	public boolean isCollateralMover(Level world, BlockPos source, Direction moveDirection, BlockPos pos) {
		return moveDirection == world.getBlockState(pos).getValue(FACING);
	}

	@Override
	public MoveResult getCollateralMovement(Level world, BlockPos source, Direction moveDirection, Direction side, BlockPos pos) {
		return side == moveDirection ? MoveResult.BREAK : MoveResult.SKIP;
	}

	@Override
	public void animateTick(@Nonnull BlockState stateIn, @Nonnull Level worldIn, @Nonnull BlockPos pos, @Nonnull RandomSource rand) {
		// NO-OP
	}


}
