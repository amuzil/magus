/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
package com.amuzil.magus.animation;

import com.amuzil.magus.MagusOld;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

public class PlayerAnimator {
	public static boolean BendylibInstalled = false;

	public static void init() {
		PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
				new ResourceLocation(MagusOld.MOD_ID, "animation"), 42, player -> registerPlayerAnimationLayer(player));
	}

	private static IAnimation registerPlayerAnimationLayer(AbstractClientPlayer player) {
		// This will be invoked for every new player
		return new ModifierLayer<>();
	}

	public static KeyframeAnimation getAnimation(String animationName) {
		String directory = BendylibInstalled ? "complex/" : "simple/";
		return PlayerAnimationRegistry.getAnimation(new ResourceLocation(MagusOld.MOD_ID, directory + animationName));
	}
}
