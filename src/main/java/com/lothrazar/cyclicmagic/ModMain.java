package com.lothrazar.cyclicmagic;
import java.util.ArrayList;
import java.util.List;
import com.lothrazar.cyclicmagic.gui.ModGuiHandler;
import com.lothrazar.cyclicmagic.module.BaseModule.ModuleType;
import com.lothrazar.cyclicmagic.module.BucketBlockModule;
import com.lothrazar.cyclicmagic.module.BuilderBlockModule;
import com.lothrazar.cyclicmagic.module.CarbonPaperModule;
import com.lothrazar.cyclicmagic.module.ConveyorPlateModule;
import com.lothrazar.cyclicmagic.module.CyclicWandModule;
import com.lothrazar.cyclicmagic.module.EmeraldArmorModule;
import com.lothrazar.cyclicmagic.module.FragileBlockModule;
import com.lothrazar.cyclicmagic.module.HorseFoodModule;
import com.lothrazar.cyclicmagic.module.ICyclicModule;
import com.lothrazar.cyclicmagic.module.MagicBeanModule;
import com.lothrazar.cyclicmagic.module.MobSpawnModule;
import com.lothrazar.cyclicmagic.module.PotionModule;
import com.lothrazar.cyclicmagic.module.SlimepadModule;
import com.lothrazar.cyclicmagic.module.StackSizeModule;
import com.lothrazar.cyclicmagic.module.UnbreakableSpawnerModule;
import com.lothrazar.cyclicmagic.module.UncrafterModule;
import com.lothrazar.cyclicmagic.proxy.CommonProxy;
import com.lothrazar.cyclicmagic.registry.*;
import com.lothrazar.cyclicmagic.registry.CapabilityRegistry.IPlayerExtendedProperties;
import com.lothrazar.cyclicmagic.util.Const;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

@Mod(modid = Const.MODID, useMetadata = true, canBeDeactivated = false, updateJSON = "https://raw.githubusercontent.com/PrinceOfAmber/CyclicMagic/master/update.json", acceptableRemoteVersions = "*", guiFactory = "com.lothrazar." + Const.MODID + ".gui.IngameConfigFactory")
public class ModMain {
  private List<ICyclicModule> modules = new ArrayList<ICyclicModule>();
  @Instance(value = Const.MODID)
  public static ModMain instance;
  @SidedProxy(clientSide = "com.lothrazar." + Const.MODID + ".proxy.ClientProxy", serverSide = "com.lothrazar." + Const.MODID + ".proxy.CommonProxy")
  public static CommonProxy proxy;
  public static ModLogger logger;
  private static Configuration config;
  private EventRegistry events;
  public static SimpleNetworkWrapper network;
  public final static CreativeTabs TAB = new CreativeTabs(Const.MODID) {
    @Override
    public Item getTabIconItem() {
      return ItemRegistry.cyclic_wand_build == null ? Items.DIAMOND : ItemRegistry.cyclic_wand_build;
    }
  };
  // thank you for the examples forge. player data storage based on API source code example:
  // https://github.com/MinecraftForge/MinecraftForge/blob/1.9/src/test/java/net/minecraftforge/test/NoBedSleepingTest.java
  @CapabilityInject(IPlayerExtendedProperties.class)
  public static final Capability<IPlayerExtendedProperties> CAPABILITYSTORAGE = null;
  @EventHandler
  public void onPreInit(FMLPreInitializationEvent event) {
    logger = new ModLogger(event.getModLog());
    config = new Configuration(event.getSuggestedConfigurationFile());
    config.load();
    network = NetworkRegistry.INSTANCE.newSimpleChannel(Const.MODID);
    MinecraftForge.EVENT_BUS.register(instance);
    SoundRegistry.register();
    CapabilityRegistry.register();
    ReflectionRegistry.register();
    PacketRegistry.register(network);
    events = new EventRegistry();
    modules.add(new BuilderBlockModule().setType(ModuleType.INIT));
    modules.add(new BucketBlockModule().setType(ModuleType.INIT));
    modules.add(new CarbonPaperModule().setType(ModuleType.INIT));
    modules.add(new ConveyorPlateModule().setType(ModuleType.INIT));
    modules.add(new CyclicWandModule().setType(ModuleType.INIT));
    modules.add(new EmeraldArmorModule().setType(ModuleType.INIT));
    modules.add(new FragileBlockModule().setType(ModuleType.INIT));
    modules.add(new HorseFoodModule().setType(ModuleType.INIT));
    modules.add(new MagicBeanModule().setType(ModuleType.INIT));
    modules.add(new MobSpawnModule().setType(ModuleType.INIT));
    modules.add(new PotionModule().setType(ModuleType.INIT));
    modules.add(new SlimepadModule().setType(ModuleType.INIT));
    modules.add(new StackSizeModule().setType(ModuleType.INIT));
    modules.add(new UnbreakableSpawnerModule().setType(ModuleType.INIT));
    modules.add(new UncrafterModule().setType(ModuleType.INIT));
    
    ItemRegistry.construct(); //modules here
   
    events.register();
    this.syncConfig();

    registerModulesByType(ModuleType.PREINIT);
    EnchantRegistry.register();//modules here
  }
  @EventHandler
  public void onInit(FMLInitializationEvent event) {
    registerModulesByType(ModuleType.INIT);
  
    ItemRegistry.register();
    BlockRegistry.registerDimensionOres();
    WorldGenRegistry.register();
    FuelRegistry.register();
    // StackSizeRegistry.register();
    RecipeAlterRegistry.register();
    RecipeNewRegistry.register();
    VillageTradeRegistry.register();
    proxy.register();
    NetworkRegistry.INSTANCE.registerGuiHandler(this, new ModGuiHandler());
    ProjectileRegistry.register(event);
    //finally, some items have extra forge events to hook into.
    MinecraftForge.EVENT_BUS.register(ItemRegistry.corrupted_chorus);
    MinecraftForge.EVENT_BUS.register(ItemRegistry.heart_food);
    MinecraftForge.EVENT_BUS.register(ItemRegistry.tool_push);
    MinecraftForge.EVENT_BUS.register(EnchantRegistry.launch);
    MinecraftForge.EVENT_BUS.register(EnchantRegistry.magnet);
    MinecraftForge.EVENT_BUS.register(EnchantRegistry.venom);
    MinecraftForge.EVENT_BUS.register(EnchantRegistry.lifeleech);
  }
  @EventHandler
  public void onPostInit(FMLPostInitializationEvent event) {
    registerModulesByType(ModuleType.POSTINIT);
    // registers all plantable crops. 
    DispenserBehaviorRegistry.register();
  }
  @EventHandler
  public void onServerStarting(FMLServerStartingEvent event) {
    registerModulesByType(ModuleType.SERVERSTART);
    CommandRegistry.register(event);
  }
  private void registerModulesByType(ModuleType type){
    for(ICyclicModule module : modules) 
      if(module.isEnabled() && module.getType() == type){
        module.register();
      }
  }
  @SubscribeEvent
  public void onConfigChanged(OnConfigChangedEvent event) {
    if (event.getModID().equals(Const.MODID)) {
      ModMain.instance.syncConfig();
    }
  }
  public static Configuration getConfig() {
    return config;
  }
  public void syncConfig() {
    // hit on startup and on change event from
    // we cant make this a list/loop because the order does matter
    Configuration c = getConfig();
    for(ICyclicModule module : modules) {
      module.syncConfig(c);
    }
    EnchantRegistry.syncConfig(c);
    WorldGenRegistry.syncConfig(c);
   
    events.syncConfig(c); 
    ItemRegistry.syncConfig(c);
    FuelRegistry.syncConfig(c);
    RecipeAlterRegistry.syncConfig(c);
    RecipeNewRegistry.syncConfig(c);
    DispenserBehaviorRegistry.syncConfig(c);
    CommandRegistry.syncConfig(c);
    VillageTradeRegistry.syncConfig(c);
    KeyInventoryShiftRegistry.syncConfig(c);
    c.save();
  }
  
  /*
   * 
   * TODO:
   * 
   * 
   * https://www.reddit.com/r/minecraftsuggestions/comments/4smwb5/if_lightning_strikes_a_skeleton_it_turns_into_a/
   * 
   * -- spawn inspector: SKEL/ZOMBIE VARIANTS: stray==winter
   * 
   *  is there a bug in searchrecipe? or a bug in new repeater rec?
   * 
   * fragile torches config: float oddsWillBreak = 0.01F;//  : in config or something? or make this  
   * 
   * frozen/snow effect
   * 
   * config: command search spawner max distances
   * 
   * circle sometimes gets duplicate positions, slowing it down
   * 
   * Some way to keep certain items after death - runestone? enchantment?
   * 
   * ButtonBuildToggle-store ptr to wand not player??
   * 
   * [ Trading Tool // gui]
   *     Upgrade villager gui: either make my own or add buttons/some way to view all trades at once
   *    --inspired by extrautils trading table that is apparently gone after 1710
   * 
   * exp bottler: item with a gui/inventory
   *      put bottles in, toggle on/off and it slowly drains your exp into the bottles at a given ratio
   * 
   * pets live longer and/or respawn
   * 
   * add some of my items to loot tables ?
   *        https://github.com/MinecraftForge/MinecraftForge/blob/master/src/test/java/net/minecraftforge/debug/LootTablesDebug.java
   * 
   * crafting table hotkeys - numpad?
   *
   * ROTATE: STAIRS: allow switch from top to bottom
   * 
   * Fix sorting : UtilInventorySort.sort(p, openInventory);
   * 
   */
}
