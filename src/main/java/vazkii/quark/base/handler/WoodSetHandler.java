package vazkii.quark.base.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.PressurePlateBlock.Sensitivity;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import vazkii.arl.util.RegistryHelper;
import vazkii.quark.base.Quark;
import vazkii.quark.base.block.IQuarkBlock;
import vazkii.quark.base.block.QuarkBlock;
import vazkii.quark.base.block.QuarkDoorBlock;
import vazkii.quark.base.block.QuarkFenceBlock;
import vazkii.quark.base.block.QuarkFenceGateBlock;
import vazkii.quark.base.block.QuarkPillarBlock;
import vazkii.quark.base.block.QuarkPressurePlateBlock;
import vazkii.quark.base.block.QuarkStandingSignBlock;
import vazkii.quark.base.block.QuarkTrapdoorBlock;
import vazkii.quark.base.block.QuarkWallSignBlock;
import vazkii.quark.base.block.QuarkWoodenButtonBlock;
import vazkii.quark.base.client.render.QuarkBoatRenderer;
import vazkii.quark.base.item.QuarkSignItem;
import vazkii.quark.base.item.boat.QuarkBoat;
import vazkii.quark.base.item.boat.QuarkBoatDispenseItemBehavior;
import vazkii.quark.base.item.boat.QuarkBoatItem;
import vazkii.quark.base.item.boat.QuarkChestBoat;
import vazkii.quark.base.module.ModuleLoader;
import vazkii.quark.base.module.QuarkModule;
import vazkii.quark.content.building.block.HollowLogBlock;
import vazkii.quark.content.building.block.VariantBookshelfBlock;
import vazkii.quark.content.building.block.VariantLadderBlock;
import vazkii.quark.content.building.block.WoodPostBlock;
import vazkii.quark.content.building.module.HollowLogsModule;
import vazkii.quark.content.building.module.VariantBookshelvesModule;
import vazkii.quark.content.building.module.VariantChestsModule;
import vazkii.quark.content.building.module.VariantLaddersModule;
import vazkii.quark.content.building.module.VerticalPlanksModule;
import vazkii.quark.content.building.module.WoodenPostsModule;

public class WoodSetHandler {

	public static record QuarkBoatType(String name, Item boat, Item chestBoat, Block planks) {}
	private static final Map<String, QuarkBoatType> quarkBoatTypes = new HashMap<>();
	
	public static EntityType<QuarkBoat> quarkBoatEntityType = null;
	public static EntityType<QuarkChestBoat> quarkChestBoatEntityType = null;

	private static final List<WoodSet> woodSets = new ArrayList<>();

	public static void register() {
		quarkBoatEntityType = EntityType.Builder.<QuarkBoat>of(QuarkBoat::new, MobCategory.MISC)
				.sized(1.375F, 0.5625F)
				.clientTrackingRange(10)
				.setCustomClientFactory((spawnEntity, world) -> new QuarkBoat(quarkBoatEntityType, world))
				.build("quark_boat");
		
		quarkChestBoatEntityType = EntityType.Builder.<QuarkChestBoat>of(QuarkChestBoat::new, MobCategory.MISC)
				.sized(1.375F, 0.5625F)
				.clientTrackingRange(10)
				.setCustomClientFactory((spawnEntity, world) -> new QuarkChestBoat(quarkChestBoatEntityType, world))
				.build("quark_chest_boat");

		RegistryHelper.register(quarkBoatEntityType, "quark_boat", ForgeRegistries.ENTITY_TYPES);
		RegistryHelper.register(quarkChestBoatEntityType, "quark_chest_boat", ForgeRegistries.ENTITY_TYPES);
	}

	public static void setup(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			Map<Item, DispenseItemBehavior> registry = DispenserBlock.DISPENSER_REGISTRY;
			for(WoodSet set : woodSets) {
				registry.put(set.boatItem, new QuarkBoatDispenseItemBehavior(set.name, false));
				registry.put(set.chestBoatItem, new QuarkBoatDispenseItemBehavior(set.name, true));
			}
		});
	}

	@OnlyIn(Dist.CLIENT)
	public static void clientSetup(FMLClientSetupEvent event) {
		EntityRenderers.register(quarkBoatEntityType, r -> new QuarkBoatRenderer(r, false));
		EntityRenderers.register(quarkChestBoatEntityType, r -> new QuarkBoatRenderer(r, true));

		event.enqueueWork(() -> {
			for (WoodSet set : woodSets) {
				Sheets.addWoodType(set.type);
			}
		});
	}

	public static WoodSet addWoodSet(QuarkModule module, String name, MaterialColor color, MaterialColor barkColor) {
		return addWoodSet(module, name, color, barkColor, true, true);
	}
	
	public static WoodSet addWoodSet(QuarkModule module, String name, MaterialColor color, MaterialColor barkColor, boolean hasLog, boolean hasBoat) {
		String namespacedName = Quark.MOD_ID + ":" + name;
		BlockSetType setType = BlockSetType.register(new BlockSetType(namespacedName));
		WoodType woodType = WoodType.register(new WoodType(namespacedName, setType));
		
		WoodSet set = new WoodSet(name, module, setType, woodType);

		if(hasLog) {
			set.log = log(name + "_log", module, color, barkColor);
			set.wood = new QuarkPillarBlock(name + "_wood", module, CreativeModeTabs.BUILDING_BLOCKS, BlockBehaviour.Properties.of(Material.WOOD, barkColor).strength(2.0F).sound(SoundType.WOOD));
			set.strippedLog = log("stripped_" + name + "_log", module, color, color);
			set.strippedWood = new QuarkPillarBlock("stripped_" + name + "_wood", module, CreativeModeTabs.BUILDING_BLOCKS, BlockBehaviour.Properties.of(Material.WOOD, color).strength(2.0F).sound(SoundType.WOOD));
		}		
		
		set.planks = new QuarkBlock(name + "_planks", module, CreativeModeTabs.BUILDING_BLOCKS, Properties.of(Material.WOOD, color).strength(2.0F, 3.0F).sound(SoundType.WOOD));
		
		set.slab = VariantHandler.addSlab((IQuarkBlock) set.planks).getBlock();
		set.stairs = VariantHandler.addStairs((IQuarkBlock) set.planks).getBlock();
		set.fence = new QuarkFenceBlock(name + "_fence", module, CreativeModeTabs.BUILDING_BLOCKS, BlockBehaviour.Properties.of(Material.WOOD, color).strength(2.0F, 3.0F).sound(SoundType.WOOD));
		set.fenceGate = new QuarkFenceGateBlock(name + "_fence_gate", module, CreativeModeTabs.REDSTONE_BLOCKS, BlockBehaviour.Properties.of(Material.WOOD, color).strength(2.0F, 3.0F).sound(SoundType.WOOD), woodType);

		set.door = new QuarkDoorBlock(name + "_door", module, CreativeModeTabs.REDSTONE_BLOCKS, BlockBehaviour.Properties.of(Material.WOOD, color).strength(3.0F).sound(SoundType.WOOD).noOcclusion(), setType);
		set.trapdoor = new QuarkTrapdoorBlock(name + "_trapdoor", module, CreativeModeTabs.REDSTONE_BLOCKS, BlockBehaviour.Properties.of(Material.WOOD, color).strength(3.0F).sound(SoundType.WOOD).noOcclusion().isValidSpawn((s, g, p, e) -> false), setType);

		set.button = new QuarkWoodenButtonBlock(name + "_button", module, BlockBehaviour.Properties.of(Material.DECORATION).noCollission().strength(0.5F).sound(SoundType.WOOD), setType);
		set.pressurePlate = new QuarkPressurePlateBlock(Sensitivity.EVERYTHING, name + "_pressure_plate", module, CreativeModeTabs.REDSTONE_BLOCKS, BlockBehaviour.Properties.of(Material.WOOD, color).noCollission().strength(0.5F).sound(SoundType.WOOD), setType);

		set.sign = new QuarkStandingSignBlock(name + "_sign", module, woodType, BlockBehaviour.Properties.of(Material.WOOD, color).noCollission().strength(1.0F).sound(SoundType.WOOD));
		set.wallSign = new QuarkWallSignBlock(name + "_wall_sign", module, woodType, BlockBehaviour.Properties.of(Material.WOOD, color).noCollission().strength(1.0F).sound(SoundType.WOOD).lootFrom(() -> set.sign));

		set.bookshelf = new VariantBookshelfBlock(name, module, true).setCondition(() -> ModuleLoader.INSTANCE.isModuleEnabledOrOverlapping(VariantBookshelvesModule.class));
		set.ladder = new VariantLadderBlock(name, module, true).setCondition(() -> ModuleLoader.INSTANCE.isModuleEnabledOrOverlapping(VariantLaddersModule.class));
		
		set.post = new WoodPostBlock(module, set.fence, "", false).setCondition(() -> ModuleLoader.INSTANCE.isModuleEnabledOrOverlapping(WoodenPostsModule.class));
		set.strippedPost = new WoodPostBlock(module, set.fence, "stripped_", false).setCondition(() -> ModuleLoader.INSTANCE.isModuleEnabledOrOverlapping(WoodenPostsModule.class));
		
		set.verticalPlanks = VerticalPlanksModule.add(name, set.planks, module).setCondition(() -> ModuleLoader.INSTANCE.isModuleEnabledOrOverlapping(VerticalPlanksModule.class));

		if(hasLog) {
			set.hollowLog = new HollowLogBlock(set.log, module, false).setCondition(() -> ModuleLoader.INSTANCE.isModuleEnabledOrOverlapping(HollowLogsModule.class));
		}
		
		VariantChestsModule.addChest(name, module, Block.Properties.copy(Blocks.CHEST), true);

		set.signItem = new QuarkSignItem(module, set.sign, set.wallSign);
		
		if(hasBoat) {
			set.boatItem = new QuarkBoatItem(name, module, false);
			set.chestBoatItem = new QuarkBoatItem(name, module, true);
		}

		makeSignWork(set.sign, set.wallSign);

		if(hasLog) {
			ToolInteractionHandler.registerInteraction(ToolActions.AXE_STRIP, set.log, set.strippedLog);
			ToolInteractionHandler.registerInteraction(ToolActions.AXE_STRIP, set.wood, set.strippedWood);
		}
		ToolInteractionHandler.registerInteraction(ToolActions.AXE_STRIP, set.post, set.strippedPost);

		VariantLaddersModule.variantLadders.add(set.ladder);

		if(hasBoat) {
			FuelHandler.addFuel(set.boatItem, 60 * 20);
			FuelHandler.addFuel(set.chestBoatItem, 60 * 20);

			addQuarkBoatType(name, new QuarkBoatType(name, set.boatItem, set.chestBoatItem, set.planks));
		}

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
		return new QuarkPillarBlock(name, module, CreativeModeTabs.BUILDING_BLOCKS,
				BlockBehaviour.Properties.of(Material.WOOD, s -> s.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y ? topColor : sideColor)
				.strength(2.0F).sound(SoundType.WOOD));
	}
	
	public static void addQuarkBoatType(String name, QuarkBoatType type) {
		quarkBoatTypes.put(name, type);
	}

	public static QuarkBoatType getQuarkBoatType(String name) {
		return quarkBoatTypes.get(name);
	}

	public static Stream<String> boatTypes() {
		return quarkBoatTypes.keySet().stream();
	}

	public static class WoodSet {

		public final String name;
		public final BlockSetType setType;
		public final WoodType type;
		public final QuarkModule module;

		public Block log, wood, planks, strippedLog, strippedWood,
		slab, stairs, fence, fenceGate,
		door, trapdoor, button, pressurePlate, sign, wallSign,
		bookshelf, ladder, post, strippedPost, verticalPlanks,
		hollowLog;

		public Item signItem, boatItem, chestBoatItem;

		public WoodSet(String name, QuarkModule module, BlockSetType setType, WoodType type) {
			this.name = name;
			this.module = module;
			this.setType = setType;
			this.type = type;
		}

	}

}
