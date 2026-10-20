package com.automine;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AutoMineClient implements ClientModInitializer {
    /** AutoMine attivo o no (non viene salvato: parte sempre spento). */
    public static boolean enabled = false;

    private static boolean openScreenNextTick = false;
    private static boolean holdingAttack = false;
    private static boolean lowToolWarned = false;

    private static KeyMapping toggleKey;
    private static KeyMapping guiKey;

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> AutoMineConfig.load());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> AutoMineConfig.save());

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.automine.toggle", InputConstants.Type.KEYSYM, InputConstants.KEY_R, KeyMapping.Category.MISC));
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.automine.gui", InputConstants.Type.KEYSYM, InputConstants.KEY_G, KeyMapping.Category.MISC));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerCommands(dispatcher));
        ClientTickEvents.START_CLIENT_TICK.register(AutoMineClient::tick);
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("automine")
                // /automine -> apre il layout di selezione
                .executes(ctx -> {
                    openScreenNextTick = true;
                    return 1;
                })
                // /automine on
                .then(ClientCommandManager.literal("on").executes(ctx -> {
                    setEnabled(ctx.getSource().getClient(), true);
                    return 1;
                }))
                // /automine off
                .then(ClientCommandManager.literal("off").executes(ctx -> {
                    setEnabled(ctx.getSource().getClient(), false);
                    return 1;
                }))
                // /automine toggle
                .then(ClientCommandManager.literal("toggle").executes(ctx -> {
                    setEnabled(ctx.getSource().getClient(), !enabled);
                    return 1;
                }))
                // /automine status
                .then(ClientCommandManager.literal("status").executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.translatable("automine.msg.status",
                            Component.translatable(enabled ? "automine.on" : "automine.off"),
                            AutoMineConfig.selectedCount()).withStyle(ChatFormatting.AQUA));
                    return 1;
                })));
    }

    public static void setEnabled(Minecraft mc, boolean value) {
        enabled = value;
        lowToolWarned = false;
        if (!value) releaseAttack(mc);
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable(value ? "automine.msg.on" : "automine.msg.off")
                    .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED), true);
            if (value && AutoMineConfig.selectedCount() == 0) {
                mc.player.displayClientMessage(Component.translatable("automine.msg.no_blocks")
                        .withStyle(ChatFormatting.YELLOW), false);
            }
        }
    }

    private static void releaseAttack(Minecraft mc) {
        if (holdingAttack) {
            holdingAttack = false;
            mc.options.keyAttack.setDown(false);
        }
    }

    private static void tick(Minecraft mc) {
        // Tasti rapidi
        while (toggleKey.consumeClick()) {
            if (mc.player != null) setEnabled(mc, !enabled);
        }
        while (guiKey.consumeClick()) {
            openScreenNextTick = true;
        }

        // Apertura GUI (rimandata di un tick perche' la chat si chiude dopo il comando)
        if (openScreenNextTick) {
            openScreenNextTick = false;
            if (mc.player != null) mc.setScreen(new AutoMineScreen());
        }

        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            holdingAttack = false;
            return;
        }

        boolean shouldMine = false;

        if (enabled && mc.screen == null) {
            HitResult hit = mc.hitResult;
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                BlockState state = mc.level.getBlockState(blockHit.getBlockPos());
                if (AutoMineConfig.isSelected(state.getBlock())) {
                    shouldMine = toolIsSafe(mc);
                }
            }
        }

        if (shouldMine) {
            // Equivale a tenere premuto il tasto sinistro: Minecraft scava da solo
            mc.options.keyAttack.setDown(true);
            holdingAttack = true;
        } else {
            releaseAttack(mc);
        }
    }

    /** Protezione attrezzo: si ferma quando restano <= 5 usi. */
    private static boolean toolIsSafe(Minecraft mc) {
        if (!AutoMineConfig.protectTool) return true;
        ItemStack stack = mc.player.getMainHandItem();
        boolean low = stack.isDamageableItem() && (stack.getMaxDamage() - stack.getDamageValue()) <= 5;
        if (low && !lowToolWarned) {
            lowToolWarned = true;
            mc.player.displayClientMessage(Component.translatable("automine.msg.low_tool")
                    .withStyle(ChatFormatting.RED), true);
        } else if (!low) {
            lowToolWarned = false;
        }
        return !low;
    }
}
