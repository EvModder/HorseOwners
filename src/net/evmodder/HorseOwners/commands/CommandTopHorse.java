package net.evmodder.HorseOwners.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.evmodder.EvLib.extras.TabText;
import net.evmodder.EvLib.util.IndexTreeMultiMap;

public class CommandTopHorse extends HorseCommand{
	final List<String> rankLists = Arrays.asList("speed", "jump", "health");
	final int RESULTS_PER_PAGE = 9;
	final String headerOpen = "\n§a§m}{§e§m                         §7 [§a";
	final String headerClose = "§7] §e§m                         §a§m}{\n";

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
			int pages = (plugin.getDatabaseSize()+RESULTS_PER_PAGE-1)/RESULTS_PER_PAGE;
			final List<String> tabCompletes = new ArrayList<String>();
			for(int i=Math.min(pages, 100); i>0; --i) if((""+i).startsWith(args[1])) tabCompletes.add(""+i);
			if((""+pages).startsWith(args[1])) tabCompletes.add(""+pages);
			return tabCompletes;
		}
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" }) @Override
	public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	/hm top [#]

		boolean topSpeed = true, topJump = true, topHealth = true;
		String flags = StringUtils.join(args).toLowerCase();
		if(flags.isEmpty() == false){
			topSpeed = flags.contains("s");
			topJump = flags.contains("j");
			topHealth = flags.contains("h");
		}
		int page = 0;
		if(args.length == 2 && (topSpeed || topJump) != topHealth && !(topSpeed && topHealth)){
			try{page = Integer.parseInt(args[1])-1;}
			catch(NumberFormatException ex){}
		}

		if(topSpeed) showTop(sender, "Speed", "Fastest Speed", (IndexTreeMultiMap)plugin.getTopSpeed(), page);
		if(topJump) showTop(sender, "Jump", "Highest Jump", (IndexTreeMultiMap)plugin.getTopJump(), page);
		if(topHealth) showTop(sender, "Hearts", "Most Health", (IndexTreeMultiMap)plugin.getTopHealth(), page);
		int lastPage = (plugin.getDatabaseSize()-1)/RESULTS_PER_PAGE;
		if(page > lastPage){
			sender.sendMessage("§cError: page §6#"+(page+1)+"§c doesn't exist");
			if(lastPage > 0) sender.sendMessage("§7Valid page range: §61§7-§6"+(lastPage+1));
		}

		return true;
	}

	void showTop(CommandSender sender, String stat, String topPage, IndexTreeMultiMap<Object, String> values, int page){
		StringBuilder header = new StringBuilder(headerOpen);
		if(page < 1 || page*RESULTS_PER_PAGE >= values.valuesSize()){page = 0; header.append(topPage);}
		else header.append(stat).append(" page §6#").append(page+1);
		header.append(headerClose);

		final int rankBegin = values.valuesSize() - page*RESULTS_PER_PAGE;//exclusive
		final int rankEnd = Math.max(rankBegin - RESULTS_PER_PAGE, 0);//inclusive
		Object keyBeginIncl = values.getKeyAtValueIndex(rankBegin-1);
		Object keyEndIncl = values.getKeyAtValueIndex(rankEnd);
		int skipBegin = values.getCeilingIndex(keyBeginIncl) - rankBegin;
		int shown = 0;
		boolean floatingPoint = keyEndIncl instanceof Double || keyEndIncl instanceof Float;
		StringBuilder builder = new StringBuilder("");
		for(Entry<Object, Collection<String>> group :
			values.subMap(keyEndIncl, true, keyBeginIncl, true).descendingMap().entrySet()){
			for(String horseName : group.getValue()){
				if(skipBegin > 0){--skipBegin; continue;}

				builder.append("§7").append(stat).append(": §F").append(floatingPoint ?
						String.format("%.2f", group.getKey()) : group.getKey())
				.append("`§7Horse: §F").append(plugin.getHorseName(horseName));

				String owner = plugin.getHorseOwnerName(horseName);
				if(owner != null) builder.append("`§7Owner: §F").append(owner);
				if(++shown == RESULTS_PER_PAGE) break;
				builder.append('\n');
			}
			if(shown == RESULTS_PER_PAGE) break;
		}
		if(sender instanceof Player){
			header.append(TabText.parse(builder.toString(), false, false, new int[]{62+12, 36+75+13, 36+75}));
		}
		else{
			header.append(TabText.parse(builder.toString(), true, false, new int[]{12+3, 7+16+3, 7+16}));
		}
		sender.sendMessage(header.toString());
	}
}