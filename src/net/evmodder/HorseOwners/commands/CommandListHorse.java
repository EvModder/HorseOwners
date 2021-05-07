package net.evmodder.HorseOwners.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Set;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.evmodder.EvLib.extras.TabText;
import net.evmodder.EvLib.extras.TextUtils;

public class CommandListHorse extends HorseCommand{
	//TODO: enum OrderBy{ NAME, AGE, CLAIM_TS }; ?
	//TODO: hover over name for inspect?

	String getPlayerName(UUID uuid){
		Player p = plugin.getServer().getPlayer(uuid);
		if(p != null) return p.getName();
		OfflinePlayer offP = plugin.getServer().getOfflinePlayer(uuid);
		if(offP != null) return offP.getName();
		return null;
	}
	final String YOUR_HEADER = "§a}§e§m                    §7 [§aYour Horses§7] §e§m                    §a{";
	String makeHeader(String nameApostropheS, boolean mono){
		int namePxLen = TextUtils.strLen(nameApostropheS, mono);
		int namePxLenDiff = namePxLen - TextUtils.strLen("Your", mono);
		int spaceMaxPxLen = mono ? 30 : 100;
		int spacePxBefore = spaceMaxPxLen - namePxLenDiff/2;
		int spacePxAfter = (spaceMaxPxLen - namePxLenDiff/2) - namePxLenDiff%2;
		String spaceBefore = TabText.getPxSpaces(spacePxBefore, mono, "§e§m");
		String spaceAfter = TabText.getPxSpaces(spacePxAfter, mono, "§e§m");
		if(mono){
			spaceBefore = spaceBefore.replace(' ', '-');
			spaceAfter = spaceAfter.replace(' ', '-');
		}
		return new StringBuilder("§a}§e§m").append(spaceBefore)
				.append("§7 [§a").append(nameApostropheS).append(" Horses§7] §e§m").append(spaceAfter).append("§a{").toString();
	}
	String formatHorseList(Collection<String> horses, boolean monospaced){
		ArrayList<String> rawNames = new ArrayList<String>();
		int maxHorseNameLen = 0;
		for(String cleanName : horses){
			String rawName = plugin.getAPI().getHorseName(cleanName);
			maxHorseNameLen = Math.max(maxHorseNameLen, TextUtils.strLen(rawName, monospaced));
			rawNames.add(rawName);
		}
		Collections.sort(rawNames, String.CASE_INSENSITIVE_ORDER);

		int numCols = monospaced
				? (TextUtils.MAX_MONO_WIDTH + 2/*no space on last column*/) / (maxHorseNameLen + 3) // 3=",  "
				: (TextUtils.MAX_PIXEL_WIDTH + 8/*no space on last column*/) / (maxHorseNameLen + 10); // 10=",  "
		if(numCols == 0) numCols = 1;

		StringBuilder builder = new StringBuilder("");
		int nameSep = (rawNames.size() + numCols - 1) / numCols;
		for(int i = 0; i < nameSep; ++i){
			builder.append("§f").append(rawNames.get(i)).append("§a,");
			for(int col = 1; col < numCols && i + col * nameSep < rawNames.size(); ++col){
				builder.append("  `§f").append(rawNames.get(i + col * nameSep)).append("§a,");
			}
			builder.append('\n');
		}
		int[] tabs = new int[numCols];
		Arrays.fill(tabs, (monospaced ? TextUtils.MAX_MONO_WIDTH : TextUtils.MAX_PIXEL_WIDTH)/numCols);
		tabs[numCols - 1] = 0;
		StringBuilder result = new StringBuilder(TabText.parse(builder.toString(), monospaced, false, tabs));
		if(horses.size() > 9) result.append("§7Total: §f").append(horses.size()).append('\n');
		return result.toString();
	}
	void listAllHorses(CommandSender sender){
		boolean monospaced = !(sender instanceof Player);
		Collection<String> horseList = sender.hasPermission("horseowners.list.unclaimed")
				? plugin.getAPI().getAllHorses() : plugin.getAPI().getAllClaimedHorses();
		if(horseList.size() > 50/* && sender instanceof Player*/){
			plugin.getLogger().info("Showing compacted '/hm all' for "+sender.getName());
			StringBuilder builder = new StringBuilder();
			HashSet<String> unclaimed = new HashSet<>(plugin.getAPI().getAllHorses());
			int numOwners = 0;
			for(Entry<UUID, Set<String>> horseOwner : plugin.getAPI().getHorseOwnersMap().entrySet()){
				String ownerName = getPlayerName(horseOwner.getKey());
				if(ownerName == null || horseOwner.getValue() == null || horseOwner.getValue().isEmpty()) continue;
				unclaimed.removeAll(horseOwner.getValue());
				builder.append(makeHeader(ownerName+"'s", monospaced)).append('\n');
				builder.append(formatHorseList(horseOwner.getValue(), monospaced)).append('\n');
				++numOwners;
			}
			if(sender.hasPermission("horseowners.list.unclaimed") && !unclaimed.isEmpty()){
				builder.append(makeHeader("Wild", monospaced)).append('\n');
				builder.append(formatHorseList(unclaimed, monospaced)).append('\n');
				builder.append("§7Combined total: §f").append(plugin.getAPI().getAllHorses().size());
			}
			else if(numOwners > 1) builder.append("§7Combined total: §f").append(plugin.getAPI().getAllHorses().size()-unclaimed.size());
			sender.sendMessage(builder.toString());
			return;
		}
		ArrayList<String> sortedHorseList = new ArrayList<String>();
		sortedHorseList.addAll(horseList);
		Collections.sort(sortedHorseList, String.CASE_INSENSITIVE_ORDER);

		StringBuilder builder = new StringBuilder(makeHeader("All", monospaced)).append('\n');
		int maxHorseNameLen = 0, maxOwnerNameLen = 0;
		for(String cleanName : sortedHorseList){
			String owner = plugin.getAPI().getHorseOwnerName(cleanName);
			String rawName = plugin.getAPI().getHorseName(cleanName);
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

	@Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length == 1){
			args[0] = args[0].toLowerCase();
			return Stream.concat(Stream.of("all"), plugin.getServer().getOnlinePlayers().stream().map(p -> p.getName())
					.filter(name -> name.toLowerCase().startsWith(args[0]))).limit(20).collect(Collectors.toList());
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
			Set<String> targetHorses = targetP == null ? null : plugin.getAPI().getHorses(targetP.getUniqueId());
			if(targetHorses != null && !targetHorses.isEmpty()){
				StringBuilder builder = new StringBuilder("\n");
				builder.append(makeHeader(targetP.getName()+"'s", consoleSender)).append('\n');
				builder.append(formatHorseList(targetHorses, consoleSender));
				sender.sendMessage(builder.toString());
				//for(String horseName : horses) sender.sendMessage("§7Horse: §f"+plugin.getHorseName(horseName));
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
			else sender.sendMessage("§cYou do not own any horses!");*/
			Collection<String> horses = plugin.getAPI().getHorses(((Player)sender).getUniqueId());
			if(horses == null || horses.isEmpty()){
				sender.sendMessage("§cYou do not own any horses!");
			}
			else{
				// }20spaces 1space [4ch 1space 6ch] 1space 20spaces{
				sender.sendMessage(YOUR_HEADER);
				sender.sendMessage(formatHorseList(horses, false));
			}
			if(sender.hasPermission("horseowners.list.all")) sender.sendMessage("§7Use §2/hm list @a§7 to view all horses");
		}
		COMMAND_SUCCESS = true;
		return true;
	}
}