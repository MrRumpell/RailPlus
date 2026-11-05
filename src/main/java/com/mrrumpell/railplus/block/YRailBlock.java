package com.mrrumpell.railplus.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A T junction: the stem runs into a straight track, with one branch leaving to the side.
 * <p>
 * facing is the direction the rail is placed against, which is the direction the model is built for;
 * the stem is its opposite. left selects which side the branch leaves on: false is clockwise from
 * the stem, true is counter-clockwise. Redstone only changes the route of a cart entering from the
 * stem: unpowered it carries on straight, powered it turns onto the branch. Carts entering from the
 * straight or the branch always leave through the stem.
 */
public class YRailBlock extends BaseRailBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<RailShape> SHAPE = EnumProperty.create("shape", RailShape.class,
            RailShape.NORTH_EAST,
            RailShape.NORTH_WEST,
            RailShape.SOUTH_EAST,
            RailShape.SOUTH_WEST,
            RailShape.NORTH_SOUTH,
            RailShape.EAST_WEST);

    public YRailBlock(Properties properties) {
        super(false, properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LEFT, false)
                .setValue(POWERED, false)
                .setValue(SHAPE, RailShape.NORTH_SOUTH)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends BaseRailBlock> codec() {
        return simpleCodec(YRailBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEFT, POWERED, SHAPE, WATERLOGGED);
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
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction stem = detectStem(context);
        Direction facing = stem.getOpposite();

        boolean counterClockwiseRail = hasRail(level, pos.relative(stem.getCounterClockWise()));
        boolean clockwiseRail = hasRail(level, pos.relative(stem.getClockWise()));

        boolean branchOnLeft;
        if (counterClockwiseRail != clockwiseRail) {
            branchOnLeft = counterClockwiseRail;
        } else {
            // The model is mirrored relative to the physics LEFT value, so invert the
            // free-choice side here to keep sneak placement on the same visual side.
            branchOnLeft = context.getPlayer() == null || !context.getPlayer().isShiftKeyDown();
        }

        FluidState fluid = level.getFluidState(pos);
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(LEFT, branchOnLeft)
                .setValue(POWERED, level.hasNeighborSignal(pos))
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    /**
     * Picks the stem as the end of the connected straight track that is nearest the player. When the
     * surrounding track does not make the axis obvious, the side the player is standing on wins.
     * The stored facing is the opposite of this, so it keeps matching the exported model names.
     */
    private static Direction detectStem(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        Direction towardPlayer = context.getHorizontalDirection().getOpposite();

        boolean north = hasRail(level, pos.north());
        boolean south = hasRail(level, pos.south());
        boolean east = hasRail(level, pos.east());
        boolean west = hasRail(level, pos.west());

        boolean northSouth = north && south;
        boolean eastWest = east && west;

        if (northSouth && !eastWest && context.getPlayer() != null) {
            return context.getPlayer().getZ() < pos.getZ() ? Direction.NORTH : Direction.SOUTH;
        }
        if (eastWest && !northSouth && context.getPlayer() != null) {
            return context.getPlayer().getX() < pos.getX() ? Direction.WEST : Direction.EAST;
        }
        return towardPlayer;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && !oldState.is(this)) {
            updateBranchSide(level, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide) {
            return;
        }

        BlockState current = level.getBlockState(pos);
        if (!current.is(this)) {
            return;
        }

        boolean powered = level.hasNeighborSignal(pos);
        if (powered != current.getValue(POWERED)) {
            level.setBlock(pos, current.setValue(POWERED, powered), Block.UPDATE_ALL);
        }

        updateBranchSide(level, pos);
    }

    /** Keeps the rendered branch on the same side as the rail that is actually connected to it. */
    private void updateBranchSide(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) {
            return;
        }

        Direction stem = state.getValue(FACING).getOpposite();
        boolean counterClockwiseRail = hasRail(level, pos.relative(stem.getCounterClockWise()));
        boolean clockwiseRail = hasRail(level, pos.relative(stem.getClockWise()));

        // With rails on both sides or neither side there is nothing unambiguous to follow.
        if (counterClockwiseRail == clockwiseRail) {
            return;
        }

        boolean branchOnLeft = counterClockwiseRail;
        if (state.getValue(LEFT) != branchOnLeft) {
            level.setBlock(pos, state.setValue(LEFT, branchOnLeft), Block.UPDATE_ALL);
        }
    }

    private static boolean hasRail(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.RAILS);
    }

    /**
     * Chooses the route from the cart's position and motion. A cart on the branch side has to use the
     * curve, a cart entering from the stem follows it only while the rail is powered, and everything
     * else runs straight through.
     */
    @Override
    public RailShape getRailDirection(BlockState state, BlockGetter level, BlockPos pos, @Nullable AbstractMinecart cart) {
        Direction stem = state.getValue(FACING).getOpposite();
        Direction branch = state.getValue(LEFT) ? stem.getCounterClockWise() : stem.getClockWise();
        Direction straight = stem.getOpposite();

        if (cart == null) {
            return shapeOf(stem, straight);
        }

        Vec3 motion = cart.getDeltaMovement();
        Vec3 offset = cart.position().subtract(Vec3.atCenterOf(pos));

        double alongMotion = motion.x * stem.getStepX() + motion.z * stem.getStepZ();
        double sideMotion = motion.x * branch.getStepX() + motion.z * branch.getStepZ();
        double along = offset.x * stem.getStepX() + offset.z * stem.getStepZ();
        double side = offset.x * branch.getStepX() + offset.z * branch.getStepZ();

        boolean onBranchSide = Math.abs(side) > Math.abs(along) + 1.0E-4;
        boolean movingAlongBranch = Math.abs(sideMotion) > Math.abs(alongMotion) + 1.0E-4;
        if (onBranchSide || movingAlongBranch) {
            return curveOf(stem, branch);
        }

        // The stem points away from the junction, so entering from the stem means moving against it
        // while still on the stem side of the block. A cart coming from the straight side is already
        // on the other half and must never be diverted, powered or not.
        boolean enteringFromStem = alongMotion < -1.0E-4 && along > 0.01;
        if (state.getValue(POWERED) && enteringFromStem) {
            return curveOf(stem, branch);
        }

        return shapeOf(stem, straight);
    }

    private static RailShape shapeOf(Direction a, Direction b) {
        return a.getAxis() == Direction.Axis.Z && b.getAxis() == Direction.Axis.Z
                ? RailShape.NORTH_SOUTH
                : RailShape.EAST_WEST;
    }

    private static RailShape curveOf(Direction a, Direction b) {
        boolean north = a == Direction.NORTH || b == Direction.NORTH;
        boolean south = a == Direction.SOUTH || b == Direction.SOUTH;
        boolean east = a == Direction.EAST || b == Direction.EAST;
        boolean west = a == Direction.WEST || b == Direction.WEST;

        if (north && east) {
            return RailShape.NORTH_EAST;
        }
        if (north && west) {
            return RailShape.NORTH_WEST;
        }
        if (south && east) {
            return RailShape.SOUTH_EAST;
        }
        if (south && west) {
            return RailShape.SOUTH_WEST;
        }
        return RailShape.NORTH_EAST;
    }
}
