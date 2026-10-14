package ce.ajneb97;


import ce.ajneb97.api.ConditionalEventsAPI;
import ce.ajneb97.api.ExpansionCE;
import ce.ajneb97.configs.ConfigsManager;
import ce.ajneb97.libs.armorequipevent.ArmorListener;
import ce.ajneb97.libs.itemselectevent.ItemSelectListener;
import ce.ajneb97.libs.itemselectevent.ItemSelectListenerNew;
import ce.ajneb97.libs.offhandevent.OffHandListener;
import ce.ajneb97.libs.repairevent.RepairListener;
import ce.ajneb97.listeners.*;
import ce.ajneb97.listeners.dependencies.CitizensListener;
import ce.ajneb97.managers.dependencies.Metrics;
import ce.ajneb97.listeners.dependencies.WGRegionEventsListener;
import ce.ajneb97.managers.*;
import ce.ajneb97.managers.commandregister.CommandRegisterManager;
import ce.ajneb97.model.EventType;
import ce.ajneb97.model.internal.ConditionEvent;
import ce.ajneb97.model.internal.UpdateCheckerResult;
import ce.ajneb97.tasks.PlayerDataSaveTask;
import ce.ajneb97.utils.ServerVersion;
import ce.ajneb97.utils.scheduler.SchedulerAdapter;
import ce.ajneb97.utils.scheduler.SchedulerAdapterFactory;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;

public class ConditionalEvents extends JavaPlugin {
  
	PluginDescriptionFile pdfFile = getDescription();
	public String version = pdfFile.getVersion();
	public static ServerVersion serverVersion;
	public static String prefix;
  public static SchedulerAdapter scheduler;

	private EventsManager eventsManager;
	private DependencyManager dependencyManager;
	private PlayerManager playerManager;
	private ConfigsManager configsManager;
	private DebugManager debugManager;
  private StatsManager statsManager;
  private FragmentsManager fragmentsManager;
	private BungeeMessagingManager bungeeMessagingManager;
	private MessagesManager messagesManager;
	private VerifyManager verifyManager;
	private UpdateCheckerManager updateCheckerManager;
	private CommandRegisterManager commandRegisterManager;
	private SavedItemsManager savedItemsManager;
	private APIManager apiManager;
	private InterruptEventManager interruptEventManager;

	private PlayerDataSaveTask playerDataSaveTask;
  private ce.ajneb97.variables.VariablesManager variablesManager;
  private ce.ajneb97.variables.LocalVariablesManager localVariablesManager;
  private ce.ajneb97.managers.ItemEffectManager itemEffectManager;

	
	public void onEnable(){
		setVersion();
		setPrefix();
          scheduler = SchedulerAdapterFactory.get();

		this.eventsManager = new EventsManager(this);
		this.dependencyManager = new DependencyManager(this);
		this.bungeeMessagingManager = new BungeeMessagingManager(this);
		this.debugManager = new DebugManager(this);
          this.statsManager = new StatsManager(this);
          this.fragmentsManager = new FragmentsManager(this);
		this.playerManager =  new PlayerManager(this);
		this.savedItemsManager = new SavedItemsManager(this);
		this.apiManager = new APIManager(this);
		this.interruptEventManager = new InterruptEventManager(this);
		this.configsManager = new ConfigsManager(this);
		this.configsManager.configure();

          this.variablesManager = new ce.ajneb97.variables.VariablesManager(this);
          this.variablesManager.load();
          this.localVariablesManager = new ce.ajneb97.variables.LocalVariablesManager();
          ce.ajneb97.utils.ItemEditor.init(this);
          this.itemEffectManager = new ce.ajneb97.managers.ItemEffectManager(this);
          this.itemEffectManager.start();

		registerEvents();
		registerCommands();

		this.verifyManager = new VerifyManager(this);
		this.verifyManager.verifyEvents();

		this.commandRegisterManager = new CommandRegisterManager(this);
		commandRegisterManager.registerCommands();



		reloadPlayerDataSaveTask();

		ConditionalEventsAPI api = new ConditionalEventsAPI(this);
		if(getServer().getPluginManager().getPlugin("PlaceholderAPI") != null){
			new ExpansionCE(this).register();
		}
		Metrics metrics = new Metrics(this, 34693);

		Bukkit.getConsoleSender().sendMessage(MessagesManager.getLegacyColoredMessage(prefix+" &eHas been enabled! &fVersion: "+version));
        Bukkit.getConsoleSender().sendMessage(MessagesManager.getLegacyColoredMessage(prefix+" &eThanks for using my plugin!   &f~Ajneb97"));

		updateCheckerManager = new UpdateCheckerManager(version);
		if(configsManager.getMainConfigManager().isUpdateNotifications()){
			updateMessage(updateCheckerManager.check());
		}

		new ConditionEvent(this, null, null, EventType.SERVER_START, null)
				.checkEvent();
	}
	  
	public ce.ajneb97.variables.VariablesManager getVariablesManager() { return variablesManager; }

	  
	public ce.ajneb97.variables.LocalVariablesManager getLocalVariablesManager() { return localVariablesManager; }

	  
	public ce.ajneb97.managers.ItemEffectManager getItemEffectManager() { return itemEffectManager; }


	  
	public void onDisable(){
          if(this.itemEffectManager != null) this.itemEffectManager.stop();
          if(this.variablesManager != null) this.variablesManager.save();
new ConditionEvent(this, null, null, EventType.SERVER_STOP, null)
				.checkEvent();
		this.configsManager.getPlayerConfigsManager().saveConfigs();
		Bukkit.getConsoleSender().sendMessage(MessagesManager.getLegacyColoredMessage(prefix+" &eHas been disabled! &fVersion: "+version));
	}

	public void registerEvents() {
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new PlayerEventsListener(this), this);
		pm.registerEvents(new ItemEventsListener(this), this);
		pm.registerEvents(new ArmorListener(new ArrayList<>()), this);
		pm.registerEvents(new ItemSelectListener(this), this);
		pm.registerEvents(new OtherEventsListener(this), this);
		pm.registerEvents(new CustomEventListener(this), this);
          pm.registerEvents(new ce.ajneb97.gui.ItemGui(this), this);
          pm.registerEvents(new ce.ajneb97.gui.EventsGui(), this);

		if(serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_9_R1)){
			pm.registerEvents(new ItemSelectListenerNew(), this);
			pm.registerEvents(new PlayerEventsListenerNew1_9(this), this);
			pm.registerEvents(new OffHandListener(this), this);
		}
		if(serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_16_R3)){
			pm.registerEvents(new RepairListener(this), this);
		}
		if(serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_16_R1)){
			pm.registerEvents(new PlayerEventsListenerNew1_16(this), this);
		}

		if(dependencyManager.isCitizens()){
			pm.registerEvents(new CitizensListener(this), this);
		}
		if(dependencyManager.isWorldGuardEvents()){
			pm.registerEvents(new WGRegionEventsListener(this), this);
		}
		if(dependencyManager.isPaper()){
			if(serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_12_R1)){
				pm.registerEvents(new PaperEventsListener(this), this);
			}
		}
	}

	public void setPrefix(){
		prefix = MessagesManager.getLegacyColoredMessage("&4[&bConditionalEvents&4]");
	}

	public void setVersion(){
		String packageName = Bukkit.getServer().getClass().getPackage().getName();
		String minecraftVersion = ServerVersion.getMinecraftVersion();
		switch(minecraftVersion){
			case "1.20.5":
			case "1.20.6":
				serverVersion = ServerVersion.v1_20_R4;
				break;
			case "1.21":
			case "1.21.1":
				serverVersion = ServerVersion.v1_21_R1;
				break;
			case "1.21.2":
			case "1.21.3":
				serverVersion = ServerVersion.v1_21_R2;
				break;
			case "1.21.4":
				serverVersion = ServerVersion.v1_21_R3;
				break;
			case "1.21.5":
				serverVersion = ServerVersion.v1_21_R4;
				break;
			case "1.21.6":
			case "1.21.7":
			case "1.21.8":
				serverVersion = ServerVersion.v1_21_R5;
				break;
			case "1.21.9":
			case "1.21.10":
				serverVersion = ServerVersion.v1_21_R6;
				break;
			case "1.21.11":
				serverVersion = ServerVersion.v1_21_R7;
				break;
			case "26.1":
			case "26.1.1":
			case "26.1.2":
				serverVersion = ServerVersion.v26_1;
				break;
			case "26.2":
				serverVersion = ServerVersion.v26_2;
				break;
			case "26.3":
				serverVersion = ServerVersion.v26_3;
				break;
			default:
				try{
					serverVersion = ServerVersion.valueOf(packageName.replace("org.bukkit.craftbukkit.", ""));
				}catch(Exception e){
					serverVersion = ServerVersion.v26_3;
				}
		}
	}

	public void reloadEvents(){
		HandlerList.unregisterAll(this);
		registerEvents();
	}

	public void reloadPlayerDataSaveTask() {
		if(playerDataSaveTask != null) {
			playerDataSaveTask.end();
		}
		playerDataSaveTask = new PlayerDataSaveTask(this);
		playerDataSaveTask.start(configsManager.getMainConfigManager().getConfig().getInt("Config.data_save_time"));
	}

	public void registerCommands(){
		this.getCommand("pyxelcore").setExecutor(new MainCommand(this));
	}

	public EventsManager getEventsManager() {
		return eventsManager;
	}

	public DependencyManager getDependencyManager() {
		return dependencyManager;
	}

	public ConfigsManager getConfigsManager() {
		return configsManager;
	}

	public DebugManager getDebugManager() {
          return debugManager;
  }

  public StatsManager getStatsManager() {
          return statsManager;
  }

  public FragmentsManager getFragmentsManager() {
          return fragmentsManager;
  }

	public BungeeMessagingManager getBungeeMessagingManager() {
		return bungeeMessagingManager;
	}

	public PlayerManager getPlayerManager() {
		return playerManager;
	}

	public MessagesManager getMessagesManager() {
		return messagesManager;
	}

	public void setMessagesManager(MessagesManager messagesManager) {
		this.messagesManager = messagesManager;
	}

	public VerifyManager getVerifyManager() {
		return verifyManager;
	}

	public UpdateCheckerManager getUpdateCheckerManager() {
		return updateCheckerManager;
	}

	public CommandRegisterManager getCommandRegisterManager() {
		return commandRegisterManager;
	}

	public APIManager getApiManager() {
		return apiManager;
	}

	public SavedItemsManager getSavedItemsManager() {
		return savedItemsManager;
	}

	public InterruptEventManager getInterruptEventManager() {
		return interruptEventManager;
	}

	public void updateMessage(UpdateCheckerResult result){
		if(!result.isError()){
			String latestVersion = result.getLatestVersion();
			if(latestVersion != null){
				Bukkit.getConsoleSender().sendMessage(MessagesManager.getLegacyColoredMessage("&cThere is a new version available. &e(&7"+latestVersion+"&e)"));
				Bukkit.getConsoleSender().sendMessage(MessagesManager.getLegacyColoredMessage("&cYou can download it at: &fhttps://modrinth.com/plugin/conditionalevents"));
			}
		}else{
			Bukkit.getConsoleSender().sendMessage(MessagesManager.getLegacyColoredMessage(prefix+" &cError while checking update."));
		}

	}

}
