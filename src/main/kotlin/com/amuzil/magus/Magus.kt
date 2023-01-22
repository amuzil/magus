/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
package com.amuzil.magus

import com.mojang.logging.LogUtils
import net.minecraft.client.Minecraft
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.Material
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.CreativeModeTabEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

@Mod(Magus.MOD_ID)
class Magus {
	companion object {
		const val MOD_ID = "magus"
		private val LOGGER = LogUtils.getLogger()

		internal val BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID)
		internal val ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID)

		val EXAMPLE_BLOCK: RegistryObject<Block> =
			BLOCKS.register("example_block") { Block(BlockBehaviour.Properties.of(Material.STONE)) }

		val EXAMPLE_ITEM: RegistryObject<BlockItem> =
			ITEMS.register("example_block") { BlockItem(EXAMPLE_BLOCK.get(), Item.Properties()) }
	}

	init {
		val modEventBus = FMLJavaModLoadingContext.get().modEventBus

		modEventBus.addListener(::onCommonSetup)

		BLOCKS.register(modEventBus)
		ITEMS.register(modEventBus)

		modEventBus.addListener { event: CreativeModeTabEvent.BuildContents ->
			if (event.tab == CreativeModeTabs.BUILDING_BLOCKS) {
				event.entries.put(
					ItemStack(EXAMPLE_BLOCK.get()),
					CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
				)
			}
		}

		MinecraftForge.EVENT_BUS.register(this)
	}

	private fun onCommonSetup(event: FMLCommonSetupEvent) {
		event.description()
		LOGGER.info("HELLO from common setup")
		LOGGER.info("DIRT BLOCK >> ${ForgeRegistries.BLOCKS.getKey(Blocks.DIRT)}")
	}

	@SubscribeEvent
	fun onServerStarting(event: ServerStartingEvent) {
		event.toString()
		LOGGER.info("HELLO from server starting")
	}

	@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = MOD_ID)
	object ClientModEvents {
		@JvmStatic
		@SubscribeEvent
		fun onClientSetup(event: FMLClientSetupEvent) {
			event.description()
			LOGGER.info("HELLO from client setup")
			LOGGER.info("MINECRAFT NAME >> ${Minecraft.getInstance().user.name}")
		}
	}
}
