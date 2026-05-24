package com.minecraft26.everythingisprojectile.client;

import com.minecraft26.everythingisprojectile.EverythingIsProjectileMod;
import com.minecraft26.everythingisprojectile.item.ProjectileGauntletItem;
import com.minecraft26.everythingisprojectile.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.ScreenEvent;

public final class GauntletSlotClientEvents {
    private static final int SLOT_SIZE = 16;
    private static final int SLOT_X = 77;
    private static final int SLOT_Y = 44;
    private static final Identifier GAUNTLET_TEXTURE = Identifier.fromNamespaceAndPath(EverythingIsProjectileMod.MODID, "textures/item/projectile_gauntlet.png");

    private GauntletSlotClientEvents() {
    }

    public static void register() {
        ContainerScreenEvent.Render.Background.BUS.addListener(GauntletSlotClientEvents::renderBackground);
        ContainerScreenEvent.Render.Foreground.BUS.addListener(GauntletSlotClientEvents::renderForeground);
        ScreenEvent.Init.Post.BUS.addListener(GauntletSlotClientEvents::onScreenInit);
    }

    private static void renderBackground(ContainerScreenEvent.Render.Background event) {
        if (!(event.getContainerScreen() instanceof InventoryScreen inventoryScreen)) {
            return;
        }

        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        int x = getSlotScreenX(inventoryScreen);
        int y = getSlotScreenY(inventoryScreen);
        boolean hovered = isHoveringScreen(inventoryScreen, event.getMouseX(), event.getMouseY());
        ItemStack carried = inventoryScreen.getMenu().getCarried();
        boolean carryingGauntlet = ProjectileGauntletItem.isGauntlet(carried);

        graphics.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, hovered ? 0xB0202020 : 0x90101010);
        graphics.outline(x - 1, y - 1, SLOT_SIZE + 2, SLOT_SIZE + 2, carryingGauntlet || hovered ? 0xFFA6D96A : 0xFF6A6A6A);
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, GAUNTLET_TEXTURE, x, y, 0.0F, 0.0F, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, ARGB.white(0.35F));
    }

    private static void renderForeground(ContainerScreenEvent.Render.Foreground event) {
        if (!(event.getContainerScreen() instanceof InventoryScreen inventoryScreen)) {
            return;
        }

        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        ItemStack stack = GauntletSlotClientState.getStack();
        int x = SLOT_X;
        int y = SLOT_Y;
        if (!stack.isEmpty()) {
            Minecraft minecraft = Minecraft.getInstance();
            graphics.item(stack, x, y);
            graphics.itemDecorations(minecraft.font, stack, x, y);
        }

        double localMouseX = event.getMouseX() - inventoryScreen.getGuiLeft();
        double localMouseY = event.getMouseY() - inventoryScreen.getGuiTop();
        if (!isHoveringLocal(localMouseX, localMouseY)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!stack.isEmpty()) {
            graphics.setTooltipForNextFrame(minecraft.font, stack, event.getMouseX(), event.getMouseY());
        } else {
            graphics.setTooltipForNextFrame(Component.translatable("gui.everythingisprojectile.gauntlet_slot"), event.getMouseX(), event.getMouseY());
        }
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen inventoryScreen)) {
            return;
        }

        event.addListener(new GauntletSlotClickWidget(getSlotScreenX(inventoryScreen), getSlotScreenY(inventoryScreen)));
    }

    private static int getSlotScreenX(AbstractContainerScreen<?> screen) {
        return screen.getGuiLeft() + SLOT_X;
    }

    private static int getSlotScreenY(AbstractContainerScreen<?> screen) {
        return screen.getGuiTop() + SLOT_Y;
    }

    private static boolean isHoveringScreen(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        int x = getSlotScreenX(screen);
        int y = getSlotScreenY(screen);
        return mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
    }

    private static boolean isHoveringLocal(double mouseX, double mouseY) {
        return mouseX >= SLOT_X && mouseX < SLOT_X + SLOT_SIZE && mouseY >= SLOT_Y && mouseY < SLOT_Y + SLOT_SIZE;
    }

    private static final class GauntletSlotClickWidget extends AbstractWidget {
        private GauntletSlotClickWidget(int x, int y) {
            super(x, y, SLOT_SIZE, SLOT_SIZE, Component.translatable("gui.everythingisprojectile.gauntlet_slot"));
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }

        @Override
        protected boolean isValidClickButton(net.minecraft.client.input.MouseButtonInfo buttonInfo) {
            return buttonInfo.button() == 0 || buttonInfo.button() == 1;
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            NetworkHandler.sendGauntletClick(event.button());
        }
    }
}
