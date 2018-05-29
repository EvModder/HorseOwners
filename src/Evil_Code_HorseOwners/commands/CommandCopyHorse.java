package Evil_Code_HorseOwners.commands;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import Evil_Code_HorseOwners.HorseLibrary;

public class CommandCopyHorse extends HorseCommand {
	boolean safeTeleports;

	public CommandCopyHorse(){
		safeTeleports = plugin.getConfig().getBoolean("teleport-only-if-safe");
	}

	@Override
	public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	usage: /hm copy [horse]
		if(sender instanceof Player == false){
			sender.sendMessage(ChatColor.RED+"This command can only be run by in-game players");
			return true;
		}

		Player p = (Player)sender;
		String target = StringUtils.join(args, ' ');
		AbstractHorse horse;

		if(safeTeleports && !HorseLibrary.safeForHorses(p.getLocation())){
			p.sendMessage(ChatColor.RED+
				"Unable to spawn horse - Please move to a more open area to prevent risk of horse suffocation");
			return true;
		}

		if(!plugin.isPrivateHorse(target)){
			sender.sendMessage(ChatColor.RED+"Unknown horse '"+ChatColor.GRAY+target+ChatColor.RED+"'");
//			sender.sendMessage(ChatColor.RED+"Unclaimed horses cannot be copied via command, you must first use /claimhorse");
			return false;
		}
		else if(!plugin.canAccess(p, target)){
			p.sendMessage(ChatColor.RED+"You cannot copy horses which you do not own");
			return false;
		}
		horse = plugin.findClaimedHorse(target, null);
//		horse = HorseLibrary.findAnyHorse(target);
		if(horse == null){
			p.sendMessage(ChatColor.RED+"Unable to find your horse! Perhaps its location was unloaded?");
			return true;
		}

		AbstractHorse newHorse = (AbstractHorse) p.getWorld().spawnEntity(p.getLocation(), horse.getType());
		newHorse.setCustomName("Copy of "+horse.getCustomName());
		if(horse instanceof Horse){
			((Horse)newHorse).setColor(((Horse)horse).getColor());
			((Horse)newHorse).setStyle(((Horse)horse).getStyle());
		}
		newHorse.setJumpStrength(horse.getJumpStrength());
		HorseLibrary.speedCalc.setHorseSpeed(newHorse, HorseLibrary.speedCalc.getHorseSpeed(horse));
		newHorse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(
				horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
		newHorse.setTamed(horse.isTamed());

		// Other Entity Attributes
		if(horse.isAdult()) newHorse.setAdult();
//		newHorse.setOwner(horse.getOwner());
		newHorse.setSilent(horse.isSilent());

		sender.sendMessage(ChatColor.GREEN+"Successfully spawned your horse!");
		return true;
	}
}