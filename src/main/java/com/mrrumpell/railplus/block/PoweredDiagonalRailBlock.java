package com.mrrumpell.railplus.block;

import com.mojang.serialization.MapCodec;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.context.BlockPlaceContext;
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

/**
 * A powered diagonal rail. It uses the same four 45 degree shapes and placement rules as the
 * normal diagonal rail, but its POWERED state means "give carts a boost" rather than "swing to the
 * other branch".
 * <p>
 * NeoForge calls {@link BaseRailBlock#onMinecartPass(BlockState, Level, BlockPos, AbstractMinecart)}
 * for custom rail blocks, so this does not need a Mixin. Power propagates along connected diagonal
 * segments for up to eight blocks, matching the range of a vanilla powered rail.
 */
public class PoweredDiagonalRailBlock extends BaseRailBlock {
    public static final EnumProperty<RailShape> SHAPE = EnumProperty.create("shape", RailShape.class,
            RailShape.NORTH_EAST,
            RailShape.NORTH_WEST,
            RailShape.SOUTH_EAST,
            RailShape.SOUTH_WEST);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final int MAX_POWER_DISTANCE = 8;
    private static final double BOOST = 0.06D;
    private static final double LAUNCH_SPEED = 0.02D;

    public PoweredDiagonalRailBlock(Properties properties) {
        super(false, properties);
        registerDefaultState(stateDefinition.any()
                .setValue(SHAPE, RailShape.NORTH_EAST)
                .setValue(POWERED, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends BaseRailBlock> codec() {
        return simpleCodec(PoweredDiagonalRailBlock::new);
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
        RailShape shape = DiagonalRailBlock.shapeForPlacement(context);

        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState()
                .setValue(SHAPE, shape)
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()))
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && !oldState.is(this)) {
            updatePower(level, pos);
            // A newly inserted rail can split a diagonal chain. Make the far side
            // recompute even when this rail itself did not change power state.
            notifyRailNeighbours(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            // Removing a rail can leave the rest of a diagonal chain without its power path.
            notifyRailNeighbours(level, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide) {
            updatePower(level, pos);
        }
    }

    /**
     * Recomputes whether this rail is powered, then lets connected diagonal rails recompute as well.
     * Face neighbours normally receive a block update from setBlock; diagonal neighbours do not, so
     * they are notified explicitly.
     */
    private void updatePower(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) {
            return;
        }

        boolean powered = level.hasNeighborSignal(pos)
                || findPower(level, pos, state, new HashSet<>(), 0);
        if (powered == state.getValue(POWERED)) {
            return;
        }

        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        notifyRailNeighbours(level, pos);
    }

    private static boolean findPower(Level level, BlockPos pos, BlockState state,
                                     Set<BlockPos> visited, int distance) {
        if (distance >= MAX_POWER_DISTANCE) {
            return false;
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                BlockPos neighbourPos = pos.offset(dx, 0, dz);
                if (visited.contains(neighbourPos)) {
                    continue;
                }

                BlockState neighbour = level.getBlockState(neighbourPos);
                if (!neighbour.is(state.getBlock()) || !connected(state, pos, neighbour, neighbourPos)) {
                    continue;
                }

                visited.add(neighbourPos);
                if (level.hasNeighborSignal(neighbourPos)
                        || findPower(level, neighbourPos, neighbour, visited, distance + 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean connected(BlockState fromState, BlockPos fromPos,
                                     BlockState toState, BlockPos toPos) {
        int dx = Integer.signum(toPos.getX() - fromPos.getX());
        int dz = Integer.signum(toPos.getZ() - fromPos.getZ());
        RailShape fromShape = fromState.getValue(SHAPE);
        RailShape toShape = toState.getValue(SHAPE);

        if (dx != 0 && dz != 0) {
            return fromShape == toShape && isAlongDiagonal(fromShape, dx, dz);
        }
        if (dx == 0 && dz == 0) {
            return false;
        }

        Direction direction = dx == 1 ? Direction.EAST
                : dx == -1 ? Direction.WEST
                : dz == 1 ? Direction.SOUTH
                : Direction.NORTH;
        return isEnd(fromShape, direction) || isEnd(toShape, direction.getOpposite());
    }

    /** A diagonal chain continues only along the axis its model is built on. */
    private static boolean isAlongDiagonal(RailShape shape, int dx, int dz) {
        return switch (shape) {
            case NORTH_EAST, SOUTH_WEST -> (dx > 0 && dz < 0) || (dx < 0 && dz > 0);
            case NORTH_WEST, SOUTH_EAST -> (dx < 0 && dz < 0) || (dx > 0 && dz > 0);
            default -> false;
        };
    }

    private static boolean isEnd(RailShape shape, Direction direction) {
        Direction[] ends = ends(shape);
        return direction == ends[0] || direction == ends[1];
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

    private void notifyRailNeighbours(Level level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                BlockPos neighbour = pos.offset(dx, 0, dz);
                if (level.getBlockState(neighbour).is(this)) {
                    level.neighborChanged(neighbour, this, pos);
                }
            }
        }
    }

    @Override
    public void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        Vec3 motion = cart.getDeltaMovement();
        if (!state.getValue(POWERED)) {
            // Match the braking behavior of an unpowered vanilla powered rail.
            if (motion.horizontalDistance() < 0.03D) {
                cart.setDeltaMovement(Vec3.ZERO);
            } else {
                cart.setDeltaMovement(motion.multiply(0.5D, 0.0D, 0.5D));
            }
            return;
        }

        double speed = motion.horizontalDistance();
        if (speed > 0.01D) {
            cart.setDeltaMovement(motion.add(
                    motion.x / speed * BOOST,
                    0.0D,
                    motion.z / speed * BOOST));
            return;
        }

        Vec3 launch = launchVelocity(level, pos, state.getValue(SHAPE));
        if (launch != null) {
            cart.setDeltaMovement(launch.x, motion.y, launch.z);
        }
    }

    /**
     * A stationary cart is launched away from a solid block at one end, just like a vanilla powered
     * rail. With solid blocks at both ends, or neither end, it stays put.
     */
    private static Vec3 launchVelocity(Level level, BlockPos pos, RailShape shape) {
        Direction[] ends = ends(shape);
        boolean firstSolid = isRedstoneConductor(level, pos.relative(ends[0]));
        boolean secondSolid = isRedstoneConductor(level, pos.relative(ends[1]));
        if (firstSolid == secondSolid) {
            return null;
        }

        Direction away = firstSolid ? ends[1] : ends[0];
        return new Vec3(away.getStepX() * LAUNCH_SPEED, 0.0D, away.getStepZ() * LAUNCH_SPEED);
    }

    private static boolean isRedstoneConductor(Level level, BlockPos pos) {
        return level.getBlockState(pos).isRedstoneConductor(level, pos);
    }
}
