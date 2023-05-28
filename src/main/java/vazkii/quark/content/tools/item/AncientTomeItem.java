package vazkii.quark.content.tools.item;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.CreativeModeTabEvent.BuildContents;
import net.minecraftforge.registries.ForgeRegistries;
import vazkii.arl.util.RegistryHelper;
import vazkii.quark.base.item.QuarkItem;
import vazkii.quark.base.module.QuarkModule;
import vazkii.quark.content.experimental.module.EnchantmentsBegoneModule;
import vazkii.quark.content.tools.module.AncientTomesModule;

public class AncientTomeItem extends QuarkItem {

	public AncientTomeItem(QuarkModule module) {
		super("ancient_tome", module,
				new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
		RegistryHelper.setCreativeTab(this, CreativeModeTabs.TOOLS_AND_UTILITIES);
	}

	@Override
	public boolean isEnchantable(@Nonnull ItemStack stack) {
		return false;
	}

	@Override
	public boolean isFoil(@Nonnull ItemStack stack) {
		return true;
	}

	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
		return false;
	}

	public static ItemStack getEnchantedItemStack(Enchantment ench) {
		ItemStack newStack = new ItemStack(AncientTomesModule.ancient_tome);
		EnchantedBookItem.addEnchantment(newStack, new EnchantmentInstance(ench, ench.getMaxLevel()));
		return newStack;
	}

	@Override
	public void addCreativeModeExtras(CreativeModeTab tab, BuildContents event) {
		ForgeRegistries.ENCHANTMENTS.forEach(ench -> {
			if (!EnchantmentsBegoneModule.shouldBegone(ench) && ench.getMaxLevel() != 1) {
				if (!AncientTomesModule.isInitialized() || AncientTomesModule.validEnchants.contains(ench)) {
					event.accept(getEnchantedItemStack(ench));
				}
			}
		});
	}

	public static Component getFullTooltipText(Enchantment ench) {
		return Component.translatable("quark.misc.ancient_tome_tooltip", Component.translatable(ench.getDescriptionId()), Component.translatable("enchantment.level." + (ench.getMaxLevel() + 1))).withStyle(ChatFormatting.GRAY);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(@Nonnull ItemStack stack, Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
		super.appendHoverText(stack, worldIn, tooltip, flagIn);

		Enchantment ench = AncientTomesModule.getTomeEnchantment(stack);
		if(ench != null)
			tooltip.add(getFullTooltipText(ench));
		else 
			tooltip.add(Component.translatable("quark.misc.ancient_tome_tooltip_any").withStyle(ChatFormatting.GRAY));

		if(AncientTomesModule.curseGear){
			tooltip.add(Component.translatable("quark.misc.ancient_tome_tooltip_curse").withStyle(ChatFormatting.RED));
		}
	}

}
