package Evil_Code_HorseManager;
//package Evil_Code_HorseManager;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.HashMap;
//import java.util.Set;
//import java.util.UUID;
//
//import org.bukkit.OfflinePlayer;
//import org.bukkit.World;
//import org.bukkit.configuration.ConfigurationSection;
//import org.bukkit.configuration.file.FileConfiguration;
//import org.bukkit.configuration.file.YamlConfiguration;
//import org.bukkit.entity.Horse;
//import org.bukkit.entity.Player;
//import org.bukkit.plugin.java.JavaPlugin;
//
//import Evil_Code_HorseManager.listeners.*;
//import Evil_Code_HorseOwners.commands.CommandManager;
//
//public final class HorseManager_Old extends JavaPlugin{
//	private static HorseManager_Old plugin;
//	
//	private Map<UUID, Set<String>> horseOwners;
//	private Set<String> lockedHorses;
//	private Set<String> oneTimeAccess;
//	private Set<HorseData> horseStats;
//	private FileConfiguration config;
//	
//	@Override public void onEnable(){
////		getLogger().info("Loading " + getDescription().getFullName());
//		plugin = this;
//		config = FileIO.loadConfig(this, "config-horseowners.yml", getResource("/config.yml"));
//		
//		horseOwners = new HashMap<UUID, Set<String>>();
////		lockedHorses = new HashSet<String>();
//		horseStats = new HashSet<HorseData>();
//		oneTimeAccess = new HashSet<String>();
//		
////		//------- Load config --------------------------------
////		File file = new File("./plugins/EvFolder/config-horseowners.yml");
////		if(!file.exists()){
////			try{
////				BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/config.yml")));
////				
////				String line = reader.readLine();
////				StringBuilder builder = new StringBuilder(line);
////				
////				while((line = reader.readLine()) != null){
////					builder.append('\n');
////					builder.append(line);
////				}
////				reader.close();
////				
////				BufferedWriter writer = new BufferedWriter(new FileWriter(file));
////				writer.write(builder.toString()); writer.close();
////			}
////			catch(IOException ex1){getLogger().info(ex1.getStackTrace().toString());}
////		}
////		config = YamlConfiguration.loadConfiguration(file);
////		//----------------------------------------------------
//		registerListeners();
//		
//		//load files
////		loadOldFiles();
//		loadFiles();
//		
//		new CommandManager();
//	}
//	@Override public void onDisable(){saveFiles();}
//	
//	private void loadFiles(){
//		String horseFile = FileIO.loadFile("horses.txt", "");
//		if(horseFile.length() > 37){//if file isn't empty
//			for(String node : horseFile.substring(1, horseFile.length()-2).split("\\], ")){
//				horseOwners.put(UUID.fromString(node.split("=\\[")[0]),
//								new HashSet<String>(Arrays.asList(node.split("=\\[")[1].split(", "))));
//			}
//		}
//		
//		YamlConfiguration horseDataFile = YamlConfiguration.loadConfiguration(new File("./plugins/EvFolder/horses-data.txt"));
//		if(horseDataFile != null)
//		for(String horseName : horseDataFile.getKeys(false)){
//			ConfigurationSection data = horseDataFile.getConfigurationSection(horseName);
//			
//			horseStats.add(new HorseData(data.getString("name"),
//					data.getBoolean("locked"),
//					data.getInt("chunk-x"), data.getInt("chunk-z"),
//					data.getDouble("jump"), data.getDouble("speed"), data.getDouble("health")));
//		}
//		
//		
///*		//Next, load the 'locked-horses' file
//		String lockedHorseFile = FileIO.loadFile("locked-horses.txt", "");
//		if(lockedHorseFile.length() > 1){//if the file isn't empty
//			lockedHorses.addAll(Arrays.asList(lockedHorseFile.substring(1, lockedHorseFile.length()-1).split(", ")));
//		}*/
//		boolean saveCoords = config.getBoolean("save-horse-coordinates");
//		String dataFile = FileIO.loadFile("horses-rankings.txt", "");
//		boolean doRankings = config.getBoolean("rank-claimed-horses");
//		if(dataFile.length() > 4)//if the file isn't empty
//			for(String node : dataFile.substring(1, dataFile.length()-1).split(", ")){
//				String[] data = node.substring(1, node.length()-1).split(",");
//				if(doRankings && data.length == 6){
//					horseStats.add(new HorseData(data[0], Integer.parseInt(data[1]), Integer.parseInt(data[2]),
//							Double.parseDouble(data[3]), Double.parseDouble(data[4]), Double.parseDouble(data[5])));
//				}
//				else horseStats.add(new HorseData(data[0], Integer.parseInt(data[1]), Integer.parseInt(data[2])));
//			}
//		}
//		/* delete old data
//		File file = new File("./plugins/EvFolder/horse-rankings.txt");
//		if(file.exists()){
//			if(file.delete()) getLogger().info("Deleted file: "+file.getName());
//			else getLogger().warning("Error deleting file: "+file.getName());
//		}*/
//	}
//	
//	@SuppressWarnings("unused")
//	private void loadOldFiles(){
//		try{
//			BufferedReader reader = new BufferedReader(new FileReader("./plugins/EvFolder/horses.txt"));
//			String line = null;
//			UUID uuid;
//			while((line = reader.readLine()) != null){
//				line = line.replace(" ", "");
//				
//				if(line.contains(":")){
//					uuid = UUID.fromString(line.split(":")[1]);
//					if(horseOwners.containsKey(uuid)) horseOwners.get(uuid).add(line.split(":")[2]);
//					else horseOwners.put(UUID.fromString(line.split(":")[1]), new HashSet<String>(Arrays.asList(line.split(":")[2])));
//				}
//
//			}
//			reader.close();
//			//load locked
//			reader = new BufferedReader(new FileReader("./plugins/EvFolder/locked-horses.txt"));
//			
//			while((line = reader.readLine()) != null) if(line.contains("[") == false) lockedHorses.add(line);
//			reader.close();
//		}
//		catch(IOException e){getLogger().info(e.getStackTrace().toString());}
//	}
//	
//	private void registerListeners(){
//		if(config.getBoolean("modified-breeding")){
//			getServer().getPluginManager().registerEvents(new BreedListener(), this);
//		}
//		if(config.getBoolean("reduce-fall-damage")){
//			getServer().getPluginManager().registerEvents(new DamageListener(), this);
//		}
//		if(config.getBoolean("keep-horse-chunks-in-memory")){
//			getServer().getPluginManager().registerEvents(new ChunkUnloadListener(), this);
//		}
//		if(config.getBoolean("prevent-lead-breaking")){
//			getServer().getPluginManager().registerEvents(new LeadBreakListener(), this);
//		}
//		if(config.getBoolean("prevent-lead-fence-breaking")){
//			getServer().getPluginManager().registerEvents(new LeadFenceBreakListener(), this);
//		}
//		if(config.getBoolean("enable-teleporting")){
//			getServer().getPluginManager().registerEvents(new TeleportListener(), this);
//		}
//		if(config.getBoolean("claim-on-tame")){
//			getServer().getPluginManager().registerEvents(new TameListener(), this);
//		}
//		//the DeathListener is always used.
//		getServer().getPluginManager().registerEvents(new DeathListener(), this);
//		//the ClickListener is always used.
//		getServer().getPluginManager().registerEvents(new ClickListener(), this);
//	}
//	
//	private void saveFiles(){
//		if(!horseOwners.isEmpty()){
//			FileIO.saveFile("horses.txt", horseOwners.toString());
//		}
//		if(!lockedHorses.isEmpty()){
//			FileIO.saveFile("locked-horses.txt", lockedHorses.toString());
//		}
//		if(horseStats != null && !horseStats.isEmpty()){
//			FileIO.saveFile("horse-rankings.txt", horseStats.toString());
//		}
//	}
//	
//	//--------------- Member functions ------------------------------------------------------
//	public Set<String> getAllHorses(){
//		Set<String> horseList = new HashSet<String>();
//		for(Set<String> horses : horseOwners.values()) horseList.addAll(horses);
//		return horseList;
//	}
//	public Map<UUID, Set<String>> getHorseOwners(){return horseOwners;}
//	
//	
//	@Deprecated
//	public boolean canAccess(Player p, Horse h){
//		return (h.getCustomName() != null) ? canAccess(p, h.getCustomName()) : true;
//	}
//	public boolean canAccess(Player p, String horseName){
//		boolean isOwner = horseOwners.containsKey(p.getUniqueId())
//						&& HorseLibrary.containsIgnoreCaseAndColor(horseOwners.get(p.getUniqueId()), horseName);
//		
//		if(isOwner) return true;
//		else if(isPrivateHorse(horseName) == false) return true;
//		else if(p.hasPermission("evp.horseowners.override")){
//			p.sendMessage("�7Owner override");
//			return true;
//		}
//		else return false;
//	}
//	public boolean canAccess(UUID uuid, String horseName){
//		boolean isOwner = horseOwners.containsKey(uuid)
//						&& HorseLibrary.containsIgnoreCaseAndColor(horseOwners.get(uuid), horseName);
//		
//		if(isOwner) return true;
//		else if(isPrivateHorse(horseName) == false) return true;
//		else{
//			Player p = getServer().getPlayer(uuid);
//			if(p != null && p.hasPermission("evp.horseowners.override")){
//				p.sendMessage("�7Owner override");
//				return true;
//			}
//			else return false;
//		}
//	}
//	
//	@Deprecated
//	public boolean isOwner(Player p, Horse h){
//		return h.getCustomName() != null && horseOwners.containsKey(p.getUniqueId())
//				&& horseOwners.get(p.getUniqueId()).contains(h.getCustomName());
//	}
//	public boolean isOwner(Player p, String horseName){
//		return horseOwners.containsKey(p.getUniqueId()) &&
//				HorseLibrary.containsIgnoreCaseAndColor(horseOwners.get(p.getUniqueId()), (horseName));
//	}
//	public boolean isOwner(UUID uuid, String horseName){
//		return horseOwners.containsKey(uuid) && HorseLibrary.containsIgnoreCaseAndColor(horseOwners.get(uuid), (horseName));
//	}
//	
//	@Deprecated
//	public boolean isLockedHorse(Horse h){
//		return (h.getCustomName() != null && lockedHorses.contains(h.getCustomName()));
//	}
//	public boolean isLockedHorse(String horseName){
//		return lockedHorses.contains(horseName);
//	}
//	
//	@Deprecated
//	public void lockHorse(Horse h){
//		if(h.getCustomName() != null) lockedHorses.add(h.getCustomName());
//	}
//	public void lockHorse(String horseName){
//		lockedHorses.add(horseName);
//	}
//	
//	@Deprecated
//	public boolean isPrivateHorse(Horse h){
//		return (h.getCustomName() != null) ? isPrivateHorse(h.getCustomName()) : false;
//	}
//	public boolean isPrivateHorse(String horseName){
//		for(Set<String> horses : horseOwners.values()) if(HorseLibrary.containsIgnoreCaseAndColor(horses, horseName)) return true;
//		return false;
//	}
//	
//	public void addHorse(Player p, Horse h){
//		if(h.getCustomName() == null) return;
//		
//		if(horseOwners.containsKey(p.getUniqueId())) horseOwners.get(p.getUniqueId()).add(h.getCustomName());
//		else horseOwners.put(p.getUniqueId(), new HashSet<String>(Arrays.asList(h.getCustomName())));
//		
//		if(horseStats != null) addHorseToRankings(h);
//	}
//	public void addHorse(Player p, String horseName){
//		if(horseOwners.containsKey(p.getUniqueId())) horseOwners.get(p.getUniqueId()).add(horseName);
//		else horseOwners.put(p.getUniqueId(), new HashSet<String>(Arrays.asList(horseName)));
//		
//		if(horseStats != null) addHorseToRankings(HorseLibrary.findHorse(horseName));
//	}
//	
//	@Deprecated
//	public boolean removeHorse(Player p, Horse h){
//		if(h.getCustomName() == null) return false;
//		
//		if(horseStats != null) removeHorseFromRankings(h.getCustomName());
//		return (horseOwners.containsKey(p.getUniqueId()) && horseOwners.get(p.getUniqueId()).remove(h.getCustomName()));
//	}
//	public boolean removeHorse(Player p, String horseName){
//		if(horseStats != null) removeHorseFromRankings(horseName);
//		return (horseOwners.containsKey(p.getUniqueId()) && horseOwners.get(p.getUniqueId()).remove(horseName));
//	}
//	
//	@Deprecated
//	public boolean removeHorse(Horse h){
//		if(h.getCustomName() == null) return false;
//		
//		if(horseStats != null) removeHorseFromRankings(h.getCustomName());
//		boolean contained = false;
//		for(Set<String> horses : horseOwners.values()) if(horses.remove(h.getCustomName())) contained = true;
//		return contained;
//	}
//	public boolean removeHorse(String horseName){
//		if(horseStats != null) removeHorseFromRankings(horseName);
//		boolean contained = false;
//		for(Set<String> horses : horseOwners.values()) if(horses.remove(horseName)) contained = true;
//		return contained;
//	}
//	
//	@Deprecated
//	public OfflinePlayer getOwner(Horse h){
//		if(h.getCustomName() != null){
//			for(UUID uuid : horseOwners.keySet()){
//				if(horseOwners.get(uuid).contains(h.getCustomName())) return getServer().getOfflinePlayer(uuid);
//			}
//		}
//		return null;
//	}
//	public OfflinePlayer getOwner(String horseName){
//		for(UUID uuid : horseOwners.keySet()){
//			if(horseOwners.get(uuid).contains(horseName)) return getServer().getOfflinePlayer(uuid);
//		}
//		return null;
//	}
//	
//	@Deprecated
//	public void grantOneTimeAccess(Player p, Horse h){
//		if(h.getCustomName() != null) oneTimeAccess.add(p.getName()+':'+h.getCustomName());
//	}
//	public void grantOneTimeAccess(Player p, String horseName){
//		oneTimeAccess.add(p.getName()+':'+horseName);
//	}
//	public void grantOneTimeAccess(String playerName, String horseName){
//		oneTimeAccess.add(playerName+':'+horseName);
//	}
//	
//	@Deprecated
//	public boolean useOneTimeAccess(Player p, Horse h){
//		return (h.getCustomName() == null || oneTimeAccess.remove(p.getName()+':'+h.getCustomName()));
//	}
//	public boolean useOneTimeAccess(Player p, String horseName){
//		return oneTimeAccess.remove(p.getName()+':'+horseName);
//	}
//	public boolean useOneTimeAccess(String playerName, String horseName){
//		return oneTimeAccess.remove(playerName+':'+horseName);
//	}
//	
//	private void removeHorseFromRankings(String horseName){
//		for(HorseData horse : horseStats){
//			if(horseName.equals(horse.name)){
//				horseStats.remove(horse);
//				return;
//			}
//		}
//	}
//	private void addHorseToRankings(Horse h){
//		horseStats.add(new HorseData(h.getCustomName(),
//									HorseLibrary.getNormalJump(h),
//									HorseLibrary.getNormalSpeed(h),
//									h.getMaxHealth()));
//	}
//	
//	public int[] getRankings(HorseData horse){
//		if(horseStats == null || horseStats.isEmpty()) return null;
//		
//		int higherJumpCount=1, equalJumpCount=0;
//		int higherSpeedCount=1, equalSpeedCount=0;
//		int higherHealthCount=1, equalHealthCount=0;
//		boolean unranked = true;
//		for(HorseData h : horseStats){
//			if(h.name.equals(horse.name)){unranked=false; continue;}
//			if(h.jump > horse.jump) ++higherJumpCount;
//			else if(h.jump == horse.jump) ++equalJumpCount;
//			if(h.speed > horse.speed) ++higherSpeedCount;
//			else if(h.speed == horse.speed) ++equalSpeedCount;
//			if(h.health > horse.health) ++higherHealthCount;
//			else if(h.health == horse.health) ++equalHealthCount;
//		}
//		if(unranked) horseStats.add(horse);
//		return new int[]{
//			higherJumpCount,higherJumpCount+equalJumpCount,
//			higherSpeedCount,higherSpeedCount+equalSpeedCount,
//			higherHealthCount,higherHealthCount+equalHealthCount
//		};
//	}
//	
//	public static HorseManager_Old getPlugin(){return plugin;}
//	@Override public FileConfiguration getConfig(){return config;}
//	@Override public void saveConfig(){
//		try{config.save(new File("./plugins/EvFolder/config-manager.yml"));}
//		catch(IOException ex){ex.printStackTrace();}
//	}
//}
