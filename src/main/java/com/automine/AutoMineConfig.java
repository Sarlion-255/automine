package com.automine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Configurazione salvata in config/automine.json + cache dei blocchi. */
public final class AutoMineConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("automine.json");

    private static final Set<Block> SELECTED = new HashSet<>();
    private static final Map<Block, String> IDS = new HashMap<>();
    private static final Map<Block, ItemStack> STACKS = new HashMap<>();
    private static List<Block> allBlocks;

    /** Se true, AutoMine si ferma quando l'attrezzo in mano sta per rompersi. */
    public static boolean protectTool = true;

    private static class Data {
        List<String> selected = new ArrayList<>();
        boolean protectTool = true;
    }

    private AutoMineConfig() {}

    /** Lista di tutti i blocchi scavabili (con item associato), ordinata per id. */
    public static synchronized List<Block> allBlocks() {
        if (allBlocks == null) {
            List<Block> list = new ArrayList<>();
            for (Block block : BuiltInRegistries.BLOCK) {
                if (block.defaultBlockState().isAir()) continue;
                if (block.asItem() == Items.AIR) continue;
                if (block.defaultDestroyTime() < 0) continue; // bedrock, barrier, ecc.
                list.add(block);
                IDS.put(block, BuiltInRegistries.BLOCK.getKey(block).toString());
                STACKS.put(block, new ItemStack(block.asItem()));
            }
            list.sort(Comparator.comparing(IDS::get));
            allBlocks = list;
        }
        return allBlocks;
    }

    public static String idOf(Block block) {
        allBlocks();
        String id = IDS.get(block);
        return id != null ? id : BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    public static ItemStack stackOf(Block block) {
        allBlocks();
        ItemStack s = STACKS.get(block);
        return s != null ? s : ItemStack.EMPTY;
    }

    public static boolean isSelected(Block block) {
        return SELECTED.contains(block);
    }

    public static int selectedCount() {
        return SELECTED.size();
    }

    public static void toggle(Block block) {
        if (!SELECTED.remove(block)) SELECTED.add(block);
    }

    public static void clear() {
        SELECTED.clear();
    }

    /** Seleziona tutti i minerali (id che finisce con _ore + detriti antichi). */
    public static void selectOres() {
        for (Block b : allBlocks()) {
            String id = idOf(b);
            if (id.endsWith("_ore") || id.equals("minecraft:ancient_debris")) {
                SELECTED.add(b);
            }
        }
    }

    public static void load() {
        allBlocks();
        SELECTED.clear();
        if (!Files.exists(FILE)) return;
        try (Reader reader = Files.newBufferedReader(FILE)) {
            Data data = GSON.fromJson(reader, Data.class);
            if (data == null) return;
            protectTool = data.protectTool;
            Set<String> wanted = new HashSet<>(data.selected);
            for (Block b : allBlocks()) {
                if (wanted.contains(idOf(b))) SELECTED.add(b);
            }
        } catch (Exception e) {
            System.err.println("[AutoMine] Impossibile leggere la config: " + e);
        }
    }

    public static void save() {
        Data data = new Data();
        data.protectTool = protectTool;
        for (Block b : SELECTED) data.selected.add(idOf(b));
        Collections.sort(data.selected);
        try (Writer writer = Files.newBufferedWriter(FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("[AutoMine] Impossibile salvare la config: " + e);
        }
    }
}
