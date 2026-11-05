package com.mrrumpell.railplus.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * A rail that always renders as one of the four 45 degree segments.
 * <p>
 * The shape property is deliberately limited to the four curve values: vanilla's minecart movement
 * projects a cart onto the straight chord between the two exit points of a curve, and those chords
 * are exactly the 45 degree lines the models are built on.
 * <p>
 * Redstone flips the segment around the end that is attached to a normal rail, so a diagonal next to
 * a straight track can be switched between the two branches that meet at that track. If neither end
 * is attached to a normal rail, the segment stays put.
 */
public class DiagonalRailBlock extends BaseRailBlock {
    public static final EnumProperty<RailShape> SHAPE = EnumProperty.create("shape", RailShape.class,
            RailShape.NORTH_EAST,
            RailShape.NORTH_WEST,
            RailShape.SOUTH_EAST,
            RailShape.SOUTH_WEST);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public DiagonalRailBlock(Properties properties) {
        super(false, properties);
        registerDefaultState(stateDefinition.any()
                .setValue(SHAPE, RailShape.NORTH_EAST)
                .setValue(POWERED, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends BaseRailBlock> codec() {
        return simpleCodec(DiagonalRailBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, POWERED, WATERLOGGED);
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
        RailShape shape = shapeForPlacement(context);

        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState()
                .setValue(SHAPE, shape)
                .setValue(POWERED, hasDirectPower(context.getLevel(), context.getClickedPos()))
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    /**
     * Picks the placement shape. A connected diagonal neighbour fixes the end that must attach to
     * it; the player's facing chooses the free end, and sneaking mirrors that choice. Without a
     * connected diagonal neighbour, the original facing/sneak shape is used.
     */
    static RailShape shapeForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        boolean sneaking = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();

        RailShape facingShape = switch (facing) {
            case NORTH -> RailShape.NORTH_EAST;
            case SOUTH -> RailShape.SOUTH_EAST;
            case EAST -> RailShape.SOUTH_EAST;
            case WEST -> RailShape.NORTH_WEST;
            default -> RailShape.NORTH_EAST;
        };

        Direction attachment = connectedDiagonalEnd(context.getLevel(), context.getClickedPos(), facing);
        if (attachment == null) {
            return sneaking ? mirrored(facingShape, facing) : facingShape;
        }

        RailShape shape = withEnd(facingShape, attachment);
        return sneaking ? mirrored(shape, attachment) : shape;
    }

    /**
     * Returns the side of {@code pos} that must connect to an existing diagonal rail, trying the
     * player's facing direction first and the other horizontal directions afterwards.
     */
    private static Direction connectedDiagonalEnd(Level level, BlockPos pos, Direction facing) {
        for (Direction side : placementOrder(facing)) {
            BlockState neighbour = level.getBlockState(pos.relative(side));
            RailShape neighbourShape = diagonalShape(neighbour);

            if (neighbourShape != null && isEnd(neighbourShape, side.getOpposite())) {
                return side;
            }
        }

        return null;
    }

    private static Direction[] placementOrder(Direction facing) {
        Direction[] order = new Direction[4];
        order[0] = facing;
        int index = 1;

        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (side != facing) {
                order[index++] = side;
            }
        }

        return order;
    }

    private static RailShape diagonalShape(BlockState state) {
        if (state.getBlock() instanceof DiagonalRailBlock) {
            return state.getValue(DiagonalRailBlock.SHAPE);
        }
        if (state.getBlock() instanceof PoweredDiagonalRailBlock) {
            return state.getValue(PoweredDiagonalRailBlock.SHAPE);
        }
        return null;
    }

    /**
     * Returns a shape that contains {@code end}. If {@code shape} already contains it, the shape is
     * unchanged; otherwise the free end is moved to the other side of the block.
     */
    private static RailShape withEnd(RailShape shape, Direction end) {
        if (isEnd(shape, end)) {
            return shape;
        }

        Direction opposite = end.getOpposite();
        for (Direction existing : ends(shape)) {
            if (existing != opposite) {
                return mirrored(shape, existing);
            }
        }

        return shape;
    }

    private static boolean isEnd(RailShape shape, Direction direction) {
        for (Direction end : ends(shape)) {
            if (end == direction) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        // A rail placed into an already powered position starts on the swung branch.
        if (!level.isClientSide && !oldState.is(this) && state.getValue(POWERED)) {
            BlockState current = level.getBlockState(pos);
            if (current.is(this)) {
                level.setBlock(pos, current.setValue(SHAPE, swing(current.getValue(SHAPE), level, pos)), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        // Let vanilla do its support check; it must not touch the shape, which updateState keeps as-is.
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide) {
            return;
        }

        BlockState current = level.getBlockState(pos);
        if (!current.is(this)) {
            return;
        }

        boolean powered = hasDirectPower(level, pos);
        if (powered != current.getValue(POWERED)) {
            RailShape flipped = swing(current.getValue(SHAPE), level, pos);
            level.setBlock(pos, current.setValue(POWERED, powered).setValue(SHAPE, flipped), Block.UPDATE_ALL);
        }
    }

    /**
     * A diagonal only follows a redstone component that is feeding the rail directly, such as dust
     * or a repeater pointing into it. A solid block that merely happens to be powered nearby must not
     * flip it, otherwise one lever switches every diagonal around it.
     */
    private static boolean hasDirectPower(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos sourcePos = pos.relative(direction);
            BlockState source = level.getBlockState(sourcePos);
            if (isRedstoneComponent(source.getBlock())
                    && level.getSignal(sourcePos, direction.getOpposite()) > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRedstoneComponent(Block block) {
        return block instanceof LeverBlock
                || block instanceof ButtonBlock
                || block instanceof RedStoneWireBlock
                || block instanceof DiodeBlock
                || block == Blocks.REDSTONE_BLOCK
                || block == Blocks.REDSTONE_TORCH
                || block == Blocks.REDSTONE_WALL_TORCH
                || block instanceof ObserverBlock
                || block instanceof PressurePlateBlock
                || block instanceof TripWireHookBlock;
    }

    /**
     * Swings the free end of the segment to the other side while the end attached to a normal rail
     * stays put. If neither end is attached to a normal rail, the shape is returned unchanged.
     */
    private static RailShape swing(RailShape shape, Level level, BlockPos pos) {
        for (Direction end : ends(shape)) {
            BlockState neighbour = level.getBlockState(pos.relative(end));
            if (neighbour.is(BlockTags.RAILS) && !(neighbour.getBlock() instanceof DiagonalRailBlock)) {
                return mirrored(shape, end);
            }
        }

        return shape;
    }

    private static Direction[] ends(RailShape shape) {
        return switch (shape) {
            case NORTH_EAST -> new Direction[]{Direction.NORTH, Direction.EAST};
            case NORTH_WEST -> new Direction[]{Direction.NORTH, Direction.WEST};
            case SOUTH_EAST -> new Direction[]{Direction.SOUTH, Direction.EAST};
            case SOUTH_WEST -> new Direction[]{Direction.SOUTH, Direction.WEST};
            default -> new Direction[]{Direction.NORTH, Direction.EAST};
        };
    }

    private static RailShape mirrored(RailShape shape, Direction hinge) {
        return switch (shape) {
            case NORTH_EAST -> hinge == Direction.NORTH ? RailShape.NORTH_WEST : RailShape.SOUTH_EAST;
            case NORTH_WEST -> hinge == Direction.NORTH ? RailShape.NORTH_EAST : RailShape.SOUTH_WEST;
            case SOUTH_EAST -> hinge == Direction.SOUTH ? RailShape.SOUTH_WEST : RailShape.NORTH_EAST;
            case SOUTH_WEST -> hinge == Direction.SOUTH ? RailShape.SOUTH_EAST : RailShape.NORTH_WEST;
            default -> shape;
        };
    }
}
