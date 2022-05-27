package vazkii.quark.base.block;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.material.Material;
import vazkii.arl.util.RegistryHelper;
import vazkii.quark.base.datagen.QuarkBlockStateProvider;
import vazkii.quark.base.datagen.QuarkBlockTagsProvider;
import vazkii.quark.base.datagen.QuarkItemTagsProvider;
import vazkii.quark.base.module.QuarkModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;

/**
 * @author WireSegal
 * Created at 9:14 PM on 10/8/19.
 */
public abstract class QuarkButtonBlock extends ButtonBlock implements IQuarkBlock {

	private final QuarkModule module;
	private BooleanSupplier enabledSupplier = () -> true;

	public QuarkButtonBlock(String regname, QuarkModule module, CreativeModeTab creativeTab, Properties properties) {
		super(false, properties);
		this.module = module;

		RegistryHelper.registerBlock(this, regname);
		if(creativeTab != null)
			RegistryHelper.setCreativeTab(this, creativeTab);
	}

	@Nonnull
	@Override
	protected abstract SoundEvent getSound(boolean powered);

	@Override
	public abstract int getPressDuration();

	@Override
	public void fillItemCategory(@Nonnull CreativeModeTab group, @Nonnull NonNullList<ItemStack> items) {
		if(isEnabled() || group == CreativeModeTab.TAB_SEARCH)
			super.fillItemCategory(group, items);
	}

	@Override
	public QuarkButtonBlock setCondition(BooleanSupplier enabledSupplier) {
		this.enabledSupplier = enabledSupplier;
		return this;
	}

	@Override
	public boolean doesConditionApply() {
		return enabledSupplier.getAsBoolean();
	}

	@Nullable
	@Override
	public QuarkModule getModule() {
		return module;
	}

	protected ResourceLocation gennedTexture() {
		return getRegistryName();
	}

	@Override
	public void dataGen(QuarkBlockStateProvider states) {
		states.buttonBlock(this, gennedTexture());
		states.simpleBlockItem(this);
	}

	@Override
	public void dataGen(QuarkItemTagsProvider itemTags) {
		itemTags.copyInto(BlockTags.BUTTONS, ItemTags.BUTTONS);
		if (material == Material.WOOD)
			itemTags.copyInto(BlockTags.WOODEN_BUTTONS, ItemTags.WOODEN_BUTTONS);
	}

	@Override
	public void dataGen(QuarkBlockTagsProvider blockTags) {
		blockTags.tag(BlockTags.BUTTONS).add(this);
		if (material == Material.WOOD)
			blockTags.tag(BlockTags.WOODEN_BUTTONS).add(this);
	}
}
