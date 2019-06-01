package net.minecraft.server;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.shorts.ShortArraySet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class PlayerChunk {

    public static final Either<IChunkAccess, PlayerChunk.Failure> UNLOADED_CHUNK_ACCESS = Either.right(PlayerChunk.Failure.b);
    public static final CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> UNLOADED_CHUNK_ACCESS_FUTURE = CompletableFuture.completedFuture(PlayerChunk.UNLOADED_CHUNK_ACCESS);
    public static final Either<Chunk, PlayerChunk.Failure> UNLOADED_CHUNK = Either.right(PlayerChunk.Failure.b);
    private static final CompletableFuture<Either<Chunk, PlayerChunk.Failure>> UNLOADED_CHUNK_FUTURE = CompletableFuture.completedFuture(PlayerChunk.UNLOADED_CHUNK);
    private static final List<ChunkStatus> CHUNK_STATUSES = ChunkStatus.a();
    private static final PlayerChunk.State[] CHUNK_STATES = PlayerChunk.State.values();
    boolean isUpdateQueued = false; // Paper
    private final AtomicReferenceArray<CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>>> statusFutures;
    private volatile CompletableFuture<Either<Chunk, PlayerChunk.Failure>> fullChunkFuture; private int fullChunkCreateCount; private volatile boolean isFullChunkReady; // Paper - cache chunk ticking stage
    private volatile CompletableFuture<Either<Chunk, PlayerChunk.Failure>> tickingFuture; private volatile boolean isTickingReady; // Paper - cache chunk ticking stage
    private volatile CompletableFuture<Either<Chunk, PlayerChunk.Failure>> entityTickingFuture; private volatile boolean isEntityTickingReady; // Paper - cache chunk ticking stage
    private CompletableFuture<IChunkAccess> chunkSave;
    public int oldTicketLevel;
    private int ticketLevel;
    private int n;
    final ChunkCoordIntPair location; // Paper - private -> package
    private boolean p;
    private final ShortSet[] dirtyBlocks;
    private int r;
    private int s;
    private final LightEngine lightEngine;
    private final PlayerChunk.c u;
    public final PlayerChunk.d players;
    private boolean hasBeenLoaded;
    private boolean x;

    private final PlayerChunkMap chunkMap; // Paper

    public PlayerChunk(ChunkCoordIntPair chunkcoordintpair, int i, LightEngine lightengine, PlayerChunk.c playerchunk_c, PlayerChunk.d playerchunk_d) {
        this.statusFutures = new AtomicReferenceArray(PlayerChunk.CHUNK_STATUSES.size());
        this.fullChunkFuture = PlayerChunk.UNLOADED_CHUNK_FUTURE;
        this.tickingFuture = PlayerChunk.UNLOADED_CHUNK_FUTURE;
        this.entityTickingFuture = PlayerChunk.UNLOADED_CHUNK_FUTURE;
        this.chunkSave = CompletableFuture.completedFuture(null); // CraftBukkit - decompile error
        this.dirtyBlocks = new ShortSet[16];
        this.location = chunkcoordintpair;
        this.lightEngine = lightengine;
        this.u = playerchunk_c;
        this.players = playerchunk_d;
        this.oldTicketLevel = PlayerChunkMap.GOLDEN_TICKET + 1;
        this.ticketLevel = this.oldTicketLevel;
        this.n = this.oldTicketLevel;
        this.a(i);
        this.chunkMap = (PlayerChunkMap)playerchunk_d; // Paper
    }

    // Paper start
    @Nullable
    public final Chunk getEntityTickingChunk() {
        CompletableFuture<Either<Chunk, PlayerChunk.Failure>> completablefuture = this.entityTickingFuture;
        Either<Chunk, PlayerChunk.Failure> either = completablefuture.getNow(null);

        return either == null ? null : either.left().orElse(null);
    }

    @Nullable
    public final Chunk getTickingChunk() {
        CompletableFuture<Either<Chunk, PlayerChunk.Failure>> completablefuture = this.tickingFuture;
        Either<Chunk, PlayerChunk.Failure> either = completablefuture.getNow(null);

        return either == null ? null : either.left().orElse(null);
    }

    @Nullable
    public final Chunk getFullReadyChunk() {
        CompletableFuture<Either<Chunk, PlayerChunk.Failure>> completablefuture = this.fullChunkFuture;
        Either<Chunk, PlayerChunk.Failure> either = completablefuture.getNow(null);

        return either == null ? null : either.left().orElse(null);
    }

    public final boolean isEntityTickingReady() {
        return this.isEntityTickingReady;
    }

    public final boolean isTickingReady() {
        return this.isTickingReady;
    }

    public final boolean isFullChunkReady() {
        return this.isFullChunkReady;
    }
    // Paper end

    // CraftBukkit start
    public final Chunk getFullChunk() { // Paper - final for inline
        if (!getChunkState(this.oldTicketLevel).isAtLeast(PlayerChunk.State.BORDER)) return null; // note: using oldTicketLevel for isLoaded checks
        CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> statusFuture = this.getStatusFutureUnchecked(ChunkStatus.FULL);
        Either<IChunkAccess, PlayerChunk.Failure> either = (Either<IChunkAccess, PlayerChunk.Failure>) statusFuture.getNow(null);
        return either == null ? null : (Chunk) either.left().orElse(null);
    }
    // CraftBukkit end
    // Paper start - "real" get full chunk immediately
    public final Chunk getFullChunkIfCached() {
        // Note: Copied from above without ticket level check
        CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> statusFuture = this.getStatusFutureUnchecked(ChunkStatus.FULL);
        Either<IChunkAccess, PlayerChunk.Failure> either = (Either<IChunkAccess, PlayerChunk.Failure>) statusFuture.getNow(null);
        return either == null ? null : (Chunk) either.left().orElse(null);
    }
    // Paper end

    public CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> getStatusFutureUnchecked(ChunkStatus chunkstatus) {
        CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completablefuture = (CompletableFuture) this.statusFutures.get(chunkstatus.c());

        return completablefuture == null ? PlayerChunk.UNLOADED_CHUNK_ACCESS_FUTURE : completablefuture;
    }

    public CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> b(ChunkStatus chunkstatus) {
        return getChunkStatus(this.ticketLevel).b(chunkstatus) ? this.getStatusFutureUnchecked(chunkstatus) : PlayerChunk.UNLOADED_CHUNK_ACCESS_FUTURE;
    }

    public final CompletableFuture<Either<Chunk, PlayerChunk.Failure>> getTickingFuture() { return this.a(); } // Paper - OBFHELPER
    public final CompletableFuture<Either<Chunk, PlayerChunk.Failure>> a() { // Paper - final for inline
        return this.tickingFuture;
    }

    public final CompletableFuture<Either<Chunk, PlayerChunk.Failure>> getEntityTickingFuture() { return this.b(); } // Paper - OBFHELPER
    public final CompletableFuture<Either<Chunk, PlayerChunk.Failure>> b() { // Paper - final for inline
        return this.entityTickingFuture;
    }

    public final CompletableFuture<Either<Chunk, PlayerChunk.Failure>> getFullChunkFuture() { return this.c(); } // Paper - OBFHELPER
    public final CompletableFuture<Either<Chunk, PlayerChunk.Failure>> c() { // Paper - final for inline
        return this.fullChunkFuture;
    }

    @Nullable
    public final Chunk getChunk() { // Paper - final for inline
        CompletableFuture<Either<Chunk, PlayerChunk.Failure>> completablefuture = this.a();
        Either<Chunk, PlayerChunk.Failure> either = (Either) completablefuture.getNow(null); // CraftBukkit - decompile error

        return either == null ? null : (Chunk) either.left().orElse(null); // CraftBukkit - decompile error
    }

    @Nullable
    public IChunkAccess f() {
        for (int i = PlayerChunk.CHUNK_STATUSES.size() - 1; i >= 0; --i) {
            ChunkStatus chunkstatus = (ChunkStatus) PlayerChunk.CHUNK_STATUSES.get(i);
            CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completablefuture = this.getStatusFutureUnchecked(chunkstatus);

            if (!completablefuture.isCompletedExceptionally()) {
                Optional<IChunkAccess> optional = ((Either) completablefuture.getNow(PlayerChunk.UNLOADED_CHUNK_ACCESS)).left();

                if (optional.isPresent()) {
                    return (IChunkAccess) optional.get();
                }
            }
        }

        return null;
    }

    public final CompletableFuture<IChunkAccess> getChunkSave() { // Paper - final for inline
        return this.chunkSave;
    }

    public void a(BlockPosition blockposition) {
        Chunk chunk = this.getChunk();

        if (chunk != null) {
            byte b0 = (byte) SectionPosition.a(blockposition.getY());

            if (b0 >= this.dirtyBlocks.length) return; // CraftBukkit - SPIGOT-6086
            if (this.dirtyBlocks[b0] == null) {
                this.p = true;
                this.dirtyBlocks[b0] = new ShortArraySet();
            }

            this.dirtyBlocks[b0].add(SectionPosition.b(blockposition));
        }
    }

    public void a(EnumSkyBlock enumskyblock, int i) {
        Chunk chunk = this.getChunk();

        if (chunk != null) {
            chunk.setNeedsSaving(true);
            if (enumskyblock == EnumSkyBlock.SKY) {
                this.s |= 1 << i - -1;
            } else {
                this.r |= 1 << i - -1;
            }

        }
    }

    public void a(Chunk chunk) {
        if (this.p || this.s != 0 || this.r != 0) {
            World world = chunk.getWorld();
            int i = 0;

            int j;

            for (j = 0; j < this.dirtyBlocks.length; ++j) {
                i += this.dirtyBlocks[j] != null ? this.dirtyBlocks[j].size() : 0;
            }

            this.x |= i >= 64;
            if (this.s != 0 || this.r != 0) {
                this.a(new PacketPlayOutLightUpdate(chunk.getPos(), this.lightEngine, this.s, this.r, true), !this.x);
                this.s = 0;
                this.r = 0;
            }

            for (j = 0; j < this.dirtyBlocks.length; ++j) {
                ShortSet shortset = this.dirtyBlocks[j];

                if (shortset != null) {
                    SectionPosition sectionposition = SectionPosition.a(chunk.getPos(), j);

                    if (shortset.size() == 1) {
                        BlockPosition blockposition = sectionposition.g(shortset.iterator().nextShort());
                        IBlockData iblockdata = world.getType(blockposition);

                        this.a(new PacketPlayOutBlockChange(blockposition, iblockdata), false);
                        this.a(world, blockposition, iblockdata);
                    } else {
                        ChunkSection chunksection = chunk.getSections()[sectionposition.getY()];
                        PacketPlayOutMultiBlockChange packetplayoutmultiblockchange = new PacketPlayOutMultiBlockChange(sectionposition, shortset, chunksection, this.x);

                        this.a(packetplayoutmultiblockchange, false);
                        packetplayoutmultiblockchange.a((blockposition1, iblockdata1) -> {
                            this.a(world, blockposition1, iblockdata1);
                        });
                    }

                    this.dirtyBlocks[j] = null;
                }
            }

            this.p = false;
        }
    }

    private void a(World world, BlockPosition blockposition, IBlockData iblockdata) {
        if (iblockdata.getBlock().isTileEntity()) {
            this.a(world, blockposition);
        }

    }

    private void a(World world, BlockPosition blockposition) {
        TileEntity tileentity = world.getTileEntity(blockposition);

        if (tileentity != null) {
            PacketPlayOutTileEntityData packetplayouttileentitydata = tileentity.getUpdatePacket();

            if (packetplayouttileentitydata != null) {
                this.a(packetplayouttileentitydata, false);
            }
        }

    }

    private void a(Packet<?> packet, boolean flag) {
        this.players.a(this.location, flag).forEach((entityplayer) -> {
            entityplayer.playerConnection.sendPacket(packet);
        });
    }

    public CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> a(ChunkStatus chunkstatus, PlayerChunkMap playerchunkmap) {
        int i = chunkstatus.c();
        CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completablefuture = (CompletableFuture) this.statusFutures.get(i);

        if (completablefuture != null) {
            Either<IChunkAccess, PlayerChunk.Failure> either = (Either) completablefuture.getNow(null); // CraftBukkit - decompile error

            if (either == null || either.left().isPresent()) {
                return completablefuture;
            }
        }

        if (getChunkStatus(this.ticketLevel).b(chunkstatus)) {
            CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completablefuture1 = playerchunkmap.a(this, chunkstatus);

            this.a(completablefuture1);
            this.statusFutures.set(i, completablefuture1);
            return completablefuture1;
        } else {
            return completablefuture == null ? PlayerChunk.UNLOADED_CHUNK_ACCESS_FUTURE : completablefuture;
        }
    }

    private void a(CompletableFuture<? extends Either<? extends IChunkAccess, PlayerChunk.Failure>> completablefuture) {
        this.chunkSave = this.chunkSave.thenCombine(completablefuture, (ichunkaccess, either) -> {
            return (IChunkAccess) either.map((ichunkaccess1) -> {
                return ichunkaccess1;
            }, (playerchunk_failure) -> {
                return ichunkaccess;
            });
        });
    }

    public final ChunkCoordIntPair i() { // Paper - final for inline
        return this.location;
    }

    public final int getTicketLevel() { // Paper - final for inline
        return this.ticketLevel;
    }

    public int k() {
        return this.n;
    }

    private void d(int i) {
        this.n = i;
    }

    public void a(int i) {
        this.ticketLevel = i;
    }

    protected void a(PlayerChunkMap playerchunkmap) {
        ChunkStatus chunkstatus = getChunkStatus(this.oldTicketLevel);
        ChunkStatus chunkstatus1 = getChunkStatus(this.ticketLevel);
        boolean flag = this.oldTicketLevel <= PlayerChunkMap.GOLDEN_TICKET;
        boolean flag1 = this.ticketLevel <= PlayerChunkMap.GOLDEN_TICKET;
        PlayerChunk.State playerchunk_state = getChunkState(this.oldTicketLevel);
        PlayerChunk.State playerchunk_state1 = getChunkState(this.ticketLevel);
        // CraftBukkit start
        // ChunkUnloadEvent: Called before the chunk is unloaded: isChunkLoaded is still true and chunk can still be modified by plugins.
        if (playerchunk_state.isAtLeast(PlayerChunk.State.BORDER) && !playerchunk_state1.isAtLeast(PlayerChunk.State.BORDER)) {
            this.getStatusFutureUnchecked(ChunkStatus.FULL).thenAccept((either) -> {
                Chunk chunk = (Chunk)either.left().orElse(null);
                if (chunk != null) {
                    playerchunkmap.callbackExecutor.execute(() -> {
                        // Minecraft will apply the chunks tick lists to the world once the chunk got loaded, and then store the tick
                        // lists again inside the chunk once the chunk becomes inaccessible and set the chunk's needsSaving flag.
                        // These actions may however happen deferred, so we manually set the needsSaving flag already here.
                        chunk.setNeedsSaving(true);
                        chunk.unloadCallback();
                    });
                }
            }).exceptionally((throwable) -> {
                // ensure exceptions are printed, by default this is not the case
                MinecraftServer.LOGGER.fatal("Failed to schedule unload callback for chunk " + PlayerChunk.this.location, throwable);
                return null;
            });

            // Run callback right away if the future was already done
            playerchunkmap.callbackExecutor.run();
        }
        // CraftBukkit end
        CompletableFuture completablefuture;

        if (flag) {
            Either<IChunkAccess, PlayerChunk.Failure> either = Either.right(new PlayerChunk.Failure() {
                public String toString() {
                    return "Unloaded ticket level " + PlayerChunk.this.location.toString();
                }
            });

            for (int i = flag1 ? chunkstatus1.c() + 1 : 0; i <= chunkstatus.c(); ++i) {
                completablefuture = (CompletableFuture) this.statusFutures.get(i);
                if (completablefuture != null) {
                    completablefuture.complete(either);
                } else {
                    this.statusFutures.set(i, CompletableFuture.completedFuture(either));
                }
            }
        }

        boolean flag2 = playerchunk_state.isAtLeast(PlayerChunk.State.BORDER);
        boolean flag3 = playerchunk_state1.isAtLeast(PlayerChunk.State.BORDER);

        this.hasBeenLoaded |= flag3;
        if (!flag2 && flag3) {
            // Paper start - cache ticking ready status
            int expectCreateCount = ++this.fullChunkCreateCount;
            this.fullChunkFuture = playerchunkmap.b(this); this.fullChunkFuture.thenAccept((either) -> {
                if (either.left().isPresent() && PlayerChunk.this.fullChunkCreateCount == expectCreateCount) {
                    // note: Here is a very good place to add callbacks to logic waiting on this.
                    Chunk fullChunk = either.left().get();
                    PlayerChunk.this.isFullChunkReady = true;
                    fullChunk.playerChunk = PlayerChunk.this;


                }
            });
            // Paper end
            this.a(this.fullChunkFuture);
        }

        if (flag2 && !flag3) {
            completablefuture = this.fullChunkFuture;
            this.fullChunkFuture = PlayerChunk.UNLOADED_CHUNK_FUTURE;
            ++this.fullChunkCreateCount; // Paper - cache ticking ready status
            this.isFullChunkReady = false; // Paper - cache ticking ready status
            this.a(((CompletableFuture<Either<Chunk, PlayerChunk.Failure>>) completablefuture).thenApply((either1) -> { // CraftBukkit - decompile error
                playerchunkmap.getClass();
                return either1.ifLeft(playerchunkmap::a);
            }));
        }

        boolean flag4 = playerchunk_state.isAtLeast(PlayerChunk.State.TICKING);
        boolean flag5 = playerchunk_state1.isAtLeast(PlayerChunk.State.TICKING);

        if (!flag4 && flag5) {
            // Paper start - cache ticking ready status
            this.tickingFuture = playerchunkmap.a(this); this.tickingFuture.thenAccept((either) -> {
                if (either.left().isPresent()) {
                    // note: Here is a very good place to add callbacks to logic waiting on this.
                    Chunk tickingChunk = either.left().get();
                    PlayerChunk.this.isTickingReady = true;




                }
            });
            // Paper end
            this.a(this.tickingFuture);
        }

        if (flag4 && !flag5) {
            this.tickingFuture.complete(PlayerChunk.UNLOADED_CHUNK); this.isTickingReady = false; // Paper - cache chunk ticking stage
            this.tickingFuture = PlayerChunk.UNLOADED_CHUNK_FUTURE;
        }

        boolean flag6 = playerchunk_state.isAtLeast(PlayerChunk.State.ENTITY_TICKING);
        boolean flag7 = playerchunk_state1.isAtLeast(PlayerChunk.State.ENTITY_TICKING);

        if (!flag6 && flag7) {
            if (this.entityTickingFuture != PlayerChunk.UNLOADED_CHUNK_FUTURE) {
                throw (IllegalStateException) SystemUtils.c((Throwable) (new IllegalStateException()));
            }

            // Paper start - cache ticking ready status
            this.entityTickingFuture = playerchunkmap.b(this.location); this.entityTickingFuture.thenAccept((either) -> {
                if (either.left().isPresent()) {
                    // note: Here is a very good place to add callbacks to logic waiting on this.
                    Chunk entityTickingChunk = either.left().get();
                    PlayerChunk.this.isEntityTickingReady = true;




                }
            });
            // Paper end
            this.a(this.entityTickingFuture);
        }

        if (flag6 && !flag7) {
            this.entityTickingFuture.complete(PlayerChunk.UNLOADED_CHUNK); this.isEntityTickingReady = false; // Paper - cache chunk ticking stage
            this.entityTickingFuture = PlayerChunk.UNLOADED_CHUNK_FUTURE;
        }

        this.u.a(this.location, this::k, this.ticketLevel, this::d);
        this.oldTicketLevel = this.ticketLevel;
        // CraftBukkit start
        // ChunkLoadEvent: Called after the chunk is loaded: isChunkLoaded returns true and chunk is ready to be modified by plugins.
        if (!playerchunk_state.isAtLeast(PlayerChunk.State.BORDER) && playerchunk_state1.isAtLeast(PlayerChunk.State.BORDER)) {
            this.getStatusFutureUnchecked(ChunkStatus.FULL).thenAccept((either) -> {
                Chunk chunk = (Chunk)either.left().orElse(null);
                if (chunk != null) {
                    playerchunkmap.callbackExecutor.execute(() -> {
                        chunk.loadCallback();
                    });
                }
            }).exceptionally((throwable) -> {
                // ensure exceptions are printed, by default this is not the case
                MinecraftServer.LOGGER.fatal("Failed to schedule load callback for chunk " + PlayerChunk.this.location, throwable);
                return null;
            });

            // Run callback right away if the future was already done
            playerchunkmap.callbackExecutor.run();
        }
        // CraftBukkit end
    }

    public static ChunkStatus getChunkStatus(int i) {
        return i < 33 ? ChunkStatus.FULL : ChunkStatus.a(i - 33);
    }

    public static PlayerChunk.State getChunkState(int i) {
        return PlayerChunk.CHUNK_STATES[MathHelper.clamp(33 - i + 1, 0, PlayerChunk.CHUNK_STATES.length - 1)];
    }

    public boolean hasBeenLoaded() {
        return this.hasBeenLoaded;
    }

    public void m() {
        this.hasBeenLoaded = getChunkState(this.ticketLevel).isAtLeast(PlayerChunk.State.BORDER);
    }

    public void a(ProtoChunkExtension protochunkextension) {
        for (int i = 0; i < this.statusFutures.length(); ++i) {
            CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completablefuture = (CompletableFuture) this.statusFutures.get(i);

            if (completablefuture != null) {
                Optional<IChunkAccess> optional = ((Either) completablefuture.getNow(PlayerChunk.UNLOADED_CHUNK_ACCESS)).left();

                if (optional.isPresent() && optional.get() instanceof ProtoChunk) {
                    this.statusFutures.set(i, CompletableFuture.completedFuture(Either.left(protochunkextension)));
                }
            }
        }

        this.a(CompletableFuture.completedFuture(Either.left(protochunkextension.u())));
    }

    public interface d {

        Stream<EntityPlayer> a(ChunkCoordIntPair chunkcoordintpair, boolean flag);
    }

    public interface c {

        void a(ChunkCoordIntPair chunkcoordintpair, IntSupplier intsupplier, int i, IntConsumer intconsumer);
    }

    public interface Failure {

        PlayerChunk.Failure b = new PlayerChunk.Failure() {
            public String toString() {
                return "UNLOADED";
            }
        };
    }

    public static enum State {

        INACCESSIBLE, BORDER, TICKING, ENTITY_TICKING;

        private State() {}

        public boolean isAtLeast(PlayerChunk.State playerchunk_state) {
            return this.ordinal() >= playerchunk_state.ordinal();
        }
    }
}
