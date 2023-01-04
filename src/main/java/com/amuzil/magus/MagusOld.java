/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
package com.amuzil.magus;

import com.mojang.logging.LogUtils;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

// @Mod(MagusOld.MODID)
public class MagusOld {
	public static final String MOD_ID = "magus";
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

	public static final RegistryObject<Block> EXAMPLE_BLOCK =
			BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of(Material.STONE)));

	public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM =
			ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));

	public MagusOld() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

		modEventBus.addListener(this::commonSetup);
		BLOCKS.register(modEventBus);
		ITEMS.register(modEventBus);

		modEventBus.addListener((Consumer<CreativeModeTabEvent.BuildContents>) event -> {
			if (event.getTab() == CreativeModeTabs.BUILDING_BLOCKS) {
				event.getEntries()
						.put(new ItemStack(EXAMPLE_BLOCK.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
			}
		});

		MinecraftForge.EVENT_BUS.register(this);
	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		LOGGER.info("HELLO from common setup");
		LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
	}

	@SubscribeEvent
	public void onServerStarting(ServerStartingEvent event) {
		LOGGER.info("HELLO from server starting");
	}

	@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class ClientModEvents {
		@SubscribeEvent
		public static void onClientSetup(FMLClientSetupEvent event) {
			LOGGER.info("HELLO from client setup");
			LOGGER.info(
					"MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
		}
	}
}
