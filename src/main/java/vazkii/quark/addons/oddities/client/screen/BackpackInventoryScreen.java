package vazkii.quark.addons.oddities.client.screen;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import vazkii.quark.addons.oddities.inventory.BackpackMenu;
import vazkii.quark.addons.oddities.module.BackpackModule;
import vazkii.quark.base.Quark;
import vazkii.quark.base.network.QuarkNetwork;
import vazkii.quark.base.network.message.oddities.HandleBackpackMessage;

public class BackpackInventoryScreen extends InventoryScreen {

	private static final ResourceLocation BACKPACK_INVENTORY_BACKGROUND = new ResourceLocation(Quark.MOD_ID, "textures/misc/backpack_gui.png");

	private final Player player;
	private final Map<Button, Integer> buttonYs = new HashMap<>();

	private boolean closeHack = false;
	private static InventoryMenu oldContainer;

	public BackpackInventoryScreen(InventoryMenu backpack, Inventory inventory, Component component) {
		super(setBackpackContainer(inventory.player, backpack));

		this.player = inventory.player;
		setBackpackContainer(player, oldContainer);
	}

	public static Player setBackpackContainer(Player entity, InventoryMenu container) {
		oldContainer = entity.inventoryMenu;
		entity.inventoryMenu = container;

		return entity;
	}

	@Override
	public void init() {
		imageHeight = 224;
		super.init();

		buttonYs.clear();

		for(Renderable renderable : renderables)
			if(renderable instanceof Button b)
				if(b.getClass().getName().contains("GuiButtonInventoryBook")) { // class check for Patchouli
					if(!buttonYs.containsKey(b)) {
						b.setY(b.getY() - 29);
						buttonYs.put(b, b.getY());
					}
				}

	}

	@Override
	public void containerTick() {
		buttonYs.forEach(Button::setY);

		super.containerTick();

		if(!BackpackModule.isEntityWearingBackpack(player)) {
			ItemStack curr = player.containerMenu.getCarried();
			BackpackMenu.saveCraftingInventory(player);
			closeHack = true;
			QuarkNetwork.sendToServer(new HandleBackpackMessage(false));
			minecraft.setScreen(new InventoryScreen(player));
			player.inventoryMenu.setCarried(curr);
		}
	}

	@Override
	public void removed() {
		if(closeHack) {
			closeHack = false;
			return;
		}

		super.removed();
	}

	@Override
	protected void renderBg(@Nonnull PoseStack stack, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, BACKPACK_INVENTORY_BACKGROUND);
		int i = leftPos;
		int j = topPos;
		blit(stack, i, j, 0, 0, imageWidth, imageHeight);
		renderEntityInInventoryFollowsMouse(stack, i + 51, j + 75, 30, i + 51 - mouseX, j + 75 - 50 - mouseY, minecraft.player);
		moveCharmsButtons();
	}

	private void moveCharmsButtons() {
		for(Renderable renderable : renderables) {
			//Charms buttons have a static Y pos, so use that to only focus on them.
			if(renderable instanceof ImageButton img) {
				int x = img.getX();
				int y = img.getY();
				
				if(y == height / 2 - 22)
					img.setPosition(x, y - 29);
			}
		}
	}

}
