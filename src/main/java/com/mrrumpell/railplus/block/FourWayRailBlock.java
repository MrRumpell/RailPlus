package com.mrrumpell.railplus.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A four way crossing. There is no single vanilla shape for a crossing, so the shape reported to a
 * cart is simply the axis that cart is already travelling along; carts run straight through.
 */
public class FourWayRailBlock extends BaseRailBlock {
    public static final EnumProperty<RailShape> SHAPE = EnumProperty.create("shape", RailShape.class,
            RailShape.NORTH_SOUTH,
            RailShape.EAST_WEST);

    public FourWayRailBlock(Properties properties) {
        super(false, properties);
        registerDefaultState(stateDefinition.any()
                .setValue(SHAPE, RailShape.NORTH_SOUTH)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends BaseRailBlock> codec() {
        return simpleCodec(FourWayRailBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, WATERLOGGED);
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    protected BlockState updateState(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState().setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    public RailShape getRailDirection(BlockState state, BlockGetter level, BlockPos pos, @Nullable AbstractMinecart cart) {
        if (cart == null) {
            return RailShape.NORTH_SOUTH;
        }

        Vec3 motion = cart.getDeltaMovement();
        if (Math.abs(motion.x) > Math.abs(motion.z)) {
            return RailShape.EAST_WEST;
        }
        if (Math.abs(motion.z) > Math.abs(motion.x)) {
            return RailShape.NORTH_SOUTH;
        }

        double dx = Math.abs(cart.getX() - (pos.getX() + 0.5));
        double dz = Math.abs(cart.getZ() - (pos.getZ() + 0.5));
        return dx > dz ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
    }
}
