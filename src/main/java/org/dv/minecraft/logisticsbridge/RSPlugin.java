package org.dv.minecraft.logisticsbridge;

import com.raoulvdberge.refinedstorage.RSBlocks;
import com.raoulvdberge.refinedstorage.RSItems;
import com.raoulvdberge.refinedstorage.api.IRSAPI;
import com.raoulvdberge.refinedstorage.api.RSAPIInject;
import com.raoulvdberge.refinedstorage.apiimpl.API;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNode;
import com.raoulvdberge.refinedstorage.block.BlockBase;
import com.raoulvdberge.refinedstorage.gui.grid.GuiGrid;
import com.raoulvdberge.refinedstorage.gui.grid.stack.IGridStack;
import com.raoulvdberge.refinedstorage.gui.grid.view.GridViewItem;
import com.raoulvdberge.refinedstorage.gui.grid.view.IGridView;
import com.raoulvdberge.refinedstorage.item.ItemProcessor;
import org.dv.minecraft.logisticsbridge.block.BlockBridgeRS;
import org.dv.minecraft.logisticsbridge.block.BlockCraftingManagerRS;
import org.dv.minecraft.logisticsbridge.block.BlockSatelliteBus;
import org.dv.minecraft.logisticsbridge.item.VirtualPatternRS;
import org.dv.minecraft.logisticsbridge.node.NetworkNodeBridge;
import org.dv.minecraft.logisticsbridge.node.NetworkNodeCraftingManager;
import org.dv.minecraft.logisticsbridge.node.NetworkNodeSatellite;
import org.dv.minecraft.logisticsbridge.tileentity.TileEntityBridgeRS;
import org.dv.minecraft.logisticsbridge.tileentity.TileEntityCraftingManagerRS;
import org.dv.minecraft.logisticsbridge.tileentity.TileEntitySatelliteBus;
import logisticspipes.LPItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.List;
import java.util.function.BiFunction;

public class RSPlugin {
    @RSAPIInject
    public static IRSAPI rsapi;

    public static VirtualPatternRS virtualPattern;
    public static BlockBase satelliteBus, craftingManager;

    public static void preInit() {
        virtualPattern = new VirtualPatternRS();
        satelliteBus = new BlockSatelliteBus();
        craftingManager = new BlockCraftingManagerRS();
        LB_ItemStore.bridgeRS = new BlockBridgeRS().setTranslationKey("lb.bridge.rs");
        registerBlock((BlockBase) LB_ItemStore.bridgeRS);
        registerBlock(satelliteBus);
        registerBlock(craftingManager);
        LogisticsBridge.registerItem(virtualPattern, true);
        GameRegistry.registerTileEntity(TileEntityBridgeRS.class, new ResourceLocation(Reference.MOD_ID, "bridge_rs"));
        GameRegistry.registerTileEntity(TileEntitySatelliteBus.class, new ResourceLocation(Reference.MOD_ID, "satellite_bus_rs"));
        GameRegistry.registerTileEntity(TileEntityCraftingManagerRS.class, new ResourceLocation(Reference.MOD_ID, "craftingmanager_rs"));
        registerNode(NetworkNodeBridge.ID, NetworkNodeBridge::new);
        registerNode(NetworkNodeSatellite.ID, NetworkNodeSatellite::new);
        registerNode(NetworkNodeCraftingManager.ID, NetworkNodeCraftingManager::new);
    }

    private static void registerNode(String id, BiFunction<World, BlockPos, NetworkNode> constr) {
        API.instance().getNetworkNodeRegistry().add(id, (tag, world, pos) -> {
            NetworkNode node = constr.apply(world, pos);
            node.read(tag);
            return node;
        });
    }

    private static void registerBlock(BlockBase block) {
        LogisticsBridge.registerBlock(block, BlockBase::createItem);
    }

    public static void loadRecipes(ResourceLocation group) {
        ForgeRegistries.RECIPES.register(new ShapedOreRecipe(group, new ItemStack(LB_ItemStore.bridgeRS), "iei", "bIb", "ici",
                'i', "ingotIron",
                'b', LPItems.pipeBasic,
                'I', RSBlocks.INTERFACE,
                'c', new ItemStack(RSItems.PROCESSOR, 1, ItemProcessor.TYPE_IMPROVED),
                'e', new ItemStack(RSItems.PROCESSOR, 1, ItemProcessor.TYPE_ADVANCED)).
                setRegistryName(new ResourceLocation(Reference.MOD_ID, "recipes/bridge_rs")));
        ForgeRegistries.RECIPES.register(new ShapedOreRecipe(group, new ItemStack(satelliteBus), "ii ", "Ic-", "ii ",
                'i', "ingotIron",
                '-', RSBlocks.CABLE,
                'I', new ItemStack(RSItems.CORE, 1, 0),
                'c', new ItemStack(RSItems.PROCESSOR, 1, ItemProcessor.TYPE_IMPROVED)).
                setRegistryName(new ResourceLocation(Reference.MOD_ID, "recipes/satellite_bus_rs")));
        ForgeRegistries.RECIPES.register(new ShapedOreRecipe(group, new ItemStack(craftingManager), "cIc", "bab", "iIi",
                'i', "ingotIron",
                'c', RSBlocks.CRAFTER,
                'b', new ItemStack(RSItems.PROCESSOR, 1, ItemProcessor.TYPE_BASIC),
                'I', new ItemStack(RSItems.PROCESSOR, 1, ItemProcessor.TYPE_IMPROVED),
                'a', new ItemStack(RSItems.PROCESSOR, 1, ItemProcessor.TYPE_ADVANCED)).
                setRegistryName(new ResourceLocation(Reference.MOD_ID, "recipes/crafting_manager_rs")));
    }

    @SideOnly(Side.CLIENT)
    public static void hideFakeItems(GuiScreenEvent.BackgroundDrawnEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof GuiGrid && !GuiScreen.isCtrlKeyDown()) {
            IGridView view = ((GuiGrid) mc.currentScreen).getView();
            if (view instanceof GridViewItem) {
                List<IGridStack> stacks = view.getStacks();
                stacks.removeIf(s -> {
                    Object ing = s.getIngredient();
                    if (ing instanceof ItemStack) {
                        ItemStack is = (ItemStack) ing;
                        return is.getItem() == LB_ItemStore.logisticsFakeItem || (is.getItem() == LB_ItemStore.packageItem && s.getQuantity() == 0);
                    }
                    return false;
                });
            }
        }
    }
}
