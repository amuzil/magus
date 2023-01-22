/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
package com.amuzil.magus.client.keybindings;

import com.amuzil.magus.MagusOld;
import com.amuzil.magus.animation.PlayerAnimator;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public class MagusKeybindings {
	public static KeyMapping DEBUG_PLAY_ANIMATION_KEYBINDING =
			new KeyMapping("key.animation.play", GLFW.GLFW_KEY_R, "key.animation.category");

	@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MagusOld.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
	public class KeybindingForgeEvents {
		@SubscribeEvent
		public static void handleKeys(InputEvent.Key ev) {
			if (DEBUG_PLAY_ANIMATION_KEYBINDING.consumeClick()) {
				var player = Minecraft.getInstance().player;

				var animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player)
						.get(new ResourceLocation(MagusOld.MOD_ID, "animation"));

				// play animation
				KeyframeAnimation anim = PlayerAnimator.getAnimation("air_gather_hands");
				animation.replaceAnimationWithFade(
						AbstractFadeModifier.standardFadeIn(anim.endTick, Ease.INOUTSINE),
						new KeyframeAnimationPlayer(anim));
			}
		}
	}

	@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MagusOld.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
	public class KeybindingModBusEvents {
		@SubscribeEvent
		public static void onKeyRegister(RegisterKeyMappingsEvent event) {
			event.register(DEBUG_PLAY_ANIMATION_KEYBINDING);
		}
	}
}
