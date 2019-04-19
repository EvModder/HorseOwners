package net.evmodder.HorseOwners.commands;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandLockHorse extends HorseCommand{
	@Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length > 0){
			String arg = String.join(" ", args).toLowerCase();
			final List<String> tabCompletes = new ArrayList<String>();
			byte shown = 0;
			for(String horseName : sender instanceof Player ?
					plugin.getHorseOwners().get(((Player)sender).getUniqueId())
					: plugin.getAllHorses()){
				if(horseName.startsWith(arg)){
					tabCompletes.add(horseName);
					if(++shown == 20) break;
				}
			}
			return tabCompletes;
		}
		return null;
	}

	@Override
	public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	/hm lock [-name]

		Player p = (sender instanceof Player) ? (Player)sender : null;
		String horseName;

		if(p != null && p.isInsideVehicle() && plugin.isClaimableHorseType(p.getVehicle())){
			if(p.getVehicle().getCustomName() == null){
				p.sendMessage("§cThis horse is ownerless!\nTry claiming it first with /namehorse");
				return false;
			}
			else{
				horseName = p.getVehicle().getCustomName();
			}
		}
		else if(args.length == 0){
			sender.sendMessage("§cPlease specify the horse you want to lock!§7\n"+command.getUsage());
			return false;
		}
		else horseName = StringUtils.join(args, ' ');

		//by this point, a name has been determined
		if(plugin.isLockedHorse(horseName)){
			sender.sendMessage("§7This horse's has already been locked");
			return false;
		}
		if(plugin.isClaimedHorse(horseName)){
			if(p != null && plugin.canAccess(p, horseName) == false){
				sender.sendMessage("§cYou cannot lock horses which you do not own");
				return false;
			}
		}
/*		//Check to confirm the horse's existence, even though we don't need the entity
		else if(plugin.findClaimedHorse(horseName, null) == null){
			sender.sendMessage("§cUnable to find your horse! Perhaps the chunk it is in was unloaded?");
			return true;
		}*/

		//Yay got to here! Now make it forever!
		sender.sendMessage("§7Your horse's name been locked! Nobody can change it now.");
		plugin.lockHorse(horseName);
		return true;
	}
}