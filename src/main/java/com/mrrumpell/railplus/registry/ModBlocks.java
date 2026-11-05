package com.mrrumpell.railplus.registry;

import com.mrrumpell.railplus.RailPlus;
import com.mrrumpell.railplus.block.DiagonalRailBlock;
import com.mrrumpell.railplus.block.FourWayRailBlock;
import com.mrrumpell.railplus.block.PoweredDiagonalRailBlock;
import com.mrrumpell.railplus.block.RailEndBlock;
import com.mrrumpell.railplus.block.TrainLightBlock;
import com.mrrumpell.railplus.block.YRailBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RailPlus.MOD_ID);

    public static final DeferredBlock<DiagonalRailBlock> DIAGONAL_RAIL = BLOCKS.registerBlock(
            "diagonal_rail",
            DiagonalRailBlock::new,
            railProperties()
    );

    public static final DeferredBlock<PoweredDiagonalRailBlock> POWERED_DIAGONAL_RAIL = BLOCKS.registerBlock(
            "powered_diagonal_rail",
            PoweredDiagonalRailBlock::new,
            railProperties()
    );

    public static final DeferredBlock<YRailBlock> Y_RAIL = BLOCKS.registerBlock(
            "y_rail",
            YRailBlock::new,
            railProperties()
    );

    public static final DeferredBlock<FourWayRailBlock> FOUR_WAY_RAIL = BLOCKS.registerBlock(
            "four_way_rail",
            FourWayRailBlock::new,
            railProperties()
    );

    public static final DeferredBlock<RailEndBlock> RAIL_END = BLOCKS.registerBlock(
            "rail_end",
            RailEndBlock::new,
            railEndProperties()
    );

    public static final DeferredBlock<TrainLightBlock> TRAIN_LIGHT_BLOCK = BLOCKS.registerBlock(
            "train_light_block",
            TrainLightBlock::new,
            trainLightProperties()
    );

    private static BlockBehaviour.Properties railProperties() {
        return BlockBehaviour.Properties.of()
                .noCollission()
                .strength(0.7F)
                .sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties railEndProperties() {
        return BlockBehaviour.Properties.of()
                .noOcclusion()
                .strength(0.8F)
                .sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties trainLightProperties() {
        return BlockBehaviour.Properties.of()
                .noOcclusion()
                .strength(1.0F)
                .sound(SoundType.WOOD);
    }

    private ModBlocks() {
    }
}
