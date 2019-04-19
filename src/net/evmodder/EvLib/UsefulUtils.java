package net.evmodder.EvLib;

import java.util.Vector;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

public class UsefulUtils{
	public static boolean notFar(Location from, Location to){
		int x1 = from.getBlockX(), y1 = from.getBlockY(), z1 = from.getBlockZ(),
			x2 = to.getBlockX(), y2 = to.getBlockY(), z2 = to.getBlockZ();

		return (Math.abs(x1 - x2) < 20 &&
				Math.abs(y1 - y2) < 15 &&
				Math.abs(z1 - z2) < 20 &&
				from.getWorld().getName().equals(to.getWorld().getName()));
	}

	public static Vector<String> installedEvPlugins(){
		Vector<String> evPlugins = new Vector<String>();
		for(Plugin pl : Bukkit.getServer().getPluginManager().getPlugins()){
			try{
				@SuppressWarnings("unused")
				String ver = pl.getClass().getField("EvLib_ver").get(null).toString();
				evPlugins.add(pl.getName());
				//TODO: potentially return list of different EvLib versions being used
			}
			catch(IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e){}
		}
		return evPlugins;
	}
}