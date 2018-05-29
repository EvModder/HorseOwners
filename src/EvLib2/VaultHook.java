package EvLib2;

import java.math.BigDecimal;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.plugin.RegisteredServiceProvider;

import com.earth2me.essentials.api.NoLoanPermittedException;
import com.earth2me.essentials.api.UserDoesNotExistException;

public class VaultHook {
	private static boolean vaultEnabled;
	public static Economy econ = null;
	public static Permission perms = null;
	public static Chat chat = null;

	public VaultHook(Plugin plugin){
		if(!setupEconomy(plugin)){
			if(plugin.getServer().getPluginManager().getPlugin("Essentials") == null){
				plugin.getLogger().warning("Unable to connect to Vault or EssentialsEco economies");
			}
			else{// Removed to reduce spam
//				plugin.getLogger().info("Vault not found, using EssentialsEco as economy base");
			}
		}
		else{
			vaultEnabled = true;
			setupPermissions(plugin);
			setupChat(plugin);
		}
	}

	private boolean setupEconomy(Plugin plugin) {
		if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	private boolean setupChat(Plugin plugin) {
		RegisteredServiceProvider<Chat> rsp = plugin.getServer().getServicesManager().getRegistration(Chat.class);
		chat = rsp.getProvider();
		return chat != null;
	}

	private boolean setupPermissions(Plugin plugin) {
		RegisteredServiceProvider<Permission> rsp = plugin.getServer().getServicesManager().getRegistration(Permission.class);
		perms = rsp.getProvider();
		return perms != null;
	}

	public static double getBalance(OfflinePlayer p) throws UserDoesNotExistException {
		if(VaultHook.vaultEnabled()) return VaultHook.econ.getBalance(p);
		else return net.ess3.api.Economy.getMoneyExact(p.getName()).doubleValue();
	}

	public static boolean hasAtLeast(OfflinePlayer p, double amount){
		if(VaultHook.vaultEnabled()){
			return VaultHook.econ.has(p, amount);
		}
		else{
			try{return net.ess3.api.Economy.hasEnough(p.getName(), new BigDecimal(amount));}
			catch(UserDoesNotExistException e){return false;}
		}
	}

	public static boolean giveMoney(OfflinePlayer p, double amount){
		if(VaultHook.vaultEnabled()){
			EconomyResponse r = VaultHook.econ.depositPlayer(p, amount);
			return r.transactionSuccess();
		}
		else{
			try{net.ess3.api.Economy.add(p.getName(), new BigDecimal(amount));}
			// returns false if it encounters an error
			catch(NoLoanPermittedException e){return false;}
			catch(UserDoesNotExistException e){return false;}
			return true;
		}
	}

	public static boolean chargeFee(OfflinePlayer p, double amount){
		if(VaultHook.vaultEnabled()){
			EconomyResponse r = VaultHook.econ.withdrawPlayer(p, amount);
			if(r.transactionSuccess() == false) return false;
		}
		else{
			// check money
			try{if(net.ess3.api.Economy.hasEnough(p.getName(), new BigDecimal(amount)) == false) return false;}
			catch(UserDoesNotExistException e){return false;}
			
			// take money
			try{net.ess3.api.Economy.substract(p.getName(), new BigDecimal(amount));}
			// returns false if it encounters an error
			catch(NoLoanPermittedException e){return false;}
			catch(UserDoesNotExistException e){return false;}
		}
		return true;
	}

	public static boolean vaultEnabled(){return vaultEnabled;}
}