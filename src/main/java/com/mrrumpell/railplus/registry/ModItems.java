package com.mrrumpell.railplus.registry;

import com.mrrumpell.railplus.RailPlus;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RailPlus.MOD_ID);

    public static final DeferredItem<BlockItem> DIAGONAL_RAIL =
            ITEMS.registerSimpleBlockItem(ModBlocks.DIAGONAL_RAIL);
    public static final DeferredItem<BlockItem> POWERED_DIAGONAL_RAIL =
            ITEMS.registerSimpleBlockItem(ModBlocks.POWERED_DIAGONAL_RAIL);
    public static final DeferredItem<BlockItem> Y_RAIL =
            ITEMS.registerSimpleBlockItem(ModBlocks.Y_RAIL);
    public static final DeferredItem<BlockItem> FOUR_WAY_RAIL =
            ITEMS.registerSimpleBlockItem(ModBlocks.FOUR_WAY_RAIL);
    public static final DeferredItem<BlockItem> RAIL_END =
            ITEMS.registerSimpleBlockItem(ModBlocks.RAIL_END);
    public static final DeferredItem<BlockItem> TRAIN_LIGHT_BLOCK =
            ITEMS.registerSimpleBlockItem(ModBlocks.TRAIN_LIGHT_BLOCK);

    private ModItems() {
    }
}
