/**
 * This class was created by <WireSegal>. It's distributed as
 * part of the Quark Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Quark
 * <p>
 * Quark is Open Source and distributed under the
 * CC-BY-NC-SA 3.0 License: https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB
 * <p>
 * File Created @ [Jul 05, 2019, 16:56 AM (EST)]
 */
package vazkii.quark.content.tweaks.module;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import vazkii.quark.api.event.SimpleHarvestEvent;
import vazkii.quark.api.event.SimpleHarvestEvent.ActionType;
import vazkii.quark.base.Quark;
import vazkii.quark.base.handler.MiscUtil;
import vazkii.quark.base.module.LoadModule;
import vazkii.quark.base.module.ModuleCategory;
import vazkii.quark.base.module.QuarkModule;
import vazkii.quark.base.module.config.Config;
import vazkii.quark.base.network.QuarkNetwork;
import vazkii.quark.base.network.message.HarvestMessage;
import vazkii.quark.integration.claim.IClaimIntegration;

@LoadModule(category = ModuleCategory.TWEAKS, hasSubscriptions = true)
public class SimpleHarvestModule extends QuarkModule {

    @Config(description = "Can players harvest crops with empty hand clicks?")
    public static boolean emptyHandHarvest = true;
    @Config(description = "Does harvesting crops with a hoe cost durability?")
    public static boolean harvestingCostsDurability = false;
    @Config(description = "Should Quark look for (nonvanilla) crops, and handle them?")
    public static boolean doHarvestingSearch = true;

    @Config(description = "Which crops can be harvested?\n" +
            "Format is: \"harvestState[,afterHarvest]\", i.e. \"minecraft:wheat[age=7]\" or \"minecraft:cocoa[age=2,facing=north],minecraft:cocoa[age=0,facing=north]\"")
    public static List<String> harvestableBlocks = Lists.newArrayList(
            "minecraft:wheat[age=7]",
            "minecraft:carrots[age=7]",
            "minecraft:potatoes[age=7]",
            "minecraft:beetroots[age=3]",
            "minecraft:nether_wart[age=3]",
            "minecraft:cocoa[age=2,facing=north],minecraft:cocoa[age=0,facing=north]",
            "minecraft:cocoa[age=2,facing=south],minecraft:cocoa[age=0,facing=south]",
            "minecraft:cocoa[age=2,facing=east],minecraft:cocoa[age=0,facing=east]",
            "minecraft:cocoa[age=2,facing=west],minecraft:cocoa[age=0,facing=west]");

    @Config(description = "Which blocks should right click harvesting simulate a click on instead of breaking?\n" +
            "This is for blocks like sweet berry bushes, which have right click harvesting built in.")
    public static List<String> rightClickableBlocks = Lists.newArrayList(
            "minecraft:sweet_berry_bush",
            "minecraft:cave_vines");

    public static final Map<BlockState, BlockState> crops = Maps.newHashMap();
    private static final Set<Block> cropBlocks = Sets.newHashSet(); //used for the event
    public static final Set<Block> rightClickCrops = Sets.newHashSet();

    public static TagKey<Block> simpleHarvestBlacklistedTag;

    @Override
    public void setup() {
        simpleHarvestBlacklistedTag = BlockTags.create(new ResourceLocation(Quark.MOD_ID, "simple_harvest_blacklisted"));
    }

    @Override
    public void configChanged() {
        crops.clear();
        cropBlocks.clear();
        rightClickCrops.clear();

        if (doHarvestingSearch) {
            ForgeRegistries.BLOCKS.getValues().stream()
                    .filter(b -> !isVanilla(b) && b instanceof CropBlock)
                    .map(b -> (CropBlock) b)
                    //only grabbing blocks whose max age is acceptable
                    .filter(b -> b.isMaxAge(b.defaultBlockState().setValue(b.getAgeProperty(), last(b.getAgeProperty().getPossibleValues()))))
                    .forEach(b -> crops.put(b.defaultBlockState().setValue(b.getAgeProperty(), last(b.getAgeProperty().getPossibleValues())), b.defaultBlockState()));

            ForgeRegistries.BLOCKS.getValues().stream()
                    .filter(b -> !isVanilla(b) && (b instanceof BushBlock || b instanceof GrowingPlantBlock) && b instanceof BonemealableBlock && !(b instanceof CropBlock))
                    .forEach(rightClickCrops::add);
        }

        for (String harvestKey : harvestableBlocks) {
            BlockState initial, result;
            String[] split = tokenize(harvestKey);
            initial = MiscUtil.fromString(split[0]);
            if (split.length > 1)
                result = MiscUtil.fromString(split[1]);
            else
                result = initial.getBlock().defaultBlockState();

            if (initial.getBlock() != Blocks.AIR)
                crops.put(initial, result);
        }

        for (String blockName : rightClickableBlocks) {
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockName));
            if (block != null)
                rightClickCrops.add(block);
        }

        crops.values().forEach(bl -> cropBlocks.add(bl.getBlock()));
    }

    private int last(Collection<Integer> vals) {
        return vals.stream().max(Integer::compare).orElse(0);
    }

    private String[] tokenize(String harvestKey) {
        boolean inBracket = false;
        for (int i = 0; i < harvestKey.length(); i++) {
            char charAt = harvestKey.charAt(i);
            if (charAt == '[')
                inBracket = true;
            else if (charAt == ']')
                inBracket = false;
            else if (charAt == ',' && !inBracket)
                return new String[]{harvestKey.substring(0, i), harvestKey.substring(i + 1)};
        }
        return new String[]{harvestKey};
    }

    private boolean isVanilla(Block entry) {
        ResourceLocation loc = ForgeRegistries.BLOCKS.getKey(entry);
        if (loc == null)
            return true; // Just in case

        return loc.getNamespace().equals("minecraft");
    }

    private static void harvestAndReplant(Level world, BlockPos pos, BlockState inWorld, Player player) {
        if (!(world instanceof ServerLevel serverLevel) || player.isSpectator())
            return;

        ItemStack mainHand = player.getMainHandItem();

        int fortune = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, mainHand);

        ItemStack copy = mainHand.copy();
        if (copy.isEmpty())
            copy = new ItemStack(Items.STICK);

        Map<Enchantment, Integer> enchMap = EnchantmentHelper.getEnchantments(copy);
        enchMap.put(Enchantments.BLOCK_FORTUNE, fortune);
        EnchantmentHelper.setEnchantments(enchMap, copy);

        Item blockItem = inWorld.getBlock().asItem();
        Block.getDrops(inWorld, serverLevel, pos, world.getBlockEntity(pos), player, copy)
                .forEach((stack) -> {
                    if (stack.getItem() == blockItem)
                        stack.shrink(1);

                    if (!stack.isEmpty())
                        Block.popResource(world, pos, stack);
                });
        inWorld.spawnAfterBreak(serverLevel, pos, copy, true); // true = is player

        // ServerLevel sets this to `false` in the constructor, do we really need this check?
        if (!world.isClientSide && !inWorld.is(simpleHarvestBlacklistedTag)) {
            BlockState newBlock = crops.get(inWorld);
            world.levelEvent(2001, pos, Block.getId(newBlock));
            world.setBlockAndUpdate(pos, newBlock);
        }
    }

    private boolean isHarvesting = false;

    @SubscribeEvent
    public void onClick(PlayerInteractEvent.RightClickBlock event) {
        if (isHarvesting)
            return;
        isHarvesting = true;
        if (click(event.getEntity(), event.getHand(), event.getPos(), event.getHitVec())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        }
        isHarvesting = false;
    }

    private static boolean handle(Player player, InteractionHand hand, BlockPos pos, boolean doRightClick, boolean isHoe) {
        if (!player.level.mayInteract(player, pos) || player == null || player.isSpectator())
            return false;

        BlockState worldBlock = player.level.getBlockState(pos);
        if (!worldBlock.is(simpleHarvestBlacklistedTag)) {
            //prevents firing event for non crop blocks
            if (cropBlocks.contains(worldBlock.getBlock())) {
                //event stuff
                ActionType action = getAction(worldBlock, doRightClick);
                SimpleHarvestEvent event = new SimpleHarvestEvent(worldBlock, pos, hand, player, isHoe, action);
                MinecraftForge.EVENT_BUS.post(event);
                if (event.isCanceled()) return false;

                BlockPos newPos = event.getTargetPos();
                if (newPos != pos) worldBlock = player.level.getBlockState(newPos);
                action = event.getAction();

                if (action == ActionType.HARVEST) {
                    harvestAndReplant(player.level, pos, worldBlock, player);
                    return true;
                } else if (action == ActionType.CLICK) {
                    if (!player.level.isClientSide)
                        return true;
                    return Quark.proxy.clientUseItem(player, player.level, hand,
                            new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, true)).consumesAction();
                }
            }
        }

        return false;
    }

    private static ActionType getAction(BlockState state, boolean doRightClick) {
        if (crops.containsKey(state)) return ActionType.HARVEST;
        else if (doRightClick && rightClickCrops.contains(state.getBlock())) return ActionType.CLICK;
        return ActionType.NONE;
    }

    public static boolean click(Player player, InteractionHand hand, BlockPos pos, BlockHitResult pick) {
        if (player == null || hand == null || player.isSpectator())
            return false;

        if (pick.getType() != HitResult.Type.BLOCK || !pick.getBlockPos().equals(pos))
            return false;

        if(!IClaimIntegration.INSTANCE.canBreak(player, pos))
            return false;

        BlockState stateAt = player.level.getBlockState(pos);
        if (stateAt.getToolModifiedState(new UseOnContext(player, hand, pick), ToolActions.HOE_TILL, true) != null)
            return false;

        ItemStack inHand = player.getItemInHand(hand);
        boolean isHoe = HoeHarvestingModule.isHoe(inHand);

        if (!emptyHandHarvest && !isHoe)
            return false;

        int range = HoeHarvestingModule.getRange(inHand);

        boolean hasHarvested = false;

        for (int x = 1 - range; x < range; x++)
            for (int z = 1 - range; z < range; z++) {
                BlockPos shiftPos = pos.offset(x, 0, z);

                if (!handle(player, hand, shiftPos, range > 1, isHoe)) {
                    shiftPos = shiftPos.above();

                    if (handle(player, hand, shiftPos, range > 1, isHoe))
                        hasHarvested = true;
                } else {
                    hasHarvested = true;
                }
            }

        if (!hasHarvested)
            return false;

        if (player.level.isClientSide) {
            if (inHand.isEmpty())
                QuarkNetwork.sendToServer(new HarvestMessage(pos, hand));
        } else {
            if (harvestingCostsDurability && isHoe)
                inHand.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        }

        return true;
    }
}
