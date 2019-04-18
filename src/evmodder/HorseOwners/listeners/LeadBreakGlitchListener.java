package evmodder.HorseOwners.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import evmodder.HorseOwners.HorseLibrary;
import evmodder.HorseOwners.HorseManager;

public class LeadBreakGlitchListener implements Listener{
	private HorseManager plugin;

	public LeadBreakGlitchListener(){
		plugin = HorseManager.getPlugin();
	}

	@EventHandler
	public void onLeashBreak(HangingBreakEvent evt){
		if(evt instanceof HangingBreakByEntityEvent
				&& ((HangingBreakByEntityEvent)evt).getRemover() instanceof Player) return;//Handled separately

		if(evt.getEntity() instanceof LeashHitch && HorseLibrary
				.isLeashableBlock(evt.getEntity().getLocation().getBlock().getType()))
		{
			for(Entity e : evt.getEntity().getNearbyEntities(15, 15, 15)){
				if(e.getCustomName() != null && plugin.isClaimableHorseType(e) && e instanceof LivingEntity
						&& plugin.isClaimedHorse(e.getCustomName()))
				{
					LivingEntity h = (LivingEntity) e;
					if(h.isLeashed() && h.getLeashHolder().getUniqueId().equals(evt.getEntity().getUniqueId())){
						evt.setCancelled(true);
					}//if entity is attached to leash
				}//if entity is claimed horse
			}//for each nearby entity
		}//if broken hanging entity is a fence-post leash
	}//when hanging entity breaks

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent evt){
		if(evt.isNewChunk()) return;
//		final HashMap<Location, Entity> leashes = new HashMap<Location, Entity>();

		for(Entity e : evt.getChunk().getEntities()){
			if(e.getCustomName() != null && plugin.isClaimableHorseType(e) && e instanceof LivingEntity
					&& plugin.isClaimedHorse(e.getCustomName()))
			{
				final Location loc = plugin.getHorseHitch(e.getCustomName());
				if(loc != null){
					loc.setWorld(evt.getWorld());
					if(HorseLibrary.isLeashableBlock(loc.getBlock().getType())){
						final LivingEntity h = (LivingEntity) e;
						new BukkitRunnable(){@Override public void run(){
							if(h != null && h.isValid() && !h.isDead()){
								if(h.isLeashed()){
									h.getLeashHolder().remove();
									h.setLeashHolder(null);
								}
//								if(leashes.containsKey(loc)) h.setLeashHolder(leashes.get(loc));
//								else{
									Entity leash = h.getWorld().spawnEntity(loc, EntityType.LEASH_HITCH);
									h.setLeashHolder(leash);
//									leashes.put(loc, leash);
//								}
							}
						}}.runTaskLater(plugin, 40);//wait a full 2 seconds
					}
				}
			}
		}
	}
}