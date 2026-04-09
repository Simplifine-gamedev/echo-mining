package com.orca.echomining;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EchoMiningMod implements ModInitializer {
    public static final String MOD_ID = "echo-mining";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final int PULSE_RADIUS = 10;
    private static final long COOLDOWN_MS = 5000;
    private static final int GLOW_DURATION_TICKS = 40; // 2 seconds

    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();

    // Ore color mappings (RGB normalized 0-1)
    private static final Map<Block, Vector3f> ORE_COLORS = new HashMap<>();

    static {
        // Iron = white
        ORE_COLORS.put(Blocks.IRON_ORE, new Vector3f(1.0f, 1.0f, 1.0f));
        ORE_COLORS.put(Blocks.DEEPSLATE_IRON_ORE, new Vector3f(1.0f, 1.0f, 1.0f));
        // Gold = yellow
        ORE_COLORS.put(Blocks.GOLD_ORE, new Vector3f(1.0f, 0.84f, 0.0f));
        ORE_COLORS.put(Blocks.DEEPSLATE_GOLD_ORE, new Vector3f(1.0f, 0.84f, 0.0f));
        ORE_COLORS.put(Blocks.NETHER_GOLD_ORE, new Vector3f(1.0f, 0.84f, 0.0f));
        // Diamond = cyan
        ORE_COLORS.put(Blocks.DIAMOND_ORE, new Vector3f(0.0f, 1.0f, 1.0f));
        ORE_COLORS.put(Blocks.DEEPSLATE_DIAMOND_ORE, new Vector3f(0.0f, 1.0f, 1.0f));
        // Emerald = green
        ORE_COLORS.put(Blocks.EMERALD_ORE, new Vector3f(0.0f, 1.0f, 0.2f));
        ORE_COLORS.put(Blocks.DEEPSLATE_EMERALD_ORE, new Vector3f(0.0f, 1.0f, 0.2f));
        // Redstone = red
        ORE_COLORS.put(Blocks.REDSTONE_ORE, new Vector3f(1.0f, 0.0f, 0.0f));
        ORE_COLORS.put(Blocks.DEEPSLATE_REDSTONE_ORE, new Vector3f(1.0f, 0.0f, 0.0f));
        // Lapis = blue
        ORE_COLORS.put(Blocks.LAPIS_ORE, new Vector3f(0.0f, 0.4f, 1.0f));
        ORE_COLORS.put(Blocks.DEEPSLATE_LAPIS_ORE, new Vector3f(0.0f, 0.4f, 1.0f));
        // Copper = orange
        ORE_COLORS.put(Blocks.COPPER_ORE, new Vector3f(1.0f, 0.5f, 0.0f));
        ORE_COLORS.put(Blocks.DEEPSLATE_COPPER_ORE, new Vector3f(1.0f, 0.5f, 0.0f));
        // Coal = dark gray
        ORE_COLORS.put(Blocks.COAL_ORE, new Vector3f(0.3f, 0.3f, 0.3f));
        ORE_COLORS.put(Blocks.DEEPSLATE_COAL_ORE, new Vector3f(0.3f, 0.3f, 0.3f));
        // Nether quartz = white
        ORE_COLORS.put(Blocks.NETHER_QUARTZ_ORE, new Vector3f(1.0f, 1.0f, 1.0f));
        // Ancient debris = brown
        ORE_COLORS.put(Blocks.ANCIENT_DEBRIS, new Vector3f(0.6f, 0.3f, 0.1f));
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Echo Mining initialized! Break blocks to reveal nearby ores.");

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient()) return;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

            UUID playerId = player.getUuid();
            long currentTime = System.currentTimeMillis();

            // Check cooldown
            Long lastPulse = playerCooldowns.get(playerId);
            if (lastPulse != null && currentTime - lastPulse < COOLDOWN_MS) {
                return;
            }

            // Set cooldown
            playerCooldowns.put(playerId, currentTime);

            ServerWorld serverWorld = (ServerWorld) world;

            // Trigger sonar pulse
            triggerSonarPulse(serverWorld, pos);
        });
    }

    private void triggerSonarPulse(ServerWorld world, BlockPos center) {
        // Find all ores in radius
        Set<BlockPos> orePositions = new HashSet<>();
        Map<BlockPos, Vector3f> oreColors = new HashMap<>();

        for (int x = -PULSE_RADIUS; x <= PULSE_RADIUS; x++) {
            for (int y = -PULSE_RADIUS; y <= PULSE_RADIUS; y++) {
                for (int z = -PULSE_RADIUS; z <= PULSE_RADIUS; z++) {
                    BlockPos checkPos = center.add(x, y, z);
                    double distance = Math.sqrt(x*x + y*y + z*z);

                    if (distance <= PULSE_RADIUS) {
                        BlockState state = world.getBlockState(checkPos);
                        Block block = state.getBlock();

                        if (ORE_COLORS.containsKey(block)) {
                            orePositions.add(checkPos);
                            oreColors.put(checkPos, ORE_COLORS.get(block));
                        }
                    }
                }
            }
        }

        // Spawn expanding ring particles and highlight ores
        world.getServer().execute(() -> {
            // Spawn initial pulse ring effect
            spawnPulseRing(world, center, 1);

            // Schedule expanding rings
            for (int ring = 2; ring <= PULSE_RADIUS; ring++) {
                final int radius = ring;
                world.getServer().execute(() -> {
                    try {
                        Thread.sleep(50 * radius);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                scheduleTask(world, () -> spawnPulseRing(world, center, radius), ring * 2);
            }

            // Highlight ores with colored particles
            for (Map.Entry<BlockPos, Vector3f> entry : oreColors.entrySet()) {
                BlockPos orePos = entry.getKey();
                Vector3f color = entry.getValue();

                // Spawn glowing particles at ore location for duration
                scheduleOreHighlight(world, orePos, color, GLOW_DURATION_TICKS);
            }
        });
    }

    private void spawnPulseRing(ServerWorld world, BlockPos center, int radius) {
        // Spawn particles in a spherical shell
        int particleCount = radius * 12;
        for (int i = 0; i < particleCount; i++) {
            double theta = 2 * Math.PI * i / particleCount;
            double phi = Math.PI * (i % (particleCount / 2)) / (particleCount / 2);

            double x = center.getX() + 0.5 + radius * Math.sin(phi) * Math.cos(theta);
            double y = center.getY() + 0.5 + radius * Math.cos(phi);
            double z = center.getZ() + 0.5 + radius * Math.sin(phi) * Math.sin(theta);

            // White pulse particle
            DustParticleEffect particle = new DustParticleEffect(new Vector3f(0.8f, 0.9f, 1.0f), 1.0f);
            world.spawnParticles(particle, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    private void scheduleTask(ServerWorld world, Runnable task, int delayTicks) {
        // Simple scheduling using world tick
        new Thread(() -> {
            try {
                Thread.sleep(delayTicks * 50L);
                world.getServer().execute(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void scheduleOreHighlight(ServerWorld world, BlockPos pos, Vector3f color, int durationTicks) {
        // Spawn particles repeatedly for duration
        new Thread(() -> {
            int ticks = 0;
            while (ticks < durationTicks) {
                final int currentTick = ticks;
                world.getServer().execute(() -> {
                    // Spawn colored particles around the ore
                    DustParticleEffect particle = new DustParticleEffect(color, 1.5f);

                    // Spawn multiple particles around the block
                    for (int i = 0; i < 8; i++) {
                        double offsetX = (Math.random() - 0.5) * 1.5;
                        double offsetY = (Math.random() - 0.5) * 1.5;
                        double offsetZ = (Math.random() - 0.5) * 1.5;

                        world.spawnParticles(particle,
                            pos.getX() + 0.5 + offsetX,
                            pos.getY() + 0.5 + offsetY,
                            pos.getZ() + 0.5 + offsetZ,
                            1, 0, 0, 0, 0);
                    }
                });

                try {
                    Thread.sleep(100); // Every 2 ticks
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                ticks += 2;
            }
        }).start();
    }
}
