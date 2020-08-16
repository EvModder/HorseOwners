package net.evmodder.HorseOwners.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;
import net.evmodder.EvLib.extras.TextUtils;
import net.evmodder.HorseOwners.HorseManager;

public class TameListener implements Listener{
	final boolean CLAIM_ON_TAME;
	final String NOTIFY_ON_TAME;

	public TameListener(){
		HorseManager plugin = HorseManager.getPlugin();
		CLAIM_ON_TAME = plugin.getConfig().getBoolean("claim-on-tame", true);
		NOTIFY_ON_TAME = TextUtils.translateAlternateColorCodes('&', plugin.getConfig().getString("notify-on-tame", ""));
	}

	@EventHandler
	public void onTame(EntityTameEvent evt){
		if(evt.getEntity().getCustomName() != null && HorseManager.getPlugin().getAPI().isClaimableHorseType(evt.getEntity())){
			Player p = evt.getEntity().getServer().getPlayer(evt.getOwner().getUniqueId());
			if(p != null){
				if(CLAIM_ON_TAME) HorseManager.getPlugin().getAPI().addClaimedHorse(p.getUniqueId(), evt.getEntity());
				if(!NOTIFY_ON_TAME.isBlank()) p.sendMessage(NOTIFY_ON_TAME);
			}
		}
	}
}