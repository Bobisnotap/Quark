package vazkii.quark.base.proxy;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import net.minecraftforge.registries.ForgeRegistries;
import vazkii.arl.util.ClientTicker;
import vazkii.quark.base.Quark;
import vazkii.quark.base.capability.CapabilityHandler;
import vazkii.quark.base.datagen.*;
import vazkii.quark.base.handler.*;
import vazkii.quark.base.module.ModuleLoader;
import vazkii.quark.base.module.config.IConfigCallback;
import vazkii.quark.base.network.QuarkNetwork;
import vazkii.quark.base.recipe.*;
import vazkii.quark.base.world.EntitySpawnHandler;
import vazkii.quark.base.world.WorldGenHandler;

import java.time.LocalDateTime;
import java.time.Month;

public class CommonProxy {

	private int lastConfigChange = -11;
	public static boolean jingleTheBells = false;
	private boolean registerDone = false;
	private boolean configGuiSaving = false;

	public void start() {
		ForgeRegistries.RECIPE_SERIALIZERS.register(ExclusionRecipe.SERIALIZER);
		ForgeRegistries.RECIPE_SERIALIZERS.register(DataMaintainingRecipe.SERIALIZER);
		ForgeRegistries.RECIPE_SERIALIZERS.register(DataMaintainingSmeltingRecipe.SERIALIZER);
		ForgeRegistries.RECIPE_SERIALIZERS.register(DataMaintainingCampfireRecipe.SERIALIZER);
		ForgeRegistries.RECIPE_SERIALIZERS.register(DataMaintainingSmokingRecipe.SERIALIZER);

		QuarkSounds.start();
		ModuleLoader.INSTANCE.start();
		WorldGenHandler.start();

		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		registerListeners(bus);

		LocalDateTime now = LocalDateTime.now();
		if (now.getMonth() == Month.DECEMBER && now.getDayOfMonth() >= 16 || now.getMonth() == Month.JANUARY && now.getDayOfMonth() <= 2)
			jingleTheBells = true;
	}

	public void registerListeners(IEventBus bus) {
		bus.addListener(this::setup);
		bus.addListener(this::loadComplete);
		bus.addListener(this::configChanged);
		bus.addListener(this::registerCapabilities);
		bus.addListener(this::dataGen);

		bus.register(RegistryListener.class);
	}

	public void dataGen(GatherDataEvent event) {
		var gen = event.getGenerator();
		if (event.includeClient()) {
			gen.addProvider(new QuarkBlockStateProvider(gen, Quark.MOD_ID, event.getExistingFileHelper()));
			gen.addProvider(new QuarkItemModelProvider(gen, Quark.MOD_ID, event.getExistingFileHelper()));
		}
		if (event.includeServer()) {
			var blockTags = new QuarkBlockTagsProvider(gen, Quark.MOD_ID, event.getExistingFileHelper());
			gen.addProvider(blockTags);
			gen.addProvider(new QuarkItemTagsProvider(gen, blockTags, Quark.MOD_ID, event.getExistingFileHelper()));

			gen.addProvider(new QuarkLootTableProvider(gen));
			gen.addProvider(new QuarkRecipeProvider(gen, Quark.MOD_ID));
		}
	}

	public void setup(FMLCommonSetupEvent event) {
		QuarkNetwork.setup();
		BrewingHandler.setup();
		ModuleLoader.INSTANCE.setup(event);
		initContributorRewards();

		WoodSetHandler.setup(event);
	}

	public void loadComplete(FMLLoadCompleteEvent event) {
		ModuleLoader.INSTANCE.loadComplete(event);

		WorldGenHandler.loadComplete(event);
		FuelHandler.addAllWoods();
		UndergroundBiomeHandler.init(event);
	}

	public void configChanged(ModConfigEvent event) {
		if(event.getConfig().getModId().equals(Quark.MOD_ID)
				&& ClientTicker.ticksInGame - lastConfigChange > 10
				&& !configGuiSaving) {
			lastConfigChange = ClientTicker.ticksInGame;
			handleQuarkConfigChange();
		}
	}

	public void setConfigGuiSaving(boolean saving) {
		configGuiSaving = saving;
		lastConfigChange = ClientTicker.ticksInGame;
	}

	public void registerCapabilities(RegisterCapabilitiesEvent event) {
		CapabilityHandler.registerCapabilities(event);
	}

	public void handleQuarkConfigChange() {
		ModuleLoader.INSTANCE.configChanged();
		EntitySpawnHandler.refresh();
	}

	public InteractionResult clientUseItem(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		return InteractionResult.PASS;
	}

	protected void initContributorRewards() {
		ContributorRewardHandler.init();
	}

	public IConfigCallback getConfigCallback() {
		return new IConfigCallback.Dummy();
	}

	public static final class RegistryListener {

		private static boolean registerDone;

		@SubscribeEvent(priority = EventPriority.HIGHEST)
		public static void registerContent(RegistryEvent.Register<?> event) {
			if(registerDone)
				return;
			registerDone = true;

			ModuleLoader.INSTANCE.register();
			WoodSetHandler.register();
		}

	}

}
