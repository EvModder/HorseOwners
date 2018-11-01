package Evil_Code_HorseOwners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import com.onarandombox.MultiverseCore.MultiverseCore;

public class HorseLibrary {
	public static SpeedCalculator speedCalc = new SpeedCalculator(/*org.bukkit.Bukkit.getBukkitVersion()*/);
	private static String defaultWorld = org.bukkit.Bukkit.getServer().getWorlds().get(0).getName();//[0] is the default world
	private static GameMode defaultGameMode = org.bukkit.Bukkit.getServer().getDefaultGameMode();
	//--------------- Library functions -----------------------------------------------------
	public static boolean isHorseFood(ItemStack item){
		Material mat = item.getType();
		if(mat == Material.SUGAR || mat == Material.WHEAT || 
		   mat == Material.GOLDEN_APPLE || mat == Material.GOLDEN_CARROT || 
		   mat == Material.BREAD || mat == Material.APPLE ||
		   mat == Material.HAY_BLOCK) return true;
		else return false;
	}

	public static boolean isBreedingFood(ItemStack item){
		Material mat = item.getType();
		if(mat == Material.GOLDEN_APPLE || mat == Material.GOLDEN_CARROT) return true;
		else return false;
	}

	public static String cleanName(String horseName){
		return horseName == null ? null :
			ChatColor.stripColor(horseName).replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
	}

	public static boolean notFar(Location from, Location to){
		int x1 = from.getBlockX(), y1 = from.getBlockY(), z1 = from.getBlockZ();
		int x2 = to.getBlockX(), y2 = to.getBlockY(), z2 = to.getBlockZ();

		return(Math.abs(Math.abs(x1)-Math.abs(x2)) < 20
			&& Math.abs(Math.abs(y1)-Math.abs(y2)) < 6
			&& Math.abs(Math.abs(z1)-Math.abs(z2)) < 20
			&& from.getWorld().getName().equals(to.getWorld().getName()));
	}

	public static boolean safeForHorses(Location loc){
		int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
		World w = loc.getWorld();
		for(int cx = -1; cx <= 1; ++cx){
			for(int cz = -1; cz <= 1; ++cz){
				for(int cy = 0; cy <= 2; ++cy){
					if(w.getBlockAt(x +cx, y +cy, z+cz).getType().isOccluding()) return false;
				}
			}
		}
		return true;
	}

	public static void teleportEntityWithPassengers(Entity e, Location toLoc){
		if(e.getPassengers() == null || e.getPassengers().isEmpty()){
			if(!e.getLocation().getChunk().isLoaded()) e.getLocation().getChunk().load();
			if(!toLoc.getChunk().isLoaded()) toLoc.getChunk().load();
			e.teleport(toLoc);
		}
		else{
			List<Entity> passengers = e.getPassengers();
			e.eject();
			for(Entity passenger : passengers){
				teleportEntityWithPassengers(passenger, toLoc);
			}
			e.teleport(toLoc);
			for(Entity passenger : passengers){
				e.addPassenger(passenger);
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static void teleportEntityWithPassengersOld(Entity e, Location toLoc){
		if(e.getPassenger() == null){
			HorseManager.getPlugin().getLogger().info("No passenger");
			if(!e.getLocation().getChunk().isLoaded()) e.getLocation().getChunk().load();
			e.teleport(toLoc);
		}
		else{
			List<Entity> passengers = new ArrayList<Entity>();
			Entity nextPassenger = e;
			do{
				passengers.add(nextPassenger.getPassenger());
				nextPassenger.eject();

				if(!nextPassenger.getLocation().getChunk().isLoaded()) nextPassenger.getLocation().getChunk().load();
				nextPassenger.teleport(toLoc);
				nextPassenger = passengers.get(passengers.size()-1);
			}while(nextPassenger != null);

			nextPassenger = e;
			for(Entity passenger : passengers){
				nextPassenger.setPassenger(passenger);
				nextPassenger = passenger;//last value in "passengers" will be null.
			}
		}
	}

	public static AbstractHorse findAnyHorse(String horseName, World... worlds){
		horseName = cleanName(horseName);
		for(World world : (worlds == null ? org.bukkit.Bukkit.getWorlds() : Arrays.asList(worlds))){
			for(AbstractHorse horse : world.getEntitiesByClass(AbstractHorse.class)){
				if(horse.getCustomName() != null && cleanName(horse.getCustomName()).equals(horseName)){
					return horse;
				}
			}
		}
		return null;
	}

	public static boolean isLeashableBlock(Material mat){
		return (mat == Material.FENCE || mat == Material.NETHER_FENCE || mat == Material.BIRCH_FENCE ||
				mat == Material.SPRUCE_FENCE || mat == Material.JUNGLE_FENCE || mat == Material.ACACIA_FENCE ||
				mat == Material.DARK_OAK_FENCE);
	}

	public static boolean containsIgnoreCaseAndColor(Set<String> list, String search){
		search = ChatColor.stripColor(search);
		for(String test : list) if(ChatColor.stripColor(test).equalsIgnoreCase(search)) return true;
		return false;
	}

	public static GameMode getWorldGameMode(World world){
		MultiverseCore mv = (MultiverseCore) org.bukkit.Bukkit.getPluginManager().getPlugin("Multiverse-Core");
		if(mv != null && mv.isEnabled()) return mv.getMVWorldManager().getMVWorld(world).getGameMode();
		else if(world.getName().contains(defaultWorld)) return defaultGameMode;//contains => NAME, NAME_nether, & NAME_the_end
		else return null;
	}

/*	public static boolean isSuffocating(Block b){
	Material mat = b.getType();
	if(mat != Material.AIR && mat != Material.TORCH && mat != Material.CARPET && mat != Material.BED &&
	mat != Material.FENCE && mat != Material.FENCE_GATE && mat != Material.DEAD_BUSH && mat != Material.FLOWER_POT &&
	mat != Material.RED_ROSE && mat != Material.YELLOW_FLOWER && mat != Material.SKULL_ITEM && mat != Material.CAKE_BLOCK &&
	mat != Material.ACTIVATOR_RAIL && mat != Material.RAILS && mat != Material.POWERED_RAIL && mat != Material.VINE &&
	mat != Material.SIGN_POST && mat != Material.WALL_SIGN && mat != Material.SUGAR_CANE_BLOCK && mat != Material.WOOD_DOOR &&
	mat != Material.WEB && mat != Material.TRAP_DOOR &&  mat != Material.WOOD_STAIRS && mat != Material.TRAPPED_CHEST &&
	mat != Material.WOOD_BUTTON && mat != Material.CHEST && mat != Material.LEVER && mat != Material.LEAVES &&
	mat != Material.SNOW && mat != Material.DIODE_BLOCK_OFF && mat != Material.REDSTONE && mat != Material.REDSTONE_TORCH_ON &&
	mat != Material.REDSTONE_TORCH_OFF && mat != Material.DIODE_BLOCK_ON && mat != Material.GLASS && mat != Material.IRON_FENCE &&
	mat != Material.BREWING_STAND && mat != Material.STAINED_GLASS_PANE && mat != Material.DETECTOR_RAIL && mat != Material.DEAD_BUSH
		) return true;
		else return false;
	}*/

	public static void setMother(Entity horse, String mother){
		horse.setMetadata("mother", new FixedMetadataValue(HorseManager.getPlugin(), mother));
	}
	public static void setFather(Entity horse, String father){
		horse.setMetadata("father", new FixedMetadataValue(HorseManager.getPlugin(), father));
	}

	public static String getMother(Entity horse){
		return horse.hasMetadata("mother") ? horse.getMetadata("mother").get(0).asString() : null;
	}
	public static String getFather(Entity horse){
		return horse.hasMetadata("father") ? horse.getMetadata("father").get(0).asString() : null;
	}

	public static double getNormalSpeed(Entity horse){
		return normalizeSpeed(speedCalc.getHorseSpeed(horse));
	}
	public static double getNormalJump(AbstractHorse horse){
		return normalizeJump(horse.getJumpStrength());
	}
	public static int getNormalHealth(LivingEntity horse){
		return (int)Math.rint(horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
	}
	public static double normalizeSpeed(double speed){
		return Math.rint(speed*1000) / 25;
	}
	public static double normalizeJump(double jump){
		return Math.rint(jump*500) / 100;
	}
	public static double denormalizeSpeed(double speed){
		return 0.025*speed;
	}
	public static double denormalizeJump(double jump){
		return 0.2*jump;
	}
	public static <E extends Attributable & Damageable> void setMaxHealth(E target, double health){
		target.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
		target.setHealth(health);
	}
}