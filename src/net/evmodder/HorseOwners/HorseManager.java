package net.evmodder.HorseOwners;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.metadata.FixedMetadataValue;
import net.evmodder.EvLib.EvPlugin;
import net.evmodder.EvLib.FileIO;
import net.evmodder.EvLib.util.IndexTreeMultiMap;
import net.evmodder.HorseOwners.api.events.HorseClaimEvent;
import net.evmodder.HorseOwners.api.events.HorseRenameEvent;
import net.evmodder.HorseOwners.commands.*;
import net.evmodder.HorseOwners.listeners.*;

//OBSOLETE: Make inbred mutation relative to % DNA shared (look online for calculation) // Using simulated DNA
//OBSOLETE: Display spawn reason in /hm inspect (
//DONE-TEST: add /hm rename <a> <b> with perm for renaming without riding
//DONE-TEST: Display age in /hm inspect
//TODO: /hm list by type (eg: /hm list type:DONKEY -> list donkeys only) AND/OR sort /hm list (by claim date?)
//TODO: fix tab-complete for /hm spawn
//TODO: BreedListener (above natural limits)
//TODO: Add 'isClean' bool to args for all library calls to skip cleanName
//TODO: Enable remote claiming
public final class HorseManager extends EvPlugin{
	private static HorseManager plugin; public static HorseManager getPlugin(){return plugin;}

	private Set<String> oneTimeAccess;
	private Set<EntityType> claimableTypes;
	private YamlConfiguration horses;
	private Map<UUID, Set<String>> horseOwnersMap;
	private boolean saveCoords, saveStats, saveLineage, saveAge=true, saveClaimHistory/*, saveLeashes,*/;
	private boolean rankUnclaimed, safeEditing;
	private IndexTreeMultiMap<Double, String> topSpeed;
	private IndexTreeMultiMap<Double, String> topJump;
	private IndexTreeMultiMap<Integer, String> topHealth;

	@Override public void onEvEnable(){
		if(config.getBoolean("update-plugin", true)){
			//new Updater(this, 888777, getFile(), Updater.UpdateType.DEFAULT, false);
		}
		plugin = this;
		horseOwnersMap = new HashMap<UUID, Set<String>>();
		oneTimeAccess = new HashSet<String>();
		saveCoords = config.getBoolean("save-horse-coordinates", true);
		saveStats = config.getBoolean("rank-claimed-horses", true);
		saveLineage = config.getBoolean("save-horse-lineage", true);
		rankUnclaimed = config.getBoolean("rank-unclaimed-horses", true);
		safeEditing = config.getBoolean("config-update-checking", true);
		//saveLeashes = config.getBoolean("prevent-glitch-lead-breaking", true);
		claimableTypes = new HashSet<EntityType>();
		if(config.contains("valid-horses"))
			for(String type : config.getStringList("valid-horses"))
				try{claimableTypes.add(EntityType.valueOf(type.toUpperCase()));}
				catch(IllegalArgumentException ex){ex.printStackTrace();}
		else
			claimableTypes.addAll(Arrays.asList(new EntityType[]{
					EntityType.HORSE, EntityType.DONKEY, EntityType.MULE,
					EntityType.SKELETON_HORSE, EntityType.ZOMBIE_HORSE,
					EntityType.LLAMA, EntityType.TRADER_LLAMA}));
		loadHorses();
		registerListeners();
		registerCommands();
	}
	@Override public void onEvDisable(){getLogger().info("disabling...");}

	public void loadHorses(){
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
	}

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
		if(saveCoords){
			try{
				Class.forName("org.spigotmc.event.entity.EntityDismountEvent");
				getServer().getPluginManager().registerEvents(new DismountListener(), this);
			}
			catch(ClassNotFoundException e){}
		}
		if(config.getBoolean("enable-teleporting", true)){
			getServer().getPluginManager().registerEvents(new TeleportListener(), this);
		}
		if(config.getBoolean("claim-on-tame", true)){
			getServer().getPluginManager().registerEvents(new TameListener(), this);
		}
		if(rankUnclaimed || MAX_JUMP != -1 || MAX_SPEED != -1 || MAX_HEALTH != -1){
			getServer().getPluginManager().registerEvents(new SpawnListener(), this);
		}
		//the DeathListener is always used.
		getServer().getPluginManager().registerEvents(new DeathListener(), this);
		//the ClickListener is always used.
		getServer().getPluginManager().registerEvents(new ClickListener(), this);
	}

	private void registerCommands(){
		new CommandHorseManager();
		new CommandAllowRide();
		new CommandClaimHorse();
		new CommandCopyHorse();
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

	//--------------- Member functions ------------------------------------------------------
	//TODO: Move into an API
	public Set<String> getAllClaimedHorses(){
		Set<String> horseList = new HashSet<String>();
		for(Set<String> horses : horseOwnersMap.values()) horseList.addAll(horses);
		return horseList;
	}
	public Set<String> getAllHorses(){return horses.getKeys(false);}
	public Map<UUID, Set<String>> getHorseOwners(){return horseOwnersMap;}
	public int getDatabaseSize(){return horses.getKeys(false).size();}

	public boolean isClaimableHorseType(Entity h){
		return /*h instanceof AbstractHorse && */claimableTypes.contains(h.getType());
	}
	public Set<EntityType> getClaimableHorseTypes(){return new HashSet<>(claimableTypes);}

	public boolean canAccess(Player p, String horseName){
		if(horseName == null) return true;
		horseName = HorseLibrary.cleanName(horseName);
		
		if(isOwner(p.getUniqueId(), horseName)) return true;
		else if(isClaimedHorse(horseName) == false) return true;
		else if(p.hasPermission("horseowners.override")){
			p.sendMessage(ChatColor.GRAY+"Owner override");
			return true;
		}
		else return false;
	}
	public boolean canAccess(UUID playerUUID, String horseName){
		if(horseName == null) return true;
		horseName = HorseLibrary.cleanName(horseName);
		
		if(isOwner(playerUUID, horseName)) return true;
		else if(isClaimedHorse(horseName) == false) return true;
		else{
			Player p = getServer().getPlayer(playerUUID);
			if(p != null && p.hasPermission("horseowners.override")){
				p.sendMessage(ChatColor.GRAY+"Owner override");
				return true;
			}
			else return false;
		}
	}

	public boolean isOwner(UUID uuid, String horseName){
		final Set<String> horseOwner;
		return horseName != null && (horseOwner = horseOwnersMap.get(uuid)) != null
				&& HorseLibrary.containsIgnoreCaseAndColor(horseOwner, HorseLibrary.cleanName(horseName));
	}

	public boolean isLockedHorse(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
		return horses.contains(horseName) && horses.getConfigurationSection(horseName).getBoolean("locked");
	}

	public void lockHorse(String horseName){
		if(safeEditing) loadHorses();
		horses.getConfigurationSection(HorseLibrary.cleanName(horseName)).set("locked", true);
		saveHorses();
	}

	public boolean isClaimedHorse(String horseName){
		return horses.contains(HorseLibrary.cleanName(horseName)+".owner");
	}

	public boolean horseExists(String horseName){
		return horses.isConfigurationSection(HorseLibrary.cleanName(horseName));
	}

	public boolean addClaimedHorse(UUID playerUUID, Entity horse){// Or reclaim existing horse
		if(horse.getCustomName() == null) return false;

		HorseClaimEvent horseClaimEvent = new HorseClaimEvent(horse, playerUUID, horse.getCustomName());
		getServer().getPluginManager().callEvent(horseClaimEvent);
		if(horseClaimEvent.isCancelled()) return false;

		String cleanName = HorseLibrary.cleanName(horse.getCustomName());
		long timestamp_now = System.currentTimeMillis();

		boolean newlyClaimed = true;
		Set<String> myHorses = horseOwnersMap.get(playerUUID);
		if(myHorses != null) newlyClaimed = myHorses.add(cleanName);
		else horseOwnersMap.put(playerUUID, new HashSet<String>(Arrays.asList(cleanName)));
		if(newlyClaimed) HorseLibrary.setClaimedBy(horse, playerUUID, timestamp_now);

		if(safeEditing) loadHorses();
		updateData(horse);
		horses.set(cleanName+".owner", playerUUID.toString());
		if(saveClaimHistory) horses.set(cleanName+".claim-ts", timestamp_now);
		saveHorses();
		return newlyClaimed;
	}

	public boolean addUnownedHorse(AbstractHorse h){
		if(h.getCustomName() == null) return false;
		String cleanName = HorseLibrary.cleanName(h.getCustomName());

		boolean wasUnclaimed = true;
		for(Set<String> horses : horseOwnersMap.values()) if(horses.contains(cleanName)) wasUnclaimed = false;
		if(wasUnclaimed){
			if(safeEditing) loadHorses();
			updateData(h);
			saveHorses();
		}
		return wasUnclaimed;
	}

	public boolean renameHorse(String oldName, String newNameRaw){
		oldName = HorseLibrary.cleanName(oldName);
		String oldNameRaw = horses.getString(oldName+".name", oldName); //Don't trust caller to give oldNameRaw
		if(oldNameRaw.equals(newNameRaw)) return false;
		String newName = HorseLibrary.cleanName(newNameRaw);
		if(horses.isConfigurationSection(newName)) return false;

		HorseRenameEvent horseRenameEvent = new HorseRenameEvent(oldName, newName, oldNameRaw, newNameRaw);
		getServer().getPluginManager().callEvent(horseRenameEvent);
		if(horseRenameEvent.isCancelled()) return false;

		if(oldName.equals(newName)){
			horses.set(oldName+".name", newNameRaw);
			return true;
		}

		if(safeEditing) loadHorses();
		ConfigurationSection oldHorse = horses.getConfigurationSection(oldName);
		ConfigurationSection newHorse = horses.createSection(newName);
		if(oldHorse != null){
			for(String key : oldHorse.getKeys(false)) newHorse.set(key, oldHorse.get(key));
			horses.set(oldName, null);
		}
		horses.set(newName+".name", newNameRaw);
		getLogger().info("horses.yml entry renamed.");
		saveHorses();

		if(saveLineage) for(String h : horses.getKeys(false)){
			if(horses.getString(h+".mother", "").equals(oldName)) horses.set(h+".mother", newName);
			if(horses.getString(h+".father", "").equals(oldName)) horses.set(h+".father", newName);
		}

		for(Set<String> horses : horseOwnersMap.values()) if(horses.remove(oldName)) horses.add(newName);

		if(saveStats && (rankUnclaimed || oldHorse.contains("owner"))){
			double speed = oldHorse.getDouble("speed", -1);
			double jump = oldHorse.getDouble("jump", -1);
			int health = oldHorse.getInt("health", -1);
			if(speed != -1){topSpeed.remove(speed, oldName); topSpeed.put(speed, newName);}
			if(jump != -1){topJump.remove(jump, oldName); topJump.put(jump, newName);}
			if(health != -1){topHealth.remove(health, oldName); topHealth.put(health, newName);}
		}
		return true;
	}

	public boolean removeHorse(UUID playerUUID, String horseName, boolean removeCompletely){
		horseName = HorseLibrary.cleanName(horseName);
		Set<String> myHorses = horseOwnersMap.get(playerUUID);
		if(myHorses != null && myHorses.remove(horseName)){
			if(safeEditing) loadHorses();
			if(removeCompletely){
				getLogger().info("Removing all horse data for: "+horseName);
				ConfigurationSection cs = horses.getConfigurationSection(horseName);
				topSpeed.remove(cs.getDouble("speed", -1), horseName);
				topJump.remove(cs.getDouble("jump", -1), horseName);
				topHealth.remove(cs.getInt("health", -1), horseName);
				horses.set(horseName, null);
			}
			else horses.getConfigurationSection(horseName).set("owner", null);
			if(myHorses.isEmpty()) horseOwnersMap.remove(playerUUID);
			saveHorses();
			return true;
		}
		return false;
	}

	public boolean removeHorse(String horseName, boolean removeCompletely){
		horseName = HorseLibrary.cleanName(horseName);
		boolean contained = false;
//		Set<UUID> emptyOwners = new HashSet<UUID>();
		for(Entry<UUID, Set<String>> horseOwner : horseOwnersMap.entrySet()){
			if(horseOwner.getValue().remove(horseName)){
				contained = true;
//				if(horseOwner.getValue().isEmpty()) emptyOwners.add(horseOwner.getKey());
				if(horseOwner.getValue().isEmpty()) horseOwnersMap.remove(horseOwner.getKey());
				break;
			}
		}
//		if(contained) for(UUID uuid : emptyOwners) horseOwnersMap.remove(uuid);
		if(horses.contains(horseName)){
			contained = true;
			if(safeEditing) loadHorses();
			if(removeCompletely){
				ConfigurationSection cs = horses.getConfigurationSection(horseName);
				topSpeed.remove(cs.getDouble("speed", -1), horseName);
				topJump.remove(cs.getDouble("jump", -1), horseName);
				topHealth.remove(cs.getInt("health", -1), horseName);
				horses.set(horseName, null);
			}
			else horses.set(horseName+".owner", null);
			saveHorses();
		}
		return contained;
	}

	public void grantOneTimeAccess(UUID playerUUID, String horseName){
		oneTimeAccess.add(playerUUID+":"+HorseLibrary.cleanName(horseName));
	}
	public boolean useOneTimeAccess(UUID playerUUID, String horseName){
		return oneTimeAccess.remove(playerUUID+":"+HorseLibrary.cleanName(horseName));
	}

	public Entity findClaimedHorse(String horseName, World... worlds){
		getLogger().info("called findClaimedHorse!");
		getLogger().info("savecoords: "+saveCoords);
		getLogger().info("havecoords: "+horses.contains((horseName = HorseLibrary.cleanName(horseName))+".chunk-x"));
		if(saveCoords && horses.contains((horseName = HorseLibrary.cleanName(horseName))+".chunk-x")){
			getLogger().info("Starting horse search...");

			int x = horses.getInt(horseName+".chunk-x"), z = horses.getInt(horseName+".chunk-z");

			for(World world : (worlds == null ? getServer().getWorlds() : Arrays.asList(worlds))){
				Chunk chunk = world.getChunkAt(x, z);
				chunk.load(false);
				getLogger().info("entities in chunk: "+chunk.getEntities().length);
				for(Entity entity : chunk.getEntities()){
					if(entity.getCustomName() != null && isClaimableHorseType(entity)){
						getLogger().info("Horse: "+entity.getCustomName());
						if(HorseLibrary.cleanName(entity.getCustomName()).equals(horseName)){
							getLogger().info("Found!");
							return entity;
						}
					}
				}//for each entity
			}//for each world
		}//if saveCoords
		return HorseLibrary.findAnyHorse(horseName, worlds);
	}

	private void updateRanklists(String horseName, double speed, double jump, int health){
		topSpeed.put(speed, horseName);
		topJump.put(jump, horseName);
		topHealth.put(health, horseName);
	}

	public final IndexTreeMultiMap<Double, String> getTopSpeed(){return topSpeed;}
	public final IndexTreeMultiMap<Double, String> getTopJump(){return topJump;}
	public final IndexTreeMultiMap<Integer, String> getTopHealth(){return topHealth;}
	public final ConfigurationSection getData(String horseName){return horses.getConfigurationSection(horseName);}

	private void updateLineage(Entity h, ConfigurationSection data){
//		List<String> parents = new ArrayList<String>();
//		for(MetadataValue parent : h.getMetadata("parents")) parents.add(parent.asString());
//		if(!parents.isEmpty()) data.set("parents", parents);
		if(data.contains("mother")) h.setMetadata("mother", new FixedMetadataValue(plugin, data.get("mother")));
		else if(h.hasMetadata("mother")) data.set("mother", h.getMetadata("mother").get(0).asString());

		if(data.contains("father")) h.setMetadata("father", new FixedMetadataValue(plugin, data.get("father")));
		else if(h.hasMetadata("father")) data.set("father", h.getMetadata("father").get(0).asString());
	}
	private void updateTameable(Tameable h, ConfigurationSection data){
		if(/*saveTamer &&*/h.getOwner() != null) data.set("tamer", h.getOwner().getUniqueId().toString());
	}
	/*private void updateLivingEntity(LivingEntity h, ConfigurationSection data){
		if(saveLeashes && h.isLeashed() && h.getLeashHolder().getType() == EntityType.LEASH_HITCH){
			data.set("leash-x", h.getLeashHolder().getLocation().getBlockX());
			data.set("leash-y", h.getLeashHolder().getLocation().getBlockY());
			data.set("leash-z", h.getLeashHolder().getLocation().getBlockZ());
		}
	}*/
	private void updateHorseStats(AbstractHorse h, ConfigurationSection data){
		if(saveStats && (rankUnclaimed || data.contains("owner"))){
			double speed = HorseLibrary.getNormalSpeed(h);
			double jump = HorseLibrary.getNormalJump(h);
			int health = HorseLibrary.getNormalMaxHealth(h);
			data.set("speed", speed);
			data.set("jump", jump);
			data.set("health", health);
			updateRanklists(data.getName(), speed, jump, health);
		}
	}
	public void updateData(Entity h){
		if(h.getCustomName() == null) return;
		if(rankUnclaimed == false && isClaimedHorse(h.getCustomName()) == false) return;

		String displayName = h.getCustomName();
		String horseName = HorseLibrary.cleanName(displayName);

		ConfigurationSection data = horses.getConfigurationSection(horseName);
		if(data == null) data = horses.createSection(horseName);

		data.set("name", displayName);// Full name (including spaces and/or special chars)
		data.set("uuid", h.getUniqueId().toString());//TODO: Prepare for the eventuality non-unique names
		data.set("type", h.getType().name());
		if(saveCoords){
			data.set("chunk-x", h.getLocation().getChunk().getX());
			data.set("chunk-z", h.getLocation().getChunk().getZ());
		}
		if(saveLineage) updateLineage(h, data);
		if(saveAge) data.set("age", h.getTicksLived()*50);

		if(h instanceof Tameable) updateTameable((Tameable)h, data);
		//if(h instanceof LivingEntity) updateLivingEntity((LivingEntity)h, data);
		if(h instanceof AbstractHorse) updateHorseStats((AbstractHorse)h, data);
	}

	public int[] getRankings(String horseName){
		if(!saveStats) return null;
		horseName = HorseLibrary.cleanName(horseName);

		if(!horses.contains(horseName+".speed")) return null;
		ConfigurationSection data = horses.getConfigurationSection(horseName);
		double speed = data.getDouble("speed"), jump = data.getDouble("jump");
		int health = data.getInt("health");

		getLogger().info("topSpeed.valuesSize(): "+topSpeed.valuesSize()
			+", topSpeed.getCeilingIndex("+speed+"): "+topSpeed.getCeilingIndex(speed)
			+", topSpeed.getFloorIndex("+speed+"): "+topSpeed.getFloorIndex(speed));
		int higherSpeedCount = topSpeed.valuesSize() - topSpeed.getCeilingIndex(speed);
		int equalSpeedCount = topSpeed.get(speed).size();
		int higherJumpCount = topJump.valuesSize() - topJump.getCeilingIndex(jump);
		int equalJumpCount = topJump.get(jump).size();
		int higherHealthCount = topHealth.valuesSize() - topHealth.getCeilingIndex(health);
		int equalHealthCount = topHealth.get(health).size();
		return new int[]{//+1 = 1-based indexing
			higherSpeedCount+1, higherSpeedCount+equalSpeedCount,
			higherJumpCount+1, higherJumpCount+equalJumpCount,
			higherHealthCount+1, higherHealthCount+equalHealthCount
		};
	}

	public String getHorseName(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
		if(!horses.contains(horseName)) return null;
		return horses.getConfigurationSection(horseName).getString("name", null);
	}
	public UUID getHorseOwner(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
		String uuid = horses.getString(horseName+".owner");
		plugin.getLogger().fine("GetHorseOwner() called, name: '"+horseName+"', owner: '"+uuid+"'");
		return uuid == null ? null : UUID.fromString(uuid);
		/*
		for(UUID uuid : horseOwnersMap.keySet()){
			if(horseOwnersMap.get(uuid).contains(horseName)) return uuid;
		}
		return null;
		*/
	}
	public String getHorseOwnerName(String horseName){
		UUID uuid = plugin.getHorseOwner(horseName);
		OfflinePlayer owner = (uuid == null ? null : plugin.getServer().getOfflinePlayer(uuid));
		return owner == null ? null : owner.getName();
	}
	public UUID getHorseTamer(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
		String uuid = horses.getString(horseName+".tamer");
		return uuid == null ? null : UUID.fromString(uuid);
	}
	public String getHorseTamerName(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
		UUID uuid = getHorseTamer(horseName);
		OfflinePlayer tamer = (uuid == null ? null : plugin.getServer().getOfflinePlayer(uuid));
		return tamer == null ? null : tamer.getName();
	}
	public double getHorseSpeed(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
		if(!saveStats || !horses.contains(horseName)) return -1;
		return horses.getConfigurationSection(horseName).getDouble("speed", -1);
	}
	public double getHorseJump(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
		if(!saveStats || !horses.contains(horseName)) return -1;
		return horses.getConfigurationSection(horseName).getDouble("jump", -1);
	}
	public int getHorseHealth(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
		if(!saveStats || !horses.contains(horseName)) return -1;
		return horses.getConfigurationSection(horseName).getInt("health", -1);
	}
	public long getHorseAge(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
		if(!saveAge || !horses.contains(horseName)) return -1;
		return horses.getConfigurationSection(horseName).getLong("age", -1);
	}
	public long getHorseClaimTime(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
		if(!saveClaimHistory || !horses.contains(horseName)) return -1;
		return horses.getConfigurationSection(horseName).getLong("claim-ts", -1);
	}
	public EntityType getHorseType(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
		String type = horses.getString(horseName+".type");
		if(type == null) return null;
		try{return EntityType.valueOf(type);}
		catch(IllegalArgumentException ex){return null;}
	}
	/*public Location getHorseHitch(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
		if(horses.contains(horseName+".leash-x")) return new Location(
				getServer().getWorlds().get(0),
				horses.getDouble(horseName+".leash-x"),
				horses.getDouble(horseName+".leash-y"),
				horses.getDouble(horseName+".leash-z"));
		else return null;
	}*/
	public List<String> getHorseParents(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
//		return horses.getStringList(horseName+".parents");
		if(horses.contains(horseName+".mother"))
			return Arrays.asList(
					horses.getString(horseName+".mother", null),
					horses.getString(horseName+".father", null));
		else return null;
	}
	public List<String> getHorseParents(Entity h){
		if(h.hasMetadata("mother"))
			return Arrays.asList(
					h.getMetadata("mother").get(0).asString(),
					h.getMetadata("father").get(0).asString());
		else if(h.getCustomName() != null){
			String horseName = HorseLibrary.cleanName(h.getCustomName());
			if(horses.contains(horseName+".mother")/*or father*/){
				updateLineage(h, horses.getConfigurationSection(horseName));//Set parent metadata on entity
				return Arrays.asList(
						horses.getString(horseName+".mother", null),
						horses.getString(horseName+".father", null));
			}
		}
		return null;
	}
	public Integer getHorseBlockX(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
		if(!saveCoords || !horses.contains(horseName+".chunk-x")) return null;
		ConfigurationSection data = horses.getConfigurationSection(horseName);
		int chunkX = data.getInt("chunk-x"), chunkZ = data.getInt("chunk-z", -1);
		for(World w : getServer().getWorlds()) if(w.isChunkLoaded(chunkX, chunkZ)){
			for(Entity e : w.getChunkAt(chunkX, chunkZ).getEntities()) if(isClaimableHorseType(e)
				&& e.getCustomName() != null && HorseLibrary.cleanName(e.getCustomName()).equals(horseName)){
				return e.getLocation().getBlockX();
			}
		}
		return data.getInt("chunk-x")*16;
	}
	public Integer getHorseBlockZ(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
		if(!saveCoords || !horses.contains(horseName+".chunk-z")) return null;
		ConfigurationSection data = horses.getConfigurationSection(horseName);
		int chunkX = data.getInt("chunk-x", -1), chunkZ = data.getInt("chunk-z");
		for(World w : getServer().getWorlds()) if(w.isChunkLoaded(chunkX, chunkZ)){
			for(Entity e : w.getChunkAt(chunkX, chunkZ).getEntities()) if(isClaimableHorseType(e)
				&& e.getCustomName() != null && HorseLibrary.cleanName(e.getCustomName()).equals(horseName)){
				return e.getLocation().getBlockZ();
			}
		}
		return data.getInt("chunk-z")*16;
	}
}