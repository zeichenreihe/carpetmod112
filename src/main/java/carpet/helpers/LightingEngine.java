package carpet.helpers;

/*
 * Copyright PhiPro
 */

import carpet.mixin.accessors.DirectionAccessor;
import carpet.utils.extensions.NewLightChunk;

import net.minecraft.block.state.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.Deque;

public class LightingEngine {
	private static final int MAX_SCHEDULED_COUNT = 1 << 22;

	private static final int MAX_LIGHT = 15;

	private static final Logger logger = LogManager.getLogger();
	//Layout parameters
	//Length of bit segments
	private static final int lX = 26, lY = 8, lZ = 26, lL = 4;
	//Bit segment shifts/positions
	private static final int sZ = 0, sX = sZ + lZ, sY = sX + lX, sL = sY + lY;
	//Bit segment masks
	private static final long mX = (1L << lX) - 1, mY = (1L << lY) - 1, mZ = (1L << lZ) - 1, mL = (1L << lL) - 1, mPos = (mY << sY) | (mX << sX) | (mZ << sZ);
	//Bit to check whether y had overflow
	private static final long yCheck = 1L << (sY + lY);
	private static final long[] neighborShifts = new long[6];
	//Mask to extract chunk idenitfier
	private static final long mChunk = ((mX >> 4) << (4 + sX)) | ((mZ >> 4) << (4 + sZ));
	private static final int CACHED_QUEUE_SEGMENTS_COUNT = 1 << 12;
	private static final int QUEUE_SEGMENT_SIZE = 1 << 10;

	static {
		for (int i = 0; i < 6; ++i) {
			final Vec3i offset = ((DirectionAccessor) (Object) DirectionAccessor.getValues()[i]).getVector();
			neighborShifts[i] = ((long) offset.getY() << sY) | ((long) offset.getX() << sX) | ((long) offset.getZ() << sZ);
		}
	}

	private final World world;
	private final Profiler profiler;
	//Layout of longs: [padding(4)] [y(8)] [x(26)] [z(26)]
	private final PooledLongQueue[] queuedLightUpdates = new PooledLongQueue[LightType.values().length];
	//Layout of longs: see above
	private final PooledLongQueue[] queuedDarkenings = new PooledLongQueue[MAX_LIGHT + 1];
	private final PooledLongQueue[] queuedBrightenings = new PooledLongQueue[MAX_LIGHT + 1];
	//Layout of longs: [newLight(4)] [pos(60)]
	private final PooledLongQueue initialBrightenings = new PooledLongQueue();
	//Layout of longs: [padding(4)] [pos(60)]
	private final PooledLongQueue initialDarkenings = new PooledLongQueue();
	//Iteration state data
	//Cache position to avoid allocation of new object each time
	private final Mutable curPos = new Mutable();
	private final WorldChunk[] neighborsChunk = new WorldChunk[6];
	private final Mutable[] neighborsPos = new Mutable[6];
	private final long[] neighborsLongPos = new long[6];
	private final int[] neighborsLight = new int[6];
	private final Deque<PooledLongQueueSegment> segmentPool = new ArrayDeque<PooledLongQueueSegment>();
	private boolean updating = false;
	//Stored light type to reduce amount of method parameters
	private LightType lightType;
	private PooledLongQueue curQueue;
	private WorldChunk curChunk;
	private long curChunkIdentifier;
	private long curData;
	//Cached data about neighboring blocks (of tempPos)
	private boolean isNeighborDataValid = false;

	public LightingEngine(final World world) {
		this.world = world;
		this.profiler = world.profiler;

		for (int i = 0; i < LightType.values().length; ++i) {
			this.queuedLightUpdates[i] = new PooledLongQueue();
		}

		for (int i = 0; i < this.queuedDarkenings.length; ++i) {
			this.queuedDarkenings[i] = new PooledLongQueue();
		}

		for (int i = 0; i < this.queuedBrightenings.length; ++i) {
			this.queuedBrightenings[i] = new PooledLongQueue();
		}

		for (int i = 0; i < this.neighborsPos.length; ++i) {
			this.neighborsPos[i] = new Mutable();
		}
	}

	private static Mutable longToPos(final Mutable pos, final long longPos) {
		final int posX = (int) (longPos >> sX & mX) - (1 << lX - 1);
		final int posY = (int) (longPos >> sY & mY);
		final int posZ = (int) (longPos >> sZ & mZ) - (1 << lZ - 1);
		return pos.set(posX, posY, posZ);
	}

	private static long posToLong(final BlockPos pos) {
		return posToLong(pos.getX(), pos.getY(), pos.getZ());
	}

	private static long posToLong(final long x, final long y, final long z) {
		return (y << sY) | (x + (1 << lX - 1) << sX) | (z + (1 << lZ - 1) << sZ);
	}

	private static BlockState posToState(final BlockPos pos, final WorldChunk chunk) {
		return chunk.getBlockState(pos.getX(), pos.getY(), pos.getZ());
	}

	/**
	 * Schedules a light update for the specified light type and position to be processed later by {@link #procLightUpdates(LightType)}
	 */
	public void scheduleLightUpdate(final LightType lightType, final BlockPos pos) {
		this.scheduleLightUpdate(lightType, posToLong(pos));
	}

	/**
	 * Schedules a light update for the specified light type and position to be processed later by {@link #procLightUpdates()}
	 */
	private void scheduleLightUpdate(final LightType lightType, final long pos) {
		final PooledLongQueue queue = this.queuedLightUpdates[lightType.ordinal()];

		queue.add(pos);

		//make sure there are not too many queued light updates
		if (queue.size() >= MAX_SCHEDULED_COUNT) {
			this.procLightUpdates(lightType);
		}
	}

	/**
	 * Calls {@link #procLightUpdates(LightType)} for both light types
	 */
	public void procLightUpdates() {
		this.procLightUpdates(LightType.SKY);
		this.procLightUpdates(LightType.BLOCK);
	}

	/**
	 * Processes light updates of the given light type
	 */
	public void procLightUpdates(final LightType lightType) {
		final PooledLongQueue queue = this.queuedLightUpdates[lightType.ordinal()];

		if (queue.isEmpty()) {
			return;
		}

		//avoid nested calls
		if (this.updating) {
			logger.warn("Trying to access light values during relighting");
			return;
		}

		this.updating = true;
		this.curChunkIdentifier = -1; //reset chunk cache

		this.profiler.push("lighting");

		this.lightType = lightType;

		this.profiler.push("checking");

		//process the queued updates and enqueue them for further processing
		for (this.curQueue = queue; this.nextItem(); ) {
			if (this.curChunk == null) {
				continue;
			}

			final int oldLight = this.curToCachedLight();
			final int newLight = this.calcNewLightFromCur();

			if (oldLight < newLight) {
				//don't enqueue directly for brightening in order to avoid duplicate scheduling
				this.initialBrightenings.add(((long) newLight << sL) | this.curData);
			} else if (oldLight > newLight) {
				//don't enqueue directly for darkening in order to avoid duplicate scheduling
				this.initialDarkenings.add(this.curData);
			}
		}

		for (this.curQueue = this.initialBrightenings; this.nextItem(); ) {
			final int newLight = (int) (this.curData >> sL & mL);

			if (newLight > this.curToCachedLight()) {
				//Sets the light to newLight to only schedule once. Clear leading bits of curData for later
				this.enqueueBrightening(this.curPos, this.curData & mPos, newLight, this.curChunk);
			}
		}

		for (this.curQueue = this.initialDarkenings; this.nextItem(); ) {
			final int oldLight = this.curToCachedLight();

			if (oldLight != 0) {
				//Sets the light to 0 to only schedule once
				this.enqueueDarkening(this.curPos, this.curData, oldLight, this.curChunk);
			}
		}

		this.profiler.pop();

		//Iterate through enqueued updates (brightening and darkening in parallel) from brightest to darkest so that we only need to iterate once
		for (int curLight = MAX_LIGHT; curLight >= 0; --curLight) {
			this.profiler.push("darkening");

			for (this.curQueue = this.queuedDarkenings[curLight]; this.nextItem(); ) {
				if (this.curToCachedLight() >= curLight) //don't darken if we got brighter due to some other change
				{
					continue;
				}

				final BlockState state = this.curToState();
				final int luminosity = this.curToLuminosity(state);
				final int opacity = luminosity >= MAX_LIGHT - 1 ? 1 : this.curToOpac(state); //if luminosity is high enough, opacity is irrelevant

				//only darken neighbors if we indeed became darker
				if (this.calcNewLightFromCur(luminosity, opacity) < curLight) {
					//need to calculate new light value from neighbors IGNORING neighbors which are scheduled for darkening
					int newLight = luminosity;

					this.fetchNeighborDataFromCur();

					for (int i = 0; i < 6; ++i) {
						final WorldChunk nChunk = this.neighborsChunk[i];

						if (nChunk == null) {
							LightingHooks.flagSecBoundaryForUpdate(this.curChunk,
									this.curPos,
									this.lightType,
									DirectionAccessor.getValues()[i],
									LightingHooks.EnumBoundaryFacing.OUT
							);
							continue;
						}

						final int nLight = this.neighborsLight[i];

						if (nLight == 0) {
							continue;
						}

						final Mutable nPos = this.neighborsPos[i];

						if (curLight - this.posToOpac(nPos, posToState(nPos, nChunk)) >= nLight) //schedule neighbor for darkening if we possibly light it
						{
							this.enqueueDarkening(nPos, this.neighborsLongPos[i], nLight, nChunk);
						} else //only use for new light calculation if not
						{
							//if we can't darken the neighbor, no one else can (because of processing order) -> safe to let us be illuminated by it
							newLight = Math.max(newLight, nLight - opacity);
						}
					}

					//schedule brightening since light level was set to 0
					this.enqueueBrighteningFromCur(newLight);
				} else //we didn't become darker, so we need to re-set our initial light value (was set to 0) and notify neighbors
				{
					this.enqueueBrighteningFromCur(curLight); //do not spread to neighbors immediately to avoid scheduling multiple times
				}
			}

			this.profiler.swap("brightening");

			for (this.curQueue = this.queuedBrightenings[curLight]; this.nextItem(); ) {
				final int oldLight = this.curToCachedLight();

				if (oldLight == curLight) //only process this if nothing else has happened at this position since scheduling
				{
					this.world.onLightChanged(this.curPos);

					if (curLight > 1) {
						this.spreadLightFromCur(curLight);
					}
				}
			}

			this.profiler.pop();
		}

		this.profiler.pop();

		this.updating = false;
	}

	/**
	 * Gets data for neighbors of <code>curPos</code> and saves the results into neighbor state data members. If a neighbor can't be accessed/doesn't exist, the
	 * corresponding entry in <code>neighborChunks</code> is <code>null</code> - others are not reset
	 */
	private void fetchNeighborDataFromCur() {
		//only update if curPos was changed
		if (this.isNeighborDataValid) {
			return;
		}

		this.isNeighborDataValid = true;

		for (int i = 0; i < 6; ++i) {
			final long nLongPos = this.neighborsLongPos[i] = this.curData + neighborShifts[i];

			if ((nLongPos & yCheck) != 0) {
				this.neighborsChunk[i] = null;
				continue;
			}

			final Mutable nPos = longToPos(this.neighborsPos[i], nLongPos);

			final long nChunkIdentifier = nLongPos & mChunk;

			final WorldChunk nChunk = this.neighborsChunk[i] = nChunkIdentifier == this.curChunkIdentifier ? this.curChunk : this.posToChunk(nPos);

			if (nChunk != null) {
				this.neighborsLight[i] = this.posToCachedLight(nPos, nChunk);
			}
		}
	}

	private int calcNewLightFromCur() {
		final BlockState state = this.curToState();
		final int luminosity = this.curToLuminosity(state);

		return this.calcNewLightFromCur(luminosity, luminosity >= MAX_LIGHT - 1 ? 1 : this.curToOpac(state));
	}

	private int calcNewLightFromCur(final int luminosity, final int opacity) {
		if (luminosity >= MAX_LIGHT - opacity) {
			return luminosity;
		}

		int newLight = luminosity;
		this.fetchNeighborDataFromCur();

		for (int i = 0; i < 6; ++i) {
			if (this.neighborsChunk[i] == null) {
				LightingHooks.flagSecBoundaryForUpdate(this.curChunk,
						this.curPos,
						this.lightType,
						DirectionAccessor.getValues()[i],
						LightingHooks.EnumBoundaryFacing.IN
				);
				continue;
			}

			final int nLight = this.neighborsLight[i];

			newLight = Math.max(nLight - opacity, newLight);
		}

		return newLight;
	}

	private void spreadLightFromCur(final int curLight) {
		this.fetchNeighborDataFromCur();

		for (int i = 0; i < 6; ++i) {
			final Mutable nPos = this.neighborsPos[i];

			final WorldChunk nChunk = this.neighborsChunk[i];

			if (nChunk == null) {
				LightingHooks.flagSecBoundaryForUpdate(this.curChunk,
						this.curPos,
						this.lightType,
						DirectionAccessor.getValues()[i],
						LightingHooks.EnumBoundaryFacing.OUT
				);
				continue;
			}

			final int newLight = curLight - this.posToOpac(nPos, nChunk.getBlockState(nPos));

			if (newLight > this.neighborsLight[i]) {
				this.enqueueBrightening(nPos, this.neighborsLongPos[i], newLight, nChunk);
			}
		}
	}

	private void enqueueBrighteningFromCur(final int newLight) {
		this.enqueueBrightening(this.curPos, this.curData, newLight, this.curChunk);
	}

	/**
	 * Enqueues the pos for brightening and sets its light value to <code>newLight</code>
	 */
	private void enqueueBrightening(final BlockPos pos, final long longPos, final int newLight, final WorldChunk chunk) {
		this.queuedBrightenings[newLight].add(longPos);
		chunk.setLight(this.lightType, pos, newLight);
	}

	/**
	 * Enqueues the pos for darkening and sets its light value to 0
	 */
	private void enqueueDarkening(final BlockPos pos, final long longPos, final int oldLight, final WorldChunk chunk) {
		this.queuedDarkenings[oldLight].add(longPos);
		chunk.setLight(this.lightType, pos, 0);
	}

	/**
	 * Polls a new item from <code>curQueue</code> and fills in state data members
	 *
	 * @return If there was an item to poll
	 */
	private boolean nextItem() {
		if (this.curQueue.isEmpty()) {
			return false;
		}

		this.curData = this.curQueue.poll();
		this.isNeighborDataValid = false;
		longToPos(this.curPos, this.curData);

		final long chunkIdentifier = this.curData & mChunk;

		if (this.curChunkIdentifier != chunkIdentifier) {
			this.curChunk = this.curToChunk();
			this.curChunkIdentifier = chunkIdentifier;
		}

		return true;
	}

	private int posToCachedLight(final Mutable pos, final WorldChunk chunk) {
		return ((NewLightChunk) chunk).getCachedLightFor(this.lightType, pos);
	}

	private int curToCachedLight() {
		return this.posToCachedLight(this.curPos, this.curChunk);
	}

	/**
	 * Calculates the luminosity for <code>curPos</code>, taking into account <code>lightType</code>
	 */
	private int curToLuminosity(final BlockState state) {
		if (this.lightType == LightType.SKY) {
			return this.curChunk.hasSkyAccess(this.curPos) ? LightType.SKY.defaultValue : 0;
		}

		return MathHelper.clamp(state.getLightLevel(), 0, MAX_LIGHT);
	}

	private int curToOpac(final BlockState state) {
		return this.posToOpac(this.curPos, state);
	}

	private int posToOpac(final BlockPos pos, final BlockState state) {
		return MathHelper.clamp(state.getOpacity(), 1, MAX_LIGHT);
	}

	//PooledLongQueue code
	//Implement own queue with pooled segments to reduce allocation costs and reduce idle memory footprint

	private BlockState curToState() {
		return posToState(this.curPos, this.curChunk);
	}

	private WorldChunk posToChunk(final BlockPos pos) {
		return this.world.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4);
	}

	private WorldChunk curToChunk() {
		return this.posToChunk(this.curPos);
	}

	private PooledLongQueueSegment getLongQueueSegment() {
		if (this.segmentPool.isEmpty()) {
			return new PooledLongQueueSegment();
		}

		return this.segmentPool.pop();
	}

	private class PooledLongQueueSegment {
		private final long[] longArray = new long[QUEUE_SEGMENT_SIZE];
		private int index = 0;
		private PooledLongQueueSegment next;

		private void release() {
			this.index = 0;
			this.next = null;

			if (LightingEngine.this.segmentPool.size() < CACHED_QUEUE_SEGMENTS_COUNT) {
				LightingEngine.this.segmentPool.push(this);
			}
		}

		PooledLongQueueSegment add(final long val) {
			PooledLongQueueSegment ret = this;

			if (this.index == QUEUE_SEGMENT_SIZE) {
				ret = this.next = LightingEngine.this.getLongQueueSegment();
			}

			ret.longArray[ret.index++] = val;
			return ret;
		}
	}

	private class PooledLongQueue {
		private PooledLongQueueSegment cur, last;
		private int size = 0;

		private int index = 0;

		int size() {
			return this.size;
		}

		boolean isEmpty() {
			return this.cur == null;
		}

		void add(final long val) {
			if (this.cur == null) {
				this.cur = this.last = LightingEngine.this.getLongQueueSegment();
			}

			this.last = this.last.add(val);
			++this.size;
		}

		long poll() {
			final long ret = this.cur.longArray[this.index++];
			--this.size;

			if (this.index == this.cur.index) {
				this.index = 0;
				final PooledLongQueueSegment next = this.cur.next;
				this.cur.release();
				this.cur = next;
			}

			return ret;
		}
	}
}