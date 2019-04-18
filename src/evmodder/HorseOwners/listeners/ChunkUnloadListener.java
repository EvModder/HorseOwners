package evmodder.HorseOwners.listeners;

import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import evmodder.HorseOwners.HorseManager;

public class ChunkUnloadListener implements Listener{
	private HorseManager plugin;
	boolean keepLoaded;//, saveCoords, saveLeashes;//Logic-d away

	public ChunkUnloadListener(){
		plugin = HorseManager.getPlugin();
		keepLoaded = plugin.getConfig().getBoolean("keep-horse-chunks-in-memory", false);
//		saveCoords = plugin.getConfig().getBoolean("save-horse-coordinates", true);
//		saveLeashes = plugin.getConfig().getBoolean("prevent-glitch-lead-breaking", true);
	}

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent evt){
		for(Entity entity : evt.getChunk().getEntities()){
			if(entity.getCustomName() != null && plugin.isClaimableHorseType(entity)
				&& plugin.isClaimedHorse(entity.getCustomName()))
			{
				if(keepLoaded){
					UUID uuid = plugin.getHorseOwner(entity.getCustomName());
					if(uuid != null){
						OfflinePlayer owner = plugin.getServer().getOfflinePlayer(uuid);
						if(owner != null && owner.isOnline()){
							evt.setCancelled(true);
							evt.getChunk().load();
	//						plugin.getLogger().info("Kept chunk loaded");
						}
					}
				}
				else if(entity instanceof AbstractHorse) plugin.updateData((AbstractHorse) entity);
			}//if claimed
		}//for each entity in chunk
	}
}