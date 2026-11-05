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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A buffer stop at the end of a track. The block itself is a straight rail, so carts can run onto
 * it and stop against the visible buffer instead of derailing off the end of the line.
 * <p>
 * facing points from the open rail end toward the buffer. The blockstate rotates the authored
 * model so its visible buffer points along facing.
 */
public class RailEndBlock extends BaseRailBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<RailShape> SHAPE = EnumProperty.create("shape", RailShape.class,
            RailShape.NORTH_SOUTH,
            RailShape.EAST_WEST);

    private static final VoxelShape BUFFER_NORTH = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.0625D);
    private static final VoxelShape BUFFER_SOUTH = Shapes.box(0.0D, 0.0D, 0.9375D, 1.0D, 1.0D, 1.0D);
    private static final VoxelShape BUFFER_EAST = Shapes.box(0.9375D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    private static final VoxelShape BUFFER_WEST = Shapes.box(0.0D, 0.0D, 0.0D, 0.0625D, 1.0D, 1.0D);

    public RailEndBlock(Properties properties) {
        super(false, properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(SHAPE, RailShape.NORTH_SOUTH)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends BaseRailBlock> codec() {
        return simpleCodec(RailEndBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE, WATERLOGGED);
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
        Direction facing = context.getHorizontalDirection();
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(SHAPE, shapeFor(facing))
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    public RailShape getRailDirection(BlockState state, BlockGetter level, BlockPos pos,
                                      @Nullable AbstractMinecart cart) {
        return state.getValue(SHAPE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return bufferShape(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                         CollisionContext context) {
        return bufferShape(state.getValue(FACING));
    }

    private static RailShape shapeFor(Direction facing) {
        return facing.getAxis() == Direction.Axis.Z ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST;
    }

    private static VoxelShape bufferShape(Direction facing) {
        return switch (facing) {
            case NORTH -> BUFFER_NORTH;
            case SOUTH -> BUFFER_SOUTH;
            case EAST -> BUFFER_EAST;
            case WEST -> BUFFER_WEST;
            default -> BUFFER_NORTH;
        };
    }
}
