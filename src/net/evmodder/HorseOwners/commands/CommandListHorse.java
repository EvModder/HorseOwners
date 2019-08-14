package net.evmodder.HorseOwners.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.evmodder.EvLib.extras.TabText;

public class CommandListHorse extends HorseCommand{
	@Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length == 1){
			args[0] = args[0].toLowerCase();
			final List<String> tabCompletes = new ArrayList<String>();
			tabCompletes.add("all");
			for(Player p : plugin.getServer().getOnlinePlayers()){
				if(p.getName().startsWith(args[0])){
					tabCompletes.add(p.getName());
					if(tabCompletes.size() == 20) break;
				}
			}
			return tabCompletes;
		}
		return null;
	}

	@Override public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	/hm list
		boolean listAllPerm = sender.hasPermission("horseowners.list.all");
		String target = (sender instanceof Player == false || (listAllPerm && args.length == 1))
				? args[0].toLowerCase() : sender.getName();

		if(target.contains("all") || target.equals("@a")){
			Set<String> horseList = sender.hasPermission("horseowners.list.unclaimed") ?
					plugin.getAllHorses() : plugin.getAllClaimedHorses();
			sender.sendMessage("§a§m}{§e§m       §7 [§aAll Horses§7] §e§m       §a§m}{");
			StringBuilder builder = new StringBuilder();
			for(String horse : horseList){
				String owner = plugin.getHorseOwnerName(horse);
				if(owner == null) owner = "N/A";
				builder.append("§7Horse: §f").append(plugin.getHorseName(horse))
				.append("`§7Owner: §f").append(owner).append('\n');
			}
			builder.append("§7Total: §f").append(horseList.size());
			if(sender instanceof Player)
				sender.sendMessage(TabText.parse(builder.toString(), false, false, new int[]{157,0}));
			else
				sender.sendMessage(TabText.parse(builder.toString(), true, false, new int[]{25,0}));
		}
		else if(target.equals(sender.getName())){
			UUID uuid = ((Player)sender).getUniqueId();
			if(plugin.getHorseOwners().containsKey(uuid)){
				sender.sendMessage("§a}§e§m      §7 [§aYour Horses§7] §e§m      §a{");
				Collection<String> horses = plugin.getHorseOwners().get(uuid);
				for(String horseName : horses) sender.sendMessage("§7Horse: §f"+plugin.getHorseName(horseName));
				if(horses.size() > 9) sender.sendMessage("§7Total: §f" + horses.size());
			}
			else sender.sendMessage("§cYou do not own any horses!");
			if(listAllPerm) sender.sendMessage("§7Use §2/hm list @a§7 to view all horses");
		}
		else{
			@SuppressWarnings("deprecation")
			OfflinePlayer targetP = plugin.getServer().getOfflinePlayer(target);
			if(targetP != null && plugin.getHorseOwners().containsKey(targetP.getUniqueId())){
				sender.sendMessage("§a}§e§m      §7 [§a"+target+"'s Horses§7] §e§m      §a{");
				Collection<String> horses = plugin.getHorseOwners().get(targetP.getUniqueId());
				for(String horseName : horses) sender.sendMessage("§7Horse: §f"+plugin.getHorseName(horseName));
				if(horses.size() > 9) sender.sendMessage("§7Total: §f" + horses.size());
			}
			else sender.sendMessage("§c"+target+" does not own any horses!");
			if(listAllPerm) sender.sendMessage("§7Use §2/hm list @a§7 to view all horses");
		}
		COMMAND_SUCCESS = true;
		return true;
	}
}