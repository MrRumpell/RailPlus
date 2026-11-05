package com.mrrumpell.railplus;

import com.mrrumpell.railplus.registry.ModBlocks;
import com.mrrumpell.railplus.registry.ModItems;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RailPlus: diagonal rails for Minecraft 1.21.1 on NeoForge.
 */
@Mod(RailPlus.MOD_ID)
public class RailPlus {
    public static final String MOD_ID = "railplus";
    public static final Logger LOGGER = LoggerFactory.getLogger("RailPlus");
    private static final boolean SHOW_TRAIN_LIGHT_IN_CREATIVE_TAB = false;

    public RailPlus(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            // Keep the rails together rather than at the end of the tab.
            ItemStack diagonal = new ItemStack(ModItems.DIAGONAL_RAIL.get());
            ItemStack poweredDiagonal = new ItemStack(ModItems.POWERED_DIAGONAL_RAIL.get());
            ItemStack yRail = new ItemStack(ModItems.Y_RAIL.get());
            ItemStack fourWay = new ItemStack(ModItems.FOUR_WAY_RAIL.get());
            ItemStack railEnd = new ItemStack(ModItems.RAIL_END.get());

            event.insertAfter(new ItemStack(Items.ACTIVATOR_RAIL), diagonal,
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(diagonal.copy(), poweredDiagonal,
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(poweredDiagonal.copy(), yRail,
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(yRail.copy(), fourWay,
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(fourWay.copy(), railEnd,
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            // The Train Light remains registered for /give. Flip this flag once
            // its model is finalized to make it available in the creative tab.
            if (SHOW_TRAIN_LIGHT_IN_CREATIVE_TAB) {
                ItemStack trainLight = new ItemStack(ModItems.TRAIN_LIGHT_BLOCK.get());
                event.insertAfter(railEnd.copy(), trainLight,
                        CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        }
    }
}
