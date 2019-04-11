package Evil_Code_HorseOwners.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandTopHorse extends HorseCommand{
	@Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length ==1){
			final List<String> tabCompletes = new ArrayList<String>();
			args[0] = args[0].toLowerCase();
			if("speed".startsWith(args[0])) tabCompletes.add("speed");
			if("jump".startsWith(args[0])) tabCompletes.add("jump");
			if("health".startsWith(args[0])) tabCompletes.add("health");
			return tabCompletes;
		}
		return null;
	}

	@Override
	public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	/hm top

		boolean topSpeed = true, topJump = true, topHealth = true;
		String flags = StringUtils.join(args).toLowerCase();
		if(flags.isEmpty() == false){
			topSpeed = flags.contains("s");
			topJump = flags.contains("j");
			topHealth = flags.contains("h");
		}

		//Build info message
		StringBuilder builder = new StringBuilder();

		if(topSpeed){
			builder.append("\n§a§m}{§e§m        §7 [§aFastest Speed§7] §e§m        §a§m}{");
			for(String horseName : plugin.getTopSpeed()){
				builder.append("\n§7Speed: §f").append(String.format("%.2f", plugin.getHorseSpeed(horseName)))
					.append("    §7Horse: §f").append(plugin.getHorseName(horseName));

				UUID uuid = plugin.getOwner(horseName);
				OfflinePlayer owner = (uuid == null ? null : plugin.getServer().getOfflinePlayer(uuid));
				if(owner != null) builder.append("    §7Owner: §f").append(owner.getName());
			}
		}
		if(topJump){
			builder.append("\n§a§m}{§e§m        §7 [§aHighest Jump§7] §e§m        §a§m}{");
			for(String horseName : plugin.getTopJump()){
				builder.append("\n§7Jump: §f").append(String.format("%.2f", plugin.getHorseJump(horseName)))
					.append("    §7Horse: §f").append(plugin.getHorseName(horseName));

				UUID uuid = plugin.getOwner(horseName);
				OfflinePlayer owner = (uuid == null ? null : plugin.getServer().getOfflinePlayer(uuid));
				if(owner != null) builder.append("    §7Owner: §f").append(owner.getName());
			}
		}
		if(topHealth){
			builder.append("\n§a§m}{§e§m        §7 [§aMost Health§7] §e§m        §a§m}{");
			for(String horseName : plugin.getTopHealth()){
				builder.append("\n§7Health: §f").append(plugin.getHorseHealth(horseName))
					.append("    §7Horse: §f").append(plugin.getHorseName(horseName));

				UUID uuid = plugin.getOwner(horseName);
				OfflinePlayer owner = (uuid == null ? null : plugin.getServer().getOfflinePlayer(uuid));
				if(owner != null) builder.append("    §7Owner: §f").append(owner.getName());
			}
		}

		sender.sendMessage(builder.toString());
		return true;
	}
}