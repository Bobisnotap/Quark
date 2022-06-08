package vazkii.quark.base.handler;

import com.google.common.collect.ImmutableSet;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.PressurePlateBlock.Sensitivity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import vazkii.arl.util.RegistryHelper;
import vazkii.quark.base.Quark;
import vazkii.quark.base.block.*;
import vazkii.quark.base.client.render.QuarkBoatRenderer;
import vazkii.quark.base.item.QuarkSignItem;
import vazkii.quark.base.item.boat.QuarkBoat;
import vazkii.quark.base.item.boat.QuarkBoat.QuarkBoatType;
import vazkii.quark.base.item.boat.QuarkBoatDispenseItemBehavior;
import vazkii.quark.base.item.boat.QuarkBoatItem;
import vazkii.quark.base.module.ModuleLoader;
import vazkii.quark.base.module.QuarkModule;
import vazkii.quark.content.building.block.VariantBookshelfBlock;
import vazkii.quark.content.building.block.VariantLadderBlock;
import vazkii.quark.content.building.block.WoodPostBlock;
import vazkii.quark.content.building.module.*;

import java.util.*;

public class WoodSetHandler {

	public static EntityType<QuarkBoat> quarkBoatEntityType = null;

	private static final List<WoodSet> woodSets = new ArrayList<>();

	public static void register() {
		quarkBoatEntityType = EntityType.Builder.<QuarkBoat>of(QuarkBoat::new, MobCategory.MISC)
				.sized(1.375F, 0.5625F)
				.clientTrackingRange(10)
				.setCustomClientFactory((spawnEntity, world) -> new QuarkBoat(quarkBoatEntityType, world))
				.build("quark_boat");

		RegistryHelper.register(quarkBoatEntityType, "quark_boat");
	}

	public static void setup(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			Map<Item, DispenseItemBehavior> registry = DispenserBlock.DISPENSER_REGISTRY;
			for(WoodSet set : woodSets)
				registry.put(set.boatItem, new QuarkBoatDispenseItemBehavior(set.name));
		});
	}

	@OnlyIn(Dist.CLIENT)
	public static void clientSetup(FMLClientSetupEvent event) {
		EntityRenderers.register(quarkBoatEntityType, QuarkBoatRenderer::new);

		event.enqueueWork(() -> {
			for (WoodSet set : woodSets) {
				Sheets.addWoodType(set.type);
			}
		});
	}

	public static WoodSet addWoodSet(QuarkModule module, String name, MaterialColor color, MaterialColor barkColor) {
		WoodType type = WoodType.register(WoodType.create(Quark.MOD_ID + ":" + name));
		WoodSet set = new WoodSet(name, module, type);

		set.log = log(name + "_log", module, color, barkColor);
		set.wood = new QuarkWoodBlock(name + "_wood", set.log, module, CreativeModeTab.TAB_BUILDING_BLOCKS, BlockBehaviour.Properties.of(Material.WOOD, barkColor).strength(2.0F).sound(SoundType.WOOD));
		set.planks = new QuarkPlanksBlock(name + "_planks", module, CreativeModeTab.TAB_BUILDING_BLOCKS, Properties.of(Material.WOOD, color).strength(2.0F, 3.0F).sound(SoundType.WOOD));
		set.strippedLog = log("stripped_" + name + "_log", module, color, color);
		set.strippedWood = new QuarkWoodBlock("stripped_" + name + "_wood", set.strippedLog, module, CreativeModeTab.TAB_BUILDING_BLOCKS, BlockBehaviour.Properties.of(Material.WOOD, color).strength(2.0F).sound(SoundType.WOOD));

		set.slab = VariantHandler.addSlab((IQuarkBlock) set.planks).getBlock();
		set.stairs = VariantHandler.addStairs((IQuarkBlock) set.planks).getBlock();
		set.fence = new QuarkFenceBlock(name + "_fence", set.planks, module, CreativeModeTab.TAB_DECORATIONS, BlockBehaviour.Properties.of(Material.WOOD, color).strength(2.0F, 3.0F).sound(SoundType.WOOD));
		set.fenceGate = new QuarkFenceGateBlock(name + "_fence_gate", set.planks, module, CreativeModeTab.TAB_REDSTONE, BlockBehaviour.Properties.of(Material.WOOD, color).strength(2.0F, 3.0F).sound(SoundType.WOOD));

		set.door = new QuarkDoorBlock(name + "_door", module, CreativeModeTab.TAB_DECORATIONS, BlockBehaviour.Properties.of(Material.WOOD, color).strength(3.0F).sound(SoundType.WOOD).noOcclusion());
		set.trapdoor = new QuarkTrapdoorBlock(name + "_trapdoor", module, CreativeModeTab.TAB_DECORATIONS, BlockBehaviour.Properties.of(Material.WOOD, color).strength(3.0F).sound(SoundType.WOOD).noOcclusion().isValidSpawn((s, g, p, e) -> false));

		set.button = new QuarkWoodenButtonBlock(name + "_button", set.planks, module, BlockBehaviour.Properties.of(Material.DECORATION).noCollission().strength(0.5F).sound(SoundType.WOOD));
		set.pressurePlate = new QuarkPressurePlateBlock(Sensitivity.EVERYTHING, name + "_pressure_plate", module, CreativeModeTab.TAB_REDSTONE, BlockBehaviour.Properties.of(Material.WOOD, color).noCollission().strength(0.5F).sound(SoundType.WOOD));

		set.sign = new QuarkStandingSignBlock(name + "_sign", module, type, BlockBehaviour.Properties.of(Material.WOOD, color).noCollission().strength(1.0F).sound(SoundType.WOOD));
		set.wallSign = new QuarkWallSignBlock(name + "_wall_sign", module, type, BlockBehaviour.Properties.of(Material.WOOD, color).noCollission().strength(1.0F).sound(SoundType.WOOD).lootFrom(() -> set.sign));

		set.bookshelf = new VariantBookshelfBlock(name, module, true).setCondition(() -> ModuleLoader.INSTANCE.isModuleEnabledOrOverlapping(VariantBookshelvesModule.class));
		set.ladder = new VariantLadderBlock(name, module, true).setCondition(() -> ModuleLoader.INSTANCE.isModuleEnabledOrOverlapping(VariantLaddersModule.class));
		set.post = new WoodPostBlock(module, set.fence, "", false).setCondition(() -> ModuleLoader.INSTANCE.isModuleEnabledOrOverlapping(WoodenPostsModule.class));
		set.strippedPost = new WoodPostBlock(module, set.fence, "stripped_", false).setCondition(() -> ModuleLoader.INSTANCE.isModuleEnabledOrOverlapping(WoodenPostsModule.class));
		set.verticalPlanks = VerticalPlanksModule.add(name, set.planks, module).setCondition(() -> ModuleLoader.INSTANCE.isModuleEnabledOrOverlapping(VerticalPlanksModule.class));

		VariantChestsModule.addChest(name, module, new ResourceLocation(Quark.MOD_ID, "block/" + name + "_planks"), Block.Properties.copy(Blocks.CHEST), true);

		set.signItem = new QuarkSignItem(module, set.sign, set.wallSign);
		set.boatItem = new QuarkBoatItem(name, module);

		makeSignWork(set.sign, set.wallSign);

		ToolInteractionHandler.registerInteraction(ToolActions.AXE_STRIP, set.log, set.strippedLog);
		ToolInteractionHandler.registerInteraction(ToolActions.AXE_STRIP, set.wood, set.strippedWood);
		ToolInteractionHandler.registerInteraction(ToolActions.AXE_STRIP, set.post, set.strippedPost);

		VariantLaddersModule.variantLadders.add(set.ladder);
		FuelHandler.addFuel(set.boatItem, 60 * 20);

		QuarkBoat.addQuarkBoatType(name, new QuarkBoatType(set.boatItem, set.planks));

		woodSets.add(set);

		return set;
	}

	public static void makeSignWork(Block sign, Block wallSign) {
		Set<Block> validBlocks = new HashSet<>();
		validBlocks.add(sign);
		validBlocks.add(wallSign);
		validBlocks.addAll(BlockEntityType.SIGN.validBlocks);
		BlockEntityType.SIGN.validBlocks = ImmutableSet.copyOf(validBlocks);
	}

	private static RotatedPillarBlock log(String name, QuarkModule module, MaterialColor topColor, MaterialColor sideColor) {
		return new QuarkPillarBlock(name, module, CreativeModeTab.TAB_BUILDING_BLOCKS,
				BlockBehaviour.Properties.of(Material.WOOD, s -> s.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y ? topColor : sideColor)
				.strength(2.0F).sound(SoundType.WOOD));
	}

	public static class WoodSet {

		public final String name;
		public final WoodType type;
		public final QuarkModule module;

		public Block log, wood, planks, strippedLog, strippedWood,
		slab, stairs, fence, fenceGate,
		door, trapdoor, button, pressurePlate, sign, wallSign,
		bookshelf, ladder, post, strippedPost, verticalPlanks;

		public Item signItem, boatItem;

		public WoodSet(String name, QuarkModule module, WoodType type) {
			this.name = name;
			this.module = module;
			this.type = type;
		}

	}

}
