package vazkii.quark.base.module;

import com.google.common.base.Preconditions;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.ParallelDispatchEvent;
import vazkii.quark.base.Quark;
import vazkii.quark.base.block.IQuarkBlock;
import vazkii.quark.base.datagen.*;
import vazkii.quark.base.item.IQuarkItem;
import vazkii.quark.base.module.config.ConfigResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class ModuleLoader {

	private enum Step {
		CONSTRUCT, CONSTRUCT_CLIENT, REGISTER, POST_REGISTER, CONFIG_CHANGED, CONFIG_CHANGED_CLIENT, SETUP, SETUP_CLIENT,
		MODEL_REGISTRY, MODEL_BAKE, MODEL_LAYERS, TEXTURE_STITCH, POST_TEXTURE_STITCH, LOAD_COMPLETE, FIRST_CLIENT_TICK,
		GEN_ITEM_MODELS, GEN_BLOCK_STATES, GEN_ITEM_TAGS, GEN_BLOCK_TAGS, GEN_LOOT_TABLES, GEN_RECIPES
	}

	public static final ModuleLoader INSTANCE = new ModuleLoader();

	private Map<Class<? extends QuarkModule>, QuarkModule> foundModules = new HashMap<>();
	private final List<Step> stepsHandled = new ArrayList<>();

	private ConfigResolver config;
	private Runnable onConfigReloadJEI;
	private boolean clientTicked = false;
	private ParallelDispatchEvent event;

	private ModuleLoader() { }

	public void start() {
		findModules();
		dispatch(Step.CONSTRUCT, QuarkModule::construct);
		resolveConfigSpec();
	}

	@OnlyIn(Dist.CLIENT)
	public void clientStart() {
		dispatch(Step.CONSTRUCT_CLIENT, QuarkModule::constructClient);
		MinecraftForge.EVENT_BUS.register(this);
	}

	private void findModules() {
		ModuleFinder finder = new ModuleFinder();
		finder.findModules();
		foundModules = finder.getFoundModules();
	}

	private void resolveConfigSpec() {
		config = new ConfigResolver();
		config.makeSpec();
	}

	public void register() {
		dispatch(Step.REGISTER, QuarkModule::register);
		dispatch(Step.POST_REGISTER, QuarkModule::postRegister);
		config.registerConfigBoundElements();
	}

	public void configChanged() {
		if(!stepsHandled.contains(Step.POST_REGISTER))
			return; // We don't want to mess with changing config values before objects are registered

		if (onConfigReloadJEI != null)
			onConfigReloadJEI.run();
		config.configChanged();
		dispatch(Step.CONFIG_CHANGED, QuarkModule::configChanged);
	}

	@OnlyIn(Dist.CLIENT)
	public void configChangedClient() {
		if(!stepsHandled.contains(Step.POST_REGISTER))
			return; // We don't want to mess with changing config values before objects are registered

		dispatch(Step.CONFIG_CHANGED_CLIENT, QuarkModule::configChangedClient);
	}

	public void setup(ParallelDispatchEvent event) {
		this.event = event;
		Quark.proxy.handleQuarkConfigChange();
		dispatch(Step.SETUP, QuarkModule::setup);
	}

	@OnlyIn(Dist.CLIENT)
	public void clientSetup(ParallelDispatchEvent event) {
		this.event = event;
		dispatch(Step.SETUP_CLIENT, QuarkModule::clientSetup);
	}

	@OnlyIn(Dist.CLIENT)
	public void modelRegistry() {
		dispatch(Step.MODEL_REGISTRY, QuarkModule::modelRegistry);
	}

	@OnlyIn(Dist.CLIENT)
	public void modelBake(ModelBakeEvent event) {
		dispatch(Step.MODEL_BAKE, m -> m.modelBake(event));
	}

	@OnlyIn(Dist.CLIENT)
	public void modelLayers(EntityRenderersEvent.AddLayers event) {
		dispatch(Step.MODEL_LAYERS, m -> m.modelLayers(event));
	}

	@OnlyIn(Dist.CLIENT)
	public void textureStitch(TextureStitchEvent.Pre event) {
		dispatch(Step.TEXTURE_STITCH, m -> m.textureStitch(event));
	}

	@OnlyIn(Dist.CLIENT)
	public void postTextureStitch(TextureStitchEvent.Post event) {
		dispatch(Step.POST_TEXTURE_STITCH, m -> m.postTextureStitch(event));
	}

	public void loadComplete(ParallelDispatchEvent event) {
		this.event = event;
		dispatch(Step.LOAD_COMPLETE, QuarkModule::loadComplete);
	}

	public void dataGen(QuarkItemModelProvider itemModels) {
		dispatch(Step.GEN_ITEM_MODELS, m -> m.dataGen(itemModels));
	}

	public void dataGen(QuarkBlockStateProvider states) {
		dispatch(Step.GEN_BLOCK_STATES, m -> m.dataGen(states));
	}

	public void dataGen(QuarkBlockTagsProvider blockTags) {
		dispatch(Step.GEN_BLOCK_TAGS, m -> m.dataGen(blockTags));
	}

	public void dataGen(QuarkItemTagsProvider itemTags) {
		dispatch(Step.GEN_ITEM_TAGS, m -> m.dataGen(itemTags));
	}

	public void dataGen(QuarkLootTableProvider tableProvider, Map<Block, LootTable.Builder> lootTables) {
		dispatch(Step.GEN_LOOT_TABLES, m -> m.dataGen(tableProvider, lootTables));
	}

	public void dataGen(QuarkRecipeProvider recipeProvider, Consumer<FinishedRecipe> recipes) {
		dispatch(Step.GEN_RECIPES, m -> m.dataGen(recipeProvider, recipes));
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public void firstClientTick(ClientTickEvent event) {
		if(!clientTicked && event.phase == Phase.END) {
			dispatch(Step.FIRST_CLIENT_TICK, QuarkModule::firstClientTick);
			clientTicked = true;
		}
	}

	private void dispatch(Step step, Consumer<QuarkModule> run) {
		Quark.LOG.info("Dispatching Module Step " + step);
		foundModules.values().forEach(run);
		stepsHandled.add(step);
	}

	void enqueue(Runnable r) {
		Preconditions.checkNotNull(event);
		event.enqueueWork(r);
	}

	public boolean isModuleEnabled(Class<? extends QuarkModule> moduleClazz) {
		QuarkModule module = getModuleInstance(moduleClazz);
		return module != null && module.enabled;
	}

	public boolean isModuleEnabledOrOverlapping(Class<? extends QuarkModule> moduleClazz) {
		QuarkModule module = getModuleInstance(moduleClazz);
		return module != null && (module.enabled || module.disabledByOverlap);
	}

	public QuarkModule getModuleInstance(Class<? extends QuarkModule> moduleClazz) {
		return foundModules.get(moduleClazz);
	}

	public boolean isItemEnabled(Item i) {
		if(i instanceof IQuarkItem qi) {
			return qi.isEnabled();
		}
		else if(i instanceof BlockItem bi) {
			Block b = bi.getBlock();
			if(b instanceof IQuarkBlock qb) {
				return qb.isEnabled();
			}
		}

		return true;
	}

	/**
	 * Meant only to be called internally.
	 */
	public void initJEICompat(Runnable jeiRunnable) {
		onConfigReloadJEI = jeiRunnable;
		onConfigReloadJEI.run();
	}

}
