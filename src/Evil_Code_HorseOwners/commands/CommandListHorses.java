package Evil_Code_HorseOwners.commands;

import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandListHorses extends HorseCommand{

	@Override
	public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	/hm list
		boolean listAllPerm = sender.hasPermission("evp.horseowners.list.all");

		if(sender instanceof Player == false || (listAllPerm
				&& args.length == 1 && (args[0].toLowerCase().contains("all") || args[0].equals("@a"))))
		{
			int numHorses = 0;
			sender.sendMessage("§a§m}{§e§m------§7 [§aAll Horses§7] §e§m------§a§m}{");
			for(UUID uuid : plugin.getHorseOwners().keySet()){
				String ownerName = "None";
				OfflinePlayer owner = plugin.getServer().getOfflinePlayer(uuid);
				if(owner != null) ownerName = owner.getName();
				
				for(String horseName : plugin.getHorseOwners().get(uuid)){
					sender.sendMessage("§7Horse: §f"+horseName+"    §7Owner: §f"+ownerName);
					++numHorses;
				}
			}
			sender.sendMessage("§7Total: §f" + numHorses);
		}
		else if(plugin.getHorseOwners().containsKey(((Player)sender).getUniqueId())){
			sender.sendMessage("§a}§e§m-----§7 [§aYour Horses§7] §e§m-----§a{");
			for(String horseName : plugin.getHorseOwners().get(((Player)sender).getUniqueId())){
				sender.sendMessage("§7Horse: §f"+horseName);//+"  §a§  §7Owner: §f"+sender.getName());
			}
			if(listAllPerm) sender.sendMessage("§7Use §2/hm list @a§7 to view all horses");
		}
		else{
			sender.sendMessage("§cYou do not own any horses!");
			if(listAllPerm) sender.sendMessage("§7Use §2/hm list @a§7 to view all horses");
		}
		return true;
	}
}