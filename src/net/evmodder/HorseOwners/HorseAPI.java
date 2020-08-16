package net.evmodder.HorseOwners;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.metadata.FixedMetadataValue;
import net.evmodder.EvLib.FileIO;
import net.evmodder.EvLib.util.IndexTreeMultiMap;
import net.evmodder.HorseOwners.api.events.HorseClaimEvent;
import net.evmodder.HorseOwners.api.events.HorseRenameEvent;

public class HorseAPI{
	private HorseManager pl;
	private YamlConfiguration horses;
	private HashMap<UUID, HashSet<String>> horseOwnersMap;
	private HashSet<String> oneTimeAccess;

	final boolean DO_RANKLISTS;
	private IndexTreeMultiMap<Double, String> topSpeed;
	private IndexTreeMultiMap<Double, String> topJump;
	private IndexTreeMultiMap<Integer, String> topHealth;
	final HashSet<EntityType> CLAIMABLE_TYPES;
	final boolean RELOAD_CONFIG_ON_EDIT, SAVE_UNCLAIMED;
	final boolean SAVE_EQUINE_STATS, SAVE_CLAIM_TS, SAVE_LINEAGE, SAVE_COORDS, SAVE_AGE, SAVE_SPAWN_DATA, SAVE_TAMER=true;

	public void loadHorses(){
		horses = FileIO.loadYaml("horses.yml", "#The great horse-data file\n");
		horseOwnersMap = new HashMap<>();
		for(String horseName : horses.getKeys(false)){
			ConfigurationSection data = horses.getConfigurationSection(horseName);
			if(data == null){horses.set(horseName, null); continue;}
			if(data.contains("owner")){
				UUID uuid = UUID.fromString(data.getString("owner"));
				Set<String> ownedHorses = horseOwnersMap.get(uuid);
				if(ownedHorses != null) ownedHorses.add(horseName);
				else horseOwnersMap.put(uuid, new HashSet<>(Arrays.asList(horseName)));
			}
			if(DO_RANKLISTS){
				if(data.contains("speed")) topSpeed.put(data.getDouble("speed"), horseName);
				if(data.contains("jump")) topJump.put(data.getDouble("jump"), horseName);
				if(data.contains("health")) topHealth.put(data.getInt("health"), horseName);
			}
		}
	}
	public void saveHorses(){
		try{horses.save(new File("./plugins/EvFolder/horses.yml"));}
		catch(IOException e){e.printStackTrace();}
	}

	HorseAPI(HorseManager pl){
		this.pl = pl;
		SAVE_EQUINE_STATS = pl.getConfig().getBoolean("save-equine-stats", true);
		DO_RANKLISTS = SAVE_EQUINE_STATS && pl.getConfig().getBoolean("build-ranklists", true);
		if(DO_RANKLISTS){
			topSpeed = new IndexTreeMultiMap<Double, String>();
			topJump = new IndexTreeMultiMap<Double, String>();
			topHealth = new IndexTreeMultiMap<Integer, String>();
		}
		SAVE_UNCLAIMED = pl.getConfig().getBoolean("save-unclaimed-horses", true);
		RELOAD_CONFIG_ON_EDIT = pl.getConfig().getBoolean("config-update-checking", true);
		SAVE_CLAIM_TS = pl.getConfig().getBoolean("save-claim-history", true);
		SAVE_LINEAGE = pl.getConfig().getBoolean("save-horse-lineage", true);
		SAVE_COORDS = pl.getConfig().getBoolean("save-horse-coordinates", true);
		SAVE_AGE = pl.getConfig().getBoolean("save-horse-age", true);
		SAVE_SPAWN_DATA = pl.getConfig().getBoolean("save-spawn-data", true);

		CLAIMABLE_TYPES = new HashSet<>();
		if(pl.getConfig().contains("valid-horses"))
			for(String type : pl.getConfig().getStringList("valid-horses"))
				try{CLAIMABLE_TYPES.add(EntityType.valueOf(type.toUpperCase()));}
				catch(IllegalArgumentException ex){ex.printStackTrace();}
		else CLAIMABLE_TYPES.addAll(Arrays.asList(
					EntityType.HORSE, EntityType.DONKEY, EntityType.MULE,
					EntityType.SKELETON_HORSE, EntityType.ZOMBIE_HORSE,
					EntityType.LLAMA, EntityType.TRADER_LLAMA));
		loadHorses();
		oneTimeAccess = new HashSet<String>();
	}

	//-------------------- Simple getters --------------------//
	public Set<EntityType> getClaimableHorseTypes(){return Collections.unmodifiableSet(CLAIMABLE_TYPES);}
	public Map<UUID, Set<String>> getHorseOwnersMap(){return Collections.unmodifiableMap(horseOwnersMap);}
	public Set<String> getHorses(UUID ownerUUID){return Collections.unmodifiableSet(horseOwnersMap.getOrDefault(ownerUUID, new HashSet<>()));}
	public Set<String> getAllHorses(){return Collections.unmodifiableSet(horses.getKeys(false));}
	public Set<String> getAllClaimedHorses(){
		return Collections.unmodifiableSet(horseOwnersMap.values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet()));
	}

	public final IndexTreeMultiMap<Double, String> getTopSpeed(){return topSpeed;} // TODO: make unmodifiable!
	public final IndexTreeMultiMap<Double, String> getTopJump(){return topJump;} // TODO: make unmodifiable!
	public final IndexTreeMultiMap<Integer, String> getTopHealth(){return topHealth;} // TODO: make unmodifiable!

	public boolean isClaimableHorseType(Entity h){
		return /*h instanceof AbstractHorse && */CLAIMABLE_TYPES.contains(h.getType());
	}

	//-------------------- Modifier methods --------------------//
	public boolean addClaimedHorse(UUID ownerUUID, Entity horse){
		if(horse.getCustomName() == null) return false;
		if(CLAIMABLE_TYPES.contains(horse.getType())) return false;
		HorseClaimEvent horseClaimEvent = new HorseClaimEvent(horse, ownerUUID, horse.getCustomName());
		pl.getServer().getPluginManager().callEvent(horseClaimEvent);
		if(horseClaimEvent.isCancelled()) return false;

		String cleanName = HorseUtils.cleanName(horse.getCustomName());
		long timestamp_now = System.currentTimeMillis();

		boolean newlyClaimed = true;
		Set<String> myHorses = horseOwnersMap.get(ownerUUID);
		if(myHorses != null) newlyClaimed = myHorses.add(cleanName);
		else horseOwnersMap.put(ownerUUID, new HashSet<>(Arrays.asList(cleanName)));
		if(newlyClaimed) HorseUtils.setClaimedBy(horse, ownerUUID, timestamp_now);

		if(RELOAD_CONFIG_ON_EDIT) loadHorses();
		updateDatabase(horse);
		horses.set(cleanName+".owner", ownerUUID.toString());
		if(SAVE_CLAIM_TS) horses.set(cleanName+".claim-ts", timestamp_now);
		saveHorses();
		return newlyClaimed;
	}
	public boolean addHorse(Entity horse){
		if(horse.getCustomName() == null || !SAVE_UNCLAIMED) return false;
		if(CLAIMABLE_TYPES.contains(horse.getType())) return false;
		String cleanName = HorseUtils.cleanName(horse.getCustomName());
		boolean newlyAdded = !horses.isConfigurationSection(cleanName);

		if(RELOAD_CONFIG_ON_EDIT) loadHorses();
		updateDatabase(horse);
		saveHorses();

		return newlyAdded;
	}
	public boolean removeHorse(String cleanHorseName){
		if(!horses.isConfigurationSection(cleanHorseName)) return false;
		ConfigurationSection cs = horses.getConfigurationSection(cleanHorseName);
		if(DO_RANKLISTS){
			topSpeed.remove(cs.getDouble("speed", -1), cleanHorseName);
			topJump.remove(cs.getDouble("jump", -1), cleanHorseName);
			topHealth.remove(cs.getInt("health", -1), cleanHorseName);
		}
		if(cs.contains("owner")){
			UUID ownerUUID = UUID.fromString(cs.getString("owner"));
			Set<String> ownedHorses = horseOwnersMap.get(ownerUUID);
			ownedHorses.remove(cleanHorseName);
			if(ownedHorses.isEmpty()) horseOwnersMap.remove(ownerUUID);
		}
		if(RELOAD_CONFIG_ON_EDIT) loadHorses();
		horses.set(cleanHorseName, null);
		saveHorses();
		return true;
	}
	public boolean unclaimHorse(String cleanHorseName){
		if(!SAVE_UNCLAIMED) return removeHorse(cleanHorseName);
		String uuidStr = horses.getString(cleanHorseName+".owner", null);
		if(uuidStr == null) return false;
		UUID ownerUUID = UUID.fromString(uuidStr);
		Set<String> ownedHorses = horseOwnersMap.get(ownerUUID);
		ownedHorses.remove(cleanHorseName);
		if(ownedHorses.isEmpty()) horseOwnersMap.remove(ownerUUID);
		if(RELOAD_CONFIG_ON_EDIT) loadHorses();
		horses.set(cleanHorseName+".owner", null);
		saveHorses();
		return true;
	}
	public boolean renameHorse(String oldNameClean, String newNameRaw){
		if(!horses.isConfigurationSection(oldNameClean)) return false;
		String oldNameRaw = horses.getString(oldNameClean+".name", oldNameClean);
		if(oldNameRaw.equals(newNameRaw)) return false;
		String newNameClean = HorseUtils.cleanName(newNameRaw);
		if(horses.isConfigurationSection(newNameClean) && !newNameClean.equals(oldNameClean)) return false;

		HorseRenameEvent horseRenameEvent = new HorseRenameEvent(oldNameClean, newNameClean, oldNameRaw, newNameRaw);
		pl.getServer().getPluginManager().callEvent(horseRenameEvent);
		if(horseRenameEvent.isCancelled()) return false;

		if(oldNameClean.equals(newNameClean)){
			horses.set(oldNameClean+".name", newNameRaw);
			return true;
		}

		if(RELOAD_CONFIG_ON_EDIT) loadHorses();
		ConfigurationSection oldHorseData = horses.getConfigurationSection(oldNameClean);
		ConfigurationSection newHorseData = horses.createSection(newNameClean);
		for(String key : oldHorseData.getKeys(false)) newHorseData.set(key, oldHorseData.get(key));
		horses.set(oldNameClean, null);
		newHorseData.set("name", newNameRaw);

		if(SAVE_LINEAGE) for(String h : horses.getKeys(false)){
			if(horses.getString(h+".mother", "").equals(oldNameClean)) horses.set(h+".mother", newNameClean);
			if(horses.getString(h+".father", "").equals(oldNameClean)) horses.set(h+".father", newNameClean);
		}
		saveHorses();

		boolean hasOwner = newHorseData.contains("owner");
		if(hasOwner){
			UUID ownerUUID = UUID.fromString(newHorseData.getString("owner"));
			Set<String> ownedHorses = horseOwnersMap.get(ownerUUID);
			ownedHorses.remove(oldNameClean);
			ownedHorses.add(newNameClean);
		}

		if(DO_RANKLISTS){
			double speed = newHorseData.getDouble("speed", -1);
			double jump = newHorseData.getDouble("jump", -1);
			int health = newHorseData.getInt("health", -1);
			if(topSpeed.remove(speed, oldNameClean)) topSpeed.put(speed, newNameClean);
			if(topJump.remove(jump, oldNameClean)) topJump.put(jump, newNameClean);
			if(topHealth.remove(health, oldNameClean)) topHealth.put(health, newNameClean);
		}
		return true;
	}
	public boolean setHorseNameLock(String cleanHorseName, boolean locked){
		if(!horses.isConfigurationSection(cleanHorseName)) return false;
		if(horses.getBoolean(cleanHorseName+".locked", !locked) == locked) return true;
		if(RELOAD_CONFIG_ON_EDIT) loadHorses();
		horses.set(cleanHorseName+".locked", locked);
		saveHorses();
		return true;
	}

	private void updateLineage(Entity h, ConfigurationSection data){
		//TODO: replace FixedMetadataValue with tags
		if(data.contains("mother")) h.setMetadata("mother", new FixedMetadataValue(pl, data.get("mother")));
		else if(h.hasMetadata("mother")) data.set("mother", h.getMetadata("mother").get(0).asString());
		if(data.contains("father")) h.setMetadata("father", new FixedMetadataValue(pl, data.get("father")));
		else if(h.hasMetadata("father")) data.set("father", h.getMetadata("father").get(0).asString());
		if(data.contains("dna")) h.setMetadata("dna", new FixedMetadataValue(pl, data.get("dna")));
		else if(h.hasMetadata("dna")) data.set("dna", h.getMetadata("dna").get(0).asString());
	}
	private void updateTameable(Tameable h, ConfigurationSection data){
		if(h.getOwner() != null) data.set("tamer", h.getOwner().getUniqueId().toString());
	}
	private void updateEquineStats(AbstractHorse h, ConfigurationSection data){
		double speed = HorseUtils.getNormalSpeed(h);
		double jump = HorseUtils.getNormalJump(h);
		int health = HorseUtils.getNormalMaxHealth(h);
		if(data.getDouble("speed", speed) != speed) topSpeed.remove(data.getDouble("speed", speed), data.getName());
		if(data.getDouble("jump", jump) != jump) topJump.remove(data.getDouble("jump", jump), data.getName());
		if(data.getInt("health", health) != health) topHealth.remove(data.getInt("health", health), data.getName());
		data.set("speed", speed);
		data.set("jump", jump);
		data.set("health", health);
		if(DO_RANKLISTS){
			topSpeed.put(speed, data.getName());
			topJump.put(jump, data.getName());
			topHealth.put(health, data.getName());
		}
	}
	public void updateDatabase(Entity h){
		if(h.getCustomName() == null) return;
		if(!SAVE_UNCLAIMED && !isClaimedHorse(h.getCustomName())) return;
		String displayName = h.getCustomName();
		String cleanHorseName = HorseUtils.cleanName(displayName);

		ConfigurationSection data = horses.getConfigurationSection(cleanHorseName);
		if(data == null) data = horses.createSection(cleanHorseName);

		data.set("name", displayName);
		data.set("uuid", h.getUniqueId().toString());
		data.set("type", h.getType().name());
		if(SAVE_COORDS){
			data.set("chunk-x", h.getLocation().getChunk().getX());
			data.set("chunk-z", h.getLocation().getChunk().getZ());
		}
		if(SAVE_LINEAGE) updateLineage(h, data);
		if(SAVE_AGE) data.set("age", h.getTicksLived()*50);
		if(SAVE_SPAWN_DATA){
			Long timeBorn = HorseUtils.getTimeBorn(h);
			if(timeBorn != null) data.set("birthdate", timeBorn);
			else if(data.contains("birthdate")) HorseUtils.setTimeBorn(h, data.getLong("birthdate"));
			SpawnReason spawnReason = HorseUtils.getSpawnReason(h);
			if(spawnReason != null) data.set("spawnreason", spawnReason.name());
			else if(data.contains("spawnreason")) HorseUtils.setSpawnReason(h, SpawnReason.valueOf(data.getString("spawnreason")));
			//TODO: save claimed_by events in horses.yml (uuid+timestamp pairs)
		}

		if(SAVE_TAMER && h instanceof Tameable) updateTameable((Tameable)h, data);
		if(SAVE_EQUINE_STATS && h instanceof AbstractHorse){
			updateEquineStats((AbstractHorse)h, data);
			if(h instanceof Llama) data.set("strength", ((Llama)h).getStrength());
		}
	}

	//-------------------- Accessor methods --------------------//
	public boolean horseExists(String cleanHorseName){return horses.isConfigurationSection(cleanHorseName);}
	public boolean isClaimedHorse(String cleanHorseName){return horses.contains(cleanHorseName+".owner");}
	public boolean isLockedHorse(String cleanHorseName){return horses.getBoolean(cleanHorseName+".locked", false);}
	public boolean isOwner(UUID ownerUUID, String cleanHorseName){
		return horses.getString(cleanHorseName+".owner", "").equals(ownerUUID.toString());
//		return horseOwnersMap.getOrDefault(ownerUUID, new HashSet<>()).contains(cleanHorseName);
	}
	@Deprecated
	public boolean canAccess(Player p, String cleanHorseName){
		if(isOwner(p.getUniqueId(), cleanHorseName) || !isClaimedHorse(cleanHorseName)) return true;
		if(p.hasPermission("horseowners.override")){
			p.sendMessage(ChatColor.GRAY+"Owner override");
			return true;
		}
		return false;
	}
	public boolean canAccess(UUID playerUUID, String cleanHorseName){
		if(isOwner(playerUUID, cleanHorseName) || !isClaimedHorse(cleanHorseName)) return true;
		Player player = pl.getServer().getPlayer(playerUUID);
		if(player != null && player.hasPermission("horseowners.override")){
			player.sendMessage(ChatColor.GRAY+"Owner override");
			return true;
		}
		return false;
	}

	public int[] getHorseRankings(String cleanHorseName){
		if(!DO_RANKLISTS) return null;
		ConfigurationSection cs = horses.getConfigurationSection(cleanHorseName);
		if(!cs.contains("speed")) return null;
		double speed = cs.getDouble("speed");
		double jump = cs.getDouble("jump");
		int health = cs.getInt("health");

		pl.getLogger().info("topSpeed.valuesSize(): "+topSpeed.valuesSize()
			+", topSpeed.getCeilingIndex("+speed+"): "+topSpeed.getCeilingIndex(speed)
			+", topSpeed.getFloorIndex("+speed+"): "+topSpeed.getFloorIndex(speed));
		int higherSpeedCount = topSpeed.valuesSize() - topSpeed.getCeilingIndex(speed), equalSpeedCount = topSpeed.get(speed).size();
		int higherJumpCount = topJump.valuesSize() - topJump.getCeilingIndex(jump), equalJumpCount = topJump.get(jump).size();
		int higherHealthCount = topHealth.valuesSize() - topHealth.getCeilingIndex(health), equalHealthCount = topHealth.get(health).size();
		return new int[]{//+1 = 1-based indexing
			higherSpeedCount+1, higherSpeedCount+equalSpeedCount,
			higherJumpCount+1, higherJumpCount+equalJumpCount,
			higherHealthCount+1, higherHealthCount+equalHealthCount
		};
	}
	public String getHorseName(String cleanHorseName){return horses.getString(cleanHorseName+".name", null);}
	public UUID getHorseOwner(String cleanHorseName){
		String uuidStr = horses.getString(cleanHorseName+".owner", null);
		return uuidStr == null ? null : UUID.fromString(uuidStr);
	}
	public UUID getHorseTamer(String cleanHorseName){
		String uuidStr = horses.getString(cleanHorseName+".tamer", null);
		return uuidStr == null ? null : UUID.fromString(uuidStr);
	}
	public String getHorseOwnerName(String cleanHorseName){
		UUID ownerUUID = getHorseOwner(cleanHorseName);
		OfflinePlayer owner = (ownerUUID == null ? null : pl.getServer().getOfflinePlayer(ownerUUID));
		return owner == null ? null : owner.getName();
	}
	public String getHorseTamerName(String cleanHorseName){
		UUID tamerUUID = getHorseTamer(cleanHorseName);
		OfflinePlayer tamer = (tamerUUID == null ? null : pl.getServer().getOfflinePlayer(tamerUUID));
		return tamer == null ? null : tamer.getName();
	}
	public double getHorseSpeed(String cleanHorseName){return horses.getDouble(cleanHorseName+".speed", -1);}
	public double getHorseJump(String cleanHorseName){return horses.getDouble(cleanHorseName+".jump", -1);}
	public int getHorseHealth(String cleanHorseName){return horses.getInt(cleanHorseName+".health", -1);}
	public int getLlamaStrength(String cleanHorseName){return horses.getInt(cleanHorseName+".strength", -1);}
	public long getHorseAge(String cleanHorseName){return horses.getLong(cleanHorseName+".age", -1);}
	public long getHorseClaimTime(String cleanHorseName){return horses.getLong(cleanHorseName+".claim-ts", -1);}

	public EntityType getHorseType(String cleanHorseName){
		String type = horses.getString(cleanHorseName+".type", null);
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
	public List<String> getHorseParents(String cleanHorseName){
		String mother = horses.getString(cleanHorseName+".mother", null);
		String father = horses.getString(cleanHorseName+".father", null);
		if(mother != null) return father == null ? Arrays.asList(mother) : Arrays.asList(mother, father);
		if(father != null) return Arrays.asList(father);
		return null;
	}
	public List<String> getHorseParents(Entity h){
		String mother = h.hasMetadata("mother") ? h.getMetadata("mother").get(0).asString() : null;
		String father = h.hasMetadata("father") ? h.getMetadata("father").get(0).asString() : null;
		if(h.getCustomName() != null && (mother == null || father == null)){
			String horseName = HorseUtils.cleanName(h.getCustomName());
			updateLineage(h, horses.getConfigurationSection(horseName));//Set parent metadata on entity
			if(mother == null) mother = horses.getString(horseName+".mother", null);
			if(father == null) father = horses.getString(horseName+".father", null);
		}
		if(mother != null) return father == null ? Arrays.asList(mother) : Arrays.asList(mother, father);
		if(father != null) return Arrays.asList(father);
		return null;
	}
	public Entity getHorse(String cleanHorseName, boolean loadChunk){
		pl.getLogger().info("called getHorse() for: "+cleanHorseName);
		if(horses.contains(cleanHorseName+".chunk-x")){
			ConfigurationSection data = horses.getConfigurationSection(cleanHorseName);
			int chunkX = data.getInt("chunk-x"), chunkZ = data.getInt("chunk-z");
			pl.getLogger().info("Chunk coords found, starting horse search...");
			for(World world : pl.getServer().getWorlds()){
				Chunk chunk = world.getChunkAt(chunkX, chunkZ);
				if(chunk.isLoaded() || (loadChunk && chunk.load(/*generate=*/false))){
					pl.getLogger().info("entities in chunk: "+chunk.getEntities().length);
					for(Entity e : chunk.getEntities())
						if(e.getCustomName() != null && isClaimableHorseType(e)
							&& HorseUtils.cleanName(e.getCustomName()).equals(cleanHorseName))
						return e;
				}
			}
		}
		return pl.getServer().getWorlds().stream().flatMap(world -> world.getEntities().stream()).filter(
			e -> e.getCustomName() != null && isClaimableHorseType(e) && HorseUtils.cleanName(e.getCustomName()).equals(cleanHorseName)
		).findAny().orElse(null);
	}
	public Location getHorseLocation(String cleanHorseName){
		Entity horse = getHorse(cleanHorseName, /*loadChunk=*/false);
		if(horse != null) return horse.getLocation();
		if(!horses.contains(cleanHorseName+".chunk-x")) return null;
		ConfigurationSection data = horses.getConfigurationSection(cleanHorseName);
		int chunkX = data.getInt("chunk-x"), chunkZ = data.getInt("chunk-z");
		return new Location(null, chunkX*16, -1, chunkZ*16);
	}

	//-------------------- Things to delete? --------------------//
	public void grantOneTimeAccess(UUID playerUUID, String horseName){
		oneTimeAccess.add(playerUUID+":"+HorseUtils.cleanName(horseName));
	}
	public boolean useOneTimeAccess(UUID playerUUID, String horseName){
		return oneTimeAccess.remove(playerUUID+":"+HorseUtils.cleanName(horseName));
	}

}
