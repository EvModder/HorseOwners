package net.evmodder.HorseOwners.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.evmodder.EvLib.extras.TabText;
import net.evmodder.EvLib.extras.TextUtils;
import net.evmodder.EvLib.util.IndexTreeMultiMap;

public class CommandRankHorse extends HorseCommand{
	final List<String> rankLists = Arrays.asList("speed", "jump", "health");
	final int RESULTS_PER_PAGE = 9;
	final String headerOpen = "\n§a§m}{§e§m                         §7 [§a";
	final String headerClose = "§7] §e§m                         §a§m}{\n";

	<Num extends Object> void showTop(CommandSender sender, String stat, String topPage, IndexTreeMultiMap<Num, String> values, int page){
		boolean playerSender = sender instanceof Player;
		StringBuilder header = new StringBuilder(headerOpen);
		if(page < 1 || page*RESULTS_PER_PAGE >= values.valuesSize()){page = 0; header.append(topPage);}
		else header.append(stat).append(" page §6#").append(page+1);
		header.append(headerClose);

		final int rankBegin = values.valuesSize() - page*RESULTS_PER_PAGE;//exclusive
		final int rankEnd = Math.max(rankBegin - RESULTS_PER_PAGE, 0);//inclusive
		Num keyBeginIncl = values.getKeyAtValueIndex(rankBegin-1);
		Num keyEndIncl = values.getKeyAtValueIndex(rankEnd);
		int skipBegin = values.getCeilingIndex(keyBeginIncl) - rankBegin;
		int shown = 0;
		boolean floatingPoint = keyEndIncl instanceof Double || keyEndIncl instanceof Float;
		StringBuilder builder = new StringBuilder("");
		int maxNameLen = 0;
		for(Entry<Num, Collection<String>> group :
			values.subMap(keyEndIncl, true, keyBeginIncl, true).descendingMap().entrySet()){
			for(String cleanHorseName : group.getValue()){
				if(skipBegin > 0){--skipBegin; continue;}
				String rawHorseName = plugin.getAPI().getHorseName(cleanHorseName);
				maxNameLen = Math.max(maxNameLen, TextUtils.strLen(rawHorseName, !playerSender));

				builder.append("§7").append(stat).append(": §f").append(floatingPoint ?
						String.format("%.2f", group.getKey()) : group.getKey())
				.append("`§7Horse: §f").append(rawHorseName);

				String owner = plugin.getAPI().getHorseOwnerName(cleanHorseName);
				if(owner != null) builder.append("`§7Owner: §f").append(owner);
				if(++shown == RESULTS_PER_PAGE) break;
				builder.append('\n');
			}
			if(shown == RESULTS_PER_PAGE) break;
		}
		if(playerSender){
			header.append(TabText.parse(builder.toString(), /*mono=*/false, /*flexFill=*/false, /*tabs=*/new int[]{
					62+12, // 'Speed: XX.XX'=6+6+6+6+6+2+4+6+6+2+6+6=62px + 12px(buffer) = 74
					//	36+75+13, // 'Horse: '=6+6+6+6+6+2+4=36px + '$name'=16*[2,5,7]=[32,80,112]~=75 + 13px(buffer) = 124
					36+maxNameLen+12, // 'Horse: '=6+6+6+6+6+2+4=36px + maxNameLen + 12px(buffer)
					36+75})); // 'Owner: '=6+6+6+6+6+2+4=36px + '$name'=16*[2,6]=[32,96] = 111
					// Total = 74 + 124 + 111 = 309(of 320)
		}
		else{
			header.append(TabText.parse(builder.toString(), /*mono=*/true, /*flexFill=*/false, /*tabs=*/new int[]{
					12+3, // 'Speed: XX.XX'=12ch + 3ch(buffer)
					7+16+3, // 'Horse: '=7ch + '$name'=16ch(config) + 3ch(buffer)
					7+16})); // 'Owner: '=7ch + '$name'=16ch(mojang-max) + 3ch(buffer)
		}
		sender.sendMessage(header.toString());
	}

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
			int pages = (plugin.getAPI().getAllHorses().size()+RESULTS_PER_PAGE-1)/RESULTS_PER_PAGE;
			final List<String> tabCompletes = new ArrayList<String>();
			for(int i=Math.min(pages, 100); i>0; --i) if((""+i).startsWith(args[1])) tabCompletes.add(""+i);
			if((""+pages).startsWith(args[1])) tabCompletes.add(""+pages);
			return tabCompletes;
		}
		return null;
	}

	@Override
	public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	/hm top [#]

		boolean topSpeed = true, topJump = true, topHealth = true;
		String flags = String.join("", args).toLowerCase();
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

		if(topSpeed) showTop(sender, "Speed", "Fastest Speed", plugin.getAPI().getTopSpeed(), page);
		if(topJump) showTop(sender, "Jump", "Highest Jump", plugin.getAPI().getTopJump(), page);
		if(topHealth) showTop(sender, "Hearts", "Most Health", plugin.getAPI().getTopHealth(), page);
		int lastPage = (plugin.getAPI().getAllHorses().size()-1)/RESULTS_PER_PAGE;
		if(page > lastPage){
			sender.sendMessage("§cError: page §6#"+(page+1)+"§c doesn't exist");
			if(lastPage > 0) sender.sendMessage("§7Valid page range: §61§7-§6"+(lastPage+1));
		}
		COMMAND_SUCCESS = true;
		return true;
	}
}