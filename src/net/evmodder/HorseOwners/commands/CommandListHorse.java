package net.evmodder.HorseOwners.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.evmodder.EvLib.extras.TabText;
import net.evmodder.EvLib.extras.TextUtils;

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
		boolean consoleSender = !(sender instanceof Player);
		String target = args.length == 0 ? (consoleSender ? "all" : sender.getName()) : args[0].toLowerCase();

		if((target.contains("all") || target.equals("@a")) && sender.hasPermission("horseowners.list.all")){
			listAllHorses(sender);
		}//maxNameLen = Math.max(maxNameLen, TextUtils.strLen(rawHorseName, !playerSender));
		else if(sender.hasPermission("horseowners.list.others") && !target.equalsIgnoreCase(sender.getName())){
			@SuppressWarnings("deprecation")
			OfflinePlayer targetP = plugin.getServer().getOfflinePlayer(target);
			if(targetP != null && plugin.getHorseOwners().containsKey(targetP.getUniqueId())){
				target = targetP.getName();
				int nameLen = TextUtils.strLen(target, consoleSender); // 'Your' = 16px, 4ch
				int sideSpaces = 20 - (consoleSender ? (nameLen-4) : ((nameLen-16)/4))/2;
				String sideSpace = new String(new char[sideSpaces]).replace('\0', ' ');
				StringBuilder builder = new StringBuilder("\n§a}§e§m")
						.append(sideSpace).append("§7 [§a").append(target).append("'s Horses§7] §e§m").append(sideSpace)
						.append("§a{\n");
				Collection<String> horses = plugin.getHorseOwners().get(targetP.getUniqueId());
				//for(String horseName : horses) sender.sendMessage("§7Horse: §f"+plugin.getHorseName(horseName));
				builder.append(formatHorseList(horses, consoleSender));
				sender.sendMessage(builder.toString());
			}
			else sender.sendMessage("§c"+targetP.getName()+" does not own any horses!");
			if(sender.hasPermission("horseowners.list.all")) sender.sendMessage("§7Use §2/hm list @a§7 to view all horses");
		}
		else{
			/*UUID uuid = ((Player)sender).getUniqueId();
			if(plugin.getHorseOwners().containsKey(uuid)){
				sender.sendMessage("§a}§e§m            §7 [§aYour Horses§7] §e§m            §a{");
				Collection<String> horses = plugin.getHorseOwners().get(uuid);
				for(String horseName : horses) sender.sendMessage("§7Horse: §f"+plugin.getHorseName(horseName));
				if(horses.size() > 9) sender.sendMessage("§7Total: §f" + horses.size());
			}
			else sender.sendMessage("§cYou do not own any horses!");
			if(listAllPerm) sender.sendMessage("§7Use §2/hm list @a§7 to view all horses");*/
			Collection<String> horses = plugin.getHorseOwners().get(((Player)sender).getUniqueId());
			if(horses == null){
				sender.sendMessage("§cYou do not own any horses!");
			}
			else{
				//String sideSpace = new String(new char[20]).replace('\0', ' ');
				//sender.sendMessage("§a}§e§m"+sideSpace+"§7 [§aYour Horses§7] §e§m"+sideSpace+"§a{");
				sender.sendMessage("§a}§e§m                    §7 [§aYour Horses§7] §e§m                    §a{");
				sender.sendMessage(formatHorseList(horses, false));
				if(sender.hasPermission("horseowners.list.all")) sender.sendMessage("§7Use §2/hm list @a§7 to view all horses");
			}
		}
		COMMAND_SUCCESS = true;
		return true;
	}

	String formatHorseList(Collection<String> horses, boolean monospaced){
		ArrayList<String> rawNames = new ArrayList<String>();
		int maxHorseNameLen = 0;
		for(String horseName : horses){
			String rawName = plugin.getHorseName(horseName);
			maxHorseNameLen = Math.max(maxHorseNameLen, TextUtils.strLen(rawName, monospaced));
			rawNames.add(rawName);
		}
		Collections.sort(rawNames, String.CASE_INSENSITIVE_ORDER);

		int numCols = monospaced
				? (TextUtils.MAX_MONO_WIDTH+2) / (maxHorseNameLen+4) //4=',  '+1buffer
				: (TextUtils.MAX_PIXEL_WIDTH+8) / (maxHorseNameLen+11); //10=',  '+1buffer
		if(numCols == 0) numCols = 1;

		StringBuilder builder = new StringBuilder("");
		int nameSep = (rawNames.size()+numCols-1)/numCols;
		for(int i=0; i<nameSep; ++i){
			builder.append("§f").append(rawNames.get(i)).append("§a,");
			for(int col=1; col<numCols && i+col*nameSep < rawNames.size(); ++col){
				builder.append("  `§f").append(rawNames.get(i+col*nameSep)).append("§a,");
			}
			builder.append('\n');
		}
		int[] tabs = new int[numCols]; Arrays.fill(tabs, maxHorseNameLen+11); tabs[numCols-1] = 0;
		StringBuilder result = new StringBuilder(TabText.parse(builder.toString(), monospaced, false, tabs));
		if(horses.size() > 9) result.append("§7Total: §f").append(horses.size());
		return result.toString();
	}

	void listAllHorses(CommandSender sender){
		boolean monospaced = !(sender instanceof Player);
		Set<String> horseList = sender.hasPermission("horseowners.list.unclaimed") ?
				plugin.getAllHorses() : plugin.getAllClaimedHorses();
		sender.sendMessage("§a§m}{§e§m            §7 [§aAll Horses§7] §e§m            §a§m}{");
		StringBuilder builder = new StringBuilder();
		int maxHorseNameLen = 0, maxOwnerNameLen = 0;
		for(String horse : horseList){
			String owner = plugin.getHorseOwnerName(horse);
			String rawName = plugin.getHorseName(horse);
			maxHorseNameLen = Math.max(maxHorseNameLen, TextUtils.strLen(rawName, monospaced));
			if(owner == null) owner = "N/A";
			else maxOwnerNameLen = Math.max(maxOwnerNameLen, TextUtils.strLen(owner, monospaced));
			builder.append("§7Horse: §f").append(rawName).append("`§7Owner: §f").append(owner).append('\n');
		}
		//int totalWidth = TextUtils.strLen("Horse: ", mono) + maxHorseNameLen + TextUtils.strLen("   ", mono)
		//				+ TextUtils.strLen("Owner: ", mono) + maxOwnerNameLen + TextUtils.strLen("   ", mono);
		//int totalWidth = 36+HName+12 + 36+OName+12; // = 7+HName+3 + 7+OName+3;
		int totalWitdh = maxHorseNameLen + maxOwnerNameLen + (monospaced ? 20 : 96);
		if(monospaced)
			sender.sendMessage(TabText.parse(builder.toString(), true, false, 
				totalWitdh*2 <= TextUtils.MAX_MONO_WIDTH ?
						new int[]{7+maxHorseNameLen+3, 7+maxOwnerNameLen+3, 7+maxHorseNameLen+3, 0} :
						new int[]{7+maxHorseNameLen+3, 0}));
		else
			sender.sendMessage(TabText.parse(builder.toString(), false, false,
					totalWitdh*2 <= TextUtils.MAX_PIXEL_WIDTH ?
						new int[]{36+maxHorseNameLen+12, 36+maxOwnerNameLen+12, 36+maxHorseNameLen+12, 0} :
						new int[]{36+maxHorseNameLen+12, 0}));
		sender.sendMessage("§7Total: §f"+horseList.size());
	}
}