package Evil_Code_HorseOwners.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.onarandombox.MultiverseCore.MultiverseCore;

import Evil_Code_HorseOwners.HorseLibrary;
import Evil_Code_HorseOwners.HorseManager;

public class TeleportListener implements Listener{
	private boolean teleportLeashedMobs, teleportOnlyIfNamed, teleportUnleashedHorses;
	private HorseManager plugin;

	public TeleportListener(){
		plugin = HorseManager.getPlugin();
		if(teleportLeashedMobs = plugin.getConfig().getBoolean("teleport-leashed-mobs")){
			teleportOnlyIfNamed = plugin.getConfig().getBoolean("teleport-leashed-mobs-if-named");
		}
		teleportUnleashedHorses = plugin.getConfig().getBoolean("teleport-unleashed-owned-horses");
	}

	//Bring horses when teleporting
	@EventHandler(priority = EventPriority.MONITOR)
	public void onTp(PlayerTeleportEvent evt){
		if(evt.isCancelled()) return;

		if(evt.getFrom().getWorld().getName().equals(evt.getTo().getWorld().getName()) == false &&
				evt.getPlayer().hasPermission("evp.horseowners.tpansworld.*") == false){
			if(evt.getPlayer().hasPermission("evp.horseowners.tpansworld.samegamemode")){
				MultiverseCore mv = (MultiverseCore) plugin.getServer().getPluginManager().getPlugin("Multiverse-Core");
				if(mv == null || mv.isEnabled() == false ||
					mv.getMVWorldManager().getMVWorld(evt.getFrom().getWorld()).getGameMode() != 
					mv.getMVWorldManager().getMVWorld(evt.getTo().getWorld()).getGameMode())
				{
					return;//Unable to teleport to a world with a different gamemode
				}
			}
			else{
				return;//Unable to teleport to a different world
			}
		}

		//Teleporting must be more than 20 blocks 
		if(HorseLibrary.notFar(evt.getFrom(), evt.getTo())) return;

		boolean safeForHorses = HorseLibrary.safeForHorses(evt.getTo());
		if(safeForHorses == false && teleportLeashedMobs == false) return;

		Player player = evt.getPlayer();
		for(Entity e : player.getNearbyEntities(5, 5, 5)){
			if(e instanceof LivingEntity){
				if(e instanceof AbstractHorse){
					if(safeForHorses){
						AbstractHorse horse = (AbstractHorse)e;
						boolean owned = horse.getCustomName() != null && plugin.isOwner(player.getUniqueId(), horse.getCustomName());
						
						if((horse.isLeashed() && horse.getLeashHolder().getUniqueId().equals(player.getUniqueId())) ||
							(!horse.isLeashed() && teleportUnleashedHorses
							 && player.getNearbyEntities(4, 1, 4).contains(horse) && owned))
						{
							plugin.getLogger().info("Teleporting horse, owned: "+owned);
							HorseLibrary.teleportEntityWithPassengers(e, evt.getTo());
						}//if leashed or if isOwner
					}//if safe for horses
				}//if entity == Horse
				else if(teleportLeashedMobs && ((LivingEntity)e).isLeashed()
					&& ((LivingEntity)e).getLeashHolder().getUniqueId().equals(player.getUniqueId())
					&& (e.getCustomName() != null || teleportOnlyIfNamed == false))
				{
					plugin.getLogger().info("Teleporting with leashed LivingEntity");
					HorseLibrary.teleportEntityWithPassengers(e, evt.getTo());
				}//for(entities)
			}
		}//for(surrounding entities)
	}
}