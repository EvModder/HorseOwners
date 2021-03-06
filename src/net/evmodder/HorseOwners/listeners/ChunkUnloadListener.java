package net.evmodder.HorseOwners.listeners;

import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import net.evmodder.HorseOwners.HorseUtils;
import net.evmodder.HorseOwners.HorseManager;

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
			if(entity.getCustomName() != null && plugin.getAPI().isClaimableHorseType(entity)
				&& plugin.getAPI().isClaimedHorse(HorseUtils.cleanName(entity.getCustomName())))
			{
				if(keepLoaded){
					UUID uuid = plugin.getAPI().getHorseOwner(HorseUtils.cleanName(entity.getCustomName()));
					if(uuid != null){
						OfflinePlayer owner = plugin.getServer().getOfflinePlayer(uuid);
						if(owner != null && owner.isOnline()){
							evt.getChunk().load();
	//						plugin.getLogger().info("Kept chunk loaded");
						}
					}
				}
				plugin.getAPI().updateDatabase((AbstractHorse) entity);
			}//if claimed
		}//for each entity in chunk
	}
}