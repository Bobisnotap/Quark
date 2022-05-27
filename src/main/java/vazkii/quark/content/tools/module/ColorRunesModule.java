package vazkii.quark.content.tools.module;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkDirection;
import vazkii.arl.util.ItemNBTHelper;
import vazkii.quark.api.IRuneColorProvider;
import vazkii.quark.api.QuarkCapabilities;
import vazkii.quark.base.Quark;
import vazkii.quark.base.handler.MiscUtil;
import vazkii.quark.base.module.LoadModule;
import vazkii.quark.base.module.ModuleCategory;
import vazkii.quark.base.module.QuarkModule;
import vazkii.quark.base.module.config.Config;
import vazkii.quark.base.network.QuarkNetwork;
import vazkii.quark.base.network.message.UpdateTridentMessage;
import vazkii.quark.content.tools.client.render.GlintRenderTypes;
import vazkii.quark.content.tools.item.RuneItem;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author WireSegal
 * Hacked by svenhjol
 * Created at 1:52 PM on 8/17/19.
 */
@LoadModule(category = ModuleCategory.TOOLS, hasSubscriptions = true)
public class ColorRunesModule extends QuarkModule {

	public static final String TAG_RUNE_ATTACHED = Quark.MOD_ID + ":RuneAttached";
	public static final String TAG_RUNE_COLOR = Quark.MOD_ID + ":RuneColor";

	public static final int RUNE_TYPES = 17;

	private static final ThreadLocal<ItemStack> targetStack = new ThreadLocal<>();
	public static TagKey<Item> runesTag, runesLootableTag;
	public static List<RuneItem> runes;
	public static Item rainbow_rune;
	public static Item blank_rune;

	@Config public static int dungeonWeight = 10;
	@Config public static int netherFortressWeight = 8;
	@Config public static int jungleTempleWeight = 8;
	@Config public static int desertTempleWeight = 8;
	@Config public static int itemQuality = 0;
	@Config public static int applyCost = 5;

	public static void setTargetStack(ItemStack stack) {
		targetStack.set(stack);
	}

	public static int changeColor() {
		ItemStack target = targetStack.get();

		if (target == null)
			return -1;

		LazyOptional<IRuneColorProvider> cap = get(target);

		if (cap.isPresent())
			return cap.orElse((s) -> -1).getRuneColor(target);
		if (!ItemNBTHelper.getBoolean(target, TAG_RUNE_ATTACHED, false))
			return -1;

		ItemStack proxied = ItemStack.of(ItemNBTHelper.getCompound(target, TAG_RUNE_COLOR, false));
		LazyOptional<IRuneColorProvider> proxyCap = get(proxied);
		return proxyCap.orElse((s) -> -1).getRuneColor(target);
	}

	@OnlyIn(Dist.CLIENT)
	public static RenderType getGlint() {
		return renderType(GlintRenderTypes.glint, RenderType::glint);
	}

	@OnlyIn(Dist.CLIENT)
	public static RenderType getGlintTranslucent() {
		return renderType(GlintRenderTypes.glintTranslucent, RenderType::glintTranslucent);
	}

	@OnlyIn(Dist.CLIENT)
	public static RenderType getEntityGlint() {
		return renderType(GlintRenderTypes.entityGlint, RenderType::entityGlint);
	}

	@OnlyIn(Dist.CLIENT)
	public static RenderType getGlintDirect() {
		return renderType(GlintRenderTypes.glintDirect, RenderType::glintDirect);
	}

	@OnlyIn(Dist.CLIENT)
	public static RenderType getEntityGlintDirect() {
		return renderType(GlintRenderTypes.entityGlintDirect, RenderType::entityGlintDirect);
	}

	@OnlyIn(Dist.CLIENT)
	public static RenderType getArmorGlint() {
		return renderType(GlintRenderTypes.armorGlint, RenderType::armorGlint);
	}

	@OnlyIn(Dist.CLIENT)
	public static RenderType getArmorEntityGlint() {
		return renderType(GlintRenderTypes.armorEntityGlint, RenderType::armorEntityGlint);
	}

	@OnlyIn(Dist.CLIENT)
	private static RenderType renderType(List<RenderType> list, Supplier<RenderType> vanilla) {
		int color = changeColor();
		return color >= 0 && color <= RUNE_TYPES ? list.get(color) : vanilla.get();
	}

	private static final Map<ThrownTrident, ItemStack> TRIDENT_STACK_REFERENCES = new WeakHashMap<>();

	public static void syncTrident(Consumer<Packet<?>> packetConsumer, ThrownTrident trident, boolean force) {
		ItemStack stack = trident.getPickupItem();
		ItemStack prev = TRIDENT_STACK_REFERENCES.get(trident);
		if (force || prev == null || ItemStack.isSameItemSameTags(stack, prev))
			packetConsumer.accept(QuarkNetwork.toVanillaPacket(new UpdateTridentMessage(trident.getId(), stack), NetworkDirection.PLAY_TO_CLIENT));
		else
			TRIDENT_STACK_REFERENCES.put(trident, stack);
	}

	@Override
	public void register() {
		runes = Arrays.stream(DyeColor.values()).map(color -> new RuneItem(color.getSerializedName() + "_rune", this, color.getId(), true)).toList();

		rainbow_rune = new RuneItem("rainbow_rune", this, 16, true);
		blank_rune = new RuneItem("blank_rune", this, 17, false);

		runesTag = ItemTags.create(new ResourceLocation(Quark.MOD_ID, "runes"));
		runesLootableTag = ItemTags.create(new ResourceLocation(Quark.MOD_ID, "runes_lootable"));
	}

	@SubscribeEvent
	public void onLootTableLoad(LootTableLoadEvent event) {
		int weight = 0;

		if(event.getName().equals(BuiltInLootTables.SIMPLE_DUNGEON))
			weight = dungeonWeight;
		else if(event.getName().equals(BuiltInLootTables.NETHER_BRIDGE))
			weight = netherFortressWeight;
		else if(event.getName().equals(BuiltInLootTables.JUNGLE_TEMPLE))
			weight = jungleTempleWeight;
		else if(event.getName().equals(BuiltInLootTables.DESERT_PYRAMID))
			weight = desertTempleWeight;

		if(weight > 0) {
			LootPoolEntryContainer entry = LootItem.lootTableItem(blank_rune)
					.setWeight(weight)
					.setQuality(itemQuality)
					.build();
			MiscUtil.addToLootTable(event.getTable(), entry);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onAnvilUpdate(AnvilUpdateEvent event) {
		ItemStack left = event.getLeft();
		ItemStack right = event.getRight();
		ItemStack output = event.getOutput();

		if(!left.isEmpty() && !right.isEmpty() && canHaveRune(left) && right.is(runesTag)) {
			ItemStack out = (output.isEmpty() ? left : output).copy();
			ItemNBTHelper.setBoolean(out, TAG_RUNE_ATTACHED, true);
			ItemNBTHelper.setCompound(out, TAG_RUNE_COLOR, right.serializeNBT());
			event.setOutput(out);

			String name = event.getName();
			int cost = Math.max(1, applyCost);

			if(name != null && !name.isEmpty() && (!out.hasCustomHoverName() || !out.getHoverName().getString().equals(name))) {
				out.setHoverName(new TextComponent(name));
				cost++;
			}

			event.setCost(cost);
			event.setMaterialCost(1);
		}
	}

	private static boolean canHaveRune(ItemStack stack) {
		return stack.isEnchanted() || (stack.getItem() == Items.COMPASS && CompassItem.isLodestoneCompass(stack)); // isLodestoneCompass = is lodestone compass
	}

	private static LazyOptional<IRuneColorProvider> get(ICapabilityProvider provider) {
		return provider.getCapability(QuarkCapabilities.RUNE_COLOR);
	}

}
