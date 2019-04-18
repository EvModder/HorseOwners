package evmodder.HorseOwners.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandTopHorse extends HorseCommand{
	final List<String> rankLists = Arrays.asList("speed", "jump", "health");
	final int RESULTS_PER_PAGE = 10;
	
	@Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length == 1){
			final List<String> tabCompletes = new ArrayList<String>();
			args[0] = args[0].toLowerCase();
			if("speed".startsWith(args[0])) tabCompletes.add("speed");
			if("jump".startsWith(args[0])) tabCompletes.add("jump");
			if("health".startsWith(args[0])) tabCompletes.add("health");
			return tabCompletes;
		}
		if(args.length == 2 && rankLists.contains(args[0].toLowerCase())){
			int pages = plugin.getHorseOwners().size()/RESULTS_PER_PAGE;
			final List<String> tabCompletes = new ArrayList<String>();
			for(int i=Math.min(pages, 100); i>=0; --i) if((""+i).startsWith(args[1])) tabCompletes.add(""+i);
			if((""+pages).startsWith(args[1])) tabCompletes.add(""+pages);
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
		int page = 0;
		if(args.length == 2 && rankLists.contains(args[0].toLowerCase())){
			try{page = Integer.parseInt(args[1]);}
			catch(NumberFormatException ex){}
		}

		//Build info message
		StringBuilder builder = new StringBuilder();

		if(topSpeed){
			builder.append("\n§a§m}{§e§m        §7 [§aFastest Speed§7] §e§m        §a§m}{");
			int shown = 0;
			for(Entry<Double, Collection<String>> speedGroup : plugin.getTopSpeed().descendingMap().entrySet()){
				for(String horseName : speedGroup.getValue()){
					builder.append("\n§7Speed: §f").append(String.format("%.2f", speedGroup.getKey()))
						.append("    §7Horse: §f").append(plugin.getHorseName(horseName));
	
					String owner = plugin.getHorseOwnerName(horseName);
					if(owner != null) builder.append("    §7Owner: §f").append(owner);
					if(++shown == RESULTS_PER_PAGE) break;
				}
				if(shown == RESULTS_PER_PAGE) break;
			}
		}
		if(topJump){
			builder.append("\n§a§m}{§e§m        §7 [§aHighest Jump§7] §e§m        §a§m}{");
			int shown = 0;
			for(Entry<Double, Collection<String>> jumpGroup : plugin.getTopJump().descendingMap().entrySet()){
				for(String horseName : jumpGroup.getValue()){
					builder.append("\n§7Jump: §f").append(String.format("%.2f", jumpGroup.getKey()))
						.append("    §7Horse: §f").append(plugin.getHorseName(horseName));
	
					String owner = plugin.getHorseOwnerName(horseName);
					if(owner != null) builder.append("    §7Owner: §f").append(owner);
					if(++shown == RESULTS_PER_PAGE) break;
				}
				if(shown == RESULTS_PER_PAGE) break;
			}
		}
		if(topHealth){
			builder.append("\n§a§m}{§e§m        §7 [§aMost Health§7] §e§m        §a§m}{");
			int shown = 0;
			for(Entry<Integer, Collection<String>> healthGroup : plugin.getTopHealth().descendingMap().entrySet()){
				for(String horseName : healthGroup.getValue()){
					builder.append("\n§7Health: §f").append(healthGroup.getKey())
						.append("    §7Horse: §f").append(plugin.getHorseName(horseName));
	
					String owner = plugin.getHorseOwnerName(horseName);
					if(owner != null) builder.append("    §7Owner: §f").append(owner);
					if(++shown == RESULTS_PER_PAGE) break;
				}
				if(shown == RESULTS_PER_PAGE) break;
			}
		}

		sender.sendMessage(builder.toString());
		return true;
	}
}