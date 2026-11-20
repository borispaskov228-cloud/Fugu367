package com.herbcraft;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Распространение диких трав по оверворду.
 *
 * Оптимизация: работаем напрямую с чанком и его хэйтмапом, не дёргая
 * серверный мир для поиска поверхности. На чанк — 3 попытки и дешёвые
 * проверки блока; обновление блока идёт без лишних соседних оповещений.
 */
public class HerbWorldGen {
    private static final int ATTEMPTS_PER_CHUNK = 3;

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register(HerbWorldGen::onChunkLoad);
    }

    private static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        if (world.getRegistryKey() != World.OVERWORLD) return;

        ChunkPos chunkPos = chunk.getPos();
        long seed = ((long) chunkPos.x * 73856093L) ^ ((long) chunkPos.z * 19349663L);
        Random random = Random.create(seed);

        Heightmap heightmap = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE);
        if (heightmap == null) return;

        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        for (int attempt = 0; attempt < ATTEMPTS_PER_CHUNK; attempt++) {
            int localX = random.nextInt(16);
            int localZ = random.nextInt(16);
            int y = heightmap.get(localX, localZ) - 1;
            if (y <= world.getBottomY()) continue;

            int x = startX + localX;
            int z = startZ + localZ;
            BlockPos ground = new BlockPos(x, y, z);
            BlockPos above = ground.up();

            if (!chunk.getBlockState(ground).isOf(Blocks.GRASS_BLOCK)) continue;
            if (!chunk.getBlockState(above).isAir()) continue;

            HerbType type = HerbType.values()[random.nextInt(HerbType.values().length)];
            world.setBlockState(above, HerbCraftBlocks.WILD.get(type).getDefaultState(),
                    Block.NOTIFY_LISTENERS);
        }
    }
}
