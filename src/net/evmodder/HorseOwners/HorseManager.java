package net.evmodder.HorseOwners;

import net.evmodder.EvLib.EvPlugin;
import net.evmodder.HorseOwners.commands.*;
import net.evmodder.HorseOwners.listeners.*;

//OBSOLETE: Make inbred mutation relative to % DNA shared (look online for calculation) // Using simulated DNA
//OBSOLETE: Display spawn reason in /hm inspect
//DONE-TEST: add /hm rename <a> <b> with perm for renaming without riding
//DONE-TEST: Display age in /hm inspect
//DONE-TEST: BreedListener above natural limits
//DONE-TEST: move all "save-X" settings to config
//TODO: /hm list by type (eg: /hm list type:DONKEY -> list donkeys only) AND/OR sort /hm list (by claim date?)
//TODO: remote claiming
//TODO: /hm spawn from console (but must provide world,x,y,z)
//TODO: more comprehensive /hm copy
//TODO: replace flags in EditableHorseAttributes with an enum
//TODO: when creating a baby by using a spawn egg on an adult, copy the DNA?
//TODO: llama strength leaderboard?
public final class HorseManager extends EvPlugin{
	private static HorseManager plugin; public static HorseManager getPlugin(){return plugin;}
	private HorseAPI horseAPI; public HorseAPI getAPI(){return horseAPI;}

	@Override public void onEvEnable(){
		if(config.getBoolean("update-plugin", true)){
			//new Updater(this, 888777, getFile(), Updater.UpdateType.DEFAULT, false);
		}
		plugin = this;
		horseAPI = new HorseAPI(this);
		registerListeners();
		registerCommands();
	}
	@Override public void onEvDisable(){getLogger().info("disabling...");}

	/*public void loadHorses(){
		horses = FileIO.loadYaml("horses.yml", "#The great horse-data file\n");
		if(saveStats){
			topSpeed = new IndexTreeMultiMap<Double, String>();
			topJump = new IndexTreeMultiMap<Double, String>();
			topHealth = new IndexTreeMultiMap<Integer, String>();
		}
		for(String horseName : horses.getKeys(false)){
			ConfigurationSection data = horses.getConfigurationSection(horseName);
			if(data == null){
				horses.set(horseName, null);
				continue;
			}
			if(data.contains("owner")){
				UUID uuid = UUID.fromString(data.getString("owner"));
				if(horseOwnersMap.containsKey(uuid)) horseOwnersMap.get(uuid).add(horseName);
				else horseOwnersMap.put(uuid, new HashSet<String>(Arrays.asList(horseName)));
			}
			if(saveStats && data.contains("speed") && data.contains("jump") && data.contains("health"))
				updateRanklists(horseName, data.getDouble("speed"), data.getDouble("jump"), data.getInt("health"));
		}
	}

	public void saveHorses(){
		try{horses.save(new File("./plugins/EvFolder/horses.yml"));}
		catch(IOException e){e.printStackTrace();}
	}*/

	private void registerListeners(){
		double MAX_JUMP = config.getDouble("max-jump", -1);
		double MAX_SPEED = config.getDouble("max-speed", -1);
		double MAX_HEALTH = config.getDouble("max-health", -1);
		if(MAX_JUMP != -1 || MAX_SPEED != -1 || MAX_HEALTH != -1
			|| plugin.getConfig().getBoolean("save-horse-lineage", false)
			|| config.getBoolean("name-at-birth", false) || config.getBoolean("claim-at-birth", false)
			|| config.getBoolean("extra-random-factor-at-birth", false) || config.getBoolean("inbred-mutations", false)){
			getServer().getPluginManager().registerEvents(new BreedListener(), this);
		}
		if(config.getBoolean("reduce-fall-damage", true)){
			getServer().getPluginManager().registerEvents(new DamageListener(), this);
		}
		if(config.getBoolean("save-horse-coordinates", true)
			|| config.getBoolean("keep-horse-chunks-in-memory", false)
		//	|| saveLeashes
		){
			getServer().getPluginManager().registerEvents(new ChunkUnloadListener(), this);
		}
		if(config.getBoolean("prevent-lead-breaking", true)){
			getServer().getPluginManager().registerEvents(new LeadBreakListener(), this);
		}
		if(config.getBoolean("prevent-lead-fence-breaking", true)){
			getServer().getPluginManager().registerEvents(new LeadFenceBreakListener(), this);
		}
		/*if(saveLeashes){
			getServer().getPluginManager().registerEvents(new LeadBreakGlitchListener(), this);
		}*/
		if(config.getBoolean("save-horse-coordinates", true)){
			try{
				Class.forName("org.spigotmc.event.entity.EntityDismountEvent");
				getServer().getPluginManager().registerEvents(new DismountListener(), this);
			}
			catch(ClassNotFoundException e){}
		}
		if(config.getBoolean("enable-teleporting", true)){
			getServer().getPluginManager().registerEvents(new TeleportListener(), this);
		}
		if(config.getBoolean("claim-on-tame", true) || !config.getString("notify-on-tame", "").isBlank()){
			getServer().getPluginManager().registerEvents(new TameListener(), this);
		}
		if(config.getBoolean("save-unclaimed-horses", true) || MAX_JUMP != -1 || MAX_SPEED != -1 || MAX_HEALTH != -1){
			getServer().getPluginManager().registerEvents(new SpawnListener(), this);
		}
		// DeathListener is always used.
		getServer().getPluginManager().registerEvents(new DeathListener(), this);
		// ClickListener is always used.
		getServer().getPluginManager().registerEvents(new ClickListener(), this);
	}

	private void registerCommands(){
		new CommandHorseManager();
		new CommandAllowRide();
		new CommandClaimHorse();
		new CommandCopyHorse();
		new CommandEditHorse();
		new CommandFreeHorse();
		new CommandGetHorse();
		new CommandInspectHorse();
		new CommandListHorse();
		if(config.getBoolean("enable-name-locks", false)) new CommandLockHorse();
		new CommandShadowfax();
		new CommandSpawnHorse();
		if(config.getBoolean("rank-claimed-horses", false)) new CommandRankHorse();
		new CommandRenameHorse();
		new CommandUnleashHorse();
	}
}