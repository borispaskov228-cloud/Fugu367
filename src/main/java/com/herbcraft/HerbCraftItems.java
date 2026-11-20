package com.herbcraft;

import com.herbcraft.item.EcigItem;
import com.herbcraft.item.JointItem;
import com.herbcraft.item.LiquidItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class HerbCraftItems {
    /** Высушенная трава — она же семена для грядки. */
    public static final Map<HerbType, Item> HERBS = new EnumMap<>(HerbType.class);
    /** Скрутки. */
    public static final Map<HerbType, Item> JOINTS = new EnumMap<>(HerbType.class);
    /** Жидкость для вэйпа. */
    public static final Map<HerbType, Item> LIQUIDS = new EnumMap<>(HerbType.class);
    /** Электронные сигареты. */
    public static final Map<HerbType, Item> ECIGS = new EnumMap<>(HerbType.class);
    /** Дикие кусты (для креатива). */
    public static final Map<HerbType, Item> WILD_ITEMS = new EnumMap<>(HerbType.class);

    /** O(1) поиск по объекту предмета (используется котлом). */
    private static final Map<Item, HerbType> HERB_BY_ITEM = new IdentityHashMap<>();

    public static void register() {
        for (HerbType type : HerbType.values()) {
            Item herb = register(type.id + "_herb",
                    new BlockItem(HerbCraftBlocks.CROPS.get(type), new Item.Settings()));
            HERBS.put(type, herb);
            HERB_BY_ITEM.put(herb, type);
            JOINTS.put(type, register(type.id + "_joint", new JointItem(type)));
            LIQUIDS.put(type, register(type.id + "_liquid", new LiquidItem(type)));
            ECIGS.put(type, register(type.id + "_ecig", new EcigItem(type)));
            WILD_ITEMS.put(type, register(type.id + "_wild",
                    new BlockItem(HerbCraftBlocks.WILD.get(type), new Item.Settings())));
        }
    }

    private static Item register(String path, Item item) {
        return Registry.register(Registries.ITEM, HerbCraft.id(path), item);
    }

    public static HerbType getHerbType(Item item) {
        return HERB_BY_ITEM.get(item);
    }
}
