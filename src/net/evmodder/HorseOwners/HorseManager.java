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
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import net.evmodder.EvLib.EvPlugin;
import net.evmodder.EvLib.FileIO;
import net.evmodder.EvLib.util.IndexTreeMultiMap;
import net.evmodder.HorseOwners.commands.*;
import net.evmodder.HorseOwners.listeners.*;

public final class HorseManager extends EvPlugin{
	private static HorseManager plugin; public static HorseManager getPlugin(){return plugin;}

	private Set<String> oneTimeAccess;
	private Set<EntityType> claimableTypes;
	private YamlConfiguration horses;
	private Map<UUID, Set<String>> horseOwnersMap;
	private boolean saveCoords, saveRankings, saveLineage, saveLeashes, rankUnclaimed, safeEditing;
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
		saveRankings = config.getBoolean("rank-claimed-horses", true);
		saveLineage = config.getBoolean("save-horse-lineage", true);
		rankUnclaimed = config.getBoolean("rank-unclaimed-horses", true);
		safeEditing = config.getBoolean("config-update-checking", true);
		saveLeashes = config.getBoolean("prevent-glitch-lead-breaking", true);
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
		topSpeed = new IndexTreeMultiMap<Double, String>();
		topJump = new IndexTreeMultiMap<Double, String>();
		topHealth = new IndexTreeMultiMap<Integer, String>();
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
			if(saveRankings && data.contains("speed") && data.contains("jump") && data.contains("health"))
				updateRanklists(horseName, data.getDouble("speed"), data.getDouble("jump"), data.getInt("health"));
		}
	}

	public void saveHorses(){
		try{horses.save(new File("./plugins/EvFolder/horses.yml"));}
		catch(IOException e){e.printStackTrace();}
	}

	private void registerListeners(){
		if(config.getBoolean("modified-breeding", false)){
			getServer().getPluginManager().registerEvents(new BreedListener(), this);
		}
		if(config.getBoolean("reduce-fall-damage", true)){
			getServer().getPluginManager().registerEvents(new DamageListener(), this);
		}
		if(config.getBoolean("save-horse-coordinates", true)
		|| config.getBoolean("keep-horse-chunks-in-memory", false)
		|| saveLeashes){
			getServer().getPluginManager().registerEvents(new ChunkUnloadListener(), this);
		}
		if(config.getBoolean("prevent-lead-breaking", true)){
			getServer().getPluginManager().registerEvents(new LeadBreakListener(), this);
		}
		if(config.getBoolean("prevent-lead-fence-breaking", true)){
			getServer().getPluginManager().registerEvents(new LeadFenceBreakListener(), this);
		}
		if(saveLeashes){
			getServer().getPluginManager().registerEvents(new LeadBreakGlitchListener(), this);
		}
		if(config.getBoolean("enable-teleporting", true)){
			getServer().getPluginManager().registerEvents(new TeleportListener(), this);
		}
		if(config.getBoolean("claim-on-tame", true)){
			getServer().getPluginManager().registerEvents(new TameListener(), this);
		}
		if(rankUnclaimed){
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
		if(config.getBoolean("rank-claimed-horses", false)) new CommandTopHorse();
		new CommandUnleashHorse();
	}

	//--------------- Member functions ------------------------------------------------------
	//TODO: Move into a library
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

	public boolean canAccess(Player p, String horseName){
		if(horseName == null) return true;
		horseName = HorseLibrary.cleanName(horseName);
		boolean isOwner = horseOwnersMap.containsKey(p.getUniqueId())
					&& HorseLibrary.containsIgnoreCaseAndColor(horseOwnersMap.get(p.getUniqueId()), horseName);
		
		if(isOwner) return true;
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
		boolean isOwner = horseOwnersMap.containsKey(playerUUID)
				&& HorseLibrary.containsIgnoreCaseAndColor(horseOwnersMap.get(playerUUID), horseName);
		
		if(isOwner) return true;
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
		return horseName != null && horseOwnersMap.containsKey(uuid) && HorseLibrary
				.containsIgnoreCaseAndColor(horseOwnersMap.get(uuid), HorseLibrary.cleanName(horseName));
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

	public boolean addHorse(UUID playerUUID, Entity horse){
		if(horse.getCustomName() == null) return false;
		String cleanName = HorseLibrary.cleanName(horse.getCustomName());

		if(safeEditing) loadHorses();

		if(horseOwnersMap.containsKey(playerUUID)) horseOwnersMap.get(playerUUID).add(cleanName);
		else horseOwnersMap.put(playerUUID, new HashSet<String>(Arrays.asList(cleanName)));

		horses.set(cleanName+".owner", playerUUID.toString());
		if(horses.contains(cleanName) && horse instanceof AbstractHorse) updateData((AbstractHorse)horse);
		saveHorses();
		return true;
	}

	public boolean addUnownedHorse(AbstractHorse h){
		if(h.getCustomName() == null) return false;
		String cleanName = HorseLibrary.cleanName(h.getCustomName());

		boolean wild = true;
		for(Set<String> horses : horseOwnersMap.values()) if(horses.contains(cleanName)) wild = false;
		if(wild){
			if(safeEditing) loadHorses();
			if(!horses.contains(cleanName)) horses.createSection(cleanName);
			updateData(h);
			saveHorses();
			return true;
		}
		return false;
	}

	public void renameHorse(String oldName, String newName){
		oldName = HorseLibrary.cleanName(oldName);
		newName = HorseLibrary.cleanName(newName);

		ConfigurationSection thisHorse = horses.getConfigurationSection(oldName);
		horses.set(oldName, null);
		horses.set(newName, thisHorse);

		if(saveLineage)
		for(String h : horses.getKeys(false)){
			List<String> parents = horses.getStringList(h+".parents");

			if(parents.remove(oldName)){
				parents.add(newName);
				horses.set(h+".parents", parents);
			}
		}

		for(Set<String> horses : horseOwnersMap.values()) if(horses.remove(oldName)) horses.add(newName);
	}

	public boolean removeHorse(UUID playerUUID, String horseName, boolean removeCompletely){
		horseName = HorseLibrary.cleanName(horseName);
		Set<String> myHorses = horseOwnersMap.get(playerUUID);
		if(myHorses != null && myHorses.remove(horseName)){
			if(safeEditing) loadHorses();
			if(removeCompletely){
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

	public void updateData(AbstractHorse h){
		if(h.getCustomName() == null) return;
		if(rankUnclaimed == false && isClaimedHorse(h.getCustomName()) == false) return;

		String displayName = h.getCustomName();
		String horseName = HorseLibrary.cleanName(displayName);

		ConfigurationSection data = horses.getConfigurationSection(horseName);
		if(data == null) data = horses.createSection(horseName);

		data.set("name", displayName);// Full name (including spaces and/or special chars)
		data.set("uuid", h.getUniqueId().toString());//TODO: Preparing for the eventuality of names not being unique
		if(h.getOwner() != null) data.set("tamer", h.getOwner().getUniqueId().toString());

		if(saveRankings && (rankUnclaimed || data.contains("owner"))){
			double speed = HorseLibrary.getNormalSpeed(h);
			double jump = HorseLibrary.getNormalJump(h);
			int health = HorseLibrary.getNormalMaxHealth(h);
			data.set("speed", speed);
			data.set("jump", jump);
			data.set("health", health);
			updateRanklists(horseName, speed, jump, health);
		}
		if(saveCoords){
			data.set("chunk-x", h.getLocation().getChunk().getX());
			data.set("chunk-z", h.getLocation().getChunk().getZ());
		}
		if(saveLeashes && h.isLeashed() && h.getLeashHolder().getType() == EntityType.LEASH_HITCH){
			data.set("leash-x", h.getLeashHolder().getLocation().getBlockX());
			data.set("leash-y", h.getLeashHolder().getLocation().getBlockY());
			data.set("leash-z", h.getLeashHolder().getLocation().getBlockZ());
		}
		if(saveLineage){
//			List<String> parents = new ArrayList<String>();
//			for(MetadataValue parent : h.getMetadata("parents")) parents.add(parent.asString());
//			if(!parents.isEmpty()) data.set("parents", parents);
			if(data.contains("mother")) h.setMetadata("mother", new FixedMetadataValue(plugin, data.get("mother")));
			else if(h.hasMetadata("mother")) data.set("mother", h.getMetadata("mother").get(0).asString());

			if(data.contains("father")) h.setMetadata("father", new FixedMetadataValue(plugin, data.get("father")));
			else if(h.hasMetadata("father")) data.set("father", h.getMetadata("father").get(0).asString());
		}
	}

	public int[] getRankings(String horseName){
		if(!saveRankings) return new int[]{-1, -1, -1};
		horseName = HorseLibrary.cleanName(horseName);

		if(!horses.contains(horseName)) return null;
		ConfigurationSection data = horses.getConfigurationSection(horseName);
		double jump = data.getDouble("jump"), speed = data.getDouble("speed");
		int health = data.getInt("health");

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
		plugin.getLogger().info("name: '"+horseName+"', owner: '"+uuid+"'");
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
		if(!saveRankings || !horses.contains(horseName)) return -1;
		return horses.getConfigurationSection(horseName).getDouble("speed", -1);
	}
	public double getHorseJump(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
		if(!saveRankings || !horses.contains(horseName)) return -1;
		return horses.getConfigurationSection(horseName).getDouble("jump", -1);
	}
	public int getHorseHealth(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
		if(!saveRankings || !horses.contains(horseName)) return -1;
		return horses.getConfigurationSection(horseName).getInt("health", -1);
	}
	public Location getHorseHitch(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
		if(horses.contains(horseName+".leash-x")) return new Location(
				getServer().getWorlds().get(0),
				horses.getDouble(horseName+".leash-x"),
				horses.getDouble(horseName+".leash-y"),
				horses.getDouble(horseName+".leash-z"));
		else return null;
	}
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
			if(horses.contains(horseName+".mother")){
				if(h instanceof AbstractHorse) updateData((AbstractHorse)h);
				return Arrays.asList(
						horses.getString(horseName+".mother", null),
						horses.getString(horseName+".father", null));
			}
		}
		return null;
	}
	public int getHorseBlockX(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
		if(!saveCoords || !horses.contains(horseName)) return -1;
		return horses.getConfigurationSection(horseName).getInt("chunk-x", -1)*16;
	}
	public int getHorseBlockZ(String horseName){
		horseName = HorseLibrary.cleanName(horseName);
		if(!saveCoords || !horses.contains(horseName)) return -1;
		return horses.getConfigurationSection(horseName).getInt("chunk-z", -1)*16;
	}
}