package tamaized.voidscape.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import tamaized.voidscape.Voidscape;
import tamaized.voidscape.capability.SubCapability;
import tamaized.voidscape.registry.ModBlocks;
import tamaized.voidscape.registry.ModSounds;

@SuppressWarnings("deprecation")
public class PortalBlock extends HalfTransparentBlock {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    protected static final VoxelShape X_AABB = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    protected static final VoxelShape Z_AABB = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);

    public PortalBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    @Deprecated
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_AABB : X_AABB;
    }

    @Override
    @Deprecated
    public VoxelShape getCollisionShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    public boolean tryToCreatePortal(Level level, BlockPos pos) {
        PortalBlock.Size size = this.isPortal(level, pos);
        if (size != null && (level.dimension().location().equals(Level.OVERWORLD.location()) || Voidscape.checkForVoidDimension(level))) {
            size.placePortalBlocks();
            return true;
        } else {
            return false;
        }
    }

    @Nullable
    public PortalBlock.Size isPortal(LevelAccessor world, BlockPos pos) {
        PortalBlock.Size sizeX = new PortalBlock.Size(world, pos, Direction.Axis.X);
        if (sizeX.isValid() && sizeX.portalBlockCount == 0) {
            return sizeX;
        } else {
            PortalBlock.Size sizeZ = new PortalBlock.Size(world, pos, Direction.Axis.Z);
            return sizeZ.isValid() && sizeZ.portalBlockCount == 0 ? sizeZ : null;
        }
    }

    @Override
    @Deprecated
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
        Direction.Axis directionAxis = facing.getAxis();
        Direction.Axis directionAxis1 = stateIn.getValue(AXIS);
        boolean flag = directionAxis1 != directionAxis && directionAxis.isHorizontal();
        return !flag && facingState.getBlock() != this && !(new PortalBlock.Size(worldIn, currentPos, directionAxis1)).canCreatePortal() ? Blocks.AIR.defaultBlockState() : super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    @Override
    @Deprecated
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (!entity.isPassenger() && !entity.isVehicle() && entity.canChangeDimensions()) {
            entity.getCapability(SubCapability.CAPABILITY).ifPresent(cap -> cap.get(Voidscape.subCapInsanity).ifPresent(data -> data.setInPortal(true)));
        }
    }

    @Override
    public void animateTick(BlockState stateIn, Level worldIn, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(100) == 0) {
            worldIn.playLocalSound((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, ModSounds.PORTAL.get(), SoundSource.BLOCKS, 1F, 1F, false);
        }

        double x = (float) pos.getX() + rand.nextFloat();
        double y = (float) pos.getY() + rand.nextFloat();
        double z = (float) pos.getZ() + rand.nextFloat();
        double sX = ((double) rand.nextFloat() - 0.5D) * 0.25D;
        double sY = ((double) rand.nextFloat() - 0.5D) * 0.25D;
        double sZ = ((double) rand.nextFloat() - 0.5D) * 0.25D;

        worldIn.addParticle(ParticleTypes.END_ROD, x, y, z, sX, sY, sZ);
    }

    @Override
    @Deprecated
    public BlockState rotate(BlockState state, Rotation rot) {
        return switch (rot) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> switch (state.getValue(AXIS)) {
                case Z -> state.setValue(AXIS, Direction.Axis.X);
                case X -> state.setValue(AXIS, Direction.Axis.Z);
                default -> state;
            };
            default -> state;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    public static class Size {
        private final LevelAccessor world;
        private final Direction.Axis axis;
        private final Direction rightDir;
        private int portalBlockCount;
        private BlockPos bottomLeft;
        private int height;
        private int width;
        private static final StatePredicate FRAME_TEST = (state, reader, pos) -> state.is(ModBlocks.VOIDIC_CRYSTAL_BLOCK.get()) || state.is(ModBlocks.FRAGILE_VOIDIC_CRYSTAL_BLOCK.get());
        private final Block PORTAL = ModBlocks.PORTAL.get();

        public Size(LevelAccessor worldIn, BlockPos pos, Direction.Axis facing) {
            world = worldIn;
            axis = facing;
            rightDir = facing == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
            bottomLeft = calculateBottomLeft(pos);
            if (bottomLeft == null) {
                bottomLeft = pos;
                width = 1;
                height = 1;
            } else {
                width = calculatePortalWidth();
                if (width > 0) {
                    height = calculatePortalHeight();
                }
            }
        }

        @Nullable
        private BlockPos calculateBottomLeft(BlockPos pos) {
            int i = Math.max(0, pos.getY() - 21);
            while (pos.getY() > i && isEmptyBlock(this.world.getBlockState(pos.below()))) {
                pos = pos.below();
            }

            Direction direction = this.rightDir.getOpposite();
            int j = this.getDistanceUntilEdge(pos, direction) - 1;
            return j < 0 ? null : pos.relative(direction, j);
        }

        private int getDistanceUntilEdge(BlockPos pos, Direction facing) {
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

            for (int i = 0; i < 22; ++i) {
                mutable.set(pos).move(facing, i);
                BlockState state = this.world.getBlockState(mutable);

                if (!isEmptyBlock(state)) {
                    if (FRAME_TEST.test(state, world, mutable)) {
                        return i;
                    }
                    break;
                }

                BlockState state1 = this.world.getBlockState(mutable.move(Direction.DOWN));
                if (!FRAME_TEST.test(state1, world, mutable)) {
                    break;
                }
            }

            return 0;
        }

        private int calculatePortalWidth() {
            int dist = this.getDistanceUntilEdge(this.bottomLeft, this.rightDir);
            return dist >= 2 && dist <= 21 ? dist : 0;
        }

        private int calculatePortalHeight() {
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            int dist = this.getDistanceUntilTop(mutable);
            return dist >= 3 && dist <= 21 && hasTopFrame(mutable, dist) ? dist : 0;
        }

        private int getDistanceUntilTop(BlockPos.MutableBlockPos mutable) {
            for (int i = 0; i < 21; ++i) {
                mutable.set(bottomLeft).move(Direction.UP, i).move(rightDir, -1);
                if (!FRAME_TEST.test(this.world.getBlockState(mutable), this.world, mutable)) {
                    return i;
                }

                mutable.set(bottomLeft).move(Direction.UP, i).move(rightDir, width);
                if (!FRAME_TEST.test(this.world.getBlockState(mutable), this.world, mutable)) {
                    return i;
                }


                for (int j = 0; j < width; ++j) {
                    mutable.set(bottomLeft).move(Direction.UP, i).move(rightDir, j);
                    BlockState blockstate = world.getBlockState(mutable);

                    if (!isEmptyBlock(blockstate)) {
                        return i;
                    }

                    if (blockstate.getBlock() == PORTAL) {
                        ++this.portalBlockCount;
                    }
                }
            }

            return 21;
        }

        private boolean hasTopFrame(BlockPos.MutableBlockPos mutable, int offset) {
            for (int i = 0; i < this.width; i++) {
                BlockPos.MutableBlockPos mutablepos = mutable.set(bottomLeft).move(Direction.UP, offset).move(rightDir, i);
                if (!FRAME_TEST.test(this.world.getBlockState(mutablepos), world, mutablepos)) {
                    return false;
                }
            }

            return true;
        }

        private boolean isEmptyBlock(BlockState state) {
            Block block = state.getBlock();
            return state.isAir() || block == PORTAL;
        }

        public boolean isValid() {
            return bottomLeft != null && width >= 2 && width <= 21 && height >= 3 && this.height <= 21;
        }

        public void placePortalBlocks() {
            BlockState state = PORTAL.defaultBlockState().setValue(PortalBlock.AXIS, this.axis);
            BlockPos.betweenClosed(bottomLeft, bottomLeft.relative(Direction.UP, height - 1).relative(rightDir, width - 1)).forEach((pos) -> this.world.setBlock(pos, state, 18));
        }

        public boolean canCreatePortal() {
            return this.isValid() && this.isLargeEnough();
        }

        private boolean isLargeEnough() {
            return this.portalBlockCount == this.width * this.height;
        }
    }

}
